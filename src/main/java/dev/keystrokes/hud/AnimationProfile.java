package dev.keystrokes.hud;

import dev.keystrokes.util.MathUtils;

/**
 * A named motion character: how long a press/release transition takes and
 * which easing curve it uses. Exists so the default experience is "pick a
 * feel" rather than "tune four raw numbers" - {@link #CUSTOM} is the escape
 * hatch for players who do want to tune the raw numbers
 * ({@link dev.keystrokes.config.KeystrokesConfig#animationDurationMs} etc).
 * <p>
 * Durations here are deliberately short - premium clients use restrained,
 * near-instant motion, not showy animation.
 */
public enum AnimationProfile {

    /** No transition at all; state changes are drawn on the very next frame. */
    INSTANT("Instant", 0, 0, MathUtils::easeOutQuad, MathUtils::easeOutQuad),

    /** The default: quick press, slightly gentler release. Feels precise, not floaty. */
    SMOOTH("Smooth", 90, 130, MathUtils::easeOutCubic, MathUtils::easeOutQuad),

    /** Faster and firmer, with a touch of overshoot on press for a "punchy" click feel. */
    SNAPPY("Snappy", 60, 90, MathUtils::easeOutBack, MathUtils::easeOutCubic),

    /** Slower, rounder easing on both ends - calmer, more relaxed motion. */
    SOFT("Soft", 160, 200, MathUtils::easeInOutCubic, MathUtils::easeInOutCubic),

    /** Uses the raw duration/easing fields on the config instead of fixed values. */
    CUSTOM("Custom", -1, -1, MathUtils::easeOutCubic, MathUtils::easeOutCubic);

    /** A t (0..1) -> eased-t (usually 0..1, but may briefly exceed it for overshoot) curve. */
    @FunctionalInterface
    public interface Easing {
        float apply(float t);
    }

    private final String displayName;
    private final int pressDurationMs;
    private final int releaseDurationMs;
    private final Easing pressEasing;
    private final Easing releaseEasing;

    AnimationProfile(String displayName, int pressDurationMs, int releaseDurationMs,
                      Easing pressEasing, Easing releaseEasing) {
        this.displayName = displayName;
        this.pressDurationMs = pressDurationMs;
        this.releaseDurationMs = releaseDurationMs;
        this.pressEasing = pressEasing;
        this.releaseEasing = releaseEasing;
    }

    public String displayName() {
        return displayName;
    }

    /** @param customDurationMs used only when this profile is {@link #CUSTOM} */
    public int durationMs(boolean pressing, int customDurationMs) {
        int fixed = pressing ? pressDurationMs : releaseDurationMs;
        return fixed >= 0 ? fixed : customDurationMs;
    }

    public Easing easing(boolean pressing) {
        return pressing ? pressEasing : releaseEasing;
    }

    public static AnimationProfile byName(String name) {
        for (AnimationProfile p : values()) {
            if (p.displayName.equalsIgnoreCase(name) || p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}
