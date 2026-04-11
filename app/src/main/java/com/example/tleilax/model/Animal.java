package com.example.tleilax.model;

import com.example.tleilax.simulation.Grid;

import java.util.List;
import java.util.Random;

/**
 * An Organism that can move, eat, and scan the grid for targets.
 * Concrete subclasses (Predator / Prey) define what counts as a valid target.
 */
public abstract class Animal extends Organism {

    /** Tiles moved per tick. */
    public int speed;

    /** Scan radius (Chebyshev distance) used to locate targets. */
    public int visionRange;

    /** Shared RNG — inject a seeded instance for reproducible tests. */
    protected static final Random RANDOM = new Random();

    /** Energy drained each tick simply for being alive. */
    protected static final int METABOLISM_COST = 1;

    public Animal(int x, int y, int energy, int maxEnergy, int reproductionThreshold,
                  int speed, int visionRange) {
        super(x, y, energy, maxEnergy, reproductionThreshold);
        this.speed = speed;
        this.visionRange = visionRange;
    }

    // ---------------------------------------------------------------
    // Core tick logic (template-method pattern)
    // ---------------------------------------------------------------

    @Override
    public void update(Grid grid) {
        energy -= METABOLISM_COST;
        if (!isAlive()) return;

        Organism target = findTarget(grid);
        if (target != null) {
            moveToward(target, grid);
            if (isAdjacentTo(target)) {
                eat(target);
            }
        } else {
            moveRandom(grid);
        }

        if (energy >= reproductionThreshold) {
            Organism offspring = reproduce();
            if (offspring != null) {
                grid.place(offspring);
                energy -= reproductionThreshold / 2; // reproduction costs energy
            }
        }
    }

    // ---------------------------------------------------------------
    // Movement
    // ---------------------------------------------------------------

    /**
     * Steps toward the target by up to #speed tiles.
     */
    public void move(Grid grid) {
        moveRandom(grid);
    }

    private void moveToward(Organism target, Grid grid) {
        int steps = speed;
        while (steps-- > 0) {
            int dx = Integer.signum(target.x - x);
            int dy = Integer.signum(target.y - y);
            int nx = x + dx;
            int ny = y + dy;
            if (grid.isPassable(nx, ny)) {
                grid.remove(this);
                x = nx;
                y = ny;
                grid.place(this);
            }
        }
    }

    private void moveRandom(Grid grid) {
        int[] dxArr = {-1, 0, 1, 0};
        int[] dyArr = {0, -1, 0, 1};
        for (int attempt = 0; attempt < speed; attempt++) {
            int dir = RANDOM.nextInt(4);
            int nx = x + dxArr[dir];
            int ny = y + dyArr[dir];
            if (grid.isPassable(nx, ny)) {
                grid.remove(this);
                x = nx;
                y = ny;
                grid.place(this);
            }
        }
    }

    private boolean isAdjacentTo(Entity other) {
        return Math.abs(x - other.x) <= 1 && Math.abs(y - other.y) <= 1;
    }

    // ---------------------------------------------------------------
    // Eating
    // ---------------------------------------------------------------

    /**
     * Consumes the target: gains its remaining energy, kills it.
     */
    public void eat(Organism target) {
        energy = Math.min(maxEnergy, energy + target.energy);
        target.energy = 0; // marks it dead → Grid.tick() will remove it
    }

    // ---------------------------------------------------------------
    // Scanning (subclass-defined)
    // ---------------------------------------------------------------

    /**
     * Returns the nearest valid target within #visionRange,
     * or null if none is found.
     */
    public abstract Organism findTarget(Grid grid);

    /**
     * Helper: returns all entities within visionRange of this animal.
     */
    protected List<Entity> getEntitiesInRange(Grid grid) {
        return grid.getNeighbours(x, y, visionRange);
    }
}