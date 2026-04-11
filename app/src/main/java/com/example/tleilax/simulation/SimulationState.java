package com.example.tleilax.simulation;


import com.example.tleilax.model.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * A serialisable snapshot of the entire simulation at a single point in time.
 * Used for saving to and loading from the Room database.
 */
public class SimulationState {

    /** All entities alive at the time of the snapshot. */
    public List<Entity> entities;

    public int gridWidth;
    public int gridHeight;

    /** Total number of ticks elapsed when the snapshot was taken. */
    public int tickCount;

    /** Wall-clock timestamp when the snapshot was created. */
    public long timestamp;

    /** Optional user-visible name for this save slot. */
    public String name;

    public SimulationState() {
        this.entities  = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public SimulationState(List<Entity> entities, int gridWidth, int gridHeight,
                           int tickCount, String name) {
        this.entities   = new ArrayList<>(entities);
        this.gridWidth  = gridWidth;
        this.gridHeight = gridHeight;
        this.tickCount  = tickCount;
        this.timestamp  = System.currentTimeMillis();
        this.name       = name;
    }

    @Override
    public String toString() {
        return "SimulationState{name='" + name + "', tick=" + tickCount
                + ", entities=" + entities.size() + ", ts=" + timestamp + "}";
    }
}
