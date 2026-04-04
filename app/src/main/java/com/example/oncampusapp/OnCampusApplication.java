package com.example.oncampusapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.example.oncampusapp.location.ILocationProvider;

public class OnCampusApplication extends Application {
    private ILocationProvider locationProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                GrayscaleModeManager.applyToActivity(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                GrayscaleModeManager.applyToActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    public ILocationProvider getLocationProvider() {
        return locationProvider;
    }

    public void setLocationProvider(ILocationProvider provider) {
        this.locationProvider = provider;
    }

}
