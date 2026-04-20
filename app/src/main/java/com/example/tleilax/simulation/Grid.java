package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Entity;
import com.example.tleilax.model.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mutable world grid storing terrain, plants, grass, and animals per tile.
 */
public class Grid {

    private final int width;
    private final int height;
    private final Tile[][] tiles;

    /**
     * Creates an empty grid filled with default sand terrain.
     */
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

    /**
     * Rebuilds a grid instance from a serialized world snapshot.
     */
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
                restoredAnimal.setPrecisePosition(animal.preciseX(), animal.preciseY());
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

    /**
     * Places an animal on its current tile if the destination is free and traversable.
     */
    public boolean placeAnimal(@NonNull Entity entity) {
        Tile tile = getRequiredTile(entity.getX(), entity.getY());
        if (tile.getAnimal() != null || !canAnimalOccupy(entity, entity.getX(), entity.getY())) {
            return false;
        }
        tile.setAnimal(entity);
        trampleBerryBushIfNeeded(entity, tile);
        return true;
    }

    /**
     * Removes an animal from the tile it currently occupies.
     */
    public boolean removeAnimal(@NonNull Entity entity) {
        Tile tile = getTile(entity.getX(), entity.getY());
        if (tile == null || tile.getAnimal() != entity) {
            return false;
        }
        tile.setAnimal(null);
        return true;
    }

    /**
     * Moves an animal by whole-tile coordinates.
     */
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

    /**
     * Moves an animal using sub-tile coordinates while keeping tile occupancy valid.
     */
    public boolean moveAnimalPrecise(@NonNull Entity entity, float newPreciseX, float newPreciseY) {
        float clampedX = clampToWorld(newPreciseX, width);
        float clampedY = clampToWorld(newPreciseY, height);

        int oldCellX = entity.getX();
        int oldCellY = entity.getY();
        int newCellX = (int) Math.floor(clampedX);
        int newCellY = (int) Math.floor(clampedY);

        Tile origin = getTile(oldCellX, oldCellY);
        Tile destination = getTile(newCellX, newCellY);
        if (origin == null || destination == null || origin.getAnimal() != entity) {
            return false;
        }

        if (oldCellX == newCellX && oldCellY == newCellY) {
            entity.setPrecisePosition(clampedX, clampedY);
            return true;
        }

        if (destination.getAnimal() != null || !canAnimalOccupy(entity, newCellX, newCellY)) {
            return false;
        }

        origin.setAnimal(null);
        entity.setPrecisePosition(clampedX, clampedY);
        destination.setAnimal(entity);
        trampleBerryBushIfNeeded(entity, destination);
        return true;
    }

    /**
     * Returns all currently placed animals.
     */
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

    /**
     * Returns cardinal neighbor positions for the given tile.
     */
    @NonNull
    public List<Position> getAdjacentPositions(int x, int y) {
        List<Position> neighbors = new ArrayList<>(4);
        maybeAdd(neighbors, x + 1, y);
        maybeAdd(neighbors, x - 1, y);
        maybeAdd(neighbors, x, y + 1);
        maybeAdd(neighbors, x, y - 1);
        return neighbors;
    }

    /**
     * Returns adjacent positions that do not currently contain an animal.
     */
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

    /**
     * Sets grass on a tile unless a blocking plant prevents ground growth there.
     */
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

    /**
     * Places a berry bush on the target tile when no tree is already present.
     */
    public boolean placeBerryBush(int x, int y) {
        Tile tile = getRequiredTile(x, y);
        if (tile.getPlantState() != null && tile.getPlantState().getPlantType() == PlantType.TREE) {
            return false;
        }
        tile.setPlantState(PlantState.createBerryBush());
        return true;
    }

    /**
     * Places a tree and clears grass under it.
     */
    public boolean placeTree(int x, int y, @NonNull TreeVariant treeVariant) {
        Tile tile = getRequiredTile(x, y);
        tile.clearGrass();
        tile.setPlantState(PlantState.createTree(treeVariant));
        return true;
    }

    public void clearPlant(int x, int y) {
        getRequiredTile(x, y).setPlantState(null);
    }

    /**
     * Places the selected editor item using the correct layer-specific rule.
     */
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

    private float clampToWorld(float value, int size) {
        float min = 0.001f;
        float max = size - 0.001f;
        return Math.max(min, Math.min(max, value));
    }

    public record Position(int x, int y) {
    }
}
