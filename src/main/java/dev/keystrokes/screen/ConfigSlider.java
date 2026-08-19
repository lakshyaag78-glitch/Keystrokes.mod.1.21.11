package dev.keystrokes.screen;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.text.DecimalFormat;
import java.util.function.DoubleConsumer;

/**
 * A generic labeled slider for a double value in [min, max]. Handles the
 * value <-> normalized-progress conversion and calls back on every change,
 * so each setting in the config screen only needs one line to wire up.
 */
public final class ConfigSlider extends SliderWidget {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.##");

    private final String labelPrefix;
    private final String suffix;
    private final double min;
    private final double max;
    private final DoubleConsumer onChange;

    public ConfigSlider(int x, int y, int width, int height, String labelPrefix, String suffix,
                         double min, double max, double initialValue, DoubleConsumer onChange) {
        super(x, y, width, height, Text.literal(""), progressFor(min, max, initialValue));
        this.labelPrefix = labelPrefix;
        this.suffix = suffix;
        this.min = min;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }

    private static double progressFor(double min, double max, double value) {
        if (max <= min) {
            return 0;
        }
        return (value - min) / (max - min);
    }

    private double currentValue() {
        return min + (max - min) * this.value;
    }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal(labelPrefix + FORMAT.format(currentValue()) + suffix));
    }

    @Override
    protected void applyValue() {
        onChange.accept(currentValue());
    }
}
