package com.example.tleilax.model;

public abstract class Animal extends Entity {

    private final int speed;
    private final int visionRange;

    protected Animal(int x, int y, EntityType type) {
        super(x, y, type);
        this.speed = type.getSpeed();
        this.visionRange = type.getVisionRange();
    }

    public int getSpeed() {
        return speed;
    }

    public int getVisionRange() {
        return visionRange;
    }
}
