package com.example.tleilax.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tleilax.databinding.FragmentSaveLoadBinding;
import com.example.tleilax.ui.adapters.SaveAdapter;

import java.util.Arrays;
import java.util.List;

public class SaveLoadFragment extends Fragment {

    private FragmentSaveLoadBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSaveLoadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        List<String> mockSaves = Arrays.asList(
                "Forest_World_1 (2026-03-29)",
                "Desert_Test (2026-03-28)",
                "Empty_Grid (2026-03-25)"
        );
        
        SaveAdapter adapter = new SaveAdapter(mockSaves);
        binding.recyclerSaveLoad.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerSaveLoad.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
