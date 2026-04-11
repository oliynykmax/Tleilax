package com.example.tleilax.model;

import com.example.tleilax.model.Entity;
import com.example.tleilax.model.Organism;
import com.example.tleilax.model.Plant;
import com.example.tleilax.simulation.Grid;

import java.util.List;

/**
 * An Animal that eats Plants and flees from Predators.
 * Target-finding prioritises nearby food; flee logic overrides movement
 * when a Predator is detected within visionRange.
 */
public abstract class Prey extends Animal {

    public Prey(int x, int y, int energy, int maxEnergy, int reproductionThreshold,
                int speed, int visionRange) {
        super(x, y, energy, maxEnergy, reproductionThreshold, speed, visionRange);
    }

    /**
     * Returns the nearest edible Plant subtype within visionRange.
     * Returns null when no food is visible (animal will move randomly
     * or flee instead).
     * Subclasses may override isEdible(Plant) to restrict the diet.
     */
    @Override
    public Organism findTarget(Grid grid) {
        List<Entity> neighbours = getEntitiesInRange(grid);

        // Check for a nearby predator first — if one is close, flee
        boolean predatorNearby = neighbours.stream()
                .anyMatch(e -> e instanceof Predator && e.isAlive());
        if (predatorNearby) {
            fleeFrom(nearestPredator(neighbours), grid);
            return null; // suppress normal approach logic this tick
        }

        // Otherwise seek the nearest edible plant
        Organism nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Entity e : neighbours) {
            if (e instanceof Plant && e.isAlive() && isEdible((Plant) e)) {
                int dist = chebyshev(e);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = (Organism) e;
                }
            }
        }
        return nearest;
    }

    /**
     * Returns true if this Prey species can eat the given Plant.
     * Override in concrete subclasses to enforce dietary restrictions.
     */
    protected abstract boolean isEdible(Plant plant);

    // ---------------------------------------------------------------
    // Flee logic
    // ---------------------------------------------------------------

    private void fleeFrom(Entity threat, Grid grid) {
        if (threat == null) return;
        // Move directly away from the threat
        int dx = Integer.signum(x - threat.x);
        int dy = Integer.signum(y - threat.y);
        for (int step = 0; step < speed; step++) {
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

    private Entity nearestPredator(List<Entity> neighbours) {
        Entity nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Entity e : neighbours) {
            if (e instanceof Predator && e.isAlive()) {
                int dist = chebyshev(e);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = e;
                }
            }
        }
        return nearest;
    }

    private int chebyshev(Entity other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }
}
