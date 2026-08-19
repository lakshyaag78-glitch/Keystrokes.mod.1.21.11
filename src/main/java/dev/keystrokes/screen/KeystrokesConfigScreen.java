package dev.keystrokes.screen;

import dev.keystrokes.config.KeyId;
import dev.keystrokes.config.KeystrokesConfig;
import dev.keystrokes.config.Preset;
import dev.keystrokes.hud.Animation;
import dev.keystrokes.hud.AnimationProfile;
import dev.keystrokes.hud.CpsTracker;
import dev.keystrokes.hud.KeyLayout;
import dev.keystrokes.hud.KeystrokesHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * The full Keystrokes settings screen, organized into Layout / Style /
 * Animation / Keys / CPS / Presets tabs.
 * <p>
 * The live preview above the tab bar is rendered with the exact same
 * {@link KeyLayout} and {@link KeystrokesHud#draw} call the real HUD uses -
 * it drives its own small looping demo animation so you can see press motion
 * without needing to actually hold WASD, but the geometry, colors, corner
 * rendering and text are pixel-identical to what you'll see in-game. It can
 * still be dragged directly to reposition the HUD.
 * <p>
 * Every widget writes straight into the live {@link KeystrokesConfig}
 * instance. Slider drags call {@link KeystrokesConfig#requestSave()}, which
 * debounces the actual disk write, so dragging a slider doesn't hammer disk
 * I/O; toggles/buttons/presets are infrequent enough to save immediately.
 * Closing the screen always flushes.
 */
public final class KeystrokesConfigScreen extends Screen {

    private enum Tab {
        LAYOUT("Layout"),
        STYLE("Style"),
        ANIMATION("Animation"),
        KEYS("Keys"),
        CPS("CPS"),
        PRESETS("Presets");

        final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    private final Screen parent;
    private final KeystrokesConfig cfg = KeystrokesConfig.getInstance();
    private Tab currentTab = Tab.LAYOUT;

    private boolean draggingPreview = false;
    private double dragOffsetX;
    private double dragOffsetY;

    // Independent, purely-cosmetic animation/CPS state for the preview's looping demo.
    // Never touches real input - just cycles presses so the motion is visible at a glance.
    private final Map<KeyId, Animation> previewAnimations = new EnumMap<>(KeyId.class);
    private final Map<KeyId, CpsTracker> previewCps = new EnumMap<>(KeyId.class);
    // CpsTracker only accepts discrete click registrations now (see CpsTracker javadoc), so the
    // demo loop - which only produces a level "pressed" signal per frame, same as real input used
    // to - has to detect its own press-down edges here, the same way MouseClickEvents does for
    // real clicks. Purely local to the simulated preview; never touches real CPS state.
    private final Map<KeyId, Boolean> previewWasPressed = new EnumMap<>(KeyId.class);

    private static final int CONTENT_TOP = 78;
    private static final int ROW_HEIGHT = 22;
    private static final int WIDGET_WIDTH_MAX = 240;
    private static final int WIDGET_WIDTH_MIN = 140;
    private static final int TAB_WIDTH_MAX = 78;
    private static final int TAB_WIDTH_MIN = 40;

    /** Widget width shrinks on very small/high-GUI-scale windows instead of overflowing off-screen. */
    private int widgetWidth() {
        return Math.max(WIDGET_WIDTH_MIN, Math.min(WIDGET_WIDTH_MAX, this.width - 40));
    }

    private int tabWidth() {
        int count = Tab.values().length;
        return Math.max(TAB_WIDTH_MIN, Math.min(TAB_WIDTH_MAX, this.width / count));
    }

    /** @param parent screen to return to on close, or null to return to gameplay. */
    public KeystrokesConfigScreen(Screen parent) {
        super(Text.literal("Keystrokes Settings"));
        this.parent = parent;
        for (KeyId id : KeyId.values()) {
            previewAnimations.put(id, new Animation());
            if (id.isMouseButton()) {
                previewCps.put(id, new CpsTracker());
            }
        }
    }

    @Override
    protected void init() {
        pendingLabels.clear();
        int tabWidth = tabWidth();
        int startX = this.width / 2 - (tabWidth * Tab.values().length) / 2;
        int i = 0;
        for (Tab tab : Tab.values()) {
            int x = startX + i * tabWidth;
            ButtonWidget btn = ButtonWidget.builder(Text.literal(tab.title), b -> switchTab(tab))
                    .dimensions(x, 40, tabWidth - 2, 20)
                    .build();
            btn.active = tab != currentTab;
            this.addDrawableChild(btn);
            i++;
        }

        buildTabContent();

        int doneWidth = Math.min(150, this.width - 20);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width / 2 - doneWidth / 2, this.height - 28, doneWidth, 20)
                .build());
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        this.clearChildren();
        this.init();
    }

    private int centerX() {
        return this.width / 2 - widgetWidth() / 2;
    }

    private void buildTabContent() {
        switch (currentTab) {
            case LAYOUT -> buildLayoutTab();
            case STYLE -> buildStyleTab();
            case ANIMATION -> buildAnimationTab();
            case KEYS -> buildKeysTab();
            case CPS -> buildCpsTab();
            case PRESETS -> buildPresetsTab();
        }
    }

    // ---------------------------------------------------------------
    // Layout tab: scale, key size, spacing, corner radius, position
    // ---------------------------------------------------------------
    private void buildLayoutTab() {
        int x = centerX();
        int y = CONTENT_TOP;

        y = section(y, "SIZE & SPACING");
        y = addSlider(x, y, "Scale: ", "%", 50, 200, cfg.scale * 100.0, v -> cfg.scale = (float) (v / 100.0));
        y = addSlider(x, y, "Key Width: ", "px", 10, 60, cfg.keyWidth, v -> cfg.keyWidth = (float) v);
        y = addSlider(x, y, "Key Height: ", "px", 10, 60, cfg.keyHeight, v -> cfg.keyHeight = (float) v);
        y = addSlider(x, y, "H. Spacing: ", "px", 0, 20, cfg.spacingH, v -> cfg.spacingH = (float) v);
        y = addSlider(x, y, "V. Spacing: ", "px", 0, 20, cfg.spacingV, v -> cfg.spacingV = (float) v);
        y = addSlider(x, y, "Corner Radius: ", "px", 0, 12, cfg.cornerRadius, v -> cfg.cornerRadius = (int) Math.round(v));

        y += 6;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset Position"), b -> {
                    cfg.hudX = 20f;
                    cfg.hudY = 20f;
                    cfg.save();
                })
                .dimensions(x, y, widgetWidth(), 20)
                .build());
    }

    // ---------------------------------------------------------------
    // Style tab: background, border, text
    // ---------------------------------------------------------------
    private void buildStyleTab() {
        int x = centerX();
        int y = CONTENT_TOP;

        y = section(y, "BACKGROUND");
        y = addColorField(x, y, "Color (hex)", cfg.backgroundColor, v -> cfg.backgroundColor = v);
        y = addSlider(x, y, "Opacity: ", "%", 0, 100, cfg.backgroundOpacity * 100.0, v -> cfg.backgroundOpacity = (float) (v / 100.0));
        y = addColorField(x, y, "Pressed Color (hex)", cfg.backgroundPressedColor, v -> cfg.backgroundPressedColor = v);
        y = addSlider(x, y, "Pressed Opacity: ", "%", 0, 100, cfg.backgroundPressedOpacity * 100.0, v -> cfg.backgroundPressedOpacity = (float) (v / 100.0));

        y = section(y, "BORDER");
        y = addToggle(x, y, "Border Enabled", cfg.borderEnabled, v -> cfg.borderEnabled = v);
        y = addColorField(x, y, "Border Color (hex)", cfg.borderColor, v -> cfg.borderColor = v);
        y = addSlider(x, y, "Border Opacity: ", "%", 0, 100, cfg.borderOpacity * 100.0, v -> cfg.borderOpacity = (float) (v / 100.0));
        y = addSlider(x, y, "Border Thickness: ", "px", 1, 4, cfg.borderThickness, v -> cfg.borderThickness = (float) v);

        y = section(y, "TEXT");
        y = addColorField(x, y, "Text Color (hex)", cfg.textColor, v -> cfg.textColor = v);
        y = addColorField(x, y, "Pressed Text Color (hex)", cfg.textPressedColor, v -> cfg.textPressedColor = v);
        y = addSlider(x, y, "Text Size: ", "x", 50, 200, cfg.textSize * 100.0, v -> cfg.textSize = (float) (v / 100.0));

        this.addDrawableChild(CyclingButtonWidget.<KeystrokesConfig.TextAlign>builder(a -> Text.literal(capitalize(a.name())), () -> KeystrokesConfig.TextAlign.values()[0])
                .values(KeystrokesConfig.TextAlign.values())
                .initially(cfg.textAlign)
                .build(x, y, widgetWidth(), 20, Text.literal("Text Align"), (btn, val) -> {
                    cfg.textAlign = val;
                    cfg.requestSave();
                }));
    }

    // ---------------------------------------------------------------
    // Animation tab: profile + advanced tuning
    // ---------------------------------------------------------------
    private void buildAnimationTab() {
        int x = centerX();
        int y = CONTENT_TOP;

        y = section(y, "MOTION PROFILE");
        this.addDrawableChild(CyclingButtonWidget.<AnimationProfile>builder(p -> Text.literal(p.displayName()), () -> AnimationProfile.values()[0])
                .values(AnimationProfile.values())
                .initially(cfg.animationProfile)
                .build(x, y, widgetWidth(), 20, Text.literal("Profile"), (btn, val) -> {
                    cfg.animationProfile = val;
                    cfg.requestSave();
                }));
        y += ROW_HEIGHT + 4;

        y = section(y, "MOTION LAYERS");
        y = addToggle(x, y, "Scale Animation", cfg.scaleAnimationEnabled, v -> cfg.scaleAnimationEnabled = v);
        y = addToggle(x, y, "Move Animation", cfg.moveAnimationEnabled, v -> cfg.moveAnimationEnabled = v);
        y = addToggle(x, y, "Color Animation", cfg.colorAnimationEnabled, v -> cfg.colorAnimationEnabled = v);
        y = addSlider(x, y, "Press Scale: ", "%", 0, 20, cfg.pressScaleAmount * 100.0, v -> cfg.pressScaleAmount = (float) (v / 100.0));
        y = addSlider(x, y, "Press Move: ", "px", 0, 6, cfg.pressMoveAmount, v -> cfg.pressMoveAmount = (float) v);

        y = section(y, "ADVANCED (Custom profile only)");
        addSlider(x, y, "Custom Duration: ", "ms", 30, 300, cfg.animationDurationMs, v -> cfg.animationDurationMs = (int) Math.round(v));
    }

    // ---------------------------------------------------------------
    // Keys tab: per-key visibility + screen behavior
    // ---------------------------------------------------------------
    private void buildKeysTab() {
        int x = centerX();
        int y = CONTENT_TOP;

        y = section(y, "VISIBLE KEYS");
        for (KeyId id : KeyId.values()) {
            boolean enabled = cfg.isKeyEnabled(id);
            this.addDrawableChild(CyclingButtonWidget.onOffBuilder(enabled)
                    .build(x, y, widgetWidth(), 20, Text.literal("Show " + id.label()), (btn, val) -> {
                        cfg.setKeyEnabled(id, val);
                        cfg.requestSave();
                    }));
            y += ROW_HEIGHT;
        }

        y = section(y, "VISIBILITY");
        y = addToggle(x, y, "Visible With Inventory Open", cfg.visibleWithInventoryOpen, v -> cfg.visibleWithInventoryOpen = v);
        addToggle(x, y, "Visible With Chat Open", cfg.visibleWithChatOpen, v -> cfg.visibleWithChatOpen = v);
    }

    // ---------------------------------------------------------------
    // CPS tab
    // ---------------------------------------------------------------
    private void buildCpsTab() {
        int x = centerX();
        int y = CONTENT_TOP;

        y = section(y, "CLICKS PER SECOND");
        y = addToggle(x, y, "Show CPS", cfg.cpsEnabled, v -> cfg.cpsEnabled = v);

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.cpsShowLmb)
                .build(x, y, widgetWidth() / 2 - 2, 20, Text.literal("LMB CPS"), (btn, val) -> {
                    cfg.cpsShowLmb = val;
                    cfg.requestSave();
                }));
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.cpsShowRmb)
                .build(x + widgetWidth() / 2 + 2, y, widgetWidth() / 2 - 2, 20, Text.literal("RMB CPS"), (btn, val) -> {
                    cfg.cpsShowRmb = val;
                    cfg.requestSave();
                }));
    }

    // ---------------------------------------------------------------
    // Presets tab
    // ---------------------------------------------------------------
    private void buildPresetsTab() {
        int x = centerX();
        int y = CONTENT_TOP;

        y = section(y, "BUILT-IN PRESETS");
        for (Preset preset : Preset.values()) {
            boolean active = preset.displayName().equals(cfg.selectedPreset);
            ButtonWidget btn = ButtonWidget.builder(
                            Text.literal(active ? "> " + preset.displayName() : preset.displayName()),
                            b -> {
                                preset.apply(cfg);
                                cfg.selectedPreset = preset.displayName();
                                cfg.save();
                                switchTab(Tab.PRESETS);
                            })
                    .dimensions(x, y, widgetWidth(), 20)
                    .build();
            this.addDrawableChild(btn);
            y += ROW_HEIGHT;
        }
    }

    // ---------------------------------------------------------------
    // Shared widget helpers
    // ---------------------------------------------------------------
    private int section(int y, String title) {
        pendingLabels.add(new Label(centerX(), y + 6, title, true));
        return y + 14;
    }

    private int addSlider(int x, int y, String prefix, String suffix, double min, double max,
                           double initial, DoubleConsumer setter) {
        this.addDrawableChild(new ConfigSlider(x, y, widgetWidth(), 20, prefix, suffix, min, max, initial, v -> {
            setter.accept(v);
            cfg.requestSave();
        }));
        return y + ROW_HEIGHT;
    }

    private int addToggle(int x, int y, String label, boolean initial, java.util.function.Consumer<Boolean> setter) {
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(initial)
                .build(x, y, widgetWidth(), 20, Text.literal(label), (btn, val) -> {
                    setter.accept(val);
                    cfg.requestSave();
                }));
        return y + ROW_HEIGHT;
    }

    private int addColorField(int x, int y, String label, int currentColor, IntConsumer setter) {
        int fieldWidth = 100;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x + widgetWidth() - fieldWidth, y, fieldWidth, 20,
                Text.literal(label));
        field.setMaxLength(6);
        field.setText(String.format("%06X", currentColor & 0xFFFFFF));
        field.setChangedListener(text -> {
            String hex = text.trim();
            if (hex.matches("[0-9a-fA-F]{6}")) {
                setter.accept(Integer.parseInt(hex, 16));
                cfg.requestSave();
            }
        });
        this.addDrawableChild(field);
        this.pendingLabels.add(new Label(x, y + 6, label, false));
        return y + ROW_HEIGHT;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase();
    }

    private final List<Label> pendingLabels = new ArrayList<>();

    private record Label(int x, int y, String text, boolean isSection) {
    }

    // ---------------------------------------------------------------
    // Rendering: background, tab title, labels, live draggable preview
    // ---------------------------------------------------------------
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        for (Label label : pendingLabels) {
            int color = label.isSection() ? 0x6FA8FF : 0xA0A0A0;
            context.drawTextWithShadow(this.textRenderer, label.text(), label.x(), label.y(), color);
        }

        if (currentTab == Tab.PRESETS) {
            context.drawTextWithShadow(this.textRenderer, "Current: " + cfg.selectedPreset,
                    centerX(), CONTENT_TOP + 14 + Preset.values().length * ROW_HEIGHT + 8, 0xA0A0A0);
        }

        updatePreviewDemo();
        drawPreview(context);

        cfg.tickAutosave();
    }

    /** Drives a small looping fake-press pattern so the preview shows motion without real input. */
    private void updatePreviewDemo() {
        long now = System.currentTimeMillis();
        KeyId[] ids = KeyId.values();
        for (int i = 0; i < ids.length; i++) {
            KeyId id = ids[i];
            long phase = (now + i * 220L) % 1800L;
            boolean pressed = phase < 260L;
            previewAnimations.get(id).setPressed(pressed);
            if (id.isMouseButton()) {
                boolean wasPressed = previewWasPressed.getOrDefault(id, false);
                if (pressed && !wasPressed) {
                    previewCps.get(id).registerClick();
                }
                previewWasPressed.put(id, pressed);
            }
        }
    }

    private KeyLayout.Result currentLayout() {
        return KeyLayout.compute(cfg, cfg.hudX, cfg.hudY);
    }

    private void drawPreview(DrawContext context) {
        KeystrokesHud.draw(context, cfg, previewAnimations, previewCps);

        context.drawTextWithShadow(this.textRenderer, "Drag to move",
                Math.round(cfg.hudX), Math.round(cfg.hudY - 12), draggingPreview ? 0xFF6FA8FF : 0x808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsidePreview(mouseX, mouseY) && !isOverAnyChild(mouseX, mouseY)) {
            draggingPreview = true;
            dragOffsetX = mouseX - cfg.hudX;
            dragOffsetY = mouseY - cfg.hudY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPreview) {
            KeyLayout.Result layout = currentLayout();
            float bw = layout.width;
            float bh = layout.height;
            float newX = (float) (mouseX - dragOffsetX);
            float newY = (float) (mouseY - dragOffsetY);
            // Keep at least a sliver of the HUD on screen at all times, from every edge/corner.
            float minVisible = 12f;
            newX = Math.max(minVisible - bw, Math.min(newX, this.width - minVisible));
            newY = Math.max(minVisible - bh, Math.min(newY, this.height - minVisible));
            cfg.hudX = newX;
            cfg.hudY = newY;
            cfg.requestSave();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview) {
            draggingPreview = false;
            cfg.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        KeyLayout.Result layout = currentLayout();
        return mouseX >= cfg.hudX && mouseX <= cfg.hudX + layout.width
                && mouseY >= cfg.hudY && mouseY <= cfg.hudY + layout.height;
    }

    private boolean isOverAnyChild(double mouseX, double mouseY) {
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Element element && element.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        cfg.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
