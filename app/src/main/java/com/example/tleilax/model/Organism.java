package com.example.tleilax.model;

/**
 * An Entity that can reproduce.
 * Subclasses define the concrete reproduction strategy.
 */
public abstract class Organism extends Entity {

    /** Energy ceiling — entity cannot exceed this value. */
    public int maxEnergy;

    /** When energy reaches this threshold the organism may reproduce. */
    public int reproductionThreshold;

    public Organism(int x, int y, int energy, int maxEnergy, int reproductionThreshold) {
        super(x, y, energy);
        this.maxEnergy = maxEnergy;
        this.reproductionThreshold = reproductionThreshold;
    }

    /**
     * Produces a new offspring placed adjacent to the parent.
     * Returns null if reproduction cannot happen right now
     * (insufficient energy, no free tile, random chance failed, …).
     */
    public abstract Organism reproduce();
}