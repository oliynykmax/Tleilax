package com.example.tleilax.model;

import com.example.tleilax.simulation.Grid;

public abstract class Entity {

    public int x;
    public int y;
    public int energy;

    public Entity(int x, int y, int energy) {
        this.x = x;
        this.y = y;
        this.energy = energy;
    }

    /**
     * Called once per simulation tick. Each subclass defines its own behavior.
     */
    public abstract void update(Grid grid);

    /**
     * An entity is alive as long as it has energy remaining.
     */
    public boolean isAlive() {
        return energy > 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + x + "," + y + ") e=" + energy;
    }
}
