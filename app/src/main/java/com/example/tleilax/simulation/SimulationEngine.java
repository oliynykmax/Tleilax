package com.example.tleilax.simulation;


import com.example.tleilax.model.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Drives the simulation loop: manages tick scheduling, speed control,
 * state snapshots, and per-tick statistics collection.
 * Call tick() from an Android Handler/Runnable at the interval
 * determined by tickIntervalMs() — or let the engine drive itself
 * if you pass in a Runnable scheduler.
 */
public class SimulationEngine {

    // ── Speed presets (ms per tick) ──────────────────────────────────
    public static final int SPEED_PAUSED = 0;
    public static final int SPEED_SLOW   = 800;
    public static final int SPEED_NORMAL = 400;
    public static final int SPEED_FAST   = 100;

    // ── State enum ───────────────────────────────────────────────────
    public enum SimulationState { RUNNING, PAUSED, STOPPED }

    // ── Fields ───────────────────────────────────────────────────────
    public Grid grid;
    public int  tickCount;
    public SimulationState state;

    private int currentSpeed;

    /** Listener notified after each tick (update UI, record stats, …). */
    private TickListener tickListener;

    public interface TickListener {
        void onTick(int tickCount, Map<String, Integer> stats);
    }

    // ── Constructor ──────────────────────────────────────────────────

    public SimulationEngine(int gridWidth, int gridHeight) {
        this.grid         = new Grid(gridWidth, gridHeight);
        this.tickCount    = 0;
        this.state        = SimulationState.STOPPED;
        this.currentSpeed = SPEED_NORMAL;
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    public void start() {
        state = SimulationState.RUNNING;
    }

    public void pause() {
        state = SimulationState.PAUSED;
    }

    public void resume() {
        state = SimulationState.RUNNING;
    }

    public void stop() {
        state = SimulationState.STOPPED;
    }

    // ── Speed control ────────────────────────────────────────────────

    /**
     * Sets the speed using one of the SPEED_* constants.
     * Passing #SPEED_PAUSED is equivalent to calling pause().
     */
    public void setSpeed(int speed) {
        currentSpeed = speed;
        if (speed == SPEED_PAUSED) {
            pause();
        } else if (state == SimulationState.PAUSED) {
            resume();
        }
    }

    /** Returns the current tick interval in milliseconds. */
    public int tickIntervalMs() {
        return currentSpeed;
    }

    // ── Core tick ────────────────────────────────────────────────────

    /**
     * Advances the simulation by exactly one tick.
     * Should be called only when state == RUNNING.
     */
    public void tick() {
        if (state != SimulationState.RUNNING) return;
        grid.tick();
        tickCount++;
        if (tickListener != null) {
            tickListener.onTick(tickCount, getStats());
        }
    }

    // ── Statistics ───────────────────────────────────────────────────

    /**
     * Returns a map of className → count for all live entities.
     * Example key: "Wolf", "Grass".
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> counts = new HashMap<>();
        for (Entity e : grid.getAllEntities()) {
            if (e.isAlive()) {
                String key = e.getClass().getSimpleName();
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        return counts;
    }

    // ── Save / restore ───────────────────────────────────────────────

    /**
     * Captures the current simulation as a SimulationState.
     */
    public com.example.tleilax.simulation.SimulationState captureState(String saveName) {
        return new com.example.tleilax.simulation.SimulationState(
                grid.getAllEntities(),
                grid.width,
                grid.height,
                tickCount,
                saveName
        );
    }

    /**
     * Restores the engine from a previously captured state.
     * Replaces the current grid entirely.
     */
    public void restoreState(com.example.tleilax.simulation.SimulationState savedState) {
        this.grid      = new Grid(savedState.gridWidth, savedState.gridHeight);
        this.tickCount = savedState.tickCount;
        for (Entity e : savedState.entities) {
            grid.place(e);
        }
        this.state = SimulationState.PAUSED;
    }

    // ── Listener ─────────────────────────────────────────────────────

    public void setTickListener(TickListener listener) {
        this.tickListener = listener;
    }
}
