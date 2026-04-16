package com.example.tleilax.model;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.example.tleilax.simulation.ResourceKind;

import java.util.Set;

public enum EntityType {
    WOLF("Wolf", Family.PREDATOR, 90, 60, 16, 3, 6, 24, 16, 2, false, Set.of(), 0xFFCC241D),
    RABBIT("Rabbit", Family.PREY, 55, 34, 4, 2, 5, 14, 10, 1, false, Set.of(ResourceKind.GRASS), 0xFFD79921),
    MOUSE("Mouse", Family.PREY, 45, 28, 3, 3, 3, 10, 8, 1, true, Set.of(ResourceKind.GRASS, ResourceKind.BERRIES), 0xFFBDAE93),
    DEER("Deer", Family.PREY, 70, 44, 5, 1, 4, 20, 14, 3, true, Set.of(ResourceKind.GRASS, ResourceKind.BERRIES), 0xFF689D6A),
    GRASS("Grass", Family.PLANT, 0, 0, 0, 0, 0, 0, 0, 0, false, Set.of(), 0xFF98971A),
    BERRY_BUSH("Berry Bush", Family.PLANT, 0, 0, 0, 0, 0, 0, 0, 1, false, Set.of(), 0xFFD65D0E),
    TREE("Tree", Family.PLANT, 0, 0, 0, 0, 0, 0, 0, 3, false, Set.of(), 0xFF076678);

    private final String displayName;
    private final Family family;
    private final int maxEnergy;
    private final int reproductionThreshold;
    private final int attackPower;
    private final int speed;
    private final int visionRange;
    private final int maxHealth;
    private final int attackRange;
    private final int clearanceHeight;
    private final boolean canTrampleBerryBush;
    @NonNull
    private final Set<ResourceKind> consumableResources;
    private final int renderColor;

    EntityType(
            @NonNull String displayName,
            @NonNull Family family,
            int maxEnergy,
            int reproductionThreshold,
            int attackPower,
            int speed,
            int visionRange,
            int maxHealth,
            int attackRange,
            int clearanceHeight,
            boolean canTrampleBerryBush,
            @NonNull Set<ResourceKind> consumableResources,
            @ColorInt int renderColor
    ) {
        this.displayName = displayName;
        this.family = family;
        this.maxEnergy = maxEnergy;
        this.reproductionThreshold = reproductionThreshold;
        this.attackPower = attackPower;
        this.speed = speed;
        this.visionRange = visionRange;
        this.maxHealth = maxHealth;
        this.attackRange = attackRange;
        this.clearanceHeight = clearanceHeight;
        this.canTrampleBerryBush = canTrampleBerryBush;
        this.consumableResources = consumableResources;
        this.renderColor = renderColor;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public Family getFamily() {
        return family;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getReproductionThreshold() {
        return reproductionThreshold;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public int getSpeed() {
        return speed;
    }

    public int getVisionRange() {
        return visionRange;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getAttackRange() {
        return attackRange;
    }

    public int getClearanceHeight() {
        return clearanceHeight;
    }

    public boolean canTrampleBerryBush() {
        return canTrampleBerryBush;
    }

    @NonNull
    public Set<ResourceKind> getConsumableResources() {
        return consumableResources;
    }

    @ColorInt
    public int getRenderColor() {
        return renderColor;
    }

    public boolean isPredator() {
        return family == Family.PREDATOR;
    }

    public boolean isPrey() {
        return family == Family.PREY;
    }

    public boolean isPlant() {
        return family == Family.PLANT;
    }

    public enum Family {
        PREDATOR,
        PREY,
        PLANT
    }
}
