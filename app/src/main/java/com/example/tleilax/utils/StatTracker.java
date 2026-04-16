package com.example.tleilax.utils;

import androidx.annotation.NonNull;

import com.example.tleilax.simulation.SimulationEngine;
import com.example.tleilax.simulation.WorldSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StatTracker implements SimulationEngine.Listener {

    private static final StatTracker INSTANCE = new StatTracker();
    private static final int MAX_HISTORY_SIZE = 500;

    public static StatTracker getInstance() {
        return INSTANCE;
    }

    private final List<StatisticsSnapshot> history = new ArrayList<>();

    private StatTracker() {
    }

    public synchronized void startTracking(@NonNull SimulationEngine engine) {
        engine.addListener(this);
    }

    public synchronized void stopTracking(@NonNull SimulationEngine engine) {
        engine.removeListener(this);
    }

    public synchronized void addListener(@NonNull SimulationEngine.Listener listener) {
        // No-op for now as we use the engine's listener system directly in fragments
    }

    @Override
    public synchronized void onWorldUpdated(@NonNull WorldSnapshot snapshot) {
        // Only track every 5 ticks or so to keep history manageable and avoid excessive UI updates
        // but we need enough precision for the graph.
        // Actually, let's track every tick but limit total size.
        
        StatisticsSnapshot stat = StatisticsSnapshot.fromWorldSnapshot(snapshot);
        
        // If it's a reset (tick 0), clear history
        if (snapshot.tickCount() == 0) {
            history.clear();
        }

        // Deduplicate or update last if same tick (e.g. manual update)
        if (!history.isEmpty() && history.get(history.size() - 1).getTick() == stat.getTick()) {
            history.set(history.size() - 1, stat);
        } else {
            history.add(stat);
        }

        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
    }

    public synchronized List<StatisticsSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void clear() {
        history.clear();
    }
}
