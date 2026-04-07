package com.example.tleilax.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.example.tleilax.model.EntityType;
import com.example.tleilax.simulation.PlantType;
import com.example.tleilax.simulation.TerrainType;
import com.example.tleilax.simulation.TreeVariant;

import java.util.HashMap;
import java.util.Map;

public class TextureLibrary {

    private final Context context;
    private final Map<String, Bitmap> cache = new HashMap<>();

    public TextureLibrary(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public Bitmap getTerrainTexture(@NonNull TerrainType terrainType) {
        return getOrCreate("terrain_" + terrainType.name().toLowerCase(), terrainType.getRenderColor(), Pattern.SAND);
    }

    @NonNull
    public Bitmap getGrassTexture() {
        return getOrCreate("ground_grass", EntityType.GRASS.getRenderColor(), Pattern.GRASS);
    }

    @NonNull
    public Bitmap getBushTexture(boolean dead) {
        return getOrCreate(dead ? "plant_bush_dead" : "plant_bush_alive",
                dead ? 0xFF7C6F64 : PlantType.BERRY_BUSH.getRenderColor(),
                dead ? Pattern.DEAD : Pattern.BUSH);
    }

    @NonNull
    public Bitmap getTreeTexture(@NonNull TreeVariant treeVariant, boolean dead) {
        String key = "plant_tree_" + treeVariant.name().toLowerCase() + (dead ? "_dead" : "_alive");
        @ColorInt int color = dead ? 0xFF665C54 : PlantType.TREE.getRenderColor();
        return getOrCreate(key, color, dead ? Pattern.DEAD : Pattern.TREE);
    }

    @NonNull
    public Bitmap getAnimalTexture(@NonNull EntityType entityType) {
        return getOrCreate("animal_" + entityType.name().toLowerCase(), entityType.getRenderColor(), Pattern.ANIMAL);
    }

    @NonNull
    private Bitmap getOrCreate(@NonNull String name, @ColorInt int fallbackColor, @NonNull Pattern pattern) {
        Bitmap cached = cache.get(name);
        if (cached != null) {
            return cached;
        }

        int resourceId = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        Bitmap bitmap;
        if (resourceId != 0) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        } else {
            bitmap = generateFallbackBitmap(fallbackColor, pattern);
        }
        cache.put(name, bitmap);
        return bitmap;
    }

    @NonNull
    private Bitmap generateFallbackBitmap(@ColorInt int baseColor, @NonNull Pattern pattern) {
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(baseColor);
        canvas.drawColor(adjust(baseColor, -18));
        switch (pattern) {
            case SAND -> {
                for (int i = 0; i < 12; i++) {
                    paint.setColor(i % 2 == 0 ? adjust(baseColor, 10) : adjust(baseColor, -10));
                    canvas.drawCircle(6 + (i % 4) * 16, 8 + (i / 4) * 18, 4 + (i % 3), paint);
                }
            }
            case GRASS -> {
                for (int x = 6; x < 64; x += 10) {
                    paint.setStrokeWidth(3f);
                    paint.setColor(adjust(baseColor, 20));
                    canvas.drawLine(x, 63, x - 3, 28, paint);
                    paint.setColor(adjust(baseColor, -8));
                    canvas.drawLine(x + 3, 63, x + 6, 24, paint);
                }
            }
            case BUSH -> {
                paint.setColor(adjust(baseColor, 16));
                canvas.drawCircle(24, 30, 14, paint);
                canvas.drawCircle(40, 30, 16, paint);
                paint.setColor(adjust(baseColor, -15));
                canvas.drawCircle(32, 42, 18, paint);
            }
            case TREE -> {
                paint.setColor(0xFF5B3A29);
                canvas.drawRect(26, 26, 38, 63, paint);
                paint.setColor(adjust(baseColor, 10));
                canvas.drawCircle(32, 18, 18, paint);
                paint.setColor(adjust(baseColor, -12));
                canvas.drawCircle(20, 24, 12, paint);
                canvas.drawCircle(44, 24, 12, paint);
            }
            case ANIMAL -> {
                paint.setColor(adjust(baseColor, 10));
                canvas.drawCircle(32, 32, 18, paint);
                paint.setColor(adjust(baseColor, -18));
                canvas.drawCircle(20, 20, 8, paint);
                canvas.drawCircle(44, 20, 8, paint);
            }
            case DEAD -> {
                paint.setColor(adjust(baseColor, 8));
                canvas.drawLine(12, 12, 52, 52, paint);
                canvas.drawLine(12, 52, 52, 12, paint);
            }
        }
        return bitmap;
    }

    @ColorInt
    private int adjust(@ColorInt int color, int delta) {
        return Color.argb(
                Color.alpha(color),
                clamp(Color.red(color) + delta),
                clamp(Color.green(color) + delta),
                clamp(Color.blue(color) + delta)
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private enum Pattern {
        SAND,
        GRASS,
        BUSH,
        TREE,
        ANIMAL,
        DEAD
    }
}
