package com.example.tleilax.model;

import com.example.tleilax.model.Organism;
import com.example.tleilax.simulation.Grid;

/**
 * Tree — does NOT spread; its tile blocks animal movement.
 * A Deer can browse it, draining energy without killing it.
 * Provides a high energy yield if fully consumed (e.g., by an environmental event).
 */
public class Tree extends Plant {

    public static final int INITIAL_ENERGY        = 60;
    public static final int MAX_ENERGY            = 80;
    public static final int REPRODUCTION_THRESHOLD = Integer.MAX_VALUE; // never reproduces
    public static final float SPREAD_CHANCE       = 0.0f;               // never spreads

    public Tree(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPREAD_CHANCE);
    }

    public Tree(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPREAD_CHANCE);
    }

    /**
     * Trees never spread — always returns null.
     */
    @Override
    public Organism spread(Grid grid) {
        return null;
    }

    /**
     * A Tree's tile is impassable to animals.
     * The Grid checks isPassable() before allowing movement onto a tile.
     */
    public boolean isPassable() {
        return false;
    }

    @Override
    public String toString() {
        return "Tree(" + x + "," + y + ")";
    }
}
