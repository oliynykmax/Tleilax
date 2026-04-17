package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Entity;
import com.example.tleilax.model.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Grid {

    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public Grid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive.");
        }
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = new Tile(TerrainType.SAND);
            }
        }
    }

    @NonNull
    public static Grid fromSnapshot(@NonNull WorldSnapshot snapshot) {
        Grid grid = new Grid(snapshot.width(), snapshot.height());
        for (WorldSnapshot.CellSnapshot cell : snapshot.cells()) {
            Tile tile = grid.getRequiredTile(cell.x(), cell.y());
            tile.setGrassAmount(cell.grassAmount());
            if (cell.plant() != null) {
                WorldSnapshot.PlantSnapshot plant = cell.plant();
                PlantState restoredPlant = plant.plantType() == PlantType.BERRY_BUSH
                        ? PlantState.restoreBerryBush(
                                plant.durability(),
                                plant.resourceAmount(),
                                plant.regrowProgress(),
                                plant.lifecycleTicksRemaining(),
                                plant.dead()
                        )
                        : PlantState.restoreTree(
                                plant.treeVariant() != null ? plant.treeVariant() : TreeVariant.MEDIUM,
                                plant.treeLifeStage() != null ? plant.treeLifeStage() : TreeLifeStage.MATURE,
                                plant.durability(),
                                plant.lifecycleTicksRemaining(),
                                plant.dead()
                        );
                tile.setPlantState(restoredPlant);
            }
            if (cell.animal() != null) {
                WorldSnapshot.AnimalSnapshot animal = cell.animal();
                Entity restoredAnimal = Entity.restore(
                        animal.type(),
                        cell.x(),
                        cell.y(),
                        animal.energy(),
                        animal.health()
                );
                tile.setAnimal(restoredAnimal);
            }
        }
        return grid;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    @Nullable
    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) {
            return null;
        }
        return tiles[y][x];
    }

    @Nullable
    public Entity getAnimal(int x, int y) {
        Tile tile = getTile(x, y);
        return tile != null ? tile.getAnimal() : null;
    }

    public boolean placeAnimal(@NonNull Entity entity) {
        Tile tile = getRequiredTile(entity.getX(), entity.getY());
        if (tile.getAnimal() != null || !canAnimalOccupy(entity, entity.getX(), entity.getY())) {
            return false;
        }
        tile.setAnimal(entity);
        trampleBerryBushIfNeeded(entity, tile);
        return true;
    }

    public boolean removeAnimal(@NonNull Entity entity) {
        Tile tile = getTile(entity.getX(), entity.getY());
        if (tile == null || tile.getAnimal() != entity) {
            return false;
        }
        tile.setAnimal(null);
        return true;
    }

    public boolean moveAnimal(@NonNull Entity entity, int newX, int newY) {
        Tile origin = getTile(entity.getX(), entity.getY());
        Tile destination = getTile(newX, newY);
        if (origin == null || destination == null || origin.getAnimal() != entity || destination.getAnimal() != null) {
            return false;
        }
        if (!canAnimalOccupy(entity, newX, newY)) {
            return false;
        }
        origin.setAnimal(null);
        entity.setX(newX);
        entity.setY(newY);
        destination.setAnimal(entity);
        trampleBerryBushIfNeeded(entity, destination);
        return true;
    }

    @NonNull
    public List<Entity> getAnimals() {
        List<Entity> animals = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (tiles[y][x].getAnimal() != null) {
                    animals.add(tiles[y][x].getAnimal());
                }
            }
        }
        return animals;
    }

    @NonNull
    public List<Position> getAdjacentPositions(int x, int y) {
        List<Position> neighbors = new ArrayList<>(4);
        maybeAdd(neighbors, x + 1, y);
        maybeAdd(neighbors, x - 1, y);
        maybeAdd(neighbors, x, y + 1);
        maybeAdd(neighbors, x, y - 1);
        return neighbors;
    }

    @NonNull
    public List<Position> getAdjacentEmptyPositions(int x, int y) {
        List<Position> emptyPositions = new ArrayList<>();
        for (Position position : getAdjacentPositions(x, y)) {
            Tile tile = getTile(position.x(), position.y());
            if (tile != null && tile.getAnimal() == null) {
                emptyPositions.add(position);
            }
        }
        return emptyPositions;
    }

    /**
     * Returns adjacent positions that are empty and traversable by the given entity.
     * Unlike {@link #getAdjacentEmptyPositions(int, int)}, this also checks
     * {@link #canAnimalOccupy}, filtering out tiles blocked by plants
     * (e.g. tall trees that the entity cannot pass through).
     */
    @NonNull
    public List<Position> getAdjacentEmptyPositions(int x, int y, @NonNull Entity entity) {
        List<Position> emptyPositions = new ArrayList<>();
        for (Position position : getAdjacentPositions(x, y)) {
            Tile tile = getTile(position.x(), position.y());
            if (tile != null && tile.getAnimal() == null && canAnimalOccupy(entity, position.x(), position.y())) {
                emptyPositions.add(position);
            }
        }
        return emptyPositions;
    }

    public void setGrass(int x, int y, int grassAmount) {
        Tile tile = getRequiredTile(x, y);
        if (tile.getPlantState() != null && !tile.getPlantState().canSpreadGrassUnderneath()) {
            tile.clearGrass();
            return;
        }
        tile.setGrassAmount(grassAmount);
    }

    public void clearGrass(int x, int y) {
        getRequiredTile(x, y).clearGrass();
    }

    public boolean placeBerryBush(int x, int y) {
        Tile tile = getRequiredTile(x, y);
        if (tile.getPlantState() != null && tile.getPlantState().getPlantType() == PlantType.TREE) {
            return false;
        }
        tile.setPlantState(PlantState.createBerryBush());
        return true;
    }

    public boolean placeTree(int x, int y, @NonNull TreeVariant treeVariant) {
        Tile tile = getRequiredTile(x, y);
        tile.clearGrass();
        tile.setPlantState(PlantState.createTree(treeVariant));
        return true;
    }

    public void clearPlant(int x, int y) {
        getRequiredTile(x, y).setPlantState(null);
    }

    public boolean placeSelection(@NonNull EntityType entityType, int x, int y) {
        return switch (entityType) {
            case WOLF, RABBIT, MOUSE, DEER -> replaceAnimal(entityType, x, y);
            case GRASS -> {
                setGrass(x, y, 1);
                yield true;
            }
            case BERRY_BUSH -> placeBerryBush(x, y);
            case TREE -> placeTree(x, y, TreeVariant.MEDIUM);
        };
    }

    public void seedGrassPatches(@NonNull Random random, int targetCoveragePercent) {
        boolean[][] grassMask = new boolean[height][width];
        float[][] densityMap = new float[height][width];

        float phaseA = random.nextFloat() * 6.2831855f;
        float phaseB = random.nextFloat() * 6.2831855f;
        float phaseC = random.nextFloat() * 6.2831855f;
        float frequencyA = 2.2f / Math.max(width, height);
        float frequencyB = 4.7f / Math.max(width, height);
        float frequencyC = 8.1f / Math.max(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float normalizedX = x / (float) Math.max(1, width - 1);
                float normalizedY = y / (float) Math.max(1, height - 1);

                float waveA = (float) Math.sin((x + y * 0.7f) * frequencyA * width + phaseA);
                float waveB = (float) Math.sin((x * 0.35f - y) * frequencyB * width + phaseB);
                float waveC = (float) Math.cos((x * 0.8f + y * 1.15f) * frequencyC * width + phaseC);
                float radialBias = 1.0f - Math.min(1.0f,
                        (float) Math.hypot(normalizedX - 0.5f, normalizedY - 0.5f) * 1.35f);

                densityMap[y][x] = (waveA * 0.45f) + (waveB * 0.30f) + (waveC * 0.20f) + (radialBias * 0.35f);
            }
        }

        float[][] ditherMatrix = {
                {0.0f, 0.5f, 0.125f, 0.625f},
                {0.75f, 0.25f, 0.875f, 0.375f},
                {0.1875f, 0.6875f, 0.0625f, 0.5625f},
                {0.9375f, 0.4375f, 0.8125f, 0.3125f}
        };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float normalizedDensity = (densityMap[y][x] + 1.0f) / 2.0f;
                float threshold = coverageThresholdForPercent(targetCoveragePercent)
                        + ((ditherMatrix[y % 4][x % 4] - 0.5f) * 0.18f);
                grassMask[y][x] = normalizedDensity > threshold;
            }
        }

        smoothGrassMask(grassMask, 2);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grassMask[y][x]) {
                    setGrass(x, y, 1);
                }
            }
        }
    }

    private float coverageThresholdForPercent(int targetCoveragePercent) {
        float clampedPercent = Math.max(0, Math.min(100, targetCoveragePercent));
        return 0.864f - (clampedPercent * 0.00332f);
    }

    private void smoothGrassMask(@NonNull boolean[][] grassMask, int iterations) {
        for (int iteration = 0; iteration < iterations; iteration++) {
            boolean[][] nextMask = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int neighbors = countGrassNeighbors(grassMask, x, y);
                    if (grassMask[y][x]) {
                        nextMask[y][x] = neighbors >= 3;
                    } else {
                        nextMask[y][x] = neighbors >= 4;
                    }
                }
            }
            for (int y = 0; y < height; y++) {
                System.arraycopy(nextMask[y], 0, grassMask[y], 0, width);
            }
        }
    }

    private int countGrassNeighbors(@NonNull boolean[][] grassMask, int x, int y) {
        int count = 0;
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (offsetX == 0 && offsetY == 0) {
                    continue;
                }
                int neighborX = x + offsetX;
                int neighborY = y + offsetY;
                if (!isInBounds(neighborX, neighborY)) {
                    continue;
                }
                if (grassMask[neighborY][neighborX]) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean replaceAnimal(@NonNull EntityType entityType, int x, int y) {
        Tile tile = getRequiredTile(x, y);
        if (tile.getAnimal() != null) {
            if (tile.getAnimal().getType() == entityType) {
                tile.setAnimal(null);
                return true;
            }
            tile.setAnimal(null);
        }
        Entity entity = Entity.create(entityType, x, y);
        if (!canAnimalOccupy(entity, x, y)) {
            return false;
        }
        tile.setAnimal(entity);
        trampleBerryBushIfNeeded(entity, tile);
        return true;
    }

    private boolean canAnimalOccupy(@NonNull Entity entity, int x, int y) {
        Tile tile = getRequiredTile(x, y);
        PlantState plantState = tile.getPlantState();
        if (plantState == null) {
            return true;
        }
        return entity.getClearanceHeight() >= plantState.getBlockingHeight();
    }

    private void trampleBerryBushIfNeeded(@NonNull Entity entity, @NonNull Tile tile) {
        PlantState plantState = tile.getPlantState();
        if (plantState == null || plantState.getPlantType() != PlantType.BERRY_BUSH || !entity.canTrampleBerryBush()) {
            return;
        }
        plantState.damage(1);
        if (plantState.isReadyToClear()) {
            tile.setPlantState(null);
        }
    }

    @NonNull
    private Tile getRequiredTile(int x, int y) {
        Tile tile = getTile(x, y);
        if (tile == null) {
            throw new IllegalArgumentException("Position out of bounds: " + x + "," + y);
        }
        return tile;
    }

    private void maybeAdd(@NonNull List<Position> positions, int x, int y) {
        if (isInBounds(x, y)) {
            positions.add(new Position(x, y));
        }
    }

    public record Position(int x, int y) {
    }
}
