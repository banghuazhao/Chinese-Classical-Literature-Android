package com.appsbay.chineseclassicalliteratural.Controller.Menu.MoreApps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;

import java.util.List;

public class MoreAppsAdapter extends RecyclerView.Adapter<MoreAppsViewHolder> {

    private final List<MoreApp> models;

    public MoreAppsAdapter(List<MoreApp> models) {
        this.models = models;
    }

    @NonNull
    @Override
    public MoreAppsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_more_apps, parent, false);
        return new MoreAppsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoreAppsViewHolder holder, int position) {
        holder.bind(models.get(position));
    }

    @Override
    public int getItemCount() {
        return models.size();
    }
}
