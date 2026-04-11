package com.example.tleilax.model;


import com.example.tleilax.model.Organism;
import com.example.tleilax.simulation.Grid;

/**
 * Grass — spreads quickly to adjacent empty tiles; low energy yield when eaten.
 */
public class Grass extends Plant {

    public static final int INITIAL_ENERGY        = 10;
    public static final int MAX_ENERGY            = 20;
    public static final int REPRODUCTION_THRESHOLD = 10; // always ready to spread
    public static final float SPREAD_CHANCE       = 0.55f; // high spread probability

    public Grass(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPREAD_CHANCE);
    }

    public Grass(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPREAD_CHANCE);
    }

    /**
     * Attempts to grow a new Grass patch on a random adjacent empty tile.
     */
    @Override
    public Organism spread(Grid grid) {
        int[] pos = randomAdjacentEmpty(grid);
        if (pos[0] == -1) return null;
        return new Grass(pos[0], pos[1]);
    }

    @Override
    public String toString() {
        return "Grass(" + x + "," + y + ")";
    }
}
