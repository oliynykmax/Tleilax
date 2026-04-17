package com.example.tleilax.simulation;

import androidx.annotation.NonNull;
import com.example.tleilax.model.Entity;
import com.example.tleilax.model.EntityType;

import java.util.Random;

/**
 * Handles the procedural generation of the simulation world.
 * This includes generating terrain (grass patches using wave functions and dithering),
 * placing initial plant life, and spawning the starting animal populations.
 */
public class WorldGenerator {

    /**
     * Seeds the grid with initial grass, plants, and animals.
     */
    public void seedWorld(
            @NonNull Grid grid,
            @NonNull Random random,
            int grassCoveragePercent
    ) {
        seedGrassPatches(grid, random, grassCoveragePercent);
        seedPlantsNearGrass(grid, random);
        addRandomAnimals(grid, EntityType.WOLF, 3, random);
        addRandomAnimals(grid, EntityType.RABBIT, 12, random);
        addRandomAnimals(grid, EntityType.MOUSE, 10, random);
        addRandomAnimals(grid, EntityType.DEER, 6, random);
    }

    private void seedGrassPatches(@NonNull Grid grid, @NonNull Random random, int targetCoveragePercent) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        boolean[][] grassMask = new boolean[height][width];
        float[][] densityMap = new float[height][width];

        // Generate base density using overlapping sine waves for a natural, patchy look
        float phaseA = random.nextFloat() * 6.2831855f;
        float phaseB = random.nextFloat() * 6.2831855f;
        float phaseC = random.nextFloat() * 6.2831855f;
        float frequencyA = 2.2f / Math.max(width, height);
        float frequencyB = 4.7f / Math.max(width, height);
        float frequencyC = 8.1f / Math.max(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float normalizedX = x / (float) Math.max(1, width - 1);
                float normalizedY = y / (float) Math.max(1, height - 1);

                float waveA = (float) Math.sin((x + y * 0.7f) * frequencyA * width + phaseA);
                float waveB = (float) Math.sin((x * 0.35f - y) * frequencyB * width + phaseB);
                float waveC = (float) Math.cos((x * 0.8f + y * 1.15f) * frequencyC * width + phaseC);
                // Encourage growth towards the center of the grid
                float radialBias = 1.0f - Math.min(1.0f,
                        (float) Math.hypot(normalizedX - 0.5f, normalizedY - 0.5f) * 1.35f);

                densityMap[y][x] = (waveA * 0.45f) + (waveB * 0.30f) + (waveC * 0.20f) + (radialBias * 0.35f);
            }
        }

        // Apply dithering to create organic edges rather than sharp cutoffs
        float[][] ditherMatrix = {
                {0.0f, 0.5f, 0.125f, 0.625f},
                {0.75f, 0.25f, 0.875f, 0.375f},
                {0.1875f, 0.6875f, 0.0625f, 0.5625f},
                {0.9375f, 0.4375f, 0.8125f, 0.3125f}
        };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float normalizedDensity = (densityMap[y][x] + 1.0f) / 2.0f;
                float threshold = coverageThresholdForPercent(targetCoveragePercent)
                        + ((ditherMatrix[y % 4][x % 4] - 0.5f) * 0.18f);
                grassMask[y][x] = normalizedDensity > threshold;
            }
        }

        smoothGrassMask(grid, grassMask, 2);

        // Apply the generated mask to the actual grid
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grassMask[y][x]) {
                    grid.setGrass(x, y, 1);
                }
            }
        }
    }

    private float coverageThresholdForPercent(int targetCoveragePercent) {
        float clampedPercent = Math.max(0, Math.min(100, targetCoveragePercent));
        return 0.864f - (clampedPercent * 0.00332f);
    }

    /**
     * Applies cellular automata smoothing to the grass mask to reduce noise
     * and create contiguous grassy plains.
     */
    private void smoothGrassMask(@NonNull Grid grid, @NonNull boolean[][] grassMask, int iterations) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        for (int iteration = 0; iteration < iterations; iteration++) {
            boolean[][] nextMask = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int neighbors = countGrassNeighbors(grid, grassMask, x, y);
                    // Standard Game of Life-esque smoothing rules
                    if (grassMask[y][x]) {
                        nextMask[y][x] = neighbors >= 3;
                    } else {
                        nextMask[y][x] = neighbors >= 4;
                    }
                }
            }
            for (int y = 0; y < height; y++) {
                System.arraycopy(nextMask[y], 0, grassMask[y], 0, width);
            }
        }
    }

    private int countGrassNeighbors(@NonNull Grid grid, @NonNull boolean[][] grassMask, int x, int y) {
        int count = 0;
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (offsetX == 0 && offsetY == 0) {
                    continue;
                }
                int neighborX = x + offsetX;
                int neighborY = y + offsetY;
                if (!grid.isInBounds(neighborX, neighborY)) {
                    continue;
                }
                if (grassMask[neighborY][neighborX]) {
                    count++;
                }
            }
        }
        return count;
    }

    private void seedPlantsNearGrass(@NonNull Grid grid, @NonNull Random random) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int berryBushes = Math.max(6, width / 3);
        int trees = Math.max(8, width / 2);
        int attempts = 0;

        // Try to place plants on grass edges up to a maximum number of attempts
        while ((berryBushes > 0 || trees > 0) && attempts < 800) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            Tile tile = grid.getTile(x, y);

            if (tile == null || !tile.hasGrass()) {
                attempts++;
                continue;
            }

            boolean grassEdge = isGrassEdge(grid, x, y);

            if (trees > 0 && grassEdge && tile.getPlantState() == null) {
                TreeVariant treeVariant = switch (random.nextInt(3)) {
                    case 0 -> TreeVariant.LOW;
                    case 1 -> TreeVariant.MEDIUM;
                    default -> TreeVariant.TALL;
                };
                grid.placeTree(x, y, treeVariant);
                trees--;
            } else if (berryBushes > 0 && tile.getPlantState() == null) {
                grid.placeBerryBush(x, y);
                berryBushes--;
            }
            attempts++;
        }
    }

    private boolean isGrassEdge(@NonNull Grid grid, int x, int y) {
        for (Grid.Position neighbor : grid.getAdjacentPositions(x, y)) {
            Tile neighborTile = grid.getTile(neighbor.x(), neighbor.y());
            if (neighborTile != null && !neighborTile.hasGrass()) {
                return true;
            }
        }
        return false;
    }

    private void addRandomAnimals(@NonNull Grid grid, @NonNull EntityType type, int count, @NonNull Random random) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        int attempts = 0;
        int placed = 0;
        int maxAttempts = count * 30;

        while (placed < count && attempts < maxAttempts) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            Entity entity = Entity.create(type, x, y);
            if (grid.placeAnimal(entity)) {
                placed++;
            }
            attempts++;
        }
    }
}