package dev.keystrokes.hud;

import dev.keystrokes.config.KeystrokesConfig;
import dev.keystrokes.util.MathUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Stateless drawing helpers for one key box. Everything here is plain
 * {@link DrawContext#fill} calls (no dynamic textures, no per-frame
 * allocations) so it stays cheap even with several keys on screen at once -
 * the anti-aliased corners add a small constant number of extra fill calls
 * per key (roughly one per pixel-row of the corner radius), not a per-pixel
 * blend, to keep this lightweight.
 */
public final class KeyRenderer {

    private KeyRenderer() {
    }

    /**
     * @param x,y      top-left corner of the box, in the *already scaled* space
     * @param width    box width in the already-scaled space
     * @param height   box height in the already-scaled space
     * @param label    text to draw centered in the box (e.g. "W", "LMB")
     * @param subLabel optional second line (e.g. "CPS: 7"), or null
     * @param progress 0 (released) .. ~1 (fully pressed; profiles may overshoot slightly) animation progress
     */
    public static void draw(DrawContext ctx, KeystrokesConfig cfg, int x, int y, int width, int height,
                             String label, String subLabel, float progress) {
        TextRenderer font = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        float colorT = MathUtils.clamp(progress, 0f, 1f);

        // Radius scales with the global HUD scale only (not with this particular key's
        // width), so WASD, mouse buttons and the space bar all read as the same shape
        // family regardless of their differing widths.
        int radius = Math.round(cfg.cornerRadius * cfg.scale);
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

        int bg = withAlpha(MathUtils.lerpColor(cfg.backgroundColor, cfg.backgroundPressedColor, colorT),
                MathUtils.lerp(cfg.backgroundOpacity, cfg.backgroundPressedOpacity, colorT));

        if (cfg.borderEnabled) {
            int border = withAlpha(cfg.borderColor, cfg.borderOpacity);
            int t = Math.max(1, Math.round(cfg.borderThickness * cfg.scale));
            fillRoundedRect(ctx, x, y, width, height, radius, border);
            fillRoundedRect(ctx, x + t, y + t, width - 2 * t, height - 2 * t, Math.max(0, radius - t), bg);
        } else {
            fillRoundedRect(ctx, x, y, width, height, radius, bg);
        }

        int textColor = withAlpha(MathUtils.lerpColor(cfg.textColor, cfg.textPressedColor, colorT), 1f);

        // Text scales with both the user's text-size preference AND the global HUD
        // scale, so labels stay proportionate to their (already-scaled) box at every
        // HUD scale from 50% to 200% instead of only the box growing.
        float textScale = cfg.textSize * cfg.scale;
        float innerPad = Math.max(2f, 4f * cfg.scale);
        int labelWidth = font.getWidth(label);
        float labelLocalX = switch (cfg.textAlign) {
            case LEFT -> innerPad / textScale;
            case RIGHT -> (width - innerPad) / textScale - labelWidth;
            case CENTER -> (width / textScale - labelWidth) / 2f;
        };

        // Stack main label + optional sub-label as one vertically-centered text block,
        // using real (scaled) font metrics rather than fixed pixel offsets. Fixed offsets
        // like "4 * scale" don't grow with font size, so at small HUD scales the two lines
        // would overlap and at large scales they'd drift apart with an oddly big gap - this
        // instead keeps a constant, deliberate gap between the two baselines at every scale.
        float subScale = textScale * 0.8f;
        float mainLineHeight = font.fontHeight * textScale;
        float subLineHeight = subLabel != null ? font.fontHeight * subScale : 0f;
        float lineGap = subLabel != null ? Math.max(1f, 1.5f * cfg.scale) : 0f;
        float blockHeight = mainLineHeight + (subLabel != null ? lineGap + subLineHeight : 0f);
        float blockTop = y + (height - blockHeight) / 2f;

        int mainBaseY = Math.round(blockTop + mainLineHeight / 2f);
        int labelBaseX = Math.round(x);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(labelBaseX, mainBaseY);
        ctx.getMatrices().scale(textScale, textScale);
        ctx.drawText(font, label, Math.round(labelLocalX), -font.fontHeight / 2, textColor, false);
        ctx.getMatrices().popMatrix();

        if (subLabel != null) {
            int subWidth = font.getWidth(subLabel);
            int subColor = withAlpha(cfg.textColor, 0.85f);
            int subBaseY = Math.round(blockTop + mainLineHeight + lineGap + subLineHeight / 2f);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(Math.round(x + width / 2f), subBaseY);
            ctx.getMatrices().scale(subScale, subScale);
            ctx.drawText(font, subLabel, -subWidth / 2, -font.fontHeight / 2, subColor, false);
            ctx.getMatrices().popMatrix();
        }
    }

