package com.example.oncampusapp;

import android.app.Application;
import com.example.oncampusapp.location.ILocationProvider;

public class OnCampusApplication extends Application {
    private ILocationProvider locationProvider;

    public ILocationProvider getLocationProvider() {
        return locationProvider;
    }

    public void setLocationProvider(ILocationProvider provider) {
        this.locationProvider = provider;
    }

}
