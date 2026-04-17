package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Animal;
import com.example.tleilax.model.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Advances the simulation one tick at a time.
 *
 * Plant layers (grass spread, tree spread, berry regrow) are updated first.
 * Then each animal performs exactly one primary action per tick, chosen by
 * priority:
 *
 *   1. Metabolism (energy drain — always applied)
 *   2. Reproduce  (energy ≥ threshold + random chance)
 *   3. Flee       (prey moves away from nearest visible predator)
 *   4. Feed       (consume grass / berries / tree leaves on current tile)
 *   5. Attack     (predator attacks adjacent prey)
 *   6. Chase      (predator moves toward nearest visible prey)
 *   7. Wander     (random movement)
 */
public class TickLogic {

    private static final float GRASS_SPREAD_CHANCE = 0.11f;
    private static final float TREE_SPREAD_CHANCE = 0.03f;
    private static final int GRASS_ENERGY_GAIN = 12;
    private static final int BERRY_ENERGY_GAIN = 10;
    private static final int TREE_LEAVES_ENERGY_GAIN = 8;

    private final Random random;

    public TickLogic() {
        this(new Random());
    }

    public TickLogic(@NonNull Random random) {
        this.random = random;
    }

    // ---------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------

    public void advance(@NonNull Grid grid) {
        advancePlantLayers(grid);

        List<Entity> animals = new ArrayList<>(grid.getAnimals());
        Collections.shuffle(animals, random);
        for (Entity entity : animals) {
            // Skip if the entity was removed or moved during this tick
            if (grid.getAnimal(entity.getX(), entity.getY()) != entity) {
                continue;
            }
            handleAnimalTurn(grid, entity);
        }
    }

    // ---------------------------------------------------------------
    // Plant-layer updates (unchanged from original)
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // Animal turn — one primary action per tick
    // ---------------------------------------------------------------

    private void handleAnimalTurn(@NonNull Grid grid, @NonNull Entity entity) {
        // 1. Metabolism
        entity.changeEnergy(-1);
        if (!entity.isAlive()) {
            grid.removeAnimal(entity);
            return;
        }

        // 2. Reproduce
        if (entity.canReproduce()
                && random.nextFloat() < entity.getType().getReproductionChance()) {
            if (reproduce(grid, entity)) return;
        }

        // 3. Flee (prey only — overrides feeding when predator is nearby)
        if (entity.getType().isPrey() && fleeFromPredator(grid, entity)) return;

        // 4. Feed
        if (consumeCurrentTileResource(grid, entity)) return;

        // 5. Attack (predators only — adjacent prey)
        if (entity.getType().isPredator()) {
            Entity target = findAdjacentTarget(grid, entity);
            if (target != null) {
                attack(grid, entity, target);
                return;
            }
        }

        // 6. Chase (predators only — move toward nearest visible prey)
        if (entity.getType().isPredator() && chasePrey(grid, entity)) return;

        // 7. Wander
        moveRandomly(grid, entity);
    }

    // ---------------------------------------------------------------
    // 2. Reproduction
    // ---------------------------------------------------------------

