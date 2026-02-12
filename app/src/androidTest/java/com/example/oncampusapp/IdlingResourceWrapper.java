package com.example.oncampusapp;

import androidx.test.espresso.IdlingResource;

import com.google.android.gms.maps.GoogleMap;

/**
 * An Espresso IdlingResource that waits for Google Map camera animations to complete.
 * This is necessary because Espresso does not handle synchronization with map animations by default.
 * This class implements both {@link IdlingResource} and {@link GoogleMap.OnCameraIdleListener}.
 */
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

    /**
     * Called when the camera has stopped moving. This is the callback from the
     * {@link GoogleMap.OnCameraIdleListener}. When this is called, we transition the resource
     * to the idle state.
     */
    @Override
    public void onCameraIdle() {
        isIdle = true;
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }

    /**
     * Sets the idle state of the resource. This should be called with `false` before
     * starting a map animation to signal that the UI is busy.
     * @param idle The new idle state.
     */
    public void setIdle(boolean idle) {
        isIdle = idle;
    }
}
