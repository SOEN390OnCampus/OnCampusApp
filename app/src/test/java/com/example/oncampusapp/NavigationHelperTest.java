package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.example.oncampusapp.route.NavigationHelper;
import com.example.oncampusapp.route.Route;
import com.example.oncampusapp.route.RouteTravelMode;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class NavigationHelperTest {

    private List<LatLng> mockRoute;
    private LatLng startPoint;
    private LatLng midPoint;
    private LatLng destinationPoint;
    private final String VALID_RESPONSE = "{" +
            "\"routes\": [{" +
            "\"overview_polyline\": { \"points\": \"_p~iF~ps|U_ulLnnqC_mqNvxq`@\" }," +
            "\"legs\": [{" +
            "\"duration\": { \"text\": \"1 hour\" }" +
            "}]" +
            "}]" +
            "}";

    @Before
    public void setUp() {
        // Mocking a route around downtown Montreal (e.g., Hall to EV)
        startPoint = new LatLng(45.49725, -73.57895);
        midPoint = new LatLng(45.49650, -73.57800);
        destinationPoint = new LatLng(45.49560, -73.57735);

        mockRoute = Arrays.asList(startPoint, midPoint, destinationPoint);
    }

    // TESTS FOR hasArrived()

    @Test
    public void testHasArrived_UserIsExactlyAtDestination_ReturnsTrue() {
        boolean arrived = NavigationHelper.hasArrived(destinationPoint, mockRoute, 10.0);
        assertTrue("Should return true when user is exactly on the destination coordinates", arrived);
    }

    @Test
    public void testHasArrived_UserIsWithinThreshold_ReturnsTrue() {
        // A coordinate about ~5 meters away from the destination
        LatLng veryClosePoint = new LatLng(45.49563, -73.57735);

        boolean arrived = NavigationHelper.hasArrived(veryClosePoint, mockRoute, 10.0);
        assertTrue("Should return true when user is inside the 10-meter threshold", arrived);
    }

    @Test
    public void testHasArrived_UserIsOutsideThreshold_ReturnsFalse() {
        // User is at the start point, nowhere near the destination
        boolean arrived = NavigationHelper.hasArrived(startPoint, mockRoute, 10.0);
        assertFalse("Should return false when user is far away", arrived);
    }

    @Test
    public void testHasArrived_NullOrEmptyRoute_ReturnsFalse() {
        assertFalse(NavigationHelper.hasArrived(destinationPoint, null, 10.0));
        assertFalse(NavigationHelper.hasArrived(destinationPoint, new ArrayList<>(), 10.0));
    }

    // TESTS FOR getUpdatedPath()

    @Test
    public void testGetUpdatedPath_UserIsHalfway_SlicesPathCorrectly() {
        LatLng halfwayPoint = new LatLng(
                (midPoint.latitude + destinationPoint.latitude) / 2,
                (midPoint.longitude + destinationPoint.longitude) / 2
        );

        List<LatLng> newPath = NavigationHelper.getUpdatedPath(halfwayPoint, mockRoute, 50.0);

        assertNotNull(newPath);
        assertEquals("Path should be shortened to 2 points", 2, newPath.size());
        assertEquals("First point should now be the user's location", halfwayPoint, newPath.get(0));
        assertEquals("Last point should still be destination", destinationPoint, newPath.get(1));
    }

    @Test
    public void testGetUpdatedPath_UserIsOffRoute_ReturnsOriginalPath() {
        // User is in a completely different city/location
        LatLng wayOffRoutePoint = new LatLng(45.5017, -73.5673);

        List<LatLng> newPath = NavigationHelper.getUpdatedPath(wayOffRoutePoint, mockRoute, 50.0);

        // If the user is off-route, the method is designed to return the original route uncut
        assertEquals("Should return original route length", 3, newPath.size());
        assertEquals("Should not modify the start point", startPoint, newPath.get(0));
    }

    @Test
    public void testGetUpdatedPath_NullOrEmptyRoute_ReturnsSame() {
        List<LatLng> nullPath = NavigationHelper.getUpdatedPath(startPoint, null, 50.0);
        assertNull(nullPath);

        List<LatLng> emptyPath = NavigationHelper.getUpdatedPath(startPoint, new ArrayList<>(), 50.0);
        assertEquals(0, emptyPath.size());
    }
    @Test
    public void testEnumValues() {
        assertEquals("WALK", RouteTravelMode.WALK.getValue());
        assertEquals("DRIVE", RouteTravelMode.DRIVE.getValue());
        assertEquals("TRANSIT",RouteTravelMode.TRANSIT.getValue());
    }

    @Test
    public void testValueOf() {
        assertEquals(RouteTravelMode.WALK, RouteTravelMode.valueOf("WALK"));
        assertEquals(RouteTravelMode.DRIVE, RouteTravelMode.valueOf("DRIVE"));
        assertEquals(RouteTravelMode.TRANSIT, RouteTravelMode.valueOf("TRANSIT"));
    }

    @Test
    public void testEnumCount() {
        assertEquals(3, RouteTravelMode.values().length);
    }
}