package com.example.oncampusapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Constructor;

public class RouteDecisionHelperTest {

    @Test
    public void testWithin15Minutes_returnsTrue() {
        long previousEnd = 0;
        long nextStart = 10 * 60 * 1000;

        assertTrue(RouteDecisionHelper.shouldUsePreviousClass(previousEnd, nextStart));
    }

    @Test
    public void testExactly15Minutes_returnsTrue() {
        long previousEnd = 0;
        long nextStart = 15 * 60 * 1000;

        assertTrue(RouteDecisionHelper.shouldUsePreviousClass(previousEnd, nextStart));
    }

    @Test
    public void testMoreThan15Minutes_returnsFalse() {
        long previousEnd = 0;
        long nextStart = 16 * 60 * 1000;

        assertFalse(RouteDecisionHelper.shouldUsePreviousClass(previousEnd, nextStart));
    }

    @Test
    public void testZeroDifference_returnsTrue() {
        assertTrue(RouteDecisionHelper.shouldUsePreviousClass(1000, 1000));
    }

    @Test
    public void testNextStartBeforePreviousEnd_returnsTrue() {
        // Negative diff is <= 15, so returns true
        assertTrue(RouteDecisionHelper.shouldUsePreviousClass(20 * 60 * 1000, 0));
    }

    @Test
    public void privateConstructor_canBeInstantiatedViaReflection() throws Exception {
        Constructor<RouteDecisionHelper> ctor =
                RouteDecisionHelper.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }
}