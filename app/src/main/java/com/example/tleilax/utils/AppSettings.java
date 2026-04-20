package com.example.tleilax.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class AppSettings {

    private static final String PREFS_NAME = "tleilax_settings";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_SHOW_GRID = "show_grid";
    private static final String KEY_GRASS_COVERAGE_PERCENT = "grass_coverage_percent";
    private static final String KEY_WOLF_COUNT = "wolf_count";
    private static final String KEY_RABBIT_COUNT = "rabbit_count";
    private static final String KEY_MOUSE_COUNT = "mouse_count";
    private static final String KEY_DEER_COUNT = "deer_count";
    private static final String KEY_BERRY_BUSH_COUNT = "berry_bush_count";
    private static final String KEY_TREE_COUNT = "tree_count";
    private static final int MAX_SPAWN_COUNT = 256;

    public static final int DEFAULT_WOLF_COUNT = 3;
    public static final int DEFAULT_RABBIT_COUNT = 12;
    public static final int DEFAULT_MOUSE_COUNT = 10;
    public static final int DEFAULT_DEER_COUNT = 6;
    public static final int DEFAULT_BERRY_BUSH_COUNT = 85;
    public static final int DEFAULT_TREE_COUNT = 128;

    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();

    private AppSettings() {
    }

    public static boolean isMusicEnabled(@NonNull Context context) {
        return preferences(context).getBoolean(KEY_MUSIC_ENABLED, true);
    }

    public static void setMusicEnabled(@NonNull Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply();
        for (Listener listener : LISTENERS) {
            listener.onMusicEnabledChanged(enabled);
        }
    }

    public static boolean isGridVisible(@NonNull Context context) {
        return preferences(context).getBoolean(KEY_SHOW_GRID, true);
    }

    public static void setGridVisible(@NonNull Context context, boolean visible) {
        preferences(context).edit().putBoolean(KEY_SHOW_GRID, visible).apply();
        for (Listener listener : LISTENERS) {
            listener.onShowGridChanged(visible);
        }
    }

    public static int getGrassCoveragePercent(@NonNull Context context) {
        return preferences(context).getInt(KEY_GRASS_COVERAGE_PERCENT, 60);
    }

    public static void setGrassCoveragePercent(@NonNull Context context, int percent) {
        int normalized = Math.max(0, Math.min(100, percent));
        preferences(context).edit().putInt(KEY_GRASS_COVERAGE_PERCENT, normalized).apply();
        for (Listener listener : LISTENERS) {
            listener.onGrassCoverageChanged(normalized);
        }
    }

    public static int getWolfCount(@NonNull Context context) {
        return preferences(context).getInt(KEY_WOLF_COUNT, DEFAULT_WOLF_COUNT);
    }

    public static void setWolfCount(@NonNull Context context, int count) {
        preferences(context).edit().putInt(KEY_WOLF_COUNT, normalizeSpawnCount(count)).apply();
    }

    public static int getRabbitCount(@NonNull Context context) {
        return preferences(context).getInt(KEY_RABBIT_COUNT, DEFAULT_RABBIT_COUNT);
    }

    public static void setRabbitCount(@NonNull Context context, int count) {
        preferences(context).edit().putInt(KEY_RABBIT_COUNT, normalizeSpawnCount(count)).apply();
    }

    public static int getMouseCount(@NonNull Context context) {
        return preferences(context).getInt(KEY_MOUSE_COUNT, DEFAULT_MOUSE_COUNT);
    }

    public static void setMouseCount(@NonNull Context context, int count) {
        preferences(context).edit().putInt(KEY_MOUSE_COUNT, normalizeSpawnCount(count)).apply();
    }

    public static int getDeerCount(@NonNull Context context) {
        return preferences(context).getInt(KEY_DEER_COUNT, DEFAULT_DEER_COUNT);
    }

    public static void setDeerCount(@NonNull Context context, int count) {
        preferences(context).edit().putInt(KEY_DEER_COUNT, normalizeSpawnCount(count)).apply();
    }

    public static int getBerryBushCount(@NonNull Context context) {
        return preferences(context).getInt(KEY_BERRY_BUSH_COUNT, DEFAULT_BERRY_BUSH_COUNT);
    }

    public static void setBerryBushCount(@NonNull Context context, int count) {
        preferences(context).edit().putInt(KEY_BERRY_BUSH_COUNT, normalizeSpawnCount(count)).apply();
    }

    public static int getTreeCount(@NonNull Context context) {
        return preferences(context).getInt(KEY_TREE_COUNT, DEFAULT_TREE_COUNT);
    }

    public static void setTreeCount(@NonNull Context context, int count) {
        preferences(context).edit().putInt(KEY_TREE_COUNT, normalizeSpawnCount(count)).apply();
    }

    public static void resetToDefaults(@NonNull Context context) {
        SharedPreferences preferences = preferences(context);
        preferences.edit()
                .putBoolean(KEY_MUSIC_ENABLED, true)
                .putBoolean(KEY_SHOW_GRID, true)
                .putInt(KEY_GRASS_COVERAGE_PERCENT, 60)
                .putInt(KEY_WOLF_COUNT, DEFAULT_WOLF_COUNT)
                .putInt(KEY_RABBIT_COUNT, DEFAULT_RABBIT_COUNT)
                .putInt(KEY_MOUSE_COUNT, DEFAULT_MOUSE_COUNT)
                .putInt(KEY_DEER_COUNT, DEFAULT_DEER_COUNT)
                .putInt(KEY_BERRY_BUSH_COUNT, DEFAULT_BERRY_BUSH_COUNT)
                .putInt(KEY_TREE_COUNT, DEFAULT_TREE_COUNT)
                .apply();
        for (Listener listener : LISTENERS) {
            listener.onMusicEnabledChanged(true);
            listener.onShowGridChanged(true);
            listener.onGrassCoverageChanged(60);
        }
    }

    public static void addListener(@NonNull Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(@NonNull Listener listener) {
        LISTENERS.remove(listener);
    }

    @NonNull
    private static SharedPreferences preferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static int normalizeSpawnCount(int count) {
        return Math.max(0, Math.min(MAX_SPAWN_COUNT, count));
    }

    public interface Listener {
        void onMusicEnabledChanged(boolean enabled);

        void onShowGridChanged(boolean visible);

        void onGrassCoverageChanged(int percent);
    }
}
