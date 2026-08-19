package dev.keystrokes.hud;

import dev.keystrokes.config.KeyId;
import dev.keystrokes.config.KeystrokesConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the on-screen geometry of every enabled key for a given config.
 * <p>
 * This is the single source of truth for "where does each key box go" - both
 * {@link KeystrokesHud} (the real HUD) and the config screen's live preview
 * call {@link #compute(KeystrokesConfig, float, float)} with the same config,
 * so they can never visually diverge.
 * <p>
 * Rows reflow independently: disabling individual keys within a row shrinks
 * that row and re-centers its remaining keys, rather than leaving a gap. All
 * rows are then centered against the widest row so the whole cluster reads
 * as one deliberate shape no matter which keys are enabled.
 */
public final class KeyLayout {

    /** One key's resolved on-screen box, in already-scaled pixel space. */
    public record KeyRect(KeyId id, float x, float y, float w, float h) {
    }

    public static final class Result {
        public final List<KeyRect> rects;
        public final float width;
        public final float height;

        private Result(List<KeyRect> rects, float width, float height) {
            this.rects = rects;
            this.width = width;
            this.height = height;
        }
    }

    private KeyLayout() {
    }

    /**
     * @param originX top-left X of the whole cluster, in screen pixels
     * @param originY top-left Y of the whole cluster, in screen pixels
     */
    public static Result compute(KeystrokesConfig cfg, float originX, float originY) {
        float kw = cfg.keyWidth * cfg.scale;
        float kh = cfg.keyHeight * cfg.scale;
        float sh = cfg.spacingH * cfg.scale;
        float sv = cfg.spacingV * cfg.scale;

        boolean wOn = cfg.isKeyEnabled(KeyId.W);
        boolean aOn = cfg.isKeyEnabled(KeyId.A);
        boolean sOn = cfg.isKeyEnabled(KeyId.S);
        boolean dOn = cfg.isKeyEnabled(KeyId.D);
        boolean spaceOn = cfg.isKeyEnabled(KeyId.SPACE);
        boolean lmbOn = cfg.isKeyEnabled(KeyId.LMB);
        boolean rmbOn = cfg.isKeyEnabled(KeyId.RMB);

        int asdCount = (aOn ? 1 : 0) + (sOn ? 1 : 0) + (dOn ? 1 : 0);
        float asdWidth = asdCount > 0 ? asdCount * kw + (asdCount - 1) * sh : 0f;

        int mouseCount = (lmbOn ? 1 : 0) + (rmbOn ? 1 : 0);
        float mouseNaturalWidth = mouseCount == 2 ? 2 * kw + sh : (mouseCount == 1 ? kw : 0f);

        // The body width every row centers/stretches against. Falls back to a single
        // key's width so a lone key (e.g. only SPACE enabled) still renders sanely.
        float bodyWidth = Math.max(asdWidth, Math.max(mouseNaturalWidth, wOn ? kw : 0f));
        if (bodyWidth <= 0f) {
            bodyWidth = kw;
        }

        List<KeyRect> rects = new ArrayList<>(7);
        float cursorY = originY;
        boolean anyRowYet = false;

        if (wOn) {
            float wx = originX + (bodyWidth - kw) / 2f;
            rects.add(new KeyRect(KeyId.W, wx, cursorY, kw, kh));
            cursorY += kh;
            anyRowYet = true;
        }

        if (asdCount > 0) {
            if (anyRowYet) cursorY += sv;
            float x = originX + (bodyWidth - asdWidth) / 2f;
            if (aOn) {
                rects.add(new KeyRect(KeyId.A, x, cursorY, kw, kh));
                x += kw + sh;
            }
            if (sOn) {
                rects.add(new KeyRect(KeyId.S, x, cursorY, kw, kh));
                x += kw + sh;
            }
            if (dOn) {
                rects.add(new KeyRect(KeyId.D, x, cursorY, kw, kh));
            }
            cursorY += kh;
            anyRowYet = true;
        }

        if (mouseCount > 0) {
            if (anyRowYet) cursorY += sv;
            if (mouseCount == 2) {
                float mouseKeyWidth = (bodyWidth - sh) / 2f;
                float x = originX;
                rects.add(new KeyRect(KeyId.LMB, x, cursorY, mouseKeyWidth, kh));
                rects.add(new KeyRect(KeyId.RMB, x + mouseKeyWidth + sh, cursorY, mouseKeyWidth, kh));
            } else {
                float x = originX + (bodyWidth - kw) / 2f;
                rects.add(new KeyRect(lmbOn ? KeyId.LMB : KeyId.RMB, x, cursorY, kw, kh));
            }
            cursorY += kh;
            anyRowYet = true;
        }

        if (spaceOn) {
            if (anyRowYet) cursorY += sv;
            rects.add(new KeyRect(KeyId.SPACE, originX, cursorY, bodyWidth, kh));
            cursorY += kh;
            anyRowYet = true;
        }

        float height = anyRowYet ? (cursorY - originY) : kh;
        return new Result(rects, bodyWidth, height);
    }
}
