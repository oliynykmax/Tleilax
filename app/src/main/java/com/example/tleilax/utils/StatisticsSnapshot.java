package com.example.tleilax.utils;

import java.util.LinkedHashMap;
import java.util.Map;
public class StatisticsSnapshot {

    private final int tick;
    private final LinkedHashMap<String, Integer> populationBySpecies;
    private final String status;

    public StatisticsSnapshot(int tick, Map<String, Integer> populationBySpecies) {
        this(tick, populationBySpecies, null);
    }
    public StatisticsSnapshot(int tick, Map<String, Integer> populationBySpecies, String status) {
        this.tick = tick;
        this.populationBySpecies = new LinkedHashMap<>();

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
    public int getTotalPopulation() {
        int total = 0;
        for (Integer value : populationBySpecies.values()) {
            total += value;
        }
        return total;
    }
}
