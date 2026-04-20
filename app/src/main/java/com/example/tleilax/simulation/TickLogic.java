package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Animal;
import com.example.tleilax.model.Entity;
import com.example.tleilax.model.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    private static final float GRASS_SPREAD_CHANCE = 0.06f;
    private static final float TREE_SPREAD_CHANCE = 0.01f;
    private static final int GRASS_ENERGY_GAIN = 4;
    private static final int BERRY_ENERGY_GAIN = 5;
    private static final int TREE_LEAVES_ENERGY_GAIN = 2;
    private static final int PREY_FLEE_RADIUS = 1;
    private static final float TREE_BROWSE_DAMAGE_CHANCE = 0.1f;
    private static final float PREDATOR_KILL_ENERGY_RATIO = 1.25f;
    private static final int PREDATOR_METABOLISM_COST = 1;
    private static final float MIN_REPRODUCTION_MULTIPLIER = 0.2f;
    private static final float MAX_REPRODUCTION_MULTIPLIER = 2.4f;
    private static final float MOVEMENT_PER_SPEED_UNIT = 0.25f;
    private static final float ATTACK_RANGE_SCALE = 16f;

    @NonNull
    private static final Map<EntityType, Integer> TARGET_POPULATION_BY_TYPE = createTargetPopulationByType();

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
        Map<EntityType, Integer> populationByType = countPopulationByType(animals);
        Collections.shuffle(animals, random);
        for (Entity entity : animals) {
            if (grid.getAnimal(entity.getX(), entity.getY()) != entity) {
                continue;
            }
            handleAnimalTurn(grid, entity, populationByType);
        }
    }


    /**
     * Processes plant growth and spread for the entire grid.
     * This iterates over all tiles and determines where new grass and trees should spawn.
     */
    private void advancePlantLayers(@NonNull Grid grid) {
        List<Grid.Position> grassSeeds = new ArrayList<>();
        List<Grid.Position> treeSeeds = new ArrayList<>();

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                processTileForPlants(grid, x, y, grassSeeds, treeSeeds);
            }
        }

        for (Grid.Position position : grassSeeds) {
            spreadGrass(grid, position);
        }
        for (Grid.Position position : treeSeeds) {
            spreadTree(grid, position);
        }
    }

    /**
     * Checks a single tile for plant growth events, such as grass spreading or trees dropping seeds.
     * Clears dead plants if their lifecycle is complete.
     */
    private void processTileForPlants(
            @NonNull Grid grid, int x, int y,
            @NonNull List<Grid.Position> grassSeeds,
            @NonNull List<Grid.Position> treeSeeds
    ) {
        Tile tile = grid.getTile(x, y);
        if (tile == null) {
            return;
        }
        if (tile.hasGrass() && random.nextFloat() < GRASS_SPREAD_CHANCE) {
            grassSeeds.add(new Grid.Position(x, y));
        }

        PlantState plantState = tile.getPlantState();
        if (plantState == null) {
            return;
        }
        plantState.advanceTick();
        if (plantState.isReadyToClear()) {
            grid.clearPlant(x, y);
            return;
        }
        if (plantState.canSpreadTree() && random.nextFloat() < TREE_SPREAD_CHANCE) {
            treeSeeds.add(new Grid.Position(x, y));
        }
    }


    private void handleAnimalTurn(
            @NonNull Grid grid,
            @NonNull Entity entity,
            @NonNull Map<EntityType, Integer> populationByType
    ) {
        int speed = entity instanceof Animal animal ? animal.getSpeed() : 0;
        int metabolismCost = entity.getType().isPredator()
                ? PREDATOR_METABOLISM_COST
                : Math.max(2, speed);
        entity.changeEnergy(-metabolismCost);
        if (!entity.isAlive()) {
            grid.removeAnimal(entity);
            decrementPopulation(populationByType, entity.getType());
            return;
        }

        if (entity.canReproduce()
                && random.nextFloat() < getAdjustedReproductionChance(entity.getType(), populationByType)) {
            if (reproduce(grid, entity, populationByType)) return;
        }

        if (entity.getType().isPrey() && fleeFromPredator(grid, entity)) return;

        if (consumeCurrentTileResource(grid, entity)) return;

        if (entity.getType().isPredator()) {
            Entity target = findAttackTarget(grid, entity);
            if (target != null) {
                attack(grid, entity, target, populationByType);
                return;
            }
        }

        if (entity.getType().isPredator() && chasePrey(grid, entity)) {
            Entity target = findAttackTarget(grid, entity);
            if (target != null) {
                attack(grid, entity, target, populationByType);
            }
            return;
        }

        moveRandomly(grid, entity);
    }


    private boolean reproduce(
            @NonNull Grid grid,
            @NonNull Entity parent,
            @NonNull Map<EntityType, Integer> populationByType
    ) {
        List<Grid.Position> emptyPositions =
                shuffled(grid.getAdjacentEmptyPositions(parent.getX(), parent.getY(), parent));
        if (emptyPositions.isEmpty()) return false;

        Grid.Position pos = emptyPositions.get(0);
        Entity offspring = parent.spawnOffspring(pos.x(), pos.y());
        if (grid.placeAnimal(offspring)) {
            parent.changeEnergy(-parent.getReproductionThreshold() / 2);
            incrementPopulation(populationByType, offspring.getType());
            return true;
        }
        return false;
    }


    private boolean consumeCurrentTileResource(@NonNull Grid grid, @NonNull Entity entity) {
        Tile tile = grid.getTile(entity.getX(), entity.getY());
        if (tile == null) return false;

        if (tile.hasGrass() && entity.canConsume(ResourceKind.GRASS)) {
            tile.clearGrass();
            entity.changeEnergy(GRASS_ENERGY_GAIN);
            return true;
        }

        PlantState plantState = tile.getPlantState();
        if (plantState == null) return false;

        if (plantState.supportsResource(ResourceKind.BERRIES)
                && plantState.getBerryAmount() > 0
                && entity.canConsume(ResourceKind.BERRIES)) {
            plantState.harvestBerry();
            entity.changeEnergy(BERRY_ENERGY_GAIN);
            return true;
        }

        if (plantState.supportsResource(ResourceKind.TREE_LEAVES)
                && entity.canConsume(ResourceKind.TREE_LEAVES)) {
            entity.changeEnergy(TREE_LEAVES_ENERGY_GAIN);
            if (random.nextFloat() < TREE_BROWSE_DAMAGE_CHANCE) {
                plantState.damage(1);
            }
            if (plantState.isReadyToClear()) {
                grid.clearPlant(entity.getX(), entity.getY());
            }
            return true;
        }

        return false;
    }


    private void attack(
            @NonNull Grid grid,
            @NonNull Entity attacker,
            @NonNull Entity target,
            @NonNull Map<EntityType, Integer> populationByType
    ) {
        target.changeHealth(-attacker.getAttackPower());
        if (!target.isAlive()) {
            attacker.changeEnergy(Math.round(target.getEnergy() * PREDATOR_KILL_ENERGY_RATIO));
            grid.removeAnimal(target);
            decrementPopulation(populationByType, target.getType());
        }
    }

    @Nullable
    private Entity findAttackTarget(@NonNull Grid grid, @NonNull Entity entity) {
        float attackRange = getAttackDistance(entity);
        Entity bestCandidate = null;
        int bestPriority = Integer.MIN_VALUE;
        float bestDistance = Float.MAX_VALUE;
        for (Entity candidate : grid.getAnimals()) {
            if (candidate == entity || !candidate.getType().isPrey() || !candidate.isAlive()) {
                continue;
            }
            float distance = preciseDistance(entity, candidate);
            if (distance > attackRange) {
                continue;
            }
            int candidatePriority = getPreyPriority(candidate.getType());
            if (bestCandidate == null
                    || candidatePriority > bestPriority
                    || (candidatePriority == bestPriority && distance < bestDistance)) {
                bestCandidate = candidate;
                bestPriority = candidatePriority;
                bestDistance = distance;
            }
        }
        return bestCandidate;
    }


    /**
     * Scans the grid within the entity's vision range and returns the nearest
     * entity matching the given family filter. Uses Chebyshev distance.
     */
    @Nullable
    private Entity scanForEntity(@NonNull Grid grid, @NonNull Entity entity,
                                 boolean searchPredator, @Nullable Integer radiusOverride) {
        float range = radiusOverride != null
                ? radiusOverride
                : entity instanceof Animal animal ? animal.getVisionRange() : 0;
        if (range <= 0) return null;

        Entity closest = null;
        float minDist = Float.MAX_VALUE;

        for (Entity candidate : grid.getAnimals()) {
            if (candidate == entity || !candidate.isAlive()) {
                continue;
            }
            boolean match = searchPredator
                    ? candidate.getType().isPredator()
                    : candidate.getType().isPrey();
            if (!match) {
                continue;
            }

            float dist = preciseDistance(entity, candidate);
            if (dist > range) {
                continue;
            }
            if (dist < minDist
                    || (!searchPredator
                    && Math.abs(dist - minDist) < 0.0001f
                    && closest != null
                    && getPreyPriority(candidate.getType()) > getPreyPriority(closest.getType()))) {
                minDist = dist;
                closest = candidate;
            }
        }
        return closest;
    }

    private int getPreyPriority(@NonNull EntityType entityType) {
        return switch (entityType) {
            case DEER -> 3;
            case RABBIT -> 2;
            case MOUSE -> 1;
            default -> 0;
        };
    }

    private boolean fleeFromPredator(@NonNull Grid grid, @NonNull Entity entity) {
        Entity predator = scanForEntity(grid, entity, true, PREY_FLEE_RADIUS);
        if (predator == null) return false;
        return moveDirected(grid, entity, predator, true);
    }

    private boolean chasePrey(@NonNull Grid grid, @NonNull Entity entity) {
        Entity prey = scanForEntity(grid, entity, false, null);
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
        float dx = target.getPreciseX() - entity.getPreciseX();
        float dy = target.getPreciseY() - entity.getPreciseY();
        if (flee) {
            dx = -dx;
            dy = -dy;
        }
        float length = (float) Math.hypot(dx, dy);
        if (length < 0.0001f) {
            return false;
        }
        float distance = getMovementDistance(entity);
        return moveContinuous(grid, entity, (dx / length) * distance, (dy / length) * distance);
    }


    private void moveRandomly(@NonNull Grid grid, @NonNull Entity entity) {
        float angle = random.nextFloat() * (float) (Math.PI * 2.0);
        float distance = getMovementDistance(entity);
        moveContinuous(grid, entity,
                (float) Math.cos(angle) * distance,
                (float) Math.sin(angle) * distance);
    }

    private boolean moveContinuous(@NonNull Grid grid, @NonNull Entity entity, float deltaX, float deltaY) {
        int subSteps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(deltaX), Math.abs(deltaY)) * 4f));
        float stepX = deltaX / subSteps;
        float stepY = deltaY / subSteps;
        boolean moved = false;

        for (int i = 0; i < subSteps; i++) {
            float nextX = entity.getPreciseX() + stepX;
            float nextY = entity.getPreciseY() + stepY;
            if (grid.moveAnimalPrecise(entity, nextX, nextY)) {
                moved = true;
                continue;
            }
            if (grid.moveAnimalPrecise(entity, nextX, entity.getPreciseY())) {
                moved = true;
                continue;
            }
            if (grid.moveAnimalPrecise(entity, entity.getPreciseX(), nextY)) {
                moved = true;
                continue;
            }
            break;
        }

        return moved;
    }

    private float getMovementDistance(@NonNull Entity entity) {
        int speed = entity instanceof Animal animal ? animal.getSpeed() : 1;
        return Math.max(0.20f, speed * MOVEMENT_PER_SPEED_UNIT);
    }

    private float getAttackDistance(@NonNull Entity entity) {
        return Math.max(0.75f, entity.getAttackRange() / ATTACK_RANGE_SCALE);
    }

    private float preciseDistance(@NonNull Entity first, @NonNull Entity second) {
        return (float) Math.hypot(first.getPreciseX() - second.getPreciseX(),
                first.getPreciseY() - second.getPreciseY());
    }


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


    @NonNull
    private List<Grid.Position> shuffled(@NonNull List<Grid.Position> positions) {
        List<Grid.Position> copy = new ArrayList<>(positions);
        Collections.shuffle(copy, random);
        return copy;
    }

    @NonNull
    private static Map<EntityType, Integer> createTargetPopulationByType() {
        Map<EntityType, Integer> targetPopulationByType = new EnumMap<>(EntityType.class);
        targetPopulationByType.put(EntityType.WOLF, 8);
        targetPopulationByType.put(EntityType.RABBIT, 34);
        targetPopulationByType.put(EntityType.MOUSE, 18);
        targetPopulationByType.put(EntityType.DEER, 10);
        return targetPopulationByType;
    }

    @NonNull
    private Map<EntityType, Integer> countPopulationByType(@NonNull List<Entity> animals) {
        Map<EntityType, Integer> populationByType = new EnumMap<>(EntityType.class);
        for (Entity animal : animals) {
            incrementPopulation(populationByType, animal.getType());
        }
        return populationByType;
    }

    private float getAdjustedReproductionChance(
            @NonNull EntityType entityType,
            @NonNull Map<EntityType, Integer> populationByType
    ) {
        int targetPopulation = TARGET_POPULATION_BY_TYPE.getOrDefault(entityType, 1);
        int currentPopulation = populationByType.getOrDefault(entityType, 0);
        if (targetPopulation <= 0) {
            return entityType.getReproductionChance();
        }

        float populationRatio = currentPopulation / (float) targetPopulation;
        float reproductionMultiplier = clamp(2.0f - populationRatio,
                MIN_REPRODUCTION_MULTIPLIER,
                MAX_REPRODUCTION_MULTIPLIER);
        return Math.min(1.0f, entityType.getReproductionChance() * reproductionMultiplier);
    }

    private void incrementPopulation(@NonNull Map<EntityType, Integer> populationByType, @NonNull EntityType entityType) {
        if (!entityType.isPredator() && !entityType.isPrey()) {
            return;
        }
        populationByType.put(entityType, populationByType.getOrDefault(entityType, 0) + 1);
    }

    private void decrementPopulation(@NonNull Map<EntityType, Integer> populationByType, @NonNull EntityType entityType) {
        if (!entityType.isPredator() && !entityType.isPrey()) {
            return;
        }
        int currentPopulation = populationByType.getOrDefault(entityType, 0);
        if (currentPopulation <= 1) {
            populationByType.remove(entityType);
            return;
        }
        populationByType.put(entityType, currentPopulation - 1);
    }

    private float clamp(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }
}
