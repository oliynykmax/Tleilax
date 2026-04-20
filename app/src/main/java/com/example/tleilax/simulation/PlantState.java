package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

public class PlantState {

    private static final int DEFAULT_BERRY_BUSH_DURABILITY = 4;
    private static final int DEFAULT_BERRY_CAPACITY = 6;
    private static final int DEFAULT_BERRY_REGROW_TICKS = 6;
    private static final int DEFAULT_BERRY_LIFETIME_TICKS = 180;
    private static final int SAPLING_TICKS = 18;
    private static final int MATURE_TICKS = 40;
    private static final int OLD_TICKS = 26;
    private static final int DEAD_TICKS = 22;

    @NonNull
    private final PlantType plantType;
    private int durability;
    private int maxDurability;
    private int blockingHeight;
    private boolean dead;
    private boolean blocksGroundGrowth;
    private int berryAmount;
    private int berryCapacity;
    private int berryRegrowTicks;
    private int berryRegrowProgress;
    @Nullable
    private TreeVariant treeVariant;
    @Nullable
    private TreeLifeStage treeLifeStage;
    private int lifecycleTicksRemaining;

    private PlantState(@NonNull PlantType plantType) {
        this.plantType = plantType;
    }

    @NonNull
    public static PlantState createBerryBush() {
        PlantState plantState = new PlantState(PlantType.BERRY_BUSH);
        plantState.durability = DEFAULT_BERRY_BUSH_DURABILITY;
        plantState.maxDurability = DEFAULT_BERRY_BUSH_DURABILITY;
        plantState.blockingHeight = 1;
        plantState.berryCapacity = DEFAULT_BERRY_CAPACITY;
        plantState.berryAmount = DEFAULT_BERRY_CAPACITY;
        plantState.berryRegrowTicks = DEFAULT_BERRY_REGROW_TICKS;
        plantState.lifecycleTicksRemaining = DEFAULT_BERRY_LIFETIME_TICKS;
        return plantState;
    }

    @NonNull
    public static PlantState createBerryBushWithRandomizedLifecycle(@NonNull Random random) {
        PlantState plantState = createBerryBush();
        int minTicks = Math.max(24, Math.round(DEFAULT_BERRY_LIFETIME_TICKS * 0.45f));
        plantState.lifecycleTicksRemaining = randomRange(random, minTicks, DEFAULT_BERRY_LIFETIME_TICKS);
        return plantState;
    }

    @NonNull
    public static PlantState createTree(@NonNull TreeVariant treeVariant) {
        PlantState plantState = new PlantState(PlantType.TREE);
        plantState.treeVariant = treeVariant;
        plantState.durability = 10;
        plantState.maxDurability = 10;
        plantState.blocksGroundGrowth = true;
        plantState.updateTreeStage(TreeLifeStage.SAPLING);
        return plantState;
    }

    @NonNull
    public static PlantState createTreeWithRandomizedLifecycle(
            @NonNull TreeVariant treeVariant,
            @NonNull Random random
    ) {
        PlantState plantState = createTree(treeVariant);
        TreeLifeStage initialStage = chooseInitialTreeStage(random);
        plantState.updateTreeStage(initialStage);
        int stageDuration = switch (initialStage) {
            case SAPLING -> SAPLING_TICKS;
            case MATURE -> MATURE_TICKS;
            case OLD -> OLD_TICKS;
            case DEAD -> DEAD_TICKS;
        };
        int minTicks = Math.max(1, Math.round(stageDuration * 0.35f));
        plantState.lifecycleTicksRemaining = randomRange(random, minTicks, stageDuration);
        return plantState;
    }

    @NonNull
    public static PlantState restoreBerryBush(
            int durability,
            int berryAmount,
            int berryRegrowProgress,
            int lifecycleTicksRemaining,
            boolean dead
    ) {
        PlantState plantState = createBerryBush();
        plantState.durability = Math.max(0, Math.min(durability, plantState.maxDurability));
        plantState.berryAmount = Math.max(0, Math.min(berryAmount, plantState.berryCapacity));
        plantState.berryRegrowProgress = Math.max(0, berryRegrowProgress);
        plantState.dead = dead;
        if (dead) {
            plantState.lifecycleTicksRemaining = Math.max(0, lifecycleTicksRemaining);
            plantState.berryAmount = 0;
        } else {
            plantState.lifecycleTicksRemaining = lifecycleTicksRemaining > 0
                    ? lifecycleTicksRemaining
                    : DEFAULT_BERRY_LIFETIME_TICKS;
        }
        return plantState;
    }

    @NonNull
    public static PlantState restoreTree(
            @NonNull TreeVariant treeVariant,
            @NonNull TreeLifeStage treeLifeStage,
            int durability,
            int lifecycleTicksRemaining,
            boolean dead
    ) {
        PlantState plantState = createTree(treeVariant);
        plantState.updateTreeStage(treeLifeStage);
        plantState.durability = Math.max(0, Math.min(durability, plantState.maxDurability));
        plantState.dead = dead || treeLifeStage == TreeLifeStage.DEAD;
        plantState.lifecycleTicksRemaining = Math.max(0, lifecycleTicksRemaining);
        return plantState;
    }

