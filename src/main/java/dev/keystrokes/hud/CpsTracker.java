package dev.keystrokes.hud;

import java.util.ArrayDeque;

/**
 * Counts clicks in a rolling one-second window rather than accumulating
 * forever, so the number always reflects "clicks in the last second" and
 * memory use stays bounded (a human physically cannot exceed a few hundred
 * clicks/sec, so the deque never grows unreasonably large).
 * <p>
 * This is a pure data sink: call {@link #registerClick()} exactly once per
 * genuine press-down edge. It does not detect edges itself and does not poll
 * anything - edge detection happens at the source (see
 * {@code dev.keystrokes.input.MouseClickEvents}, fed by a mixin observing
 * real input events), which is what lets this correctly count clicks that
 * happen faster than the ~20 Hz client tick rate instead of missing them.
 * <p>
 * Timestamps use {@link System#nanoTime()} rather than
 * {@link System#currentTimeMillis()}: this is a short-lived elapsed-time
 * measurement, not a wall-clock/calendar one, and {@code nanoTime} is
 * monotonic - immune to the count silently corrupting itself if the system
 * clock steps backward or forward (NTP sync, user changing the clock, DST),
 * which {@code currentTimeMillis} is not protected against.
 */
public final class CpsTracker {

    private static final long WINDOW_NANOS = 1_000_000_000L;

    private final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();

    /** Registers exactly one click, timestamped now. Call only on a genuine press-down edge. */
    public void registerClick() {
        clickTimestamps.addLast(System.nanoTime());
        prune();
    }

    /** Discards clicks older than the rolling window and returns the current count. */
    public int getCps() {
        prune();
        return clickTimestamps.size();
    }

    private void prune() {
        long cutoff = System.nanoTime() - WINDOW_NANOS;
        // nanoTime() is only meaningful as a difference, and can in principle wrap around;
        // "timestamp - cutoff < 0" is the wraparound-safe idiom the JavaDoc for nanoTime()
        // itself recommends, unlike a direct "timestamp < cutoff" comparison.
        while (!clickTimestamps.isEmpty() && (clickTimestamps.peekFirst() - cutoff) < 0) {
            clickTimestamps.pollFirst();
        }
    }

    /** Called on window focus loss or when leaving the world, so stray/held clicks never linger. */
    public void reset() {
        clickTimestamps.clear();
    }
}
