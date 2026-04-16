package com.example.tleilax.simulation;

import androidx.annotation.ColorInt;

public enum PlantType {
    BERRY_BUSH(0xFFD65D0E),
    TREE(0xFF076678);

    private final int renderColor;

    PlantType(@ColorInt int renderColor) {
        this.renderColor = renderColor;
    }

    @ColorInt
    public int getRenderColor() {
        return renderColor;
    }
}
