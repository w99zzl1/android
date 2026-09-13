package com.example.calculator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ModelsAdapter extends RecyclerView.Adapter<ModelsAdapter.ModelViewHolder> {
    private List<String> models;

    public void setModels(List<String> models) {
        this.models = models;
    }

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        String model = models.get(position);
        holder.modelName.setText(model);
    }

    @Override
    public int getItemCount() {
        return models != null ? models.size() : 0;
    }

    static class ModelViewHolder extends RecyclerView.ViewHolder {
        TextView modelName;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(android.R.id.text1);
        }
    }
}