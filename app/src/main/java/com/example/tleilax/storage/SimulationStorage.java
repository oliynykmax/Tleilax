package com.example.tleilax.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.EntityType;
import com.example.tleilax.simulation.WorldSnapshot;

import java.util.List;

/**
 * Room-backed implementation of simulation save/load operations.
 */
public class SimulationStorage implements SaveLoad {

    @NonNull
    private final SimulationSaveDao saveDao;

    /**
     * Creates a storage facade backed by the shared Room database.
     */
    public SimulationStorage(@NonNull Context context) {
        saveDao = SimulationDatabase.getInstance(context).simulationSaveDao();
    }

    @Override
    public long save(@NonNull String name, @NonNull WorldSnapshot state) {
        long now = System.currentTimeMillis();
        SimulationSaveEntity saveEntity = new SimulationSaveEntity(
                name,
                now,
                state.tickCount(),
                state.width(),
                state.height(),
                countAliveSpecies(state),
                WorldSnapshotJsonCodec.encode(state)
        );
        return saveDao.insert(saveEntity);
    }

    @Nullable
    @Override
    public WorldSnapshot load(long id) {
        SimulationSaveEntity saveEntity = saveDao.getById(id);
        if (saveEntity == null) {
            return null;
        }
        return WorldSnapshotJsonCodec.decode(saveEntity.stateJson);
    }

    @NonNull
    @Override
    public List<SimulationSaveEntity> listSaves() {
        return saveDao.listSaves();
    }

    @Override
    public void delete(@NonNull SimulationSaveEntity saveEntity) {
        saveDao.delete(saveEntity);
    }

    @Override
    public void deleteAll() {
        saveDao.deleteAll();
    }

    private int countAliveSpecies(@NonNull WorldSnapshot snapshot) {
        boolean hasWolf = false;
        boolean hasRabbit = false;
        boolean hasMouse = false;
        boolean hasDeer = false;
        for (WorldSnapshot.CellSnapshot cell : snapshot.cells()) {
            if (cell.animal() == null) {
                continue;
            }
            EntityType type = cell.animal().type();
            if (type == EntityType.WOLF) {
                hasWolf = true;
            } else if (type == EntityType.RABBIT) {
                hasRabbit = true;
            } else if (type == EntityType.MOUSE) {
                hasMouse = true;
            } else if (type == EntityType.DEER) {
                hasDeer = true;
            }
        }
        int count = 0;
        if (hasWolf) {
            count++;
        }
        if (hasRabbit) {
            count++;
        }
        if (hasMouse) {
            count++;
        }
        if (hasDeer) {
            count++;
        }
        return count;
    }
}
