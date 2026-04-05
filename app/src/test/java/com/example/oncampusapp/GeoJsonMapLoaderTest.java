package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.Geometry;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class GeoJsonMapLoaderTest {

    @Mock MapsActivity mockActivity;
    @Mock BuildingClassifier mockClassifier;
    @Mock BuildingDialogManager mockDialogManager;

    private GeoJsonMapLoader loader;
    private static final LatLng SGW = new LatLng(45.497, -73.579);

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        loader = new GeoJsonMapLoader(mockActivity, mockClassifier, mockDialogManager);
    }

    @Test
    public void constructor_doesNotThrow() {
        assertNotNull(loader);
    }

    // ── Private Method Reflection: handleFeatureClick ─────────────────────────

    @Test
    public void testHandleFeatureClick_nonPolygon_doesNothing() throws Exception {
        Feature mockFeature = mock(Feature.class);
        Geometry mockGeometry = mock(Geometry.class); // Not a GeoJsonPolygon
        when(mockFeature.getGeometry()).thenReturn(mockGeometry);

        GeoJsonMapLoader.FeatureClickHandler mockHandler = mock(GeoJsonMapLoader.FeatureClickHandler.class);

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("handleFeatureClick", Feature.class, GeoJsonMapLoader.FeatureClickHandler.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, mockHandler);

        // Assertion: If it's not a polygon, it returns immediately without clicking anything
        verify(mockHandler, never()).onBuildingPolygonClicked(any());
        verify(mockHandler, never()).onDetailButtonClicked(any());
    }

    @Test
    public void testHandleFeatureClick_polygonWithNullLayer_callsBuildingClick() throws Exception {
        Feature mockFeature = mock(Feature.class);
        GeoJsonPolygon mockPolygon = mock(GeoJsonPolygon.class);
        when(mockFeature.getGeometry()).thenReturn(mockPolygon);
        when(mockFeature.getProperty("layer")).thenReturn(null); // Null layer means it's a standard building

        GeoJsonMapLoader.FeatureClickHandler mockHandler = mock(GeoJsonMapLoader.FeatureClickHandler.class);

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("handleFeatureClick", Feature.class, GeoJsonMapLoader.FeatureClickHandler.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, mockHandler);

        // Assertion
        verify(mockHandler).onBuildingPolygonClicked(mockFeature);
        verify(mockHandler, never()).onDetailButtonClicked(any());
    }

    @Test
    public void testHandleFeatureClick_polygonWithDetailButtonLayer_callsDetailClick() throws Exception {
        Feature mockFeature = mock(Feature.class);
        GeoJsonPolygon mockPolygon = mock(GeoJsonPolygon.class);
        when(mockFeature.getGeometry()).thenReturn(mockPolygon);
        when(mockFeature.getProperty("layer")).thenReturn("detailButton");
        when(mockFeature.getProperty("id")).thenReturn("way/123");

        GeoJsonMapLoader.FeatureClickHandler mockHandler = mock(GeoJsonMapLoader.FeatureClickHandler.class);

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("handleFeatureClick", Feature.class, GeoJsonMapLoader.FeatureClickHandler.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, mockHandler);

        // Assertion
        verify(mockHandler).onDetailButtonClicked("way/123");
        verify(mockHandler, never()).onBuildingPolygonClicked(any());
    }

    // ── Private Method Reflection: applyFeatureStyle ──────────────────────────

    // ── Private Method Reflection: applyFeatureStyle ──────────────────────────

    @Test
    public void testApplyFeatureStyle_lineString_setsLineStyle() throws Exception {
        GeoJsonFeature mockFeature = mock(GeoJsonFeature.class);

        // Setup a mock StyleConfig using the required constructor:
        // (int fillColor, int strokeColor, float strokeWidth, boolean isLineString)
        FeatureStyler.StyleConfig config = new FeatureStyler.StyleConfig(
                Color.TRANSPARENT, Color.RED, 5f, true
        );

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("applyFeatureStyle", GeoJsonFeature.class, FeatureStyler.StyleConfig.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, config);

        // Assertion: It should apply a LineString style to the feature
        verify(mockFeature).setLineStringStyle(any(GeoJsonLineStringStyle.class));
        verify(mockFeature, never()).setPolygonStyle(any());
    }

    @Test
    public void testApplyFeatureStyle_polygon_setsPolygonStyle() throws Exception {
        GeoJsonFeature mockFeature = mock(GeoJsonFeature.class);

        // Setup a mock StyleConfig using the required constructor:
        // (int fillColor, int strokeColor, float strokeWidth, boolean isLineString)
        FeatureStyler.StyleConfig config = new FeatureStyler.StyleConfig(
                Color.BLUE, Color.BLACK, 2f, false
        );

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("applyFeatureStyle", GeoJsonFeature.class, FeatureStyler.StyleConfig.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, config);

        // Assertion: It should apply a Polygon style to the feature
        verify(mockFeature).setPolygonStyle(any(GeoJsonPolygonStyle.class));
        verify(mockFeature, never()).setLineStringStyle(any());
    }

    // ── Private Method Reflection: processFeature ─────────────────────────────

    @Test
    public void testProcessFeature_addsToSuggestions_ifNameValidAndNotDuplicate() throws Exception {
        GeoJsonFeature mockFeature = mock(GeoJsonFeature.class);
        when(mockFeature.getProperty("name")).thenReturn("Hall Building");
        when(mockFeature.getProperty("type")).thenReturn("university");

        ArrayList<String> suggestions = new ArrayList<>();
        FeatureStyler styler = new FeatureStyler();
        GeofenceManager geofenceManager = mock(GeofenceManager.class);
        BuildingManager buildingManager = mock(BuildingManager.class);
        GoogleMap map = mock(GoogleMap.class);
        List<GeoJsonFeature> pointFeatures = new ArrayList<>();

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("processFeature",
                GeoJsonFeature.class, ArrayList.class, FeatureStyler.class,
                GeofenceManager.class, BuildingManager.class, GoogleMap.class, List.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, suggestions, styler, geofenceManager, buildingManager, map, pointFeatures);

        // Assertion: Hall Building should be added to the suggestions list
        assertEquals(1, suggestions.size());
        assertEquals("Hall Building", suggestions.get(0));
    }

    @Test
    public void testProcessFeature_doesNotAddToSuggestions_ifNameNullOrEmpty() throws Exception {
        GeoJsonFeature mockFeature = mock(GeoJsonFeature.class);
        when(mockFeature.getProperty("name")).thenReturn("   "); // Blank name

        ArrayList<String> suggestions = new ArrayList<>();
        FeatureStyler styler = new FeatureStyler();

        Method method = GeoJsonMapLoader.class.getDeclaredMethod("processFeature",
                GeoJsonFeature.class, ArrayList.class, FeatureStyler.class,
                GeofenceManager.class, BuildingManager.class, GoogleMap.class, List.class);
        method.setAccessible(true);
        method.invoke(loader, mockFeature, suggestions, styler, mock(GeofenceManager.class), mock(BuildingManager.class), mock(GoogleMap.class), new ArrayList<>());

        // Assertion: Empty names are ignored for search
        assertTrue(suggestions.isEmpty());
    }

    // ── Original Geometry Math Tests ──────────────────────────────────────────

    @Test
    public void createSquareCorners_returnsFivePoints() {
        List<LatLng> corners = loader.createSquareCorners(SGW, 10);
        assertEquals(5, corners.size());
    }

    @Test
    public void createSquareCorners_closedPolygon_firstEqualsLast() {
        List<LatLng> corners = loader.createSquareCorners(SGW, 10);

        // Explicitly define our baseline (expected) and our computed result (actual)
        double expectedLatitude = corners.get(0).latitude;
        double actualLatitude = corners.get(4).latitude;

        assertEquals(expectedLatitude, actualLatitude, 1e-12);
    }

    @Test
    public void createSquareCorners_northWest_aboveCenterAndLeftOfCenter() {
        List<LatLng> c = loader.createSquareCorners(SGW, 10);
        assertTrue(c.get(0).latitude > SGW.latitude);
        assertTrue(c.get(0).longitude < SGW.longitude);
    }

    @Test
    public void createSquareCorners_largerSide_producesLargerOffsets() {
        List<LatLng> small = loader.createSquareCorners(SGW, 10);
        List<LatLng> large = loader.createSquareCorners(SGW, 100);
        assertTrue(large.get(0).latitude - SGW.latitude > small.get(0).latitude - SGW.latitude);
    }

    @Test
    public void createSquareFeature_returnsValidGeoJsonWithCorrectProperties() {
        GeoJsonFeature feature = loader.createSquareFeature(SGW, "way/123");
        assertEquals("way/123", feature.getId());
        assertEquals("way/123", feature.getProperty("id"));
        assertEquals("detailButton", feature.getProperty("layer"));
        assertTrue(feature.getGeometry() instanceof GeoJsonPolygon);
        assertEquals(5, ((GeoJsonPolygon) feature.getGeometry()).getCoordinates().get(0).size());
    }
}