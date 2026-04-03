package com.example.oncampusapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.PoiViewHolder> {

    public interface OnPoiClickListener {
        void onPoiClick(Poi poi);
    }

    private final List<Poi> poiList;
    private final OnPoiClickListener listener;

    public PoiAdapter(List<Poi> poiList, OnPoiClickListener listener) {
        this.poiList = poiList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PoiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.poi, parent, false);
        return new PoiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PoiViewHolder holder, int position) {
        Poi poi = poiList.get(position);
        holder.txtName.setText(poi.getName());
        holder.txtDistance.setText(String.format(Locale.getDefault(), "%.2f KM", poi.getDistanceKm()));

        holder.itemView.setOnClickListener(v -> listener.onPoiClick(poi));
    }

    @Override
    public int getItemCount() {
        return poiList.size();
    }

    static class PoiViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        TextView txtDistance;

        public PoiViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_poi_name);
            txtDistance = itemView.findViewById(R.id.txt_poi_distance);
        }
    }
}