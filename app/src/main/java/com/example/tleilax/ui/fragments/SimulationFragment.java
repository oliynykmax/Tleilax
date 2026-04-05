package com.example.tleilax.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tleilax.R;
import com.example.tleilax.databinding.FragmentSimulationBinding;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

public class SimulationFragment extends Fragment {

    private FragmentSimulationBinding binding;
    private boolean isPlaying = true;
    private int speedMultiplier = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSimulationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupPlayPause();
        setupSpeedControl();
        setupSpeciesSelection();
    }

    private void setupPlayPause() {
        binding.btnPlayPause.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            int iconRes = isPlaying
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play;
            binding.btnPlayPause.setIconResource(iconRes);
        });
    }

    private void setupSpeedControl() {
        binding.speedToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_speed_1x) speedMultiplier = 1;
            else if (checkedId == R.id.btn_speed_2x) speedMultiplier = 2;
            else if (checkedId == R.id.btn_speed_4x) speedMultiplier = 4;
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

        binding.btnWolf.setStrokeColorResource(R.color.accent_gold);

        for (MaterialButton btn : speciesButtons) {
            btn.setOnClickListener(v -> {
                for (MaterialButton other : speciesButtons) {
                    other.setStrokeColorResource(R.color.border_subtle);
                }
                btn.setStrokeColorResource(R.color.accent_gold);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
