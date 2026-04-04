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
                // Intentionally left blank.
                // We don't need to react to this lifecycle event.
            }

            @Override
            public void onActivityResumed(Activity activity) {
                GrayscaleModeManager.applyToActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // Intentionally left blank.
                // No action required when activity is paused.
            }

            @Override
            public void onActivityStopped(Activity activity) {
                // Intentionally left blank.

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // Intentionally left blank.
                // We are not saving any custom state here.
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // Intentionally left blank.
                // No cleanup needed for grayscale mode at destruction.
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
