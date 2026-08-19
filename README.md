# Keystrokes

A client-side Fabric mod for Minecraft Java Edition **1.21.11** that shows a
premium, animated keystrokes HUD (W / A S D / LMB / RMB / Space) with
per-key press animations, CPS counters, drag-and-drop positioning, presets,
and a full in-game settings screen with a real live preview. Everything
persists to `.minecraft/config/keystrokes.json`.

## Features

- **Real input tracking.** Reads W/A/S/D/Space/LMB/RMB directly from
  Minecraft's own `KeyBinding` state (`forwardKey`, `leftKey`, `backKey`,
  `rightKey`, `jumpKey`, `attackKey`, `useKey`). This always matches the
  player's real, current keybinds (including live rebinding), never consumes
  or blocks input, and is focus-gated so alt-tabbing can't leave a key
  looking stuck.
- **Input/CPS state is decoupled from rendering, and CPS is event-driven, not
  polled.** `KeystrokesHud.tick()` polls held-state once per game tick to drive
  the press animation, via `ClientTickEvents.END_CLIENT_TICK` - independent of
  whether the HUD element is actually drawn that frame. CPS is fed
  separately and does **not** go through tick polling at all: a small, single
  `@Inject`-only mixin (`KeyBindingMixin`, the mod's only mixin) observes
  `KeyBinding.setPressed(boolean)` - which Minecraft calls directly from its
  own GLFW input handling on every real press/release - and reports the edge
  to `MouseClickEvents`, which resolves it against the current attack/use
  bindings and hands LMB/RMB click events straight to `CpsTracker`. This
  means a real click that starts and finishes entirely between two ticks
  (i.e. faster than ~20 Hz) is still counted; polling `isPressed()` at tick
  rate cannot see that. The mixin is a pure observer - it never cancels,
  modifies, or consumes the callback, so vanilla's own attack/use handling is
  completely unaffected. `KeystrokesHud.render()` only ever reads state (held
  state for animation progress, click counts for CPS) and never mutates
  either.
- **One shared layout engine.** `KeyLayout` computes every key's exact
  position and size from the config, and is used by *both* the real HUD and
  the settings screen's preview - there is only one layout implementation,
  so the preview can never drift from what you actually see in-game.
  Disabling individual keys (just W, just one mouse button, etc.) reflows
  the remaining keys and re-centers the whole cluster instead of leaving a
  gap where the disabled key used to be.
- **Anti-aliased, scale-correct rendering.** Rounded corners are rasterized
  row-by-row with an analytic coverage-based edge (not a hard on/off step),
  so corners look clean instead of jagged at any HUD scale. Corner radius,
  border thickness and text size all scale consistently with the global HUD
  scale (50%-200%), and radius is consistent across WASD, mouse buttons and
  the space bar regardless of their differing widths.
- **A real animation profile system.** `AnimationProfile` defines named
  motion characters - **Instant**, **Smooth** (default), **Snappy** (a subtle
  overshoot "punch"), **Soft**, and **Custom** (uses the raw duration slider)
  - each with its own duration and easing for press vs. release. Animation is
  driven by wall-clock time (not frame count, so it looks the same at 30 FPS
  and 240 FPS), is fully interruptible with no popping on rapid taps, and
  scales its effective duration by how far it actually has to travel so a
  reversal that starts half-way doesn't feel sluggish or stutter.
- **Rolling one-second CPS counters** per mouse button, timestamped with
  `System.nanoTime()` (monotonic, immune to system-clock jumps - unlike
  `currentTimeMillis()`) and driven by real click events rather than polling
  (see above). Reset on window focus loss and when leaving the world, with no
  phantom or duplicate clicks - held buttons are never miscounted as repeated
  clicks, since only the press-down edge registers a click. The `CPS: n`
  sub-label is laid out as part of a single text block sized from real
  (scaled) font metrics, so it never collides with or crowds the LMB/RMB
  label at any HUD scale.
- **A real live preview**, not a fake outline box: the settings screen renders
  the actual HUD renderer with a small looping demo press pattern so you can
  see the current style and motion at a glance, and still drag it directly to
  reposition the HUD (clamped so it can never become fully inaccessible
  off-screen).
- **Debounced config saves.** Slider drags update the in-memory config
  immediately (so the preview updates live) but only hit disk at most twice a
  second while dragging, instead of on every pixel of movement; toggles,
  presets and drag-release still save right away.
- **Seven built-in presets** - Clean, Minimal, Classic, PvP, Glass, Dark,
  Accent - each changing shape, palette *and* motion character together so
  they read as genuinely different styles, not just recolors. Every preset
  explicitly sets all three motion-layer toggles (scale/move/color), so
  switching presets can never leave a toggle behind from whatever preset was
  active before it.
- **Six settings tabs**: Layout, Style, Animation, Keys, CPS, Presets. Widget
  and tab widths shrink to fit the actual window width instead of overflowing
  off-screen at small resolutions or high GUI scale.
- **Optional Mod Menu integration** - not a hard dependency; the mod works
  fully without Mod Menu installed.
- **One small, isolated mixin, and nothing else.** `KeyBindingMixin` exists
  solely for accurate CPS detection (see above) - a single non-cancellable
  `@Inject` at the head of one vanilla method. Every other piece of input
  reading, rendering and layout goes through public Fabric API and vanilla
  client-side hooks.

## Project layout

```
src/main/java/dev/keystrokes/
 ├── KeystrokesClient.java        client entrypoint, HUD + keybind registration
 ├── config/
 │    ├── KeyId.java              the 7 trackable inputs
 │    ├── KeystrokesConfig.java   all settings + JSON load/save (debounced)
 │    └── Preset.java             Clean / Minimal / Classic / PvP / Glass / Dark / Accent
 ├── hud/
 │    ├── Animation.java          time-based, interruptible press animation
 │    ├── AnimationProfile.java   named motion characters (Instant/Smooth/Snappy/Soft/Custom)
 │    ├── CpsTracker.java         rolling-window CPS counter, fed by discrete click events (nanoTime-based)
 │    ├── KeyLayout.java          shared layout engine (HUD + preview both use this)
 │    ├── KeyRenderer.java        anti-aliased rounded-rect key box + label drawing
 │    └── KeystrokesHud.java      tick()-driven held-state/animation + event-driven CPS + render()-only draw
 ├── input/
 │    ├── InputTracker.java       reads real KeyBinding held-state, focus-gated
 │    └── MouseClickEvents.java   click-event bus fed by the mixin; resolves edges to LMB/RMB
 ├── mixin/
 │    └── KeyBindingMixin.java    the mod's only mixin: one non-cancellable @Inject for click accuracy
 ├── screen/
 │    ├── ConfigSlider.java       reusable labeled slider
 │    └── KeystrokesConfigScreen.java  the settings GUI (6 tabs + live preview)
 ├── compat/
 │    └── KeystrokesModMenuIntegration.java  optional, only loaded if Mod Menu exists
 └── util/
      └── MathUtils.java          lerp/clamp/easing helpers (incl. easeOutBack for Snappy)
```

## Building

You need **JDK 21** and an internet connection (Gradle/Loom download
Minecraft, Yarn mappings and Fabric API on first run - none of that can be
vendored into this project).

```bash
# from the project root
gradle wrapper --gradle-version 8.14   # one-time: generates gradlew/gradlew.bat + the wrapper jar
./gradlew build
```

If you don't have a local Gradle install, download one from
https://gradle.org/releases/ (8.14+) just to run the `gradle wrapper` command
above once - after that `./gradlew` is self-contained.

The built mod jar appears at:

```
build/libs/keystrokes-1.0.0.jar
```

Copy that file into `.minecraft/mods/` alongside:

- **Fabric Loader** ≥ 0.16.10 for Minecraft 1.21.11
- **Fabric API** `0.141.6+1.21.11` (or newer for 1.21.11)

Mod Menu is optional - install it if you'd rather open Keystrokes' settings
from the mods screen than via a keybind.

## First run

1. Launch the game with the mods above installed.
2. Open **Options → Controls** and bind **"Open Keystrokes Config"** to a key
   (it ships unbound so it can't collide with anything you already use).
3. Press that key in-game to open the settings screen, or use Mod Menu.
4. Drag the live preview in the settings screen to reposition the HUD; every
   other setting is a slider/toggle/dropdown across the six tabs.

## Known limitations

- Settings tabs are not scrollable. Tab and widget widths now shrink to fit
  the window, and content height was checked against 854x480 through
  1920x1080 (and comfortably fits at default GUI scale), but a very small
  window combined with a very high GUI scale could still run the taller tabs
  (Style, Keys) close to the bottom of the screen. A scroll panel was
  deliberately left out to avoid adding UI complexity for that edge case; if
  it turns out to matter in practice it's a contained addition to
  `KeystrokesConfigScreen`.
- The anti-aliasing in `KeyRenderer.fillRoundedRect` is a lightweight analytic
  approximation (one blended boundary pixel per corner row), not full
  supersampling. It looks clean at the corner radii this HUD is designed for
  (0-12px) while keeping draw-call count low; it isn't a general-purpose
  vector renderer.

## A note on how this was built

The Minecraft/Fabric HUD rendering API changed significantly in 1.21.6-1.21.8
(the old `HudRenderCallback` was replaced by `HudElementRegistry`, and the 2D
matrix stack changed from `MatrixStack`/`PoseStack` to `Matrix3x2fStack`).
This project targets the *current* 1.21.11 API (`HudElementRegistry.addLast`,
`DrawContext.getMatrices().pushMatrix()/scale(x,y)/translate(x,y)`, etc.).

I was not able to actually compile this in the sandbox I built it in (no
network access to download Minecraft/Yarn/Fabric API/Gradle/Loom), so this
was written and manually reviewed rather than build-verified. Everything was
checked by hand for type correctness, import correctness, and consistency
with the 1.21.11-era API shapes used elsewhere in the original project
(`DrawContext`, `Matrix3x2fStack`, `CyclingButtonWidget`, `SliderWidget`,
`KeyBinding`, `RenderTickCounter`). There's a real chance a method name or
generic signature needs a small tweak the first time you run
`./gradlew build` - if that happens, the compiler error will point straight
at the line, and it will almost always be a one-line fix. Please treat the
first build as a verification step, not an afterthought.

**One spot that specifically deserves a look on first build:**
`KeyBindingMixin` targets `KeyBinding.setPressed(boolean)` by name, since
that's the long-standing Yarn mapping for the instance method Minecraft's own
GLFW input handling calls on every real press/release edge. I could not
verify that name against the actual 1.21.11 Yarn mappings in this sandbox
(no network access to fetch them). If Loom's annotation processor reports it
can't find a `setPressed(boolean)` method on `KeyBinding` to inject into,
check the mapped name for that method in your local Minecraft dev workspace
(e.g. via your IDE's "go to declaration" on `client.options.attackKey`, or
by decompiling `KeyBinding` through Loom) and update the `method = "..."`
value in `KeyBindingMixin` accordingly - everything else about the mixin
(the injection point, the `@At("HEAD")`, the lack of any cancellation) stays
the same regardless of what the method turns out to be named.
