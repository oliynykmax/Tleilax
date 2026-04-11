package com.example.tleilax.model;

import com.example.tleilax.model.Entity;
import com.example.tleilax.model.Organism;
import com.example.tleilax.simulation.Grid;

import java.util.List;

/**
 * An Animal that hunts Prey organisms.
 * Scans within visionRange and targets the nearest Prey subtype.
 */
public abstract class Predator extends Animal {

    public Predator(int x, int y, int energy, int maxEnergy, int reproductionThreshold,
                    int speed, int visionRange) {
        super(x, y, energy, maxEnergy, reproductionThreshold, speed, visionRange);
    }

    /**
     * Returns the nearest Prey within visionRange,
     * or null if no prey is visible.
     */
    @Override
    public Organism findTarget(Grid grid) {
        List<Entity> neighbours = getEntitiesInRange(grid);
        Organism nearest = null;
        int bestDist = Integer.MAX_VALUE;

        for (Entity e : neighbours) {
            if (e instanceof Prey && e.isAlive()) {
                int dist = chebyshev(e);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = (Organism) e;
                }
            }
        }
        return nearest;
    }

    private int chebyshev(Entity other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }
}
