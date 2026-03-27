package com.example.oncampusapp;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonPolygon;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BuildingDetailsUITest {

    @Rule(order = 1)
    public ActivityScenarioRule<MapsActivity> activityRule =
            new ActivityScenarioRule<>(MapsActivity.class);
    @Test
    public void testBuildingDetailsButton_verifyDialogIsUpdated() {

        AtomicReference<Dialog> buildingDialog = new AtomicReference<>();

        activityRule.getScenario().onActivity(activity -> {
            activity.handleBuildingDetailsButtonClick("way/103248064");
             Layer layer = activity.getLayer();
             for (Feature feature : layer.getFeatures()){
                 if (feature.getGeometry() instanceof GeoJsonPolygon) {
                     String geometryLayer = feature.getProperty("layer");
                     if (geometryLayer == null) {
                        continue;
                     }
                     if (geometryLayer.equals("detailButton")){ // Clicked on the button
                         activity.handleBuildingDetailsButtonClick(feature.getId());
                         assertNotEquals(buildingDialog.get(), activity.getCurrentBuildingDialog());
                         assertNotNull(activity.getCurrentBuildingDialog());
                         buildingDialog.set(activity.getCurrentBuildingDialog());
                         activity.getCurrentBuildingDialog().dismiss();
                     }
                 }
             }
        });

    }

    @Test
    public void testBuildingDetailsButton_demo() {

        // First dialog
        activityRule.getScenario().onActivity(activity -> activity.handleBuildingDetailsButtonClick("way/103248064"));

        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.getCurrentBuildingDialog());
            assertTrue(activity.getCurrentBuildingDialog().isShowing());
        });

        activityRule.getScenario().onActivity(activity -> activity.getCurrentBuildingDialog().dismiss());

        // Second dialog
        activityRule.getScenario().onActivity(activity -> activity.handleBuildingDetailsButtonClick("way/103248058"));

        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.getCurrentBuildingDialog());
            assertTrue(activity.getCurrentBuildingDialog().isShowing());
            activity.getCurrentBuildingDialog().dismiss();
        });
    }

}
