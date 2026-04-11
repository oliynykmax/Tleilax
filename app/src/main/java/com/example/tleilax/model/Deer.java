package com.example.tleilax.model;


import com.example.tleilax.model.Organism;
import com.example.tleilax.model.BerryBush;
import com.example.tleilax.model.Grass;
import com.example.tleilax.model.Plant;
import com.example.tleilax.model.Tree;

/**
 * Deer — slow but sturdy Prey with a varied diet.
 * Eats Grass, BerryBush, and Tree leaves (Tree loses energy but survives).
 */
public class Deer extends Prey {

    public static final int INITIAL_ENERGY        = 50;
    public static final int MAX_ENERGY            = 100;
    public static final int REPRODUCTION_THRESHOLD = 70;
    public static final int SPEED                 = 1;   // slowest animal
    public static final int VISION_RANGE          = 5;

    private static final int OFFSPRING_ENERGY = 25;

    /** Energy drained from a Tree when a Deer browses on it (Tree stays alive). */
    public static final int TREE_BROWSE_DRAIN = 10;

    public Deer(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    public Deer(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    @Override
    protected boolean isEdible(Plant plant) {
        // Deer browses on Trees without killing them; handled via eat() override
        return plant instanceof Grass
                || plant instanceof BerryBush
                || plant instanceof Tree;
    }

    /**
     * Override eat so that Trees lose only TREE_BROWSE_DRAIN energy instead of dying.
     */
    @Override
    public void eat(Organism target) {
        if (target instanceof Tree) {
            int gained = Math.min(TREE_BROWSE_DRAIN, target.energy);
            energy = Math.min(maxEnergy, energy + gained);
            target.energy -= gained; // Tree survives unless already weakened
        } else {
            super.eat(target); // fully consume Grass / BerryBush
        }
    }

    @Override
    public Organism reproduce() {
        if (energy < reproductionThreshold) return null;
        if (RANDOM.nextFloat() > 0.25f)     return null; // slower reproduction

        int offX = RANDOM.nextInt(3) - 1;
        int offY = RANDOM.nextInt(3) - 1;
        if (offX == 0 && offY == 0) offX = 1;
        return new Deer(x + offX, y + offY, OFFSPRING_ENERGY);
    }

    @Override
    public String toString() {
        return "Deer(" + x + "," + y + ") e=" + energy;
    }
}
