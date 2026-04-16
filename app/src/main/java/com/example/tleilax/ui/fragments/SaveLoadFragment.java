package com.example.tleilax.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tleilax.R;
import com.example.tleilax.databinding.FragmentSaveLoadBinding;
import com.example.tleilax.simulation.SimulationSession;
import com.example.tleilax.simulation.WorldSnapshot;
import com.example.tleilax.storage.SimulationSaveEntity;
import com.example.tleilax.storage.SimulationStorage;
import com.example.tleilax.ui.adapters.SaveAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaveLoadFragment extends Fragment {

    private final ExecutorService storageExecutor = Executors.newSingleThreadExecutor();
    private final List<SimulationSaveEntity> saveItems = new ArrayList<>();

    private FragmentSaveLoadBinding binding;
    private SaveAdapter saveAdapter;
    private SimulationStorage simulationStorage;
    @Nullable
    private ColorDrawable swipeDeleteBackground;
    @Nullable
    private Drawable swipeDeleteIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSaveLoadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        simulationStorage = new SimulationStorage(requireContext());
        swipeDeleteBackground = new ColorDrawable(Color.parseColor("#AA9D2A2A"));
        swipeDeleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
        setupRecyclerView();
        setupSaveButton();
        loadSaves();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSaves();
    }

    private void setupRecyclerView() {
        saveAdapter = new SaveAdapter(saveItems, this::loadSave);
        binding.recyclerSaveLoad.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerSaveLoad.setAdapter(saveAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT
        ) {
            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target
            ) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                deleteSave(saveAdapter.getItem(viewHolder.getAdapterPosition()));
            }

            @Override
            public void onChildDraw(
                    @NonNull Canvas canvas,
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    float dX,
                    float dY,
                    int actionState,
                    boolean isCurrentlyActive
            ) {
                View itemView = viewHolder.itemView;
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    drawDeleteFeedback(canvas, itemView, dX);
                } else if (swipeDeleteBackground != null) {
                    swipeDeleteBackground.setBounds(0, 0, 0, 0);
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.recyclerSaveLoad);
    }

    private void setupSaveButton() {
        binding.btnSaveCurrent.setOnClickListener(v -> showSaveDialog());
    }

    private void showSaveDialog() {
        if (!SimulationSession.isWorldInitialized()) {
            Toast.makeText(requireContext(), R.string.save_load_missing_world, Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_world, null, false);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.input_save_name_layout);
        TextInputEditText inputSaveName = dialogView.findViewById(R.id.input_save_name);
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
            Editable editable = inputSaveName.getText();
            String requestedName = editable != null ? editable.toString().trim() : "";
            if (TextUtils.isEmpty(requestedName)) {
                if (inputLayout != null) {
                    inputLayout.setError(getString(R.string.save_dialog_name_required));
                }
                return;
            }
            if (inputLayout != null) {
                inputLayout.setError(null);
            }
            saveCurrentWorld(requestedName);
            dialog.dismiss();
        });

        dialog.show();
        inputSaveName.requestFocus();
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

    private void saveCurrentWorld(@NonNull String requestedName) {
        binding.btnSaveCurrent.setEnabled(false);
        storageExecutor.execute(() -> {
            simulationStorage.save(requestedName, SimulationSession.getEngine().getSnapshot());
            postToUi(() -> {
                if (binding == null) {
                    return;
                }
                binding.btnSaveCurrent.setEnabled(true);
                Toast.makeText(requireContext(), R.string.save_load_saved, Toast.LENGTH_SHORT).show();
                loadSaves();
            });
        });
    }

    private void loadSaves() {
        storageExecutor.execute(() -> {
            List<SimulationSaveEntity> saves = simulationStorage.listSaves();
            postToUi(() -> {
                if (binding == null) {
                    return;
                }
                saveItems.clear();
                saveItems.addAll(saves);
                saveAdapter.notifyDataSetChanged();
                binding.textEmptySaves.setVisibility(saveItems.isEmpty() ? View.VISIBLE : View.GONE);
                binding.recyclerSaveLoad.setVisibility(saveItems.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void loadSave(@NonNull SimulationSaveEntity saveEntity) {
        storageExecutor.execute(() -> {
            WorldSnapshot snapshot = simulationStorage.load(saveEntity.id);
            postToUi(() -> {
                if (snapshot == null || binding == null) {
                    return;
                }
                SimulationSession.getEngine().loadSnapshot(snapshot);
                SimulationSession.setWorldInitialized(true);
                Toast.makeText(requireContext(), R.string.save_load_loaded, Toast.LENGTH_SHORT).show();
                BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.navigation_simulation);
            });
        });
    }

    private void deleteSave(@NonNull SimulationSaveEntity saveEntity) {
        storageExecutor.execute(() -> {
            simulationStorage.delete(saveEntity);
            postToUi(() -> {
                if (binding == null) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.save_load_deleted, Toast.LENGTH_SHORT).show();
                loadSaves();
            });
        });
    }

    private void postToUi(@NonNull Runnable runnable) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(runnable);
    }

    private void drawDeleteFeedback(@NonNull Canvas canvas, @NonNull View itemView, float dX) {
        if (swipeDeleteBackground == null) {
            return;
        }
        int backgroundLeft = itemView.getRight() + (int) dX;
        swipeDeleteBackground.setBounds(backgroundLeft, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        swipeDeleteBackground.draw(canvas);

        if (swipeDeleteIcon == null) {
            return;
        }
        int iconMargin = (itemView.getHeight() - swipeDeleteIcon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + iconMargin;
        int iconBottom = iconTop + swipeDeleteIcon.getIntrinsicHeight();
        int iconRight = itemView.getRight() - iconMargin;
        int iconLeft = iconRight - swipeDeleteIcon.getIntrinsicWidth();
        swipeDeleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        swipeDeleteIcon.draw(canvas);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        storageExecutor.shutdown();
    }
}
