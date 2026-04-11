package com.example.tleilax.model;

import com.example.tleilax.model.Organism;
import com.example.tleilax.simulation.Grid;

import java.util.Random;

/**
 * An Organism that spreads passively across the grid rather than moving.
 * reproduce() delegates to spread(Grid).
 */
public abstract class Plant extends Organism {

    protected static final Random RANDOM = new Random();

    /** Probability (0–1) that this plant spreads on a given tick. */
    protected float spreadChance;

    /** Grid reference kept for the reproduce() call — set during update(). */
    private Grid currentGrid;

    public Plant(int x, int y, int energy, int maxEnergy, int reproductionThreshold,
                 float spreadChance) {
        super(x, y, energy, maxEnergy, reproductionThreshold);
        this.spreadChance = spreadChance;
    }

    // ---------------------------------------------------------------
    // Entity contract
    // ---------------------------------------------------------------

    @Override
    public void update(Grid grid) {
        currentGrid = grid;
        // Plants don't lose energy just by existing
        if (energy >= reproductionThreshold && RANDOM.nextFloat() < spreadChance) {
            Organism offspring = reproduce();
            if (offspring != null) {
                grid.place(offspring);
            }
        }
    }

    // ---------------------------------------------------------------
    // Organism contract
    // ---------------------------------------------------------------

    /**
     * Calls spread(Grid) and returns the new plant instance (or null).
     */
    @Override
    public Organism reproduce() {
        if (currentGrid == null) return null;
        return spread(currentGrid);
    }

    // ---------------------------------------------------------------
    // Plant-specific
    // ---------------------------------------------------------------

    /**
     * Attempts to grow a new instance of this plant into an adjacent empty tile.
     *
     * @param grid the current simulation grid
     * @return a new Plant entity if a free tile was found, otherwise null
     */
    public abstract Organism spread(Grid grid);

    // ---------------------------------------------------------------
    // Helpers for subclasses
    // ---------------------------------------------------------------

    /**
     * Picks a random adjacent tile that is empty and within grid bounds.
     * Returns {-1, -1} if no such tile exists.
     */
    protected int[] randomAdjacentEmpty(Grid grid) {
        int[][] offsets = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        // shuffle
        for (int i = offsets.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            int[] tmp = offsets[i]; offsets[i] = offsets[j]; offsets[j] = tmp;
        }
        for (int[] off : offsets) {
            int nx = x + off[0];
            int ny = y + off[1];
            if (grid.isEmpty(nx, ny)) return new int[]{nx, ny};
        }
        return new int[]{-1, -1};
    }
}
