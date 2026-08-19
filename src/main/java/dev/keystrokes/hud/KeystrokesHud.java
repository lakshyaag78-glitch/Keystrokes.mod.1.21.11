package dev.keystrokes.hud;

import dev.keystrokes.config.KeyId;
import dev.keystrokes.config.KeystrokesConfig;
import dev.keystrokes.input.InputTracker;
import dev.keystrokes.input.MouseClickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Owns the live animation/CPS state for every tracked key and renders the
 * whole keystrokes cluster (W / A S D / LMB RMB / SPACE) each frame.
 * <p>
 * Registered once via {@code HudElementRegistry.addLast(...)} for drawing and
 * via {@code ClientTickEvents.END_CLIENT_TICK} for held-state updates - see
 * {@link dev.keystrokes.KeystrokesClient}. Only does work for keys that are
 * actually enabled in the config; disabled keys cost nothing. Geometry comes
 * from {@link KeyLayout}, the same layout engine the config screen's live
 * preview uses, so the two can never visually diverge.
 * <p>
 * There are two independent, intentionally-separate state paths here, since
 * they answer two different questions:
 * <ul>
 *   <li><b>Held state</b> ("is this key down right now?") drives the press
 *   {@link Animation} for every tracked key, including LMB/RMB. It is
 *   updated in {@link #tick(MinecraftClient)}, once per game tick, by
 *   polling {@link InputTracker#isPressed}.</li>
 *   <li><b>Click events</b> ("did LMB/RMB just get clicked?") drive
 *   {@link CpsTracker} for LMB/RMB only. These arrive via
 *   {@link MouseClickEvents}, an event subscription fed by a small mixin
 *   observing Minecraft's own input handling directly - <em>not</em> tick
 *   polling - because a real click can start and finish entirely between two
 *   20 Hz ticks, and a CPS counter that could silently miss that is not
 *   accurate. See {@code dev.keystrokes.mixin.KeyBindingMixin} and
 *   {@link MouseClickEvents} for how that observation happens without ever
 *   consuming or altering the actual input.</li>
 * </ul>
 * {@link #tick} and {@link #render(DrawContext, RenderTickCounter)} are
 * intentionally separate for both paths: state updates (held-state polling
 * in tick, click events via the listener below) happen regardless of whether
 * the HUD element is actually being drawn this frame, so a screen that hides
 * the HUD (inventory, chat scrollback, even our own config screen) never
 * stalls CPS counting or leaves the press animation holding a stale target.
 * {@link #render} only ever reads that state and computes the current
 * animation *progress* (a pure function of wall-clock time, so it is still
 * perfectly smooth being evaluated at render's frame rate rather than tick
 * rate) - it never mutates input/CPS state itself.
 */
public final class KeystrokesHud {

    private final InputTracker input = new InputTracker();
    private final Map<KeyId, Animation> animations = new EnumMap<>(KeyId.class);
    private final Map<KeyId, CpsTracker> cpsTrackers = new EnumMap<>(KeyId.class);

    public KeystrokesHud() {
        for (KeyId id : KeyId.values()) {
            animations.put(id, new Animation());
            if (id.isMouseButton()) {
                cpsTrackers.put(id, new CpsTracker());
            }
        }
        // Discrete click events (event-driven, full input resolution) feed CPS directly -
        // see the class doc above for why this is separate from the tick-polled held state.
        MouseClickEvents.addListener(id -> {
            CpsTracker tracker = cpsTrackers.get(id);
            if (tracker != null) {
                tracker.registerClick();
            }
        });
    }

    /**
     * Advance held-state/animation-target state by one game tick. Safe and
     * cheap to call unconditionally every tick - it does not touch rendering
     * and never blocks or consumes any gameplay input (see {@link InputTracker}).
     * CPS is intentionally not updated here; see the class doc.
     */
    public void tick(MinecraftClient client) {
        if (client.player == null || client.options == null) {
            // Not in a world (title screen, disconnecting, etc.) - hold everything
            // at rest so nothing is left mid-animation or mid-click-window for
            // whenever the player next spawns in.
            resetAll();
            return;
        }

        boolean windowFocused = client.isWindowFocused();
        for (KeyId id : KeyId.values()) {
            boolean pressed = windowFocused && input.isPressed(id);
            animations.get(id).setPressed(pressed);
        }
        if (!windowFocused) {
            // Defensive: also hard-reset CPS windows so alt-tabbing never leaves phantom clicks queued
            // (in addition to MouseClickEvents itself refusing to register clicks while unfocused).
            for (CpsTracker tracker : cpsTrackers.values()) {
                tracker.reset();
            }
        }
    }

    private void resetAll() {
        for (Animation anim : animations.values()) {
            anim.setPressed(false);
        }
        for (CpsTracker tracker : cpsTrackers.values()) {
            tracker.reset();
        }
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null) {
            return;
        }
        if (!shouldRenderForCurrentScreen(client)) {
            return;
        }

        KeystrokesConfig cfg = KeystrokesConfig.getInstance();
        draw(context, cfg, animations, cpsTrackers);
    }

    /**
     * Shared draw routine: lays out and renders every enabled key using the
     * given animation/CPS state. Public so the config screen's live preview
     * can call it with its own (looping demo) animation state and reuse the
     * exact same layout engine and renderer as the real HUD - there is only
     * ever one rendering/layout implementation, never two that could diverge.
     */
    public static void draw(DrawContext context, KeystrokesConfig cfg,
                             Map<KeyId, Animation> animations, Map<KeyId, CpsTracker> cpsTrackers) {
        KeyLayout.Result layout = KeyLayout.compute(cfg, cfg.hudX, cfg.hudY);

        for (KeyLayout.KeyRect rect : layout.rects) {
            String subLabel = null;
            if (rect.id().isMouseButton() && cfg.cpsEnabled
                    && ((rect.id() == KeyId.LMB && cfg.cpsShowLmb) || (rect.id() == KeyId.RMB && cfg.cpsShowRmb))) {
                CpsTracker tracker = cpsTrackers.get(rect.id());
                if (tracker != null) {
                    subLabel = "CPS: " + tracker.getCps();
                }
            }
            drawKey(context, cfg, animations.get(rect.id()), rect, subLabel);
        }
    }

    private boolean shouldRenderForCurrentScreen(MinecraftClient client) {
        var screen = client.currentScreen;
        if (screen == null) {
            return true;
        }
        KeystrokesConfig cfg = KeystrokesConfig.getInstance();
        if (screen instanceof ChatScreen) {
            return cfg.visibleWithChatOpen;
        }
        if (screen instanceof HandledScreen<?>) {
            return cfg.visibleWithInventoryOpen;
        }
        // Any other screen (menus, our own config screen, death screen, etc.) - hide.
        return false;
    }

    private static void drawKey(DrawContext context, KeystrokesConfig cfg, Animation anim,
                                 KeyLayout.KeyRect rect, String subLabel) {
        float progress = anim.update(cfg.animationProfile, cfg.animationDurationMs);

        float scaleAmount = cfg.scaleAnimationEnabled ? cfg.pressScaleAmount * progress : 0f;
        float moveAmount = cfg.moveAnimationEnabled ? cfg.pressMoveAmount * cfg.scale * progress : 0f;
        float visualProgress = cfg.colorAnimationEnabled ? progress : (anim.isPressed() ? 1f : 0f);

        float w = rect.w();
        float h = rect.h();
        float drawW = w * (1f - scaleAmount);
        float drawH = h * (1f - scaleAmount);

        // Clamp the vertical "push" offset so even an overshooting profile (SNAPPY)
        // can never move a key far enough to visually collide with the row above/below
        // it - the logical layout (and therefore neighbouring rows' gaps) never changes,
        // only this key's own visual offset does, and it stays safely inside its own gap.
        float maxMove = Math.max(0f, (cfg.spacingV * cfg.scale) * 0.5f);
        moveAmount = Math.max(-maxMove, Math.min(moveAmount, maxMove));

        float drawX = rect.x() + (w - drawW) / 2f;
        float drawY = rect.y() + (h - drawH) / 2f + moveAmount;

        KeyRenderer.draw(context, cfg, Math.round(drawX), Math.round(drawY),
                Math.round(drawW), Math.round(drawH), rect.id().label(), subLabel, visualProgress);
    }
}
