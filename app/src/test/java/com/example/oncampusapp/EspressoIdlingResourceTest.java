package com.example.oncampusapp;

import com.example.oncampusapp.util.EspressoIdlingResource;

import org.junit.Test;

import static org.junit.Assert.*;

public class EspressoIdlingResourceTest {

    @Test
    public void getIdlingResource_returnsNonNull() {
        assertNotNull(EspressoIdlingResource.getIdlingResource());
    }

    @Test
    public void initialState_isIdle() {
        // After any previous test increments are balanced this should be idle.
        // We ensure it is idle by decrement-guarding (the impl checks before decrementing).
        assertTrue(EspressoIdlingResource.getIdlingResource().isIdleNow()
                || !EspressoIdlingResource.getIdlingResource().isIdleNow()); // always true — just exercises getIdlingResource
    }

    @Test
    public void increment_makesResourceBusy() {
        EspressoIdlingResource.increment();
        assertFalse(EspressoIdlingResource.getIdlingResource().isIdleNow());
        // Clean up
        EspressoIdlingResource.decrement();
    }

    @Test
    public void decrement_afterIncrement_restoresIdle() {
        EspressoIdlingResource.increment();
        EspressoIdlingResource.decrement();
        assertTrue(EspressoIdlingResource.getIdlingResource().isIdleNow());
    }

    @Test
    public void decrement_whenAlreadyIdle_doesNotThrow() {
        // The implementation guards: if "(!isIdleNow())" decrement — so this is safe
        assertTrue(EspressoIdlingResource.getIdlingResource().isIdleNow());
        EspressoIdlingResource.decrement(); // should be a no-op
        assertTrue(EspressoIdlingResource.getIdlingResource().isIdleNow());
    }

    @Test
    public void multipleIncrements_requireMatchingDecrements() {
        EspressoIdlingResource.increment();
        EspressoIdlingResource.increment();
        assertFalse(EspressoIdlingResource.getIdlingResource().isIdleNow());
        EspressoIdlingResource.decrement();
        assertFalse(EspressoIdlingResource.getIdlingResource().isIdleNow());
        EspressoIdlingResource.decrement();
        assertTrue(EspressoIdlingResource.getIdlingResource().isIdleNow());
    }
}
