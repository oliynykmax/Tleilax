package com.example.tleilax.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.simulation.WorldSnapshot;

import java.util.List;

public interface SaveLoad {

    long save(@NonNull String name, @NonNull WorldSnapshot state);

    @Nullable
    WorldSnapshot load(long id);

    @NonNull
    List<SimulationSaveEntity> listSaves();

    void delete(@NonNull SimulationSaveEntity saveEntity);

    void deleteAll();
}
