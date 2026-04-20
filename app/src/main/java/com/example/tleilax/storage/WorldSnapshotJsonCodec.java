package com.example.tleilax.storage;

import androidx.annotation.NonNull;

import com.example.tleilax.model.EntityType;
import com.example.tleilax.simulation.PlantType;
import com.example.tleilax.simulation.TerrainType;
import com.example.tleilax.simulation.TreeLifeStage;
import com.example.tleilax.simulation.TreeVariant;
import com.example.tleilax.simulation.WorldSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class WorldSnapshotJsonCodec {

    private static final String HEADER_PREFIX = "TLEILAX_SNAPSHOT_V2";
    private static final String HEADER_PREFIX_V1 = "TLEILAX_SNAPSHOT_V1";
    private static final int NULL_ENUM = -1;

    private WorldSnapshotJsonCodec() {
    }

    @NonNull
    public static String encode(@NonNull WorldSnapshot snapshot) {
        StringBuilder builder = new StringBuilder(snapshot.cells().size() * 48);
        builder.append(HEADER_PREFIX)
                .append('|')
                .append(snapshot.width())
                .append('|')
                .append(snapshot.height())
                .append('|')
                .append(snapshot.tickCount())
                .append('|')
                .append(snapshot.defaultTerrainType().ordinal())
                .append('\n');

        for (WorldSnapshot.CellSnapshot cell : snapshot.cells()) {
            builder.append(cell.x()).append('|')
                    .append(cell.y()).append('|')
                    .append(cell.terrainType().ordinal()).append('|')
                    .append(cell.grassAmount()).append('|');

            appendPlant(builder, cell.plant());
            builder.append('|');
            appendAnimal(builder, cell.animal());
            builder.append('\n');
        }
        return builder.toString();
    }

    @NonNull
    public static WorldSnapshot decode(@NonNull String encodedSnapshot) {
        String[] lines = encodedSnapshot.split("\n");
        if (lines.length == 0) {
            throw new IllegalStateException("Snapshot is empty.");
        }

        String[] header = lines[0].split("\\|");
        boolean version2 = HEADER_PREFIX.equals(header[0]);
        if (header.length < 5 || (!version2 && !HEADER_PREFIX_V1.equals(header[0]))) {
            throw new IllegalStateException("Unsupported snapshot format.");
        }

        int width = Integer.parseInt(header[1]);
        int height = Integer.parseInt(header[2]);
        long tickCount = Long.parseLong(header[3]);
        TerrainType defaultTerrainType = TerrainType.values()[Integer.parseInt(header[4])];

        List<WorldSnapshot.CellSnapshot> cells = new ArrayList<>(Math.max(0, lines.length - 1));
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            String[] parts = lines[index].split("\\|", -1);
            if (parts.length < 6) {
                continue;
            }
            cells.add(new WorldSnapshot.CellSnapshot(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    TerrainType.values()[Integer.parseInt(parts[2])],
                    Integer.parseInt(parts[3]),
                    decodePlant(parts[4]),
                    decodeAnimal(parts[5], Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), version2)
            ));
        }

        return new WorldSnapshot(width, height, tickCount, defaultTerrainType, cells);
    }

    private static void appendPlant(@NonNull StringBuilder builder, WorldSnapshot.PlantSnapshot plantSnapshot) {
        if (plantSnapshot == null) {
            builder.append('~');
            return;
        }
        builder.append(plantSnapshot.plantType().ordinal()).append(',')
                .append(plantSnapshot.blockingHeight()).append(',')
                .append(plantSnapshot.dead() ? 1 : 0).append(',')
                .append(plantSnapshot.resourceAmount()).append(',')
                .append(plantSnapshot.durability()).append(',')
                .append(plantSnapshot.regrowProgress()).append(',')
                .append(plantSnapshot.lifecycleTicksRemaining()).append(',')
                .append(plantSnapshot.treeVariant() != null ? plantSnapshot.treeVariant().ordinal() : NULL_ENUM).append(',')
                .append(plantSnapshot.treeLifeStage() != null ? plantSnapshot.treeLifeStage().ordinal() : NULL_ENUM);
    }

    private static void appendAnimal(@NonNull StringBuilder builder, WorldSnapshot.AnimalSnapshot animalSnapshot) {
        if (animalSnapshot == null) {
            builder.append('~');
            return;
        }
        builder.append(animalSnapshot.type().ordinal()).append(',')
                .append(animalSnapshot.energy()).append(',')
                .append(animalSnapshot.health()).append(',')
                .append(animalSnapshot.preciseX()).append(',')
                .append(animalSnapshot.preciseY());
    }

    private static WorldSnapshot.PlantSnapshot decodePlant(@NonNull String encodedPlant) {
        if ("~".equals(encodedPlant)) {
            return null;
        }
        String[] values = encodedPlant.split(",", -1);
        int treeVariantOrdinal = Integer.parseInt(values[7]);
        int treeLifeStageOrdinal = Integer.parseInt(values[8]);
        return new WorldSnapshot.PlantSnapshot(
                PlantType.values()[Integer.parseInt(values[0])],
                Integer.parseInt(values[1]),
                Integer.parseInt(values[2]) == 1,
                Integer.parseInt(values[3]),
                Integer.parseInt(values[4]),
                Integer.parseInt(values[5]),
                Integer.parseInt(values[6]),
                treeVariantOrdinal >= 0 ? TreeVariant.values()[treeVariantOrdinal] : null,
                treeLifeStageOrdinal >= 0 ? TreeLifeStage.values()[treeLifeStageOrdinal] : null
        );
    }

    private static WorldSnapshot.AnimalSnapshot decodeAnimal(
            @NonNull String encodedAnimal,
            int cellX,
            int cellY,
            boolean version2
    ) {
        if ("~".equals(encodedAnimal)) {
            return null;
        }
        String[] values = encodedAnimal.split(",", -1);
        return new WorldSnapshot.AnimalSnapshot(
                EntityType.values()[Integer.parseInt(values[0])],
                Integer.parseInt(values[1]),
                Integer.parseInt(values[2]),
                version2 && values.length > 3 ? Float.parseFloat(values[3]) : cellX + 0.5f,
                version2 && values.length > 4 ? Float.parseFloat(values[4]) : cellY + 0.5f
        );
    }
}