    private boolean reproduce(@NonNull Grid grid, @NonNull Entity parent) {
        List<Grid.Position> emptyPositions =
                shuffled(grid.getAdjacentEmptyPositions(parent.getX(), parent.getY(), parent));
        if (emptyPositions.isEmpty()) return false;

        Grid.Position pos = emptyPositions.get(0);
        Entity offspring = parent.spawnOffspring(pos.x(), pos.y());
        if (grid.placeAnimal(offspring)) {
            parent.changeEnergy(-parent.getReproductionThreshold() / 2);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------
    // 3. Feeding
    // ---------------------------------------------------------------

    private boolean consumeCurrentTileResource(@NonNull Grid grid, @NonNull Entity entity) {
        Tile tile = grid.getTile(entity.getX(), entity.getY());
        if (tile == null) return false;

        // Grass
        if (tile.hasGrass() && entity.canConsume(ResourceKind.GRASS)) {
            tile.clearGrass();
            entity.changeEnergy(GRASS_ENERGY_GAIN);
            return true;
        }

        PlantState plantState = tile.getPlantState();
        if (plantState == null) return false;

        // Berries
        if (plantState.supportsResource(ResourceKind.BERRIES)
                && plantState.getBerryAmount() > 0
                && entity.canConsume(ResourceKind.BERRIES)) {
            plantState.harvestBerry();
            entity.changeEnergy(BERRY_ENERGY_GAIN);
            return true;
        }

        // Tree leaves — deer browses without killing the tree (small durability drain)
        if (plantState.supportsResource(ResourceKind.TREE_LEAVES)
                && entity.canConsume(ResourceKind.TREE_LEAVES)) {
            entity.changeEnergy(TREE_LEAVES_ENERGY_GAIN);
            plantState.damage(1);
            if (plantState.isReadyToClear()) {
                grid.clearPlant(entity.getX(), entity.getY());
            }
            return true;
        }

        return false;
    }

    // ---------------------------------------------------------------
    // 4. Attack
    // ---------------------------------------------------------------

    private void attack(@NonNull Grid grid, @NonNull Entity attacker, @NonNull Entity target) {
        target.changeHealth(-attacker.getAttackPower());
        if (!target.isAlive()) {
            // Predator gains energy from the kill
            attacker.changeEnergy(target.getEnergy() / 2);
            grid.removeAnimal(target);
        }
    }

    @Nullable
    private Entity findAdjacentTarget(@NonNull Grid grid, @NonNull Entity entity) {
        for (Grid.Position position : shuffled(grid.getAdjacentPositions(entity.getX(), entity.getY()))) {
            Entity candidate = grid.getAnimal(position.x(), position.y());
            if (candidate != null && candidate.getType().isPrey()) {
                return candidate;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // 5 & 6. Vision-range scanning, flee, and chase
    // ---------------------------------------------------------------

    /**
     * Scans the grid within the entity's vision range and returns the nearest
     * entity matching the given family filter. Uses Chebyshev distance.
     */
    @Nullable
    private Entity scanForEntity(@NonNull Grid grid, @NonNull Entity entity,
                                 boolean searchPredator) {
        int range = entity instanceof Animal animal ? animal.getVisionRange() : 0;
        if (range <= 0) return null;

        Entity closest = null;
        int minDist = Integer.MAX_VALUE;

        for (int dy = -range; dy <= range; dy++) {
            for (int dx = -range; dx <= range; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = entity.getX() + dx;
                int ny = entity.getY() + dy;
                Entity candidate = grid.getAnimal(nx, ny);
                if (candidate == null || !candidate.isAlive()) continue;

                boolean match = searchPredator
                        ? candidate.getType().isPredator()
                        : candidate.getType().isPrey();
                if (!match) continue;

                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                if (dist < minDist) {
                    minDist = dist;
                    closest = candidate;
                }
            }
        }
        return closest;
    }

    private boolean fleeFromPredator(@NonNull Grid grid, @NonNull Entity entity) {
        Entity predator = scanForEntity(grid, entity, true);
        if (predator == null) return false;
        return moveDirected(grid, entity, predator, true);
    }

    private boolean chasePrey(@NonNull Grid grid, @NonNull Entity entity) {
        Entity prey = scanForEntity(grid, entity, false);
        if (prey == null) return false;
        return moveDirected(grid, entity, prey, false);
    }

    /**
     * Moves the entity up to #speed steps toward or away from the target.
     * Each step picks the adjacent empty tile that minimises (chase) or
     * maximises (flee) Chebyshev distance to the target.  If the best
     * candidate is blocked (e.g. by a tree), falls through to the next-best.
     */
    private boolean moveDirected(@NonNull Grid grid, @NonNull Entity entity,
                                 @NonNull Entity target, boolean flee) {
        int steps = Math.max(1, entity instanceof Animal animal ? animal.getSpeed() : 1);
        boolean moved = false;

        for (int i = 0; i < steps; i++) {
            List<Grid.Position> candidates =
                    grid.getAdjacentEmptyPositions(entity.getX(), entity.getY(), entity);
            if (candidates.isEmpty()) break;

            // Sort candidates: flee → farthest first; chase → nearest first
            candidates.sort((a, b) -> {
                int distA = Math.max(Math.abs(a.x() - target.getX()), Math.abs(a.y() - target.getY()));
                int distB = Math.max(Math.abs(b.x() - target.getX()), Math.abs(b.y() - target.getY()));
                return flee ? Integer.compare(distB, distA) : Integer.compare(distA, distB);
            });

            boolean stepped = false;
            for (Grid.Position pos : candidates) {
                if (grid.moveAnimal(entity, pos.x(), pos.y())) {
                    moved = true;
                    stepped = true;
                    break;
                }
            }
            if (!stepped) break;
        }
        return moved;
    }

    // ---------------------------------------------------------------
    // 7. Random movement
    // ---------------------------------------------------------------

    private void moveRandomly(@NonNull Grid grid, @NonNull Entity entity) {
        int movementSteps = Math.max(1, entity instanceof Animal animal ? animal.getSpeed() : 1);
        for (int i = 0; i < movementSteps; i++) {
            List<Grid.Position> emptyPositions =
                    shuffled(grid.getAdjacentEmptyPositions(entity.getX(), entity.getY(), entity));
            if (emptyPositions.isEmpty()) return;
            Grid.Position destination = emptyPositions.get(0);
            grid.moveAnimal(entity, destination.x(), destination.y());
        }
    }

    // ---------------------------------------------------------------
    // Plant spread helpers (unchanged from original)
    // ---------------------------------------------------------------

    private void spreadGrass(@NonNull Grid grid, @NonNull Grid.Position position) {
        for (Grid.Position neighbor : shuffled(grid.getAdjacentPositions(position.x(), position.y()))) {
            Tile tile = grid.getTile(neighbor.x(), neighbor.y());
            if (tile != null
                    && !tile.hasGrass()
                    && (tile.getPlantState() == null || tile.getPlantState().canSpreadGrassUnderneath())) {
                grid.setGrass(neighbor.x(), neighbor.y(), 1);
                return;
            }
        }
    }

    private void spreadTree(@NonNull Grid grid, @NonNull Grid.Position position) {
        Tile sourceTile = grid.getTile(position.x(), position.y());
        if (sourceTile == null
                || sourceTile.getPlantState() == null
                || sourceTile.getPlantState().getTreeVariant() == null) {
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

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------

    @NonNull
    private List<Grid.Position> shuffled(@NonNull List<Grid.Position> positions) {
        List<Grid.Position> copy = new ArrayList<>(positions);
        Collections.shuffle(copy, random);
        return copy;
    }
}
