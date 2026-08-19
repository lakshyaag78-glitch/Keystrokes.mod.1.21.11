package dev.keystrokes.util;

/**
 * Small, allocation-free math helpers used by the animation and rendering code.
 * Nothing here creates objects, so it is safe to call every frame.
 */
public final class MathUtils {

    private MathUtils() {
    }

    /** Clamp {@code v} into the inclusive range [min, max]. */
    public static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    public static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    /** Linear interpolation between {@code a} and {@code b}. */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Linear interpolation between two ARGB packed colors. */
    public static int lerpColor(int colorA, int colorB, float t) {
        t = clamp(t, 0f, 1f);
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int a = Math.round(lerp(aA, aB, t));
        int r = Math.round(lerp(rA, rB, t));
        int g = Math.round(lerp(gA, gB, t));
        int b = Math.round(lerp(bA, bB, t));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** easeOutCubic: fast start, smooth settle. Good default for "press" motion. */
    public static float easeOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        float f = t - 1f;
        return f * f * f + 1f;
    }

    /** easeOutQuad: gentler than cubic. */
    public static float easeOutQuad(float t) {
        t = clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t);
    }

    /** easeInOutCubic: symmetric acceleration/deceleration. */
    public static float easeInOutCubic(float t) {
        t = clamp(t, 0f, 1f);
        return t < 0.5f
                ? 4f * t * t * t
                : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /** easeOutBack: overshoots slightly past 1 before settling - a subtle "punch". */
    public static float easeOutBack(float t) {
        t = clamp(t, 0f, 1f);
        final float c1 = 1.70158f * 0.6f; // dampened for a subtle overshoot, not a cartoonish one
        final float c3 = c1 + 1f;
        float f = t - 1f;
        return 1f + c3 * f * f * f + c1 * f * f;
    }

    public static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
