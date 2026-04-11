package com.example.tleilax.model;


import com.example.tleilax.model.Organism;
import com.example.tleilax.model.BerryBush;
import com.example.tleilax.model.Grass;
import com.example.tleilax.model.Plant;

/**
 * Rabbit — medium-speed Prey. Eats Grass and BerryBush.
 */
public class Rabbit extends Prey {

    public static final int INITIAL_ENERGY        = 30;
    public static final int MAX_ENERGY            = 60;
    public static final int REPRODUCTION_THRESHOLD = 45;
    public static final int SPEED                 = 2;
    public static final int VISION_RANGE          = 5;

    private static final int OFFSPRING_ENERGY = 15;

    public Rabbit(int x, int y) {
        super(x, y, INITIAL_ENERGY, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    public Rabbit(int x, int y, int energy) {
        super(x, y, energy, MAX_ENERGY, REPRODUCTION_THRESHOLD, SPEED, VISION_RANGE);
    }

    @Override
    protected boolean isEdible(Plant plant) {
        return plant instanceof Grass || plant instanceof BerryBush;
    }

    /** Rabbits reproduce quickly — 40 % chance per tick when energy is sufficient. */
    @Override
    public Organism reproduce() {
        if (energy < reproductionThreshold) return null;
        if (RANDOM.nextFloat() > 0.40f)     return null;

        int offX = RANDOM.nextInt(3) - 1;
        int offY = RANDOM.nextInt(3) - 1;
        if (offX == 0 && offY == 0) offX = 1;
        return new Rabbit(x + offX, y + offY, OFFSPRING_ENERGY);
    }

    @Override
    public String toString() {
        return "Rabbit(" + x + "," + y + ") e=" + energy;
    }
}
