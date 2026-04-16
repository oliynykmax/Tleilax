package com.example.tleilax.simulation;

public enum TreeVariant {
    LOW(1),
    MEDIUM(2),
    TALL(3);

    private final int baseBlockingHeight;

    TreeVariant(int baseBlockingHeight) {
        this.baseBlockingHeight = baseBlockingHeight;
    }

    public int getBaseBlockingHeight() {
        return baseBlockingHeight;
    }
}
