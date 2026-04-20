package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.EntityType;

import java.util.List;

public record WorldSnapshot(
        int width,
        int height,
        long tickCount,
        @NonNull TerrainType defaultTerrainType,
        @NonNull List<CellSnapshot> cells
) {
    public record CellSnapshot(
            int x,
            int y,
            @NonNull TerrainType terrainType,
            int grassAmount,
            @Nullable PlantSnapshot plant,
            @Nullable AnimalSnapshot animal
    ) {
    }

    public record PlantSnapshot(
            @NonNull PlantType plantType,
            int blockingHeight,
            boolean dead,
            int resourceAmount,
            int durability,
            int regrowProgress,
            int lifecycleTicksRemaining,
            @Nullable TreeVariant treeVariant,
            @Nullable TreeLifeStage treeLifeStage
    ) {
    }

    public record AnimalSnapshot(
            @NonNull EntityType type,
            int energy,
            int health,
            float preciseX,
            float preciseY
    ) {
    }
}
