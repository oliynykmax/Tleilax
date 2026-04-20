package com.example.tleilax.simulation;

import androidx.annotation.NonNull;

public record ActiveEventZone(
        @NonNull SimulationEventType eventType,
        float centerX,
        float centerY,
        float radiusTiles,
        int ticksRemaining
) {
}
