package com.example.tleilax.model;


import com.example.tleilax.model.Organism;
import com.example.tleilax.model.BerryBush;
import com.example.tleilax.model.Grass;
import com.example.tleilax.model.Plant;

/**
 * Mouse — fast but low-vision Prey. Eats Grass and BerryBush.
 * Has the smallest energy pool of all animals.
 */
public class Mouse extends Prey {

    public static final int INITIAL_ENERGY        = 20;
    public static final int MAX_ENERGY            = 40;
    public static final int REPRODUCTION_THRESHOLD = 30;
    public static final int SPEED                 = 3;   // fast — same as Wolf
    public static final int VISION_RANGE          = 3;   // but nearly blind

    private static final int OFFSPRING_ENERGY = 10;

    public Mouse(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    public Mouse(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    @Override
    protected boolean isEdible(Plant plant) {
        return plant instanceof Grass || plant instanceof BerryBush;
    }

    /** Mice reproduce very rapidly — 50 % chance, lowest energy cost. */
    @Override
    public Organism reproduce() {
        if (energy < reproductionThreshold) return null;
        if (RANDOM.nextFloat() > 0.50f)     return null;

        int offX = RANDOM.nextInt(3) - 1;
        int offY = RANDOM.nextInt(3) - 1;
        if (offX == 0 && offY == 0) offX = 1;
        return new Mouse(x + offX, y + offY, OFFSPRING_ENERGY);
    }

    @Override
    public String toString() {
        return "Mouse(" + x + "," + y + ") e=" + energy;
    }
}