    @NonNull
    public PlantType getPlantType() {
        return plantType;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getBlockingHeight() {
        return blockingHeight;
    }

    public boolean isDead() {
        return dead;
    }

    public boolean blocksGroundGrowth() {
        return blocksGroundGrowth;
    }

    public int getBerryAmount() {
        return berryAmount;
    }

    public int getBerryCapacity() {
        return berryCapacity;
    }

    public int getBerryRegrowTicks() {
        return berryRegrowTicks;
    }

    public int getBerryRegrowProgress() {
        return berryRegrowProgress;
    }

    @Nullable
    public TreeVariant getTreeVariant() {
        return treeVariant;
    }

    @Nullable
    public TreeLifeStage getTreeLifeStage() {
        return treeLifeStage;
    }

    public int getLifecycleTicksRemaining() {
        return lifecycleTicksRemaining;
    }

    public boolean canSpreadGrassUnderneath() {
        return !blocksGroundGrowth;
    }

    public boolean canSpreadTree() {
        return plantType == PlantType.TREE && treeLifeStage == TreeLifeStage.MATURE && !dead;
    }

    public boolean canRegrowBerries() {
        return plantType == PlantType.BERRY_BUSH && !dead && berryAmount < berryCapacity;
    }

    public boolean canSpreadBerryBush() {
        return plantType == PlantType.BERRY_BUSH && !dead;
    }

    public boolean supportsResource(@NonNull ResourceKind resourceKind) {
        if (resourceKind == ResourceKind.BERRIES && plantType == PlantType.BERRY_BUSH && !dead) return true;
        if (resourceKind == ResourceKind.TREE_LEAVES && plantType == PlantType.TREE && !dead && durability > 0) return true;
        return false;
    }

    public void damage(int amount) {
        durability = Math.max(0, durability - amount);
        if (durability == 0 && plantType == PlantType.BERRY_BUSH) {
            dead = true;
            berryAmount = 0;
            lifecycleTicksRemaining = DEAD_TICKS;
        }
    }

    public void harvestBerry() {
        if (berryAmount > 0) {
            berryAmount--;
            berryRegrowProgress = 0;
        }
    }

    public void advanceTick() {
        if (plantType == PlantType.BERRY_BUSH) {
            advanceBerryBushTick();
            return;
        }
        advanceTreeTick();
    }

    public boolean isReadyToClear() {
        return plantType == PlantType.BERRY_BUSH
                ? dead && lifecycleTicksRemaining == 0
                : treeLifeStage == TreeLifeStage.DEAD && lifecycleTicksRemaining == 0;
    }

    private void advanceBerryBushTick() {
        if (dead) {
            lifecycleTicksRemaining = Math.max(0, lifecycleTicksRemaining - 1);
            return;
        }
        lifecycleTicksRemaining = Math.max(0, lifecycleTicksRemaining - 1);
        if (lifecycleTicksRemaining == 0) {
            dead = true;
            berryAmount = 0;
            berryRegrowProgress = 0;
            lifecycleTicksRemaining = DEAD_TICKS;
            return;
        }
        if (berryAmount < berryCapacity) {
            berryRegrowProgress++;
            if (berryRegrowProgress >= berryRegrowTicks) {
                berryAmount++;
                berryRegrowProgress = 0;
            }
        }
    }

    private void advanceTreeTick() {
        if (treeLifeStage == null) {
            return;
        }
        lifecycleTicksRemaining = Math.max(0, lifecycleTicksRemaining - 1);
        if (lifecycleTicksRemaining > 0) {
            return;
        }
        switch (treeLifeStage) {
            case SAPLING -> updateTreeStage(TreeLifeStage.MATURE);
            case MATURE -> updateTreeStage(TreeLifeStage.OLD);
            case OLD -> updateTreeStage(TreeLifeStage.DEAD);
            case DEAD -> lifecycleTicksRemaining = 0;
        }
    }

    private void updateTreeStage(@NonNull TreeLifeStage nextStage) {
        treeLifeStage = nextStage;
        switch (nextStage) {
            case SAPLING -> {
                dead = false;
                blockingHeight = Math.max(1, treeVariant != null ? treeVariant.getBaseBlockingHeight() - 1 : 1);
                lifecycleTicksRemaining = SAPLING_TICKS;
            }
            case MATURE -> {
                dead = false;
                blockingHeight = treeVariant != null ? treeVariant.getBaseBlockingHeight() : 1;
                lifecycleTicksRemaining = MATURE_TICKS;
            }
            case OLD -> {
                dead = false;
                blockingHeight = treeVariant != null ? treeVariant.getBaseBlockingHeight() + 1 : 2;
                lifecycleTicksRemaining = OLD_TICKS;
            }
            case DEAD -> {
                dead = true;
                blockingHeight = Math.max(1, treeVariant != null ? treeVariant.getBaseBlockingHeight() - 1 : 1);
                lifecycleTicksRemaining = DEAD_TICKS;
            }
        }
    }

    @NonNull
    private static TreeLifeStage chooseInitialTreeStage(@NonNull Random random) {
        float roll = random.nextFloat();
        if (roll < 0.45f) {
            return TreeLifeStage.SAPLING;
        }
        if (roll < 0.82f) {
            return TreeLifeStage.MATURE;
        }
        return TreeLifeStage.OLD;
    }

    private static int randomRange(@NonNull Random random, int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }
}
