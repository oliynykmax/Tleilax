package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tleilax.model.Entity;

public class Tile {

    private final TerrainType terrainType;
    private int grassAmount;
    @Nullable
    private PlantState plantState;
    @Nullable
    private Entity animal;

    public Tile(@NonNull TerrainType terrainType) {
        this.terrainType = terrainType;
    }

    @NonNull
    public TerrainType getTerrainType() {
        return terrainType;
    }

    public int getGrassAmount() {
        return grassAmount;
    }

    public void setGrassAmount(int grassAmount) {
        this.grassAmount = Math.max(0, grassAmount);
    }

    public boolean hasGrass() {
        return grassAmount > 0;
    }

    public void clearGrass() {
        grassAmount = 0;
    }

    @Nullable
    public PlantState getPlantState() {
        return plantState;
    }

    public void setPlantState(@Nullable PlantState plantState) {
        this.plantState = plantState;
    }

    @Nullable
    public Entity getAnimal() {
        return animal;
    }

    public void setAnimal(@Nullable Entity animal) {
        this.animal = animal;
    }
}
