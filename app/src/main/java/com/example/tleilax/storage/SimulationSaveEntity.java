package com.example.tleilax.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "simulation_saves")
public class SimulationSaveEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    public long savedAtEpochMillis;
    public long tickCount;
    public int width;
    public int height;
    public int speciesAliveCount;

    @NonNull
    public String stateJson;

    public SimulationSaveEntity(
            @NonNull String name,
            long savedAtEpochMillis,
            long tickCount,
            int width,
            int height,
            int speciesAliveCount,
            @NonNull String stateJson
    ) {
        this.name = name;
        this.savedAtEpochMillis = savedAtEpochMillis;
        this.tickCount = tickCount;
        this.width = width;
        this.height = height;
        this.speciesAliveCount = speciesAliveCount;
        this.stateJson = stateJson;
    }
}
