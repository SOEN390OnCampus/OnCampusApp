package com.example.oncampusapp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}