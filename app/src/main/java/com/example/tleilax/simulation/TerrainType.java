package com.example.tleilax.simulation;

import androidx.annotation.ColorInt;

public enum TerrainType {
    SAND(0xFFB08968);

    private final int renderColor;

    TerrainType(@ColorInt int renderColor) {
        this.renderColor = renderColor;
    }

    @ColorInt
    public int getRenderColor() {
        return renderColor;
    }
}
