package dev.keystrokes.hud;

import dev.keystrokes.util.MathUtils;

/**
 * Tracks a single key's press animation as a function of wall-clock time
 * rather than frame count, so it looks identical at 30, 60, 120 or 240 FPS.
 * <p>
 * The animation is fully interruptible: if the key is released mid-press (or
 * re-pressed mid-release), the current visual progress becomes the new
 * starting point instead of snapping, so there is never a pop even under
 * rapid tapping. The transition's effective duration is scaled by how far it
 * actually has to travel, so a reversal that starts half-way doesn't inherit
 * the full-traversal duration and end up feeling sluggish, and rapid taps
 * never produce a visible stutter or overshoot pile-up.
 */
public final class Animation {

    private static final float MIN_DURATION_FRACTION = 0.35f;

    private boolean pressed = false;
    private float progress = 0f;          // current rendered progress, 0 = released, 1 = fully pressed
    private float progressAtTransition = 0f;
    private long transitionStartNanos = System.nanoTime();

    /** Call once per frame with the real current key state. */
    public void setPressed(boolean nowPressed) {
        if (nowPressed != this.pressed) {
            // Capture wherever we currently are so the reversal is smooth, not a snap.
            this.progressAtTransition = this.progress;
            this.transitionStartNanos = System.nanoTime();
            this.pressed = nowPressed;
        }
    }

    public boolean isPressed() {
        return pressed;
    }

    /**
     * Recompute {@link #progress} for the current instant and return it.
     *
     * @param profile          the motion character to use
     * @param customDurationMs used only when {@code profile} is {@link AnimationProfile#CUSTOM}
     */
    public float update(AnimationProfile profile, int customDurationMs) {
        float target = pressed ? 1f : 0f;
        int baseDurationMs = profile.durationMs(pressed, customDurationMs);

        if (baseDurationMs <= 0) {
            progress = target;
            return progress;
        }

        // Scale duration by remaining distance so a reversal that starts half-way
        // takes proportionally less time, keeping perceived speed consistent instead
        // of every transition taking the same wall-clock time regardless of how far
        // it actually has to travel.
        float distance = Math.abs(target - progressAtTransition);
        float durationScale = Math.max(MIN_DURATION_FRACTION, distance);
        double effectiveDurationMs = baseDurationMs * durationScale;

        long elapsedNanos = System.nanoTime() - transitionStartNanos;
        double elapsedMs = elapsedNanos / 1_000_000.0;
        float t = (float) MathUtils.clamp(elapsedMs / effectiveDurationMs, 0.0, 1.0);
        float eased = profile.easing(pressed).apply(t);

        progress = progressAtTransition + (target - progressAtTransition) * eased;
        return progress;
    }

    public float progress() {
        return progress;
    }
}