    private static int withAlpha(int rgb, float opacity) {
        int a = Math.round(MathUtils.clamp(opacity, 0f, 1f) * 255f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    /**
     * Fills a rectangle with rounded corners using per-row rasterization (no
     * textures/shaders needed - stays cheap on any GPU). The boundary pixel of
     * each corner row is drawn with coverage-weighted alpha (a lightweight
     * analytic antialiasing pass) instead of a hard on/off step, which is what
     * previously produced visibly jagged corners at most scales.
     */
    public static void fillRoundedRect(DrawContext ctx, int x, int y, int width, int height, int radius, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));

        if (radius == 0) {
            ctx.fill(x, y, x + width, y + height, argb);
            return;
        }

        int baseAlpha = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;

        // Center band (full height) between the left/right corner columns.
        ctx.fill(x + radius, y, x + width - radius, y + height, argb);
        // Left/right bands for the straight middle section (between the corner arcs).
        ctx.fill(x, y + radius, x + radius, y + height - radius, argb);
        ctx.fill(x + width - radius, y + radius, x + width, y + height - radius, argb);

        for (int row = 0; row < radius; row++) {
            // Sample at the pixel's vertical center for a correct analytic edge position.
            float dyFromCenter = radius - row - 0.5f;
            float distSq = (float) radius * radius - dyFromCenter * dyFromCenter;
            float dx = distSq > 0f ? (float) Math.sqrt(distSq) : 0f;
            float edgeXf = radius - dx; // exact (fractional) boundary column, from the corner's outer edge

            int insetFull = (int) Math.floor(edgeXf); // first column that is (at least partly) inside the arc
            float boundaryCoverage = 1f - (edgeXf - insetFull); // how much of that column is actually inside

            int topY = y + row;
            int bottomY = y + height - 1 - row;

            drawCornerRow(ctx, x, x + width, topY, radius, insetFull, boundaryCoverage, rgb, baseAlpha);
            if (bottomY != topY) {
                drawCornerRow(ctx, x, x + width, bottomY, radius, insetFull, boundaryCoverage, rgb, baseAlpha);
            }
        }
    }

    private static void drawCornerRow(DrawContext ctx, int left, int right, int rowY, int radius,
                                       int insetFull, float boundaryCoverage, int rgb, int baseAlpha) {
        // Solid interior of the arc on this row (fully inside the circle).
        if (insetFull + 1 < radius) {
            ctx.fill(left + insetFull + 1, rowY, left + radius, rowY + 1, (baseAlpha << 24) | rgb);
            ctx.fill(right - radius, rowY, right - insetFull - 1, rowY + 1, (baseAlpha << 24) | rgb);
        }
        // The single boundary pixel, blended by its analytic coverage instead of drawn solid.
        if (insetFull < radius) {
            int a = Math.round(baseAlpha * MathUtils.clamp(boundaryCoverage, 0f, 1f));
            if (a > 0) {
                int argb = (a << 24) | rgb;
                ctx.fill(left + insetFull, rowY, left + insetFull + 1, rowY + 1, argb);
                ctx.fill(right - insetFull - 1, rowY, right - insetFull, rowY + 1, argb);
            }
        }
    }
}
