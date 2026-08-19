package dev.keystrokes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.keystrokes.hud.AnimationProfile;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * All persistent, player-adjustable settings for the Keystrokes HUD.
 * <p>
 * This is a plain data object saved/loaded as JSON via Gson. Fields are safe
 * to mutate directly from the config screen. Call {@link #requestSave()}
 * after a change instead of {@link #save()} directly - it debounces actual
 * disk writes so dragging a slider doesn't hit the disk on every tick (see
 * {@link #tickAutosave()}). {@link #save()} still exists for callers that
 * genuinely need an immediate flush (e.g. on screen close).
 */
public final class KeystrokesConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "keystrokes.json";
    private static final long AUTOSAVE_INTERVAL_MS = 500L;

    private static KeystrokesConfig instance;

    // ---- Position ----
    public float hudX = 20f;
    public float hudY = 20f;

    // ---- Scale ----
    public float scale = 1.0f; // 0.5 - 2.0

    // ---- Key size ----
    public float keyWidth = 22f;
    public float keyHeight = 20f;

    // ---- Spacing ----
    public float spacingH = 3f;
    public float spacingV = 3f;

    // ---- Corner radius ----
    public int cornerRadius = 3;

    // ---- Background ----
    public int backgroundColor = 0x0A0A0F;
    public float backgroundOpacity = 0.55f;
    public int backgroundPressedColor = 0x3A82F7;
    public float backgroundPressedOpacity = 0.85f;

    // ---- Border ----
    public boolean borderEnabled = true;
    public int borderColor = 0xFFFFFF;
    public float borderOpacity = 0.18f;
    public float borderThickness = 1.0f;

    // ---- Text ----
    public int textColor = 0xE6E6EC;
    public int textPressedColor = 0xFFFFFF;
    public float textSize = 1.0f;
    public TextAlign textAlign = TextAlign.CENTER;

    // ---- Animation ----
    /** Named motion character. {@link AnimationProfile#INSTANT} is the "animations off" state. */
    public AnimationProfile animationProfile = AnimationProfile.SMOOTH;
    /** Only used when {@link #animationProfile} is {@link AnimationProfile#CUSTOM}. */
    public int animationDurationMs = 110;
    public boolean scaleAnimationEnabled = true;
    public boolean moveAnimationEnabled = true;
    public boolean colorAnimationEnabled = true;
    public float pressScaleAmount = 0.05f;  // scale shrinks by up to this much
    public float pressMoveAmount = 2.0f;    // pixels moved down on full press (before HUD scale)

    // ---- CPS ----
    public boolean cpsEnabled = true;
    public boolean cpsShowLmb = true;
    public boolean cpsShowRmb = true;

    // ---- Screen behavior ----
    public boolean visibleWithChatOpen = true;
    public boolean visibleWithInventoryOpen = false;

    // ---- Per-key enable/disable ----
    public Map<KeyId, Boolean> enabledKeys = defaultEnabledKeys();

    // ---- Bookkeeping ----
    public String selectedPreset = "Custom";

    // ---- Debounced-save state (not persisted) ----
    private transient boolean dirty = false;
    private transient long lastSaveAtMs = 0L;

    private static Map<KeyId, Boolean> defaultEnabledKeys() {
        Map<KeyId, Boolean> map = new EnumMap<>(KeyId.class);
        for (KeyId id : KeyId.values()) {
            map.put(id, true);
        }
        return map;
    }

    public boolean isKeyEnabled(KeyId id) {
        return enabledKeys.getOrDefault(id, true);
    }

    public void setKeyEnabled(KeyId id, boolean enabled) {
        enabledKeys.put(id, enabled);
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static KeystrokesConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static KeystrokesConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                KeystrokesConfig loaded = GSON.fromJson(reader, KeystrokesConfig.class);
                if (loaded != null) {
                    loaded.postLoadFix();
                    return loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                // Corrupted or unreadable config: fall back to defaults rather than crashing.
                System.err.println("[Keystrokes] Failed to load config, using defaults: " + e.getMessage());
            }
        }
        KeystrokesConfig fresh = new KeystrokesConfig();
        fresh.save();
        return fresh;
    }

    /** Fills in anything missing/invalid from an older or hand-edited config file. */
    private void postLoadFix() {
        if (enabledKeys == null) {
            enabledKeys = defaultEnabledKeys();
        } else {
            for (KeyId id : KeyId.values()) {
                enabledKeys.putIfAbsent(id, true);
            }
        }
        if (animationProfile == null) {
            animationProfile = AnimationProfile.SMOOTH;
        }
        if (textAlign == null) {
            textAlign = TextAlign.CENTER;
        }
        if (selectedPreset == null) {
            selectedPreset = "Custom";
        }
        scale = MathClampHelper.clamp(scale, 0.5f, 2.0f);
        keyWidth = Math.max(4f, keyWidth);
        keyHeight = Math.max(4f, keyHeight);
        cornerRadius = Math.max(0, cornerRadius);
        animationDurationMs = Math.max(0, animationDurationMs);
    }

    /** Marks the config changed and writes to disk if it's been a while since the last write. */
    public void requestSave() {
        dirty = true;
        tickAutosave();
    }

    /** Call once per config-screen frame; flushes a pending save at most once per {@value #AUTOSAVE_INTERVAL_MS}ms. */
    public void tickAutosave() {
        if (!dirty) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSaveAtMs >= AUTOSAVE_INTERVAL_MS) {
            save();
        }
    }

    /** Immediate, unconditional write - use for infrequent events (preset apply, screen close). */
    public void save() {
        Path path = configPath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            dirty = false;
            lastSaveAtMs = System.currentTimeMillis();
        } catch (IOException e) {
            System.err.println("[Keystrokes] Failed to save config: " + e.getMessage());
        }
    }

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    /** Tiny local clamp so this file doesn't need to depend on the hud/util package for one call. */
    private static final class MathClampHelper {
        static float clamp(float v, float min, float max) {
            return v < min ? min : Math.min(v, max);
        }
    }
}
