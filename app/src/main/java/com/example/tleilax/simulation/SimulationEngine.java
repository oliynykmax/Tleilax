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
    @NonNull
    private final List<Listener> listeners = new ArrayList<>();

    public SimulationEngine() {
    }

    @Nullable
    private Listener legacyListener;

    public void addListener(@NonNull Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    /**
     * @deprecated Use {@link #addListener(Listener)} instead.
     */
    @Deprecated
    public void setListener(@Nullable Listener listener) {
        if (legacyListener != null) {
            removeListener(legacyListener);
        }
        legacyListener = listener;
        if (legacyListener != null) {
            addListener(legacyListener);
            legacyListener.onWorldUpdated(getSnapshot());
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
        WorldGenerator generator = new WorldGenerator();
        generator.seedWorld(
                grid,
                new Random(),
                AppSettings.getGrassCoveragePercent(TleilaxApp.getAppContext()),
                AppSettings.getWolfCount(TleilaxApp.getAppContext()),
                AppSettings.getRabbitCount(TleilaxApp.getAppContext()),
                AppSettings.getMouseCount(TleilaxApp.getAppContext()),
                AppSettings.getDeerCount(TleilaxApp.getAppContext()),
                AppSettings.getBerryBushCount(TleilaxApp.getAppContext()),
                AppSettings.getTreeCount(TleilaxApp.getAppContext())
        );
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

    /**
     * Captures a point-in-time snapshot of the entire simulation state.
     */
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
                WorldSnapshot.CellSnapshot cellSnapshot = createCellSnapshot(x, y, tile);
                if (cellSnapshot != null) {
                    cells.add(cellSnapshot);
                }
            }
        }
        return new WorldSnapshot(grid.getWidth(), grid.getHeight(), tickCount, TerrainType.SAND, cells);
    }

    /**
     * Extracts a compact state representation for a single cell, 
     * returning null if the cell is completely empty.
     */
    @Nullable
    private WorldSnapshot.CellSnapshot createCellSnapshot(int x, int y, @NonNull Tile tile) {
        WorldSnapshot.PlantSnapshot plantSnapshot = null;
        if (tile.getPlantState() != null) {
            PlantState ps = tile.getPlantState();
            plantSnapshot = new WorldSnapshot.PlantSnapshot(
                    ps.getPlantType(),
                    ps.getBlockingHeight(),
                    ps.isDead(),
                    ps.getBerryAmount(),
                    ps.getDurability(),
                    ps.getBerryRegrowProgress(),
                    ps.getLifecycleTicksRemaining(),
                    ps.getTreeVariant(),
                    ps.getTreeLifeStage()
            );
        }
        WorldSnapshot.AnimalSnapshot animalSnapshot = null;
        if (tile.getAnimal() != null) {
            Entity animal = tile.getAnimal();
            animalSnapshot = new WorldSnapshot.AnimalSnapshot(
                    animal.getType(),
                    animal.getEnergy(),
                    animal.getHealth(),
                    animal.getPreciseX(),
                    animal.getPreciseY()
            );
        }
        if (tile.getGrassAmount() == 0 && plantSnapshot == null && animalSnapshot == null) {
            return null;
        }
        return new WorldSnapshot.CellSnapshot(
                x,
                y,
                tile.getTerrainType(),
                tile.getGrassAmount(),
                plantSnapshot,
                animalSnapshot
        );
    }

    private long getTickDelayMillis() {
        return switch (speedMultiplier) {
            case 2 -> 320L;
            case 4 -> 160L;
            default -> 640L;
        };
    }

    private void dispatchWorldUpdate() {
        WorldSnapshot snapshot = getSnapshot();
        for (Listener l : new ArrayList<>(listeners)) {
            l.onWorldUpdated(snapshot);
        }
    }

    public interface Listener {
        void onWorldUpdated(@NonNull WorldSnapshot snapshot);
    }
}
