package com.letters.hitboxes.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Simple float slider: label + value, calls the setter on every change. */
public class FloatSlider extends AbstractSliderButton {

    private final String label;
    private final String suffix;
    private final float min;
    private final float max;
    private final Consumer<Float> setter;

    public FloatSlider(int x, int y, int width, int height, String label, String suffix,
                       float min, float max, float value, Consumer<Float> setter) {
        super(x, y, width, height, Component.literal(label), toSliderValue(value, min, max));
        this.label = label;
        this.suffix = suffix;
        this.min = min;
        this.max = max;
        this.setter = setter;
        updateMessage();
    }

    private static double toSliderValue(float value, float min, float max) {
        if (max <= min) return 0.0D;
        double normalised = (value - min) / (max - min);
        return Math.max(0.0D, Math.min(1.0D, normalised));
    }

    public float floatValue() {
        return (float) (min + this.value * (max - min));
    }

    @Override
    protected void updateMessage() {
        if (label == null) return;
        setMessage(Component.literal(label + ": " + suffix + String.format(java.util.Locale.ROOT, "%.2f", floatValue())));
    }

    @Override
    protected void applyValue() {
        if (setter == null) return;
        setter.accept(floatValue());
    }
}
