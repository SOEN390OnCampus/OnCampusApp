package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonPolygon;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GeoJsonMapLoaderTest {

    @Mock MapsActivity mockActivity;
    @Mock BuildingClassifier mockClassifier;
    @Mock BuildingDialogManager mockDialogManager;

    private GeoJsonMapLoader loader;
    private static final LatLng SGW = new LatLng(45.497, -73.579);

    @Before
    public void setUp() {
        loader = new GeoJsonMapLoader(mockActivity, mockClassifier, mockDialogManager);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(loader);
    }

    // ── createSquareCorners – list structure ──────────────────────────────────

    @Test
    public void createSquareCorners_returnsFivePoints() {
        List<LatLng> corners = loader.createSquareCorners(SGW, 10);
        assertEquals(5, corners.size());
    }

    @Test
    public void createSquareCorners_closedPolygon_firstEqualsLast() {
        List<LatLng> corners = loader.createSquareCorners(SGW, 10);
        LatLng first = corners.get(0);
        LatLng last  = corners.get(4);
        assertEquals(first.latitude,  last.latitude,  1e-12);
        assertEquals(first.longitude, last.longitude, 1e-12);
    }

    @Test
    public void createSquareCorners_noNullPoints() {
        List<LatLng> corners = loader.createSquareCorners(SGW, 10);
        for (LatLng p : corners) assertNotNull(p);
    }

    // ── createSquareCorners – cardinal directions ─────────────────────────────

    @Test
    public void createSquareCorners_northWest_aboveCenterAndLeftOfCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        LatLng nw = c.get(0);
        assertTrue("NW lat should be above center",   nw.latitude  > SGW.latitude);
        assertTrue("NW lng should be left of center", nw.longitude < SGW.longitude);
    }

    @Test
    public void createSquareCorners_northEast_aboveCenterAndRightOfCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        LatLng ne = c.get(1);
        assertTrue("NE lat should be above center",    ne.latitude  > SGW.latitude);
        assertTrue("NE lng should be right of center", ne.longitude > SGW.longitude);
    }

    @Test
    public void createSquareCorners_southEast_belowCenterAndRightOfCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        LatLng se = c.get(2);
        assertTrue("SE lat should be below center",    se.latitude  < SGW.latitude);
        assertTrue("SE lng should be right of center", se.longitude > SGW.longitude);
    }

    @Test
    public void createSquareCorners_southWest_belowCenterAndLeftOfCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        LatLng sw = c.get(3);
        assertTrue("SW lat should be below center",   sw.latitude  < SGW.latitude);
        assertTrue("SW lng should be left of center", sw.longitude < SGW.longitude);
    }

    // ── createSquareCorners – symmetry ────────────────────────────────────────

    @Test
    public void createSquareCorners_northAndSouthSymmetricAboutCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        double latOffset = c.get(0).latitude - SGW.latitude;   // NW lat offset
        double southOff  = SGW.latitude - c.get(2).latitude;   // SE lat offset
        assertEquals(latOffset, southOff, 1e-10);
    }

    @Test
    public void createSquareCorners_eastAndWestSymmetricAboutCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        double lngOffsetEast = c.get(1).longitude - SGW.longitude;  // NE
        double lngOffsetWest = SGW.longitude - c.get(0).longitude;  // NW
        assertEquals(lngOffsetEast, lngOffsetWest, 1e-10);
    }

    // ── createSquareCorners – scaling ─────────────────────────────────────────

    @Test
    public void createSquareCorners_largerSide_producesLargerLatOffset() {
        List<LatLng> small = loader.createSquareCorners(SGW, 10);
        List<LatLng> large = loader.createSquareCorners(SGW, 100);
        double smallOff = small.get(0).latitude - SGW.latitude;
        double largeOff = large.get(0).latitude - SGW.latitude;
        assertTrue("Larger side should produce larger lat offset", largeOff > smallOff);
    }

    @Test
    public void createSquareCorners_largerSide_producesLargerLngOffset() {
        List<LatLng> small = loader.createSquareCorners(SGW, 10);
        List<LatLng> large = loader.createSquareCorners(SGW, 100);
        double smallOff = large.get(1).longitude - SGW.longitude;
        double largeOff = small.get(1).longitude - SGW.longitude;
        // large side should have more offset than small
        assertTrue("Larger side should produce larger lng offset", smallOff > largeOff);
    }

    @Test
    public void createSquareCorners_latOffsetMatchesExpectedFormula() {
        float side = 20f;
        double expectedLatOffset = (side / 2.0) / 111000f;
        List<LatLng> c = loader.createSquareCorners(SGW, side);
        double actualLatOffset = c.get(0).latitude - SGW.latitude;
        assertEquals(expectedLatOffset, actualLatOffset, 1e-10);
    }

    @Test
    public void createSquareCorners_differentCenters_offsetsMatchFormula() {
        LatLng loy = new LatLng(45.458, -73.641);
        List<LatLng> c = loader.createSquareCorners(loy, 10);
        assertEquals(5, c.size());
        assertTrue(c.get(0).latitude  > loy.latitude);
        assertTrue(c.get(0).longitude < loy.longitude);
    }

    // ── createSquareFeature ───────────────────────────────────────────────────

    @Test
    public void createSquareFeature_returnsNonNull() {
        GeoJsonFeature feature = loader.createSquareFeature(SGW, "way/123");
        assertNotNull(feature);
    }

    @Test
    public void createSquareFeature_hasCorrectId() {
        GeoJsonFeature feature = loader.createSquareFeature(SGW, "way/456");
        assertEquals("way/456", feature.getId());
    }

    @Test
    public void createSquareFeature_propertyIdMatchesInput() {
        GeoJsonFeature feature = loader.createSquareFeature(SGW, "way/789");
        assertEquals("way/789", feature.getProperty("id"));
    }

    @Test
    public void createSquareFeature_propertyLayerIsDetailButton() {
        GeoJsonFeature feature = loader.createSquareFeature(SGW, "way/123");
        assertEquals("detailButton", feature.getProperty("layer"));
    }

    @Test
    public void createSquareFeature_geometryIsPolygon() {
        GeoJsonFeature feature = loader.createSquareFeature(SGW, "way/123");
        assertTrue(feature.getGeometry() instanceof GeoJsonPolygon);
    }

    @Test
    public void createSquareFeature_polygonHasFiveCoordinates() {
        GeoJsonFeature feature  = loader.createSquareFeature(SGW, "way/123");
        GeoJsonPolygon polygon  = (GeoJsonPolygon) feature.getGeometry();
        List<LatLng> ring       = polygon.getCoordinates().get(0);
        assertEquals(5, ring.size());
    }
}