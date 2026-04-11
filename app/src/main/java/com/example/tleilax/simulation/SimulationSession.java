package com.example.tleilax.simulation;

import androidx.annotation.NonNull;

public final class SimulationSession {

    @NonNull
    private static final SimulationEngine ENGINE = new SimulationEngine();
    private static boolean worldInitialized;

    private SimulationSession() {
    }

    @NonNull
    public static SimulationEngine getEngine() {
        return ENGINE;
    }

    public static boolean isWorldInitialized() {
        return worldInitialized;
    }

    public static void setWorldInitialized(boolean initialized) {
        worldInitialized = initialized;
    }
}
