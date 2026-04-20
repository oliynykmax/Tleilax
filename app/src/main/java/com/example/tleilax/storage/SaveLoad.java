package com.example.tleilax.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.simulation.WorldSnapshot;

import java.util.List;

/**
 * Contract for persisting and restoring simulation snapshots.
 */
public interface SaveLoad {

    /**
     * Saves a world snapshot under the given display name.
     */
    long save(@NonNull String name, @NonNull WorldSnapshot state);

    /**
     * Loads a previously saved world snapshot by database id.
     */
    @Nullable
    WorldSnapshot load(long id);

    /**
     * Lists all known saved simulations.
     */
    @NonNull
    List<SimulationSaveEntity> listSaves();

    /**
     * Deletes one saved simulation entry.
     */
    void delete(@NonNull SimulationSaveEntity saveEntity);

    /**
     * Deletes every saved simulation entry.
     */
    void deleteAll();
}
