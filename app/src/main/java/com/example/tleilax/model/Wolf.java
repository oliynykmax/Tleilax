package com.example.tleilax.model;


import com.example.tleilax.model.Organism;

/**
 * Wolf — the only Predator species.
 * Highest speed and vision; hunts all Prey subtypes (Rabbit, Mouse, Deer).
 */
public class Wolf extends Predator {

    // ── Species constants ────────────────────────────────────────────
    public static final int INITIAL_ENERGY        = 60;
    public static final int MAX_ENERGY            = 120;
    public static final int REPRODUCTION_THRESHOLD = 90;
    public static final int SPEED                 = 3;
    public static final int VISION_RANGE          = 8;

    /** Energy given to a newborn Wolf. */
    private static final int OFFSPRING_ENERGY = 30;

    public Wolf(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    // Convenience constructor for spawning offspring at a specific energy level
    public Wolf(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    // ── Predator → findTarget() is inherited (scans for any Prey) ───

    /**
     * Reproduces by spawning a cub on an adjacent tile.
     * Requires energy ≥ reproductionThreshold; random 30 % chance per tick.
     */
    @Override
    public Organism reproduce() {
        if (energy < reproductionThreshold) return null;
        if (RANDOM.nextFloat() > 0.30f)     return null;

        // Pick a random adjacent tile — the Grid will reject out-of-bounds placements
        int offX = RANDOM.nextInt(3) - 1; // −1, 0, 1
        int offY = RANDOM.nextInt(3) - 1;
        if (offX == 0 && offY == 0) offX = 1; // avoid same tile

        return new Wolf(x + offX, y + offY, OFFSPRING_ENERGY);
    }

    @Override
    public String toString() {
        return "Wolf(" + x + "," + y + ") e=" + energy;
    }
}
