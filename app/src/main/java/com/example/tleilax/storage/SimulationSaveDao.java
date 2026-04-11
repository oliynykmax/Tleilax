package com.example.tleilax.storage;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SimulationSaveDao {

    @Query("SELECT * FROM simulation_saves ORDER BY savedAtEpochMillis DESC")
    List<SimulationSaveEntity> listSaves();

    @Query("SELECT * FROM simulation_saves WHERE id = :id LIMIT 1")
    SimulationSaveEntity getById(long id);

    @Insert
    long insert(SimulationSaveEntity saveEntity);

    @Delete
    void delete(SimulationSaveEntity saveEntity);

    @Query("DELETE FROM simulation_saves")
    void deleteAll();
}
