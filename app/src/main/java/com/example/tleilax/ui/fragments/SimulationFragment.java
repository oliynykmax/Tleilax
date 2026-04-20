package com.example.tleilax.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.tleilax.R;
import com.example.tleilax.databinding.FragmentSimulationBinding;
import com.example.tleilax.model.EntityType;
import com.example.tleilax.simulation.SimulationEngine;
import com.example.tleilax.simulation.SimulationSession;
import com.example.tleilax.utils.AppSettings;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SimulationFragment extends Fragment implements SimulationEngine.Listener, AppSettings.Listener {
    private static final long MIN_LOADING_DURATION_MS = 3000L;
    private static final long LOADING_PROGRESS_INTERVAL_MS = 90L;
    private static final Random LOADING_STATUS_RANDOM = new Random();

    private FragmentSimulationBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loadingProgressRunnable = this::tickLoadingProgress;
    private boolean isPlaying;
    private boolean worldReady;
    private boolean loadingCompleted;
    private long loadingStartedAtMs;
    private int speedMultiplier = 1;
    @NonNull
    private EntityType selectedEntityType = EntityType.WOLF;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSimulationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SimulationSession.getEngine().addListener(this);
        binding.btnPlayPause.setIconResource(
                SimulationSession.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
        );

        setupPlayPause();
        setupSpeedControl();
        setupSpeciesSelection();
        applySpeciesButtonIcons();
        setupCanvasInteractions();
        setupReset();

        binding.simulationCanvas.setSelectedEntityType(selectedEntityType);
        binding.simulationCanvas.setGridVisible(AppSettings.isGridVisible(requireContext()));
        AppSettings.addListener(this);

        if (SimulationSession.isWorldInitialized()) {
            worldReady = true;
            hideLoadingOverlay();
        } else {
            startLoadingSequence(R.string.loading_label_world_gen, this::initializeWorldAfterFirstFrame);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void setupPlayPause() {
        binding.btnPlayPause.setOnClickListener(v -> {
            if (!worldReady) {
                return;
            }
            boolean playing = !SimulationSession.isPlaying();
            SimulationSession.setPlaying(playing);
            binding.btnPlayPause.setIconResource(
                    playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
            );
        });
    }

    private void setupSpeedControl() {
        binding.speedToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_speed_1x) {
                speedMultiplier = 1;
            } else if (checkedId == R.id.btn_speed_2x) {
                speedMultiplier = 2;
            } else if (checkedId == R.id.btn_speed_4x) {
                speedMultiplier = 4;
            }
            SimulationSession.getEngine().setSpeedMultiplier(speedMultiplier);
        });
    }

    private void setupSpeciesSelection() {
        List<MaterialButton> speciesButtons = Arrays.asList(
                binding.btnWolf,
                binding.btnRabbit,
                binding.btnMouse,
                binding.btnDeer,
                binding.btnGrass,
                binding.btnBerryBush,
                binding.btnTree
        );
        List<EntityType> entityTypes = Arrays.asList(
                EntityType.WOLF,
                EntityType.RABBIT,
                EntityType.MOUSE,
                EntityType.DEER,
                EntityType.GRASS,
                EntityType.BERRY_BUSH,
                EntityType.TREE
        );

        binding.btnWolf.setStrokeColorResource(R.color.accent_gold);

        for (int index = 0; index < speciesButtons.size(); index++) {
            MaterialButton button = speciesButtons.get(index);
            EntityType entityType = entityTypes.get(index);
            button.setOnClickListener(v -> {
                for (MaterialButton other : speciesButtons) {
                    other.setStrokeColorResource(R.color.border_subtle);
                }
                button.setStrokeColorResource(R.color.accent_gold);
                selectedEntityType = entityType;
                binding.simulationCanvas.setSelectedEntityType(entityType);
            });
        }
    }

    private void setupCanvasInteractions() {
        binding.simulationCanvas.setOnTileTapListener((gridX, gridY) ->
                SimulationSession.getEngine().placeEntity(selectedEntityType, gridX, gridY));
    }

    private void applySpeciesButtonIcons() {
        setPixelIcon(binding.btnWolf, R.drawable.animal_wolf, 28);
        setPixelIcon(binding.btnRabbit, R.drawable.animal_rabbit, 28);
        setPixelIcon(binding.btnMouse, R.drawable.animal_mouse, 28);
        setPixelIcon(binding.btnDeer, R.drawable.animal_deer, 28);
        setPixelIcon(binding.btnGrass, R.drawable.ground_grass, 28);
        setPixelIcon(binding.btnBerryBush, R.drawable.plant_bush_alive, 28);
        setPixelIcon(binding.btnTree, R.drawable.plant_tree_low_alive, 28);
    }

    private void setPixelIcon(@NonNull MaterialButton button, @DrawableRes int drawableResId, int iconSizeDp) {
        BitmapDrawable icon = new BitmapDrawable(getResources(),
                BitmapFactory.decodeResource(getResources(), drawableResId));
        icon.setAntiAlias(false);
        icon.setFilterBitmap(false);
        icon.setDither(false);
        button.setIcon(icon);
        button.setIconSize((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                iconSizeDp,
                getResources().getDisplayMetrics()
        ));
        button.setIconTint(null);
    }

    private void setupReset() {
        binding.btnReset.setOnClickListener(v -> showResetDialog());
    }

    @Override
    public void onWorldUpdated(@NonNull com.example.tleilax.simulation.WorldSnapshot snapshot) {
        if (binding == null) {
            return;
        }
        worldReady = true;
        SimulationSession.setWorldInitialized(true);
        binding.textTickCount.setText(getString(R.string.cycle_count, snapshot.tickCount()));
        binding.simulationCanvas.setWorldSnapshot(snapshot);
        loadingCompleted = true;
        maybeFinishLoading();
    }

    @Override
    public void onDestroyView() {
        SimulationSession.getEngine().removeListener(this);
        AppSettings.removeListener(this);
        handler.removeCallbacks(loadingProgressRunnable);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMusicEnabledChanged(boolean enabled) {
    }

    @Override
    public void onShowGridChanged(boolean visible) {
        if (binding != null) {
            binding.simulationCanvas.setGridVisible(visible);
        }
    }

    @Override
    public void onGrassCoverageChanged(int percent) {
    }

    private void initializeWorldAfterFirstFrame() {
        if (binding == null) {
            return;
        }
        handler.postDelayed(() -> {
            if (binding == null) {
                return;
            }
            SimulationSession.getEngine().initializeWorld();
        }, 140L);
    }

    private void startLoadingSequence(int labelResId, @NonNull Runnable generationAction) {
        showLoadingOverlay(labelResId);
        binding.getRoot().post(generationAction);
    }

    private void showLoadingOverlay(int labelResId) {
        worldReady = false;
        loadingCompleted = false;
        loadingStartedAtMs = System.currentTimeMillis();
        binding.loadingOverlay.setVisibility(View.VISIBLE);
        binding.textLoadingLabel.setText(labelResId);
        binding.textLoadingStage.setText(pickLoadingStatus());
        binding.progressLoading.setProgress(0);
        binding.simulationCanvas.setEnabled(false);
        binding.btnPlayPause.setEnabled(false);
        binding.btnReset.setEnabled(false);
        binding.speedToggleGroup.setEnabled(false);
        handler.removeCallbacks(loadingProgressRunnable);
        handler.post(loadingProgressRunnable);
    }

    private void hideLoadingOverlay() {
        if (binding == null) {
            return;
        }
        handler.removeCallbacks(loadingProgressRunnable);
        binding.loadingOverlay.setVisibility(View.GONE);
        binding.simulationCanvas.setEnabled(true);
        binding.btnPlayPause.setEnabled(true);
        binding.btnReset.setEnabled(true);
        binding.speedToggleGroup.setEnabled(true);
    }

    private void showResetDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reset_confirmation, null, false);
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
            isPlaying = false;
            if (binding != null) {
                binding.btnPlayPause.setIconResource(android.R.drawable.ic_media_play);
                startLoadingSequence(R.string.loading_label_world_gen, () -> handler.postDelayed(() -> {
                    if (binding == null) {
                        return;
                    }
                    SimulationSession.getEngine().reset(SimulationEngine.FIXED_WORLD_SIZE, SimulationEngine.FIXED_WORLD_SIZE);
                    SimulationSession.setWorldInitialized(true);
                }, 140L));
            }
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

    private void tickLoadingProgress() {
        if (binding == null || binding.loadingOverlay.getVisibility() != View.VISIBLE) {
            return;
        }

        long elapsed = System.currentTimeMillis() - loadingStartedAtMs;
        int progress = (int) Math.min(96L, (elapsed * 100L) / MIN_LOADING_DURATION_MS);
        binding.progressLoading.setProgress(progress);

        if (loadingCompleted && elapsed >= MIN_LOADING_DURATION_MS) {
            binding.progressLoading.setProgress(100);
            hideLoadingOverlay();
            return;
        }

        handler.postDelayed(loadingProgressRunnable, LOADING_PROGRESS_INTERVAL_MS);
    }

    private void maybeFinishLoading() {
        if (binding == null || binding.loadingOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        long elapsed = System.currentTimeMillis() - loadingStartedAtMs;
        if (elapsed >= MIN_LOADING_DURATION_MS) {
            binding.progressLoading.setProgress(100);
            hideLoadingOverlay();
        }
    }

    @NonNull
    private CharSequence pickLoadingStatus() {
        String[] statuses = getResources().getStringArray(R.array.loading_status_lines);
        if (statuses.length == 0) {
            return "";
        }
        return statuses[LOADING_STATUS_RANDOM.nextInt(statuses.length)];
    }
}
