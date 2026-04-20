package com.example.tleilax.utils;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Immutable statistics view of a single simulation tick.
 */
public class StatisticsSnapshot {

    private final int tick;
    private final LinkedHashMap<String, Integer> populationBySpecies;
    private final String status;
    private final float grassCoverage;

    /**
     * Creates a statistics snapshot without additional status text.
     */
    public StatisticsSnapshot(int tick, Map<String, Integer> populationBySpecies) {
        this(tick, populationBySpecies, null, 0f);
    }
    /**
     * Creates a statistics snapshot with species counts and summary metadata.
     */
    public StatisticsSnapshot(int tick, Map<String, Integer> populationBySpecies, String status, float grassCoverage) {
        this.tick = tick;
        this.populationBySpecies = new LinkedHashMap<>();
        this.grassCoverage = grassCoverage;

        if (populationBySpecies != null) {
            for (Map.Entry<String, Integer> entry : populationBySpecies.entrySet()) {
                String species = entry.getKey();
                Integer value = entry.getValue();

                if (species == null || species.trim().isEmpty()) {
                    continue;
                }
                this.populationBySpecies.put(species, Math.max(0, value == null ? 0 : value));
            }

        }
        this.status = status;
    }

    public int getTick() {
        return tick;
    }
    public Map<String, Integer> getPopulationBySpecies() {
        return new LinkedHashMap<>(populationBySpecies);
    }
    public String getStatus() {
        return status;
    }
    public float getGrassCoverage() {
        return grassCoverage;
    }
    /**
     * Returns the total counted population across all tracked species.
     */
    public int getTotalPopulation() {
        int total = 0;
        for (Integer value : populationBySpecies.values()) {
            total += value;
        }
        return total;
    }

    /**
     * Builds a statistics snapshot directly from a world snapshot.
     */
    public static StatisticsSnapshot fromWorldSnapshot(com.example.tleilax.simulation.WorldSnapshot snapshot) {
        LinkedHashMap<String, Integer> populations = new LinkedHashMap<>();
        int grassTiles = 0;

        for (com.example.tleilax.simulation.WorldSnapshot.CellSnapshot cell : snapshot.cells()) {
            if (cell.animal() != null) {
                String type = cell.animal().type().name();
                populations.put(type, populations.getOrDefault(type, 0) + 1);
            }

            if (cell.plant() != null) {
                String type = cell.plant().plantType().name();
                populations.put(type, populations.getOrDefault(type, 0) + 1);
            }

            if (cell.grassAmount() > 0) {
                grassTiles++;
            }
        }

        float coverage = (float) grassTiles / (snapshot.width() * snapshot.height());

        return new StatisticsSnapshot((int) snapshot.tickCount(), populations, null, coverage);
    }
}
