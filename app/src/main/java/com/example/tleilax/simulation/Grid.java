package com.example.tleilax.simulation;


import com.example.tleilax.model.Entity;
import com.example.tleilax.model.Tree;
//import com.example.tleilax.model.Tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The 2-D grid that owns and manages all entities.
 * Internally the grid is a flat Entity[][] array addressed by
 * cells[x][y].  Each cell holds at most one entity (animals cannot
 * stack; a plant and an animal may temporarily share a cell during the eating
 * step but the plant is marked dead immediately after).
 */
public class Grid {

    public final int width;
    public final int height;

    /** Primary spatial index: {cells[x][y]} → entity on that tile (or null). */
    public final Entity[][] cells;

    /**
     * Flat list of all live entities — iterated every tick.
     * Kept in sync with cells.
     */
    private final List<Entity> entityList = new ArrayList<>();

    /** Buffer for entities added during a tick (to avoid ConcurrentModificationException). */
    private final List<Entity> toAdd = new ArrayList<>();

    public Grid(int width, int height) {
        this.width  = width;
        this.height = height;
        this.cells  = new Entity[width][height];
    }

    // ---------------------------------------------------------------
    // Entity management
    // ---------------------------------------------------------------

    /**
     * Places an entity on the grid.
     * No-op if the coordinates are out of bounds or the target cell is occupied.
     */
    public void place(Entity e) {
        if (!inBounds(e.x, e.y))      return;
        if (cells[e.x][e.y] != null)  return; // tile occupied
        cells[e.x][e.y] = e;
        toAdd.add(e);
    }

    /**
     * Removes an entity from its current position.
     */
    public void remove(Entity e) {
        if (!inBounds(e.x, e.y)) return;
        if (cells[e.x][e.y] == e) {
            cells[e.x][e.y] = null;
        }
        entityList.remove(e);
    }

    // ---------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------

    /**
     * Advances the simulation by one tick.
     *   Each live entity calls update(grid)
     *   Dead entities are removed from the grid and list
     *   Newly spawned entities are flushed into the main list
     */
    public void tick() {
        // Snapshot to avoid issues if the list grows during iteration
        List<Entity> snapshot = new ArrayList<>(entityList);
        for (Entity e : snapshot) {
            if (e.isAlive()) {
                e.update(this);
            }
        }

        // Remove dead entities
        Iterator<Entity> it = entityList.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (!e.isAlive()) {
                if (inBounds(e.x, e.y) && cells[e.x][e.y] == e) {
                    cells[e.x][e.y] = null;
                }
                it.remove();
            }
        }

        // Flush newly spawned entities
        for (Entity e : toAdd) {
            if (e.isAlive()) {
                entityList.add(e);
            }
        }
        toAdd.clear();
    }

    // ---------------------------------------------------------------
    // Spatial queries
    // ---------------------------------------------------------------

    /**
     * Returns all entities within a Chebyshev distance of range from (cx, cy).
     */
    public List<Entity> getNeighbours(int cx, int cy, int range) {
        List<Entity> result = new ArrayList<>();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = cx + dx;
                int ny = cy + dy;
                if (inBounds(nx, ny) && cells[nx][ny] != null) {
                    result.add(cells[nx][ny]);
                }
            }
        }
        return result;
    }

    /**
     * Overload for the UML signature — uses a default range of 1 (Moore neighbourhood).
     */
    public List<Entity> getNeighbours(int cx, int cy) {
        return getNeighbours(cx, cy, 1);
    }

    /**
     * Returns true if the tile is in-bounds, empty, and not blocked by a Tree.
     */
    public boolean isPassable(int x, int y) {
        if (!inBounds(x, y)) return false;
        Entity occupant = cells[x][y];
        if (occupant == null) return true;
        if (occupant instanceof Tree) return false;
        // Plants other than Tree don't block movement (the animal will eat them)
        return true;
    }

    /**
     * Returns true if the tile is in-bounds and completely empty.
     */
    public boolean isEmpty(int x, int y) {
        return inBounds(x, y) && cells[x][y] == null;
    }

    /** Returns an unmodifiable view of all live entities. */
    public List<Entity> getAllEntities() {
        return new ArrayList<>(entityList);
    }

    public int entityCount() {
        return entityList.size();
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
