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
    }

    @Override
    public synchronized void onWorldUpdated(@NonNull WorldSnapshot snapshot) {
        
        StatisticsSnapshot stat = StatisticsSnapshot.fromWorldSnapshot(snapshot);
        
        if (snapshot.tickCount() == 0) {
            history.clear();
        }

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
