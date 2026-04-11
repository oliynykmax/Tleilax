package com.example.tleilax.model;


import com.example.tleilax.model.Organism;
import com.example.tleilax.simulation.Grid;

/**
 * BerryBush — spreads slowly but provides a higher energy yield than Grass.
 */
public class BerryBush extends Plant {

    public static final int INITIAL_ENERGY        = 25;
    public static final int MAX_ENERGY            = 40;
    public static final int REPRODUCTION_THRESHOLD = 25;
    public static final float SPREAD_CHANCE       = 0.20f; // spreads infrequently

    public BerryBush(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPREAD_CHANCE);
    }

    public BerryBush(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPREAD_CHANCE);
    }

    /**
     * Grows a new BerryBush on a random adjacent empty tile.
     */
    @Override
    public Organism spread(Grid grid) {
        int[] pos = randomAdjacentEmpty(grid);
        if (pos[0] == -1) return null;
        return new BerryBush(pos[0], pos[1]);
    }

    @Override
    public String toString() {
        return "BerryBush(" + x + "," + y + ")";
    }
}

