package com.example.tleilax.simulation;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Entity;
import com.example.tleilax.model.EntityType;
import com.example.tleilax.TleilaxApp;
import com.example.tleilax.utils.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulationEngine {

    public static final int FIXED_WORLD_SIZE = 256;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TickLogic tickLogic = new TickLogic();
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            tickOnce();
            if (running) {
                handler.postDelayed(this, getTickDelayMillis());
            }
        }
    };

    private Grid grid;
    private boolean running;
    private int speedMultiplier = 1;
    private long tickCount;
    @Nullable
    private Listener listener;

    public SimulationEngine() {
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        if (grid != null) {
            dispatchWorldUpdate();
        }
    }

    public void initializeWorld() {
        reset(FIXED_WORLD_SIZE, FIXED_WORLD_SIZE);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        handler.removeCallbacks(tickRunnable);
        handler.postDelayed(tickRunnable, getTickDelayMillis());
    }

    public void pause() {
        running = false;
        handler.removeCallbacks(tickRunnable);
    }

    public void setSpeedMultiplier(int speedMultiplier) {
        if (speedMultiplier != 1 && speedMultiplier != 2 && speedMultiplier != 4) {
            throw new IllegalArgumentException("Speed multiplier must be 1, 2, or 4.");
        }
        this.speedMultiplier = speedMultiplier;
        if (running) {
            handler.removeCallbacks(tickRunnable);
            handler.postDelayed(tickRunnable, getTickDelayMillis());
        }
    }

    public void tickOnce() {
        if (grid == null) {
            return;
        }
        tickLogic.advance(grid);
        tickCount++;
        dispatchWorldUpdate();
    }

    public void reset(int width, int height) {
        pause();
        grid = new Grid(FIXED_WORLD_SIZE, FIXED_WORLD_SIZE);
        tickCount = 0;
        seedWorld();
        dispatchWorldUpdate();
    }

    public void loadSnapshot(@NonNull WorldSnapshot snapshot) {
        pause();
        grid = Grid.fromSnapshot(snapshot);
        tickCount = snapshot.tickCount();
        dispatchWorldUpdate();
    }

    public boolean placeEntity(@NonNull EntityType type, int x, int y) {
        if (grid == null) {
            return false;
        }
        boolean placed = grid.placeSelection(type, x, y);
        if (placed) {
            dispatchWorldUpdate();
        }
        return placed;
    }

    @NonNull
    public WorldSnapshot getSnapshot() {
        if (grid == null) {
            throw new IllegalStateException("World is not initialized.");
        }
        List<WorldSnapshot.CellSnapshot> cells = new ArrayList<>();
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Tile tile = grid.getTile(x, y);
                if (tile == null) {
                    continue;
                }
                WorldSnapshot.PlantSnapshot plantSnapshot = null;
                if (tile.getPlantState() != null) {
                    plantSnapshot = new WorldSnapshot.PlantSnapshot(
                            tile.getPlantState().getPlantType(),
                            tile.getPlantState().getBlockingHeight(),
                            tile.getPlantState().isDead(),
                            tile.getPlantState().getBerryAmount(),
                            tile.getPlantState().getDurability(),
                            tile.getPlantState().getBerryRegrowProgress(),
                            tile.getPlantState().getLifecycleTicksRemaining(),
                            tile.getPlantState().getTreeVariant(),
                            tile.getPlantState().getTreeLifeStage()
                    );
                }
                WorldSnapshot.AnimalSnapshot animalSnapshot = null;
                if (tile.getAnimal() != null) {
                    animalSnapshot = new WorldSnapshot.AnimalSnapshot(
                            tile.getAnimal().getType(),
                            tile.getAnimal().getEnergy(),
                            tile.getAnimal().getHealth()
                    );
                }
                if (tile.getGrassAmount() == 0 && plantSnapshot == null && animalSnapshot == null) {
                    continue;
                }
                cells.add(new WorldSnapshot.CellSnapshot(
                        x,
                        y,
                        tile.getTerrainType(),
                        tile.getGrassAmount(),
                        plantSnapshot,
                        animalSnapshot
                ));
            }
        }
        return new WorldSnapshot(grid.getWidth(), grid.getHeight(), tickCount, TerrainType.SAND, cells);
    }

    private void seedWorld() {
        Random random = new Random();
        grid.seedGrassPatches(random, AppSettings.getGrassCoveragePercent(TleilaxApp.getAppContext()));
        seedPlantsNearGrass(random);
        addRandomAnimals(EntityType.WOLF, 3, random);
        addRandomAnimals(EntityType.RABBIT, 12, random);
        addRandomAnimals(EntityType.MOUSE, 10, random);
        addRandomAnimals(EntityType.DEER, 6, random);
    }

    private void seedPlantsNearGrass(@NonNull Random random) {
        int berryBushes = Math.max(6, grid.getWidth() / 3);
        int trees = Math.max(8, grid.getWidth() / 2);
        int attempts = 0;
        while ((berryBushes > 0 || trees > 0) && attempts < 800) {
            int x = random.nextInt(grid.getWidth());
            int y = random.nextInt(grid.getHeight());
            Tile tile = grid.getTile(x, y);
            if (tile == null || !tile.hasGrass()) {
                attempts++;
                continue;
            }
            boolean grassEdge = false;
            for (Grid.Position neighbor : grid.getAdjacentPositions(x, y)) {
                Tile neighborTile = grid.getTile(neighbor.x(), neighbor.y());
                if (neighborTile != null && !neighborTile.hasGrass()) {
                    grassEdge = true;
                    break;
                }
            }

            if (trees > 0 && grassEdge && tile.getPlantState() == null) {
                TreeVariant treeVariant = switch (random.nextInt(3)) {
                    case 0 -> TreeVariant.LOW;
                    case 1 -> TreeVariant.MEDIUM;
                    default -> TreeVariant.TALL;
                };
                grid.placeTree(x, y, treeVariant);
                trees--;
            } else if (berryBushes > 0 && tile.getPlantState() == null) {
                grid.placeBerryBush(x, y);
                berryBushes--;
            }
            attempts++;
        }
    }

    private void addRandomAnimals(@NonNull EntityType type, int count, @NonNull Random random) {
        int attempts = 0;
        int placed = 0;
        int maxAttempts = count * 30;
        while (placed < count && attempts < maxAttempts) {
            int x = random.nextInt(grid.getWidth());
            int y = random.nextInt(grid.getHeight());
            Entity entity = Entity.create(type, x, y);
            if (grid.placeAnimal(entity)) {
                placed++;
            }
            attempts++;
        }
    }

    private long getTickDelayMillis() {
        return switch (speedMultiplier) {
            case 2 -> 320L;
            case 4 -> 160L;
            default -> 640L;
        };
    }

    private void dispatchWorldUpdate() {
        if (listener != null) {
            listener.onWorldUpdated(getSnapshot());
        }
    }

    public interface Listener {
        void onWorldUpdated(@NonNull WorldSnapshot snapshot);
    }
}
