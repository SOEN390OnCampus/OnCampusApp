package com.example.oncampusapp;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

public class BuildingDialogManager {

    private final MapsActivity activity;
    private Map<String, BuildingDetails> geoIdToBuildingDetailsMap;
    private Dialog currentBuildingDialog = null;

    public BuildingDialogManager(MapsActivity activity) {
        this.activity = activity;
    }

    public Dialog getCurrentBuildingDialog() {
        return currentBuildingDialog;
    }

    public Map<String, BuildingDetails> getGeoIdToBuildingDetailsMap() {
        return geoIdToBuildingDetailsMap;
    }

    public void loadBuildingDetails() {
        try {
            InputStream is = activity.getResources().openRawResource(R.raw.concordia_building_details);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, BuildingDetails>>() {}.getType();
            geoIdToBuildingDetailsMap = gson.fromJson(jsonBuilder.toString(), type);
        } catch (Resources.NotFoundException | IOException e) {
            throw new RuntimeException("File not found: " + e.getMessage());
        }
    }

    public void showBuildingInfoDialog(BuildingDetails buildingDetails) {
        if (currentBuildingDialog != null && currentBuildingDialog.isShowing()) {
            currentBuildingDialog.dismiss();
        }
        Dialog dialog = createAndConfigureDialog();
        populateDialogViews(dialog, buildingDetails);
        setupDialogListeners(dialog);
        dialog.show();
        currentBuildingDialog = dialog;
    }

    private Dialog createAndConfigureDialog() {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_building_details);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.8),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void populateDialogViews(Dialog dialog, BuildingDetails buildingDetails) {
        TextView txtBuildingCode    = dialog.findViewById(R.id.txt_building_code);
        TextView txtBuildingName    = dialog.findViewById(R.id.txt_building_name);
        TextView txtBuildingAddress = dialog.findViewById(R.id.txt_building_address);
        LinearLayout llOpeningHours = dialog.findViewById(R.id.layout_building_opening_hours);
        TextView txtOpeningHours    = dialog.findViewById(R.id.txt_building_opening_hours);
        LinearLayout llAccessibility = dialog.findViewById(R.id.item_accessibility);
        LinearLayout llMetroConnect  = dialog.findViewById(R.id.item_metro_connect);
        ImageView imgBuilding        = dialog.findViewById(R.id.img_building);

        txtBuildingCode.setText(buildingDetails.getCode());
        txtBuildingName.setText(buildingDetails.getName());
        txtBuildingName.setPaintFlags(txtBuildingName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        if (buildingDetails.getAddress() != null && !buildingDetails.getAddress().isEmpty()) {
            txtBuildingAddress.setText(buildingDetails.getAddress());
        }
        llAccessibility.setVisibility(buildingDetails.isAccessible() ? View.VISIBLE : View.GONE);
        llMetroConnect.setVisibility(buildingDetails.hasDirectTunnelToMetro() ? View.VISIBLE : View.GONE);

        if (buildingDetails.getSchedule() == null || buildingDetails.getSchedule().isAlwaysOpen()) {
            if (buildingDetails.getSchedule().isAlwaysOpen()){
                llOpeningHours.setVisibility(View.VISIBLE);
                txtOpeningHours.setText(R.string.always_open);
            } else {
                llOpeningHours.setVisibility(View.GONE);
            }
        } else {
            llOpeningHours.setVisibility(View.VISIBLE);
            txtOpeningHours.setText(buildingDetails.getSchedule().toString());
        }
        loadBuildingImage(imgBuilding, buildingDetails);
    }

    private void loadBuildingImage(ImageView imgBuilding, BuildingDetails buildingDetails) {
        if (buildingDetails.getImage() != null && !buildingDetails.getImage().isEmpty()) {
            Glide.with(activity)
                    .load(buildingDetails.getImage())
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .into(imgBuilding);
        } else {
            imgBuilding.setImageResource(android.R.color.darker_gray);
        }
    }

    private void setupDialogListeners(Dialog dialog) {
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            currentBuildingDialog = null;
        });
        dialog.setOnDismissListener(d -> currentBuildingDialog = null);
    }
}
