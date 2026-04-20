package com.example.tleilax.model;

import androidx.annotation.NonNull;

public abstract class Entity {

    private float preciseX;
    private float preciseY;
    private int energy;
    private int health;
    @NonNull
    private final EntityType type;
    private final int maxEnergy;
    private final int reproductionThreshold;

    protected Entity(int x, int y, @NonNull EntityType type) {
        this.preciseX = toCenterCoordinate(x);
        this.preciseY = toCenterCoordinate(y);
        this.type = type;
        this.maxEnergy = type.getMaxEnergy();
        this.reproductionThreshold = type.getReproductionThreshold();
        this.energy = maxEnergy;
        this.health = type.getMaxHealth();
    }

    @NonNull
    public static Entity create(@NonNull EntityType type, int x, int y) {
        return switch (type.getFamily()) {
            case PREDATOR -> new Predator(x, y, type);
            case PREY -> new Prey(x, y, type);
            case PLANT -> throw new IllegalArgumentException("Plants are stored in tile layers, not as entities.");
        };
    }

    @NonNull
    public static Entity restore(@NonNull EntityType type, int x, int y, int energy, int health) {
        Entity entity = create(type, x, y);
        entity.setEnergy(energy);
        entity.setHealth(health);
        return entity;
    }

    @NonNull
    public EntityType getType() {
        return type;
    }

    public int getX() {
        return (int) Math.floor(preciseX);
    }

    public void setX(int x) {
        this.preciseX = toCenterCoordinate(x);
    }

    public int getY() {
        return (int) Math.floor(preciseY);
    }

    public void setY(int y) {
        this.preciseY = toCenterCoordinate(y);
    }

    public float getPreciseX() {
        return preciseX;
    }

    public float getPreciseY() {
        return preciseY;
    }

    public void setPrecisePosition(float preciseX, float preciseY) {
        this.preciseX = preciseX;
        this.preciseY = preciseY;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, maxEnergy));
    }

    public void changeEnergy(int delta) {
        setEnergy(energy + delta);
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(health, type.getMaxHealth()));
    }

    public void changeHealth(int delta) {
        setHealth(health + delta);
    }

    public int getReproductionThreshold() {
        return reproductionThreshold;
    }

    public boolean isAlive() {
        return energy > 0 && health > 0;
    }

    public boolean canReproduce() {
        return reproductionThreshold > 0 && energy >= reproductionThreshold;
    }

    public int getAttackPower() {
        return type.getAttackPower();
    }

    public int getAttackRange() {
        return type.getAttackRange();
    }

    public int getClearanceHeight() {
        return type.getClearanceHeight();
    }

    public boolean canConsume(@NonNull com.example.tleilax.simulation.ResourceKind resourceKind) {
        return type.getConsumableResources().contains(resourceKind);
    }

    public boolean canTrampleBerryBush() {
        return type.canTrampleBerryBush();
    }

    @NonNull
    public Entity spawnOffspring(int x, int y) {
        return create(type, x, y);
    }

    private static float toCenterCoordinate(int tileCoordinate) {
        return tileCoordinate + 0.5f;
    }
}
