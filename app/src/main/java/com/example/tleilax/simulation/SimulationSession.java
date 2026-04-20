package com.example.tleilax.simulation;

import androidx.annotation.NonNull;

/**
 * Holds shared simulation runtime state used across fragments.
 */
public final class SimulationSession {

    @NonNull
    private static final SimulationEngine ENGINE = new SimulationEngine();
    private static boolean worldInitialized;
    private static boolean playing;

    private SimulationSession() {
    }

    /**
     * Returns the singleton engine used by the current app session.
     */
    @NonNull
    public static SimulationEngine getEngine() {
        return ENGINE;
    }

    /**
     * Marks whether an actual world has been generated or loaded yet.
     */
    public static boolean isWorldInitialized() {
        return worldInitialized;
    }

    public static void setWorldInitialized(boolean initialized) {
        worldInitialized = initialized;
    }

    public static boolean isPlaying() {
        return playing;
    }

    /**
     * Starts or pauses the shared engine and mirrors that state in the session flag.
     */
    public static void setPlaying(boolean isPlaying) {
        playing = isPlaying;
        if (playing) {
            ENGINE.start();
        } else {
            ENGINE.pause();
        }
    }
}
