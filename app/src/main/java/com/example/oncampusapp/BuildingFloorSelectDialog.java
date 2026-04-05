package com.example.oncampusapp;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BuildingFloorSelectDialog extends DialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_indoor_map_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rv_indoor_building);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<BuildingFloorData> buildings = loadBuildingFloorData();

        BuildingFloorAdapter adapter = new BuildingFloorAdapter(buildings);

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int screenWidth = 0;
            int screenHeight = 0;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = requireActivity().getWindowManager().getCurrentWindowMetrics();
                screenWidth = windowMetrics.getBounds().width();
                screenHeight = windowMetrics.getBounds().height();
            }
            int width = (int) (screenWidth * 0.80);
            int maxHeight = (int) (screenHeight * 0.50);

            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Cap at 50% if content is taller
            View decorView = getDialog().getWindow().getDecorView();
            decorView.post(() -> {
                if (decorView.getHeight() > maxHeight) {
                    getDialog().getWindow().setLayout(width, maxHeight);
                }
            });
        }
    }

    // Loading the floor data in res/raw
    private List<BuildingFloorData> loadBuildingFloorData() {
        List<BuildingFloorData> buildings = new ArrayList<>();

        try {
            InputStream is = getResources().openRawResource(R.raw.floor_menu);
            String json = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                json = new String(is.readAllBytes());
            }

            JSONArray buildingsArray = new JSONArray(json);

            for (int i = 0; i < buildingsArray.length(); i++) {
                JSONObject obj = buildingsArray.getJSONObject(i);

                String id = obj.getString("id");
                String fullName = obj.getString("fullName");

                JSONArray floorsJson = obj.getJSONArray("floors");
                List<BuildingFloorData.Floor> floors = new ArrayList<>();
                for (int j = 0; j < floorsJson.length(); j++) {
                    JSONObject f = floorsJson.getJSONObject(j);
                    floors.add(new BuildingFloorData.Floor(
                            f.getString("id"),
                            f.getString("name")
                    ));
                }
                buildings.add(new BuildingFloorData(id, fullName, floors));
            }

        } catch (IOException e) {
            Log.e("BuildingFloorData", "Failed to read floor_menu resource", e);
        } catch (JSONException e) {
            Log.e("BuildingFloorData", "Failed to parse floor_menu resource", e);
        }
        Log.d("Buildings", "Count: " + buildings.size());

        return buildings;
    }
}
