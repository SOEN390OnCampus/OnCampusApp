package com.example.oncampusapp;

import androidx.test.espresso.IdlingResource;

import com.google.android.gms.maps.GoogleMap;

public class IdlingResourceWrapper implements IdlingResource, GoogleMap.OnCameraIdleListener {

    private ResourceCallback resourceCallback;
    private boolean isIdle = true;

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public boolean isIdleNow() {
        return isIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }

    @Override
    public void onCameraIdle() {
        isIdle = true;
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }

    public void setIdle(boolean idle) {
        isIdle = idle;
    }
}
