package com.example.tleilax.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tleilax.databinding.ItemSaveBinding;
import com.example.tleilax.storage.SimulationSaveEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class SaveAdapter extends RecyclerView.Adapter<SaveAdapter.SaveViewHolder> {

    public interface OnSaveClickListener {
        void onSaveClicked(@NonNull SimulationSaveEntity saveEntity);
    }

    private static final DateTimeFormatter DETAILS_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm",
            Locale.US
    ).withZone(ZoneId.systemDefault());

    @NonNull
    private final List<SimulationSaveEntity> items;
    @NonNull
    private final OnSaveClickListener onSaveClickListener;

    public SaveAdapter(@NonNull List<SimulationSaveEntity> items, @NonNull OnSaveClickListener onSaveClickListener) {
        this.items = items;
        this.onSaveClickListener = onSaveClickListener;
    }

    @NonNull
    @Override
    public SaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSaveBinding binding = ItemSaveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SaveViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SaveViewHolder holder, int position) {
        SimulationSaveEntity saveEntity = items.get(position);
        holder.binding.textSaveName.setText(saveEntity.name);
        holder.binding.textSaveDetails.setText(buildDetails(saveEntity));
        holder.binding.getRoot().setOnClickListener(v -> onSaveClickListener.onSaveClicked(saveEntity));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    public SimulationSaveEntity getItem(int position) {
        return items.get(position);
    }

    @NonNull
    private String buildDetails(@NonNull SimulationSaveEntity saveEntity) {
        return DETAILS_FORMATTER.format(Instant.ofEpochMilli(saveEntity.savedAtEpochMillis))
                + " · "
                + saveEntity.tickCount
                + " ticks · "
                + saveEntity.speciesAliveCount
                + " species alive";
    }

    static class SaveViewHolder extends RecyclerView.ViewHolder {
        final ItemSaveBinding binding;

        SaveViewHolder(ItemSaveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
