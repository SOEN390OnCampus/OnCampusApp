package com.example.oncampusapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class BuildingFloorAdapter extends RecyclerView.Adapter<BuildingFloorAdapter.IndoorBuildingViewHolder> {

    private final List<BuildingFloorData> buildingFloorDataList;
    private int expandedPosition = -1;

    public BuildingFloorAdapter(List<BuildingFloorData> buildingFloorDataList) {
        this.buildingFloorDataList = buildingFloorDataList;
    }
    @NonNull
    @Override
    public IndoorBuildingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_building_floor, parent, false);
        return new IndoorBuildingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IndoorBuildingViewHolder holder, int position) {
        BuildingFloorData buildingFloorData = buildingFloorDataList.get(position);
        boolean isExpanded = position == expandedPosition;

        // Set building info
        holder.tvBuildingId.setText(buildingFloorData.getId());
        holder.tvFullName.setText(buildingFloorData.getFullName());

        // Set dropdown arrow rotation
        holder.ivDropDown.setRotation(isExpanded ? 180 : 0);

        holder.tvBuildingId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#807d0c22")));

        // Show/hide floors
        holder.floorContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Add the floor chips (block) if expanded
        if (isExpanded) {

            holder.floorChipsGroup.removeAllViews();

            for (BuildingFloorData.Floor floor : buildingFloorData.getFloors()) {
                Chip chip = generateChip(holder, floor, buildingFloorData);
                holder.floorChipsGroup.addView(chip);
            }
            holder.tvBuildingId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#912338")));
        }
        // Expand/collapse on row click
        holder.itemView.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : position;

            if (previousExpanded != -1) notifyItemChanged(previousExpanded);
            notifyItemChanged(position);
        });
    }

    @NonNull
    private Chip generateChip(@NonNull IndoorBuildingViewHolder holder, BuildingFloorData.Floor floor, BuildingFloorData buildingFloorData) {
        Chip chip = (Chip) LayoutInflater.from(holder.itemView.getContext())
                .inflate(R.layout.item_floor_chip, holder.floorChipsGroup, false);
        chip.setText(floor.id());
        chip.setOnClickListener(v -> onFloorSelected(v.getContext(), buildingFloorData.getId(), floor.id()));
        return chip;
    }

    private void onFloorSelected(android.content.Context context, String buildingId, String floorId){
        Intent intent = new Intent(context, IndoorMapActivity.class);
        intent.putExtra("BUILDING_ID", buildingId);
        intent.putExtra("FLOOR_ID", floorId);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return buildingFloorDataList.size();
    }

    public static class IndoorBuildingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBuildingId, tvFullName;
        ImageView ivDropDown;
        LinearLayout floorContainer;
        ChipGroup floorChipsGroup;

        IndoorBuildingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBuildingId = itemView.findViewById(R.id.tv_building_id);
            tvFullName = itemView.findViewById(R.id.tv_building_name);
            ivDropDown = itemView.findViewById(R.id.iv_dropdown);
            floorContainer = itemView.findViewById(R.id.floor_container);
            floorChipsGroup = itemView.findViewById(R.id.chip_group_floors);
        }
    }
}
