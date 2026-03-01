package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ShuttleHelperTest {

    @Mock
    private GoogleMap mockMap;

    @Mock
    private Context mockContext;

    @Mock
    private Marker mockMarker1;

    @Mock
    private Marker mockMarker2;

    @Before
    public void setUp() {
        when(mockContext.getString(R.string.sgw_shuttle_stop)).thenReturn("SGW Shuttle Stop");
        when(mockContext.getString(R.string.loyola_shuttle_stop)).thenReturn("Loyola Shuttle Stop");
    }

    // ==================== CONSTANTS TESTS ====================

    @Test
    public void testShuttleStopSGWCoordinates() {
        assertNotNull("SGW shuttle stop should not be null", ShuttleHelper.SHUTTLE_STOP_SGW);
        assertEquals("SGW latitude should match", 45.497163, ShuttleHelper.SHUTTLE_STOP_SGW.latitude, 0.000001);
        assertEquals("SGW longitude should match", -73.578535, ShuttleHelper.SHUTTLE_STOP_SGW.longitude, 0.000001);
    }

    @Test
    public void testShuttleStopLoyolaCoordinates() {
        assertNotNull("Loyola shuttle stop should not be null", ShuttleHelper.SHUTTLE_STOP_LOY);
        assertEquals("Loyola latitude should match", 45.458424, ShuttleHelper.SHUTTLE_STOP_LOY.latitude, 0.000001);
        assertEquals("Loyola longitude should match", -73.638369, ShuttleHelper.SHUTTLE_STOP_LOY.longitude, 0.000001);
    }

    // ==================== SHOW SHUTTLE STOPS TESTS ====================
    // Note: These tests are limited because BitmapDescriptorFactory requires Android framework
    // Full integration tests should be done in androidTest with Espresso

    @Test
    public void testShowShuttleStops_WithNullMap() {
        Marker[] result = ShuttleHelper.showShuttleStops(mockContext, null, null);
        
        assertNotNull("Result should not be null", result);
        assertEquals("Result array should have 2 elements", 2, result.length);
        assertNull("First marker should be null", result[0]);
        assertNull("Second marker should be null", result[1]);
    }

    // Cannot fully test marker creation without Robolectric due to BitmapDescriptorFactory dependency
    // These tests verify the logic but marker creation will be tested in instrumented tests

    // ==================== HIDE SHUTTLE STOPS TESTS ====================

    @Test
    public void testHideShuttleStops_WithNullArray() {
        // Should not throw exception
        ShuttleHelper.hideShuttleStops(null);
        // No verification needed, just ensure no exception thrown
    }

    @Test
    public void testHideShuttleStops_WithValidMarkers() {
        Marker[] markers = new Marker[]{mockMarker1, mockMarker2};
        
        ShuttleHelper.hideShuttleStops(markers);
        
        verify(mockMarker1, times(1)).remove();
        verify(mockMarker2, times(1)).remove();
        
        assertNull("First marker should be set to null", markers[0]);
        assertNull("Second marker should be set to null", markers[1]);
    }

    @Test
    public void testHideShuttleStops_WithPartialNullMarkers() {
        Marker[] markers = new Marker[]{mockMarker1, null};
        
        ShuttleHelper.hideShuttleStops(markers);
        
        verify(mockMarker1, times(1)).remove();
        
        assertNull("First marker should be set to null", markers[0]);
        assertNull("Second marker should remain null", markers[1]);
    }

    @Test
    public void testHideShuttleStops_WithAllNullMarkers() {
        Marker[] markers = new Marker[]{null, null};
        
        ShuttleHelper.hideShuttleStops(markers);
        
        // Verify remove was never called
        verify(mockMarker1, never()).remove();
        verify(mockMarker2, never()).remove();
    }

    // ==================== IS SHUTTLE STOP MARKER TESTS ====================

    @Test
    public void testIsShuttleStopMarker_WithNullMarker() {
        assertFalse("Null marker should return false", ShuttleHelper.isShuttleStopMarker(mockContext, null));
    }

    @Test
    public void testIsShuttleStopMarker_WithNullTitle() {
        when(mockMarker1.getTitle()).thenReturn(null);
        
        assertFalse("Marker with null title should return false", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    @Test
    public void testIsShuttleStopMarker_WithSGWTitle() {
        when(mockMarker1.getTitle()).thenReturn("SGW Shuttle Stop");
        
        assertTrue("SGW Shuttle Stop should be recognized", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    @Test
    public void testIsShuttleStopMarker_WithLoyolaTitle() {
        when(mockMarker1.getTitle()).thenReturn("Loyola Shuttle Stop");
        
        assertTrue("Loyola Shuttle Stop should be recognized", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    @Test
    public void testIsShuttleStopMarker_WithOtherTitle() {
        when(mockMarker1.getTitle()).thenReturn("Some Other Building");
        
        assertFalse("Other marker titles should return false", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    @Test
    public void testIsShuttleStopMarker_WithEmptyString() {
        when(mockMarker1.getTitle()).thenReturn("");
        
        assertFalse("Empty title should return false", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    @Test
    public void testIsShuttleStopMarker_WithSimilarTitle() {
        when(mockMarker1.getTitle()).thenReturn("SGW Shuttle");
        
        assertFalse("Similar but not exact title should return false", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    @Test
    public void testIsShuttleStopMarker_CaseSensitive() {
        when(mockMarker1.getTitle()).thenReturn("sgw shuttle stop");
        
        assertFalse("Lowercase title should return false (case sensitive)", ShuttleHelper.isShuttleStopMarker(mockContext, mockMarker1));
    }

    // ==================== IS SAME CAMPUS TESTS ====================

    private static final LatLng SGW = new LatLng(45.496107, -73.577258);
    private static final LatLng LOY = new LatLng(45.4582, -73.6405);

    @Test
    public void testIsSameCampus_BothAtSGW() {
        LatLng building1 = new LatLng(45.497163, -73.578535); // near SGW
        LatLng building2 = new LatLng(45.496500, -73.579000); // also near SGW
        assertTrue("Two SGW buildings should be same campus", ShuttleHelper.isSameCampus(building1, building2, SGW, LOY));
    }

    @Test
    public void testIsSameCampus_BothAtLoyola() {
        LatLng building1 = new LatLng(45.458424, -73.638369); // near Loyola
        LatLng building2 = new LatLng(45.459000, -73.639000); // also near Loyola
        assertTrue("Two Loyola buildings should be same campus", ShuttleHelper.isSameCampus(building1, building2, SGW, LOY));
    }

    @Test
    public void testIsSameCampus_DifferentCampuses() {
        LatLng sgwBuilding = new LatLng(45.497163, -73.578535); // SGW
        LatLng loyBuilding = new LatLng(45.458424, -73.638369); // Loyola
        assertFalse("SGW and Loyola buildings should be different campuses", ShuttleHelper.isSameCampus(sgwBuilding, loyBuilding, SGW, LOY));
    }

    @Test
    public void testIsSameCampus_WithNullPoint1() {
        LatLng loyBuilding = new LatLng(45.458424, -73.638369);
        assertFalse("Null point should return false", ShuttleHelper.isSameCampus(null, loyBuilding, SGW, LOY));
    }

    @Test
    public void testIsSameCampus_WithNullPoint2() {
        LatLng sgwBuilding = new LatLng(45.497163, -73.578535);
        assertFalse("Null point should return false", ShuttleHelper.isSameCampus(sgwBuilding, null, SGW, LOY));
    }

    @Test
    public void testIsSameCampus_BothNull() {
        assertFalse("Both null points should return false", ShuttleHelper.isSameCampus(null, null, SGW, LOY));
    }
}
