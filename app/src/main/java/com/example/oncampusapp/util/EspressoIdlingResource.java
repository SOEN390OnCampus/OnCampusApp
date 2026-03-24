package com.example.oncampusapp.util;

import androidx.test.espresso.idling.CountingIdlingResource;

public class EspressoIdlingResource {

    private static final String RESOURCE = "GLOBAL";

    // Fix #2 & #3: Made the field private and final so it cannot be overwritten
    private static final CountingIdlingResource countingIdlingResource = new CountingIdlingResource(RESOURCE);

    // Fix #1: Added a private constructor to hide the implicit public one.
    // This prevents anyone from accidentally typing: new EspressoIdlingResource()
    private EspressoIdlingResource() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Since we made the counter private, we provide a public "getter"
    // so your UI tests can still access it.
    public static CountingIdlingResource getIdlingResource() {
        return countingIdlingResource;
    }

    public static void increment() {
        countingIdlingResource.increment();
    }

    public static void decrement() {
        if (!countingIdlingResource.isIdleNow()) {
            countingIdlingResource.decrement();
        }
    }
}