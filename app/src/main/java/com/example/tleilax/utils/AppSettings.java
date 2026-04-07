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

    public static void resetToDefaults(@NonNull Context context) {
        SharedPreferences preferences = preferences(context);
        preferences.edit()
                .putBoolean(KEY_MUSIC_ENABLED, true)
                .putBoolean(KEY_SHOW_GRID, true)
                .putInt(KEY_GRASS_COVERAGE_PERCENT, 60)
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

    public interface Listener {
        void onMusicEnabledChanged(boolean enabled);

        void onShowGridChanged(boolean visible);

        void onGrassCoverageChanged(int percent);
    }
}
