package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Animal;
import com.example.tleilax.model.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TickLogic {

    private static final float GRASS_SPREAD_CHANCE = 0.11f;
    private static final float TREE_SPREAD_CHANCE = 0.03f;
    private static final int GRASS_ENERGY_GAIN = 12;
    private static final int BERRY_ENERGY_GAIN = 10;

    private final Random random;

    public TickLogic() {
        this(new Random());
    }

    public TickLogic(@NonNull Random random) {
        this.random = random;
    }

    public void advance(@NonNull Grid grid) {
        advancePlantLayers(grid);

        List<Entity> animals = new ArrayList<>(grid.getAnimals());
        Collections.shuffle(animals, random);
        for (Entity entity : animals) {
            if (grid.getAnimal(entity.getX(), entity.getY()) != entity) {
                continue;
            }
            handleAnimalTurn(grid, entity);
        }
    }

    private void advancePlantLayers(@NonNull Grid grid) {
        List<Grid.Position> grassSeeds = new ArrayList<>();
        List<Grid.Position> treeSeeds = new ArrayList<>();

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Tile tile = grid.getTile(x, y);
                if (tile == null) {
                    continue;
                }
                if (tile.hasGrass() && random.nextFloat() < GRASS_SPREAD_CHANCE) {
                    grassSeeds.add(new Grid.Position(x, y));
                }

                PlantState plantState = tile.getPlantState();
                if (plantState == null) {
                    continue;
                }
                plantState.advanceTick();
                if (plantState.isReadyToClear()) {
                    grid.clearPlant(x, y);
                    continue;
                }
                if (plantState.canSpreadTree() && random.nextFloat() < TREE_SPREAD_CHANCE) {
                    treeSeeds.add(new Grid.Position(x, y));
                }
            }
        }

        for (Grid.Position position : grassSeeds) {
            spreadGrass(grid, position);
        }
        for (Grid.Position position : treeSeeds) {
            spreadTree(grid, position);
        }
    }

    private void handleAnimalTurn(@NonNull Grid grid, @NonNull Entity entity) {
        entity.changeEnergy(-1);
        if (!entity.isAlive()) {
            grid.removeAnimal(entity);
            return;
        }

        if (consumeCurrentTileResource(grid, entity)) {
            return;
        }

        Entity target = findAdjacentTarget(grid, entity);
        if (target != null) {
            target.changeHealth(-entity.getAttackPower());
            if (!target.isAlive()) {
                grid.removeAnimal(target);
            }
            return;
        }

        moveRandomly(grid, entity);
    }

    private boolean consumeCurrentTileResource(@NonNull Grid grid, @NonNull Entity entity) {
        Tile tile = grid.getTile(entity.getX(), entity.getY());
        if (tile == null) {
            return false;
        }
        if (tile.hasGrass() && entity.canConsume(ResourceKind.GRASS)) {
            tile.clearGrass();
            entity.changeEnergy(GRASS_ENERGY_GAIN);
            return true;
        }
        PlantState plantState = tile.getPlantState();
        if (plantState != null
                && plantState.supportsResource(ResourceKind.BERRIES)
                && plantState.getBerryAmount() > 0
                && entity.canConsume(ResourceKind.BERRIES)) {
            plantState.harvestBerry();
            entity.changeEnergy(BERRY_ENERGY_GAIN);
            return true;
        }
        return false;
    }

    @Nullable
    private Entity findAdjacentTarget(@NonNull Grid grid, @NonNull Entity entity) {
        if (!entity.getType().isPredator()) {
            return null;
        }
        for (Grid.Position position : shuffled(grid.getAdjacentPositions(entity.getX(), entity.getY()))) {
            Entity candidate = grid.getAnimal(position.x(), position.y());
            if (candidate != null && candidate.getType().isPrey()) {
                return candidate;
            }
        }
        return null;
    }

    private void moveRandomly(@NonNull Grid grid, @NonNull Entity entity) {
        int movementSteps = Math.max(1, entity instanceof Animal animal ? animal.getSpeed() : 1);
        for (int i = 0; i < movementSteps; i++) {
            List<Grid.Position> emptyPositions = shuffled(grid.getAdjacentEmptyPositions(entity.getX(), entity.getY()));
            if (emptyPositions.isEmpty()) {
                return;
            }
            Grid.Position destination = emptyPositions.get(0);
            grid.moveAnimal(entity, destination.x(), destination.y());
        }
    }

    private void spreadGrass(@NonNull Grid grid, @NonNull Grid.Position position) {
        for (Grid.Position neighbor : shuffled(grid.getAdjacentPositions(position.x(), position.y()))) {
            Tile tile = grid.getTile(neighbor.x(), neighbor.y());
            if (tile != null && !tile.hasGrass() && (tile.getPlantState() == null || tile.getPlantState().canSpreadGrassUnderneath())) {
                grid.setGrass(neighbor.x(), neighbor.y(), 1);
                return;
            }
        }
    }

    private void spreadTree(@NonNull Grid grid, @NonNull Grid.Position position) {
        Tile sourceTile = grid.getTile(position.x(), position.y());
        if (sourceTile == null || sourceTile.getPlantState() == null || sourceTile.getPlantState().getTreeVariant() == null) {
            return;
        }
        for (Grid.Position neighbor : shuffled(grid.getAdjacentPositions(position.x(), position.y()))) {
            Tile tile = grid.getTile(neighbor.x(), neighbor.y());
            if (tile == null || tile.getPlantState() != null || !tile.hasGrass()) {
                continue;
            }
            grid.placeTree(neighbor.x(), neighbor.y(), sourceTile.getPlantState().getTreeVariant());
            return;
        }
    }

    @NonNull
    private List<Grid.Position> shuffled(@NonNull List<Grid.Position> positions) {
        List<Grid.Position> copy = new ArrayList<>(positions);
        Collections.shuffle(copy, random);
        return copy;
    }
}
