package com.example.tleilax.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SimulationSaveEntity.class}, version = 1, exportSchema = false)
public abstract class SimulationDatabase extends RoomDatabase {

    private static volatile SimulationDatabase instance;

    @NonNull
    public static SimulationDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SimulationDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SimulationDatabase.class,
                            "tleilax-saves.db"
                    ).build();
                }
            }
        }
        return instance;
    }

    public abstract SimulationSaveDao simulationSaveDao();
}
