package com.example.tleilax.simulation;

import androidx.annotation.NonNull;

/**
 * Immutable description of an active gameplay event area in world coordinates.
 */
public record ActiveEventZone(
        @NonNull SimulationEventType eventType,
        float centerX,
        float centerY,
        float radiusTiles,
        int ticksRemaining
) {
}
