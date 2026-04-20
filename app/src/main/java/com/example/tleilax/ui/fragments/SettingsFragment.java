package com.example.tleilax.ui.fragments;

import android.content.Context;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.tleilax.R;
import com.example.tleilax.TleilaxApp;
import com.example.tleilax.databinding.FragmentSettingsBinding;
import com.example.tleilax.simulation.SimulationEngine;
import com.example.tleilax.simulation.SimulationSession;
import com.example.tleilax.storage.SimulationStorage;
import com.example.tleilax.utils.AppSettings;
import com.google.android.material.slider.Slider;

import java.util.function.IntConsumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment implements AppSettings.Listener {

    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean musicEnabled = AppSettings.isMusicEnabled(requireContext());
        boolean gridVisible = AppSettings.isGridVisible(requireContext());
        int grassCoveragePercent = AppSettings.getGrassCoveragePercent(requireContext());

        binding.switchMusic.setChecked(musicEnabled);
        binding.switchShowGrid.setChecked(gridVisible);
        renderMusicHint(musicEnabled);
        syncWorldGenerationControls();

        binding.switchMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            renderMusicHint(isChecked);
            AppSettings.setMusicEnabled(requireContext(), isChecked);
        });
        binding.switchShowGrid.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppSettings.setGridVisible(requireContext(), isChecked));
        binding.sliderGrassCoverage.addOnChangeListener((slider, value, fromUser) -> {
            int percent = Math.round(value);
            renderGrassCoverage(percent);
            if (fromUser) {
                AppSettings.setGrassCoveragePercent(requireContext(), percent);
            }
        });
        bindSpawnCountSlider(binding.sliderWolfCount, binding.textWolfCountValue,
                AppSettings.getWolfCount(requireContext()), count -> AppSettings.setWolfCount(requireContext(), count));
        bindSpawnCountSlider(binding.sliderRabbitCount, binding.textRabbitCountValue,
                AppSettings.getRabbitCount(requireContext()), count -> AppSettings.setRabbitCount(requireContext(), count));
        bindSpawnCountSlider(binding.sliderMouseCount, binding.textMouseCountValue,
                AppSettings.getMouseCount(requireContext()), count -> AppSettings.setMouseCount(requireContext(), count));
        bindSpawnCountSlider(binding.sliderDeerCount, binding.textDeerCountValue,
                AppSettings.getDeerCount(requireContext()), count -> AppSettings.setDeerCount(requireContext(), count));
        bindSpawnCountSlider(binding.sliderBerryBushCount, binding.textBerryBushCountValue,
                AppSettings.getBerryBushCount(requireContext()), count -> AppSettings.setBerryBushCount(requireContext(), count));
        bindSpawnCountSlider(binding.sliderTreeCount, binding.textTreeCountValue,
                AppSettings.getTreeCount(requireContext()), count -> AppSettings.setTreeCount(requireContext(), count));
        binding.btnResetEverything.setOnClickListener(v -> showResetEverythingDialog());

        AppSettings.addListener(this);
    }

    @Override
    public void onDestroyView() {
        AppSettings.removeListener(this);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupExecutor.shutdown();
    }

    @Override
    public void onMusicEnabledChanged(boolean enabled) {
        if (binding != null && binding.switchMusic.isChecked() != enabled) {
            binding.switchMusic.setChecked(enabled);
            renderMusicHint(enabled);
        }
    }

    @Override
    public void onShowGridChanged(boolean visible) {
        if (binding != null && binding.switchShowGrid.isChecked() != visible) {
            binding.switchShowGrid.setChecked(visible);
        }
    }

    @Override
    public void onGrassCoverageChanged(int percent) {
        if (binding != null) {
            if (Math.round(binding.sliderGrassCoverage.getValue()) != percent) {
                binding.sliderGrassCoverage.setValue(percent);
            }
            renderGrassCoverage(percent);
        }
    }

    private void renderMusicHint(boolean musicEnabled) {
        binding.textMusicHint.setText(musicEnabled
                ? R.string.settings_music_on_hint
                : R.string.settings_music_off_hint);
    }

    private void renderGrassCoverage(int percent) {
        binding.textGrassCoverageValue.setText(getString(R.string.settings_grass_coverage_value, percent));
    }

    private void bindSpawnCountSlider(
            @NonNull Slider slider,
            @NonNull TextView valueView,
            int initialValue,
            @NonNull IntConsumer persistAction
    ) {
        slider.setValue(initialValue);
        renderSpawnCount(valueView, initialValue);
        slider.addOnChangeListener((changedSlider, value, fromUser) -> {
            int count = Math.round(value);
            renderSpawnCount(valueView, count);
            if (fromUser) {
                persistAction.accept(count);
            }
        });
    }

    private void renderSpawnCount(@NonNull TextView valueView, int count) {
        valueView.setText(getString(R.string.settings_spawn_count_value, count));
    }

    private void syncWorldGenerationControls() {
        if (binding == null) {
            return;
        }
        int grassCoveragePercent = AppSettings.getGrassCoveragePercent(requireContext());
        binding.sliderGrassCoverage.setValue(grassCoveragePercent);
        renderGrassCoverage(grassCoveragePercent);

        binding.sliderWolfCount.setValue(AppSettings.getWolfCount(requireContext()));
        binding.sliderRabbitCount.setValue(AppSettings.getRabbitCount(requireContext()));
        binding.sliderMouseCount.setValue(AppSettings.getMouseCount(requireContext()));
        binding.sliderDeerCount.setValue(AppSettings.getDeerCount(requireContext()));
        binding.sliderBerryBushCount.setValue(AppSettings.getBerryBushCount(requireContext()));
        binding.sliderTreeCount.setValue(AppSettings.getTreeCount(requireContext()));
    }

    private void showResetEverythingDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_settings_reset_everything, null, false);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.08f);
            dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        }
        dialogView.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_dialog_confirm).setOnClickListener(v -> {
            runFullReset();
            dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            int bottomOffset = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    28,
                    requireContext().getResources().getDisplayMetrics()
            );
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            dialog.getWindow().getAttributes().y = bottomOffset;
        }
    }

    private void runFullReset() {
        Context appContext = TleilaxApp.getAppContext();
        if (binding != null) {
            binding.btnResetEverything.setEnabled(false);
        }
        cleanupExecutor.execute(() -> {
            try {
                SimulationStorage simulationStorage = new SimulationStorage(appContext);
                simulationStorage.deleteAll();
                com.example.tleilax.utils.StatTracker.getInstance().clear();
                mainHandler.post(() -> {
                    AppSettings.resetToDefaults(appContext);
                    syncWorldGenerationControls();
                    SimulationSession.getEngine().reset(SimulationEngine.FIXED_WORLD_SIZE, SimulationEngine.FIXED_WORLD_SIZE);
                    SimulationSession.setWorldInitialized(true);
                    if (binding != null) {
                        binding.btnResetEverything.setEnabled(true);
                    }
                    if (isAdded()) {
                        Toast.makeText(appContext, R.string.settings_cleanup_done, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (RuntimeException exception) {
                mainHandler.post(() -> {
                    if (binding != null) {
                        binding.btnResetEverything.setEnabled(true);
                    }
                    if (isAdded()) {
                        Toast.makeText(appContext, R.string.settings_cleanup_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
