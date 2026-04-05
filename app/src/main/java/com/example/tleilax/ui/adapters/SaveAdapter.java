package com.example.tleilax.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tleilax.databinding.ItemSaveBinding;

import java.util.List;

public class SaveAdapter extends RecyclerView.Adapter<SaveAdapter.SaveViewHolder> {

    private final List<String> items;

    public SaveAdapter(List<String> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public SaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSaveBinding binding = ItemSaveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SaveViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SaveViewHolder holder, int position) {
        holder.binding.textSaveName.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SaveViewHolder extends RecyclerView.ViewHolder {
        final ItemSaveBinding binding;

        SaveViewHolder(ItemSaveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
