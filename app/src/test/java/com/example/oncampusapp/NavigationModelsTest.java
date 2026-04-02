package com.example.oncampusapp;

import com.example.oncampusapp.navigation.Direction;
import com.example.oncampusapp.navigation.Route;
import com.example.oncampusapp.navigation.RouteTravelMode;
import com.example.oncampusapp.navigation.Step;
import com.example.oncampusapp.navigation.TransitDetails;
import com.example.oncampusapp.navigation.TransitLine;
import com.example.oncampusapp.navigation.TransitVehicleType;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pure-Java tests for the navigation package POJOs and enums.
 * No Android APIs are called in any of these classes.
 */
public class NavigationModelsTest {

    // ── RouteTravelMode.fromString ────────────────────────────────────────────

    @Test
    public void fromString_walk_returnsWalk() {
        assertEquals(RouteTravelMode.WALK, RouteTravelMode.fromString("WALK"));
    }

    @Test
    public void fromString_drive_returnsDrive() {
        assertEquals(RouteTravelMode.DRIVE, RouteTravelMode.fromString("DRIVE"));
    }

    @Test
    public void fromString_transit_returnsTransit() {
        assertEquals(RouteTravelMode.TRANSIT, RouteTravelMode.fromString("TRANSIT"));
    }

    @Test
    public void fromString_shuttle_returnsShuttle() {
        assertEquals(RouteTravelMode.SHUTTLE, RouteTravelMode.fromString("SHUTTLE"));
    }

    @Test
    public void fromString_caseInsensitive() {
        assertEquals(RouteTravelMode.WALK,    RouteTravelMode.fromString("walk"));
        assertEquals(RouteTravelMode.DRIVE,   RouteTravelMode.fromString("drive"));
        assertEquals(RouteTravelMode.TRANSIT, RouteTravelMode.fromString("transit"));
    }

    @Test
    public void fromString_unknown_fallsBackToWalk() {
        assertEquals(RouteTravelMode.WALK, RouteTravelMode.fromString("FLYING"));
        assertEquals(RouteTravelMode.WALK, RouteTravelMode.fromString(""));
        assertEquals(RouteTravelMode.WALK, RouteTravelMode.fromString(null));
    }

    // ── TransitVehicleType.fromString ─────────────────────────────────────────

    @Test
    public void transitVehicleType_fromString_knownValues() {
        assertEquals(TransitVehicleType.BUS,        TransitVehicleType.fromString("BUS"));
        assertEquals(TransitVehicleType.SUBWAY,     TransitVehicleType.fromString("SUBWAY"));
        assertEquals(TransitVehicleType.METRO_RAIL, TransitVehicleType.fromString("METRO_RAIL"));
        assertEquals(TransitVehicleType.TRAM,       TransitVehicleType.fromString("TRAM"));
        assertEquals(TransitVehicleType.FERRY,      TransitVehicleType.fromString("FERRY"));
    }

    @Test
    public void transitVehicleType_fromString_unknownFallsBackToOther() {
        assertEquals(TransitVehicleType.OTHER, TransitVehicleType.fromString("HOVERCRAFT"));
        assertEquals(TransitVehicleType.OTHER, TransitVehicleType.fromString(""));
    }

    @Test
    public void transitVehicleType_enumValues_containsAllExpectedTypes() {
        TransitVehicleType[] types = TransitVehicleType.values();
        assertTrue(types.length >= 18); // 18 declared variants
    }

    // ── Route ─────────────────────────────────────────────────────────────────

    @Test
    public void route_settersAndGetters() {
        Route route = new Route();
        List<LatLng> pts = Arrays.asList(new LatLng(45.0, -73.0), new LatLng(45.1, -73.1));

        route.setDuration("15 mins");
        route.setDistance("1.2 km");
        route.setPoints(pts);
        route.setSteps(Collections.emptyList());

        assertEquals("15 mins", route.getDuration());
        assertEquals("1.2 km",  route.getDistance());
        assertSame(pts, route.getPoints());
        assertNotNull(route.getSteps());
        assertTrue(route.getSteps().isEmpty());
    }

    @Test
    public void route_defaultGetters_returnNull() {
        Route route = new Route();
        assertNull(route.getDuration());
        assertNull(route.getDistance());
        assertNull(route.getPoints());
        assertNull(route.getSteps());
    }

    // ── Step ──────────────────────────────────────────────────────────────────

    @Test
    public void step_settersAndGetters() {
        Step step = new Step();
        List<LatLng> pts = Arrays.asList(new LatLng(45.0, -73.0));

        step.setTravelMode(RouteTravelMode.TRANSIT);
        step.setPoints(pts);
        step.setInstructions("Take the 105 bus");
        step.setDistance("500 m");
        step.setDuration("8 mins");

        assertEquals(RouteTravelMode.TRANSIT, step.getTravelMode());
        assertSame(pts, step.getPoints());
        assertEquals("Take the 105 bus", step.getInstructions());
        assertEquals("500 m",   step.getDistance());
        assertEquals("8 mins",  step.getDuration());
    }

    @Test
    public void step_transitDetails_setterAndGetter() {
        Step step = new Step();
        TransitDetails td = new TransitDetails();
        step.setTransitDetails(td);
        assertSame(td, step.getTransitDetails());
    }

    @Test
    public void step_defaultGetters_returnNull() {
        Step step = new Step();
        assertNull(step.getTravelMode());
        assertNull(step.getPoints());
        assertNull(step.getTransitDetails());
        assertNull(step.getInstructions());
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    public void direction_constructor_andGetters() {
        List<LatLng> pts = Arrays.asList(new LatLng(45.0, -73.0));
        Direction dir = new Direction("Turn left", "200 m", "3 mins",
                RouteTravelMode.WALK, pts);

        assertEquals("Turn left",        dir.getInstructions());
        assertEquals("200 m",            dir.getDistance());
        assertEquals("3 mins",           dir.getDuration());
        assertEquals(RouteTravelMode.WALK, dir.getTravelMode());
        assertSame(pts,                  dir.getPoints());
    }

    @Test
    public void direction_defaultTransitStops_areNull() {
        Direction dir = new Direction("Go", "1 km", "10 mins",
                RouteTravelMode.TRANSIT, Collections.emptyList());
        assertNull(dir.getTransitDepartureStop());
        assertNull(dir.getTransitArrivalStop());
    }

    @Test
    public void direction_setTransitStops_roundTrip() {
        Direction dir = new Direction("Board bus", "0 m", "0 mins",
                RouteTravelMode.TRANSIT, Collections.emptyList());
        LatLng dep = new LatLng(45.497, -73.579);
        LatLng arr = new LatLng(45.458, -73.640);

        dir.setTransitStops(dep, arr);

        assertEquals(dep, dir.getTransitDepartureStop());
        assertEquals(arr, dir.getTransitArrivalStop());
    }

    // ── TransitDetails ────────────────────────────────────────────────────────

    @Test
    public void transitDetails_allSettersAndGetters() {
        TransitDetails td = new TransitDetails();
        LatLng dep = new LatLng(45.497, -73.579);
        LatLng arr = new LatLng(45.458, -73.640);
        TransitLine line = new TransitLine();

        td.setDepartureStopName("Berri-UQAM");
        td.setDepartureStopLocation(dep);
        td.setArrivalStopName("Côte-Vertu");
        td.setArrivalStopLocation(arr);
        td.setDepartureTime("09:00");
        td.setArrivalTime("09:22");
        td.setVehicleType(TransitVehicleType.SUBWAY);
        td.setTransitLine(line);

        assertEquals("Berri-UQAM", td.getDepartureStopName());
        assertEquals(dep,          td.getDepartureStopLocation());
        assertEquals("Côte-Vertu", td.getArrivalStopName());
        assertEquals(arr,          td.getArrivalStopLocation());
        assertEquals("09:00",      td.getDepartureTime());
        assertEquals("09:22",      td.getArrivalTime());
        assertEquals(TransitVehicleType.SUBWAY, td.getVehicleType());
        assertSame(line,           td.getTransitLine());
    }

    @Test
    public void transitDetails_defaultGetters_returnNull() {
        TransitDetails td = new TransitDetails();
        assertNull(td.getDepartureStopName());
        assertNull(td.getArrivalStopName());
        assertNull(td.getDepartureStopLocation());
        assertNull(td.getArrivalStopLocation());
        assertNull(td.getDepartureTime());
        assertNull(td.getArrivalTime());
        assertNull(td.getVehicleType());
        assertNull(td.getTransitLine());
    }

    // ── TransitVehicleType — all enum constants reachable ─────────────────────

    @Test
    public void transitVehicleType_allValuesAreReachable() {
        for (TransitVehicleType t : TransitVehicleType.values()) {
            assertEquals(t, TransitVehicleType.fromString(t.name()));
        }
    }

    @Test
    public void transitVehicleType_cableCar() {
        assertEquals(TransitVehicleType.CABLE_CAR, TransitVehicleType.fromString("CABLE_CAR"));
    }

    @Test
    public void transitVehicleType_rail() {
        assertEquals(TransitVehicleType.RAIL, TransitVehicleType.fromString("RAIL"));
    }

    @Test
    public void transitVehicleType_intercityBus() {
        assertEquals(TransitVehicleType.INTERCITY_BUS, TransitVehicleType.fromString("INTERCITY_BUS"));
    }

    // ── RouteTravelMode — fromString ──────────────────────────────────────────

    @Test
    public void routeTravelMode_fromString_nullFallsBackToWalk() {
        assertEquals(RouteTravelMode.WALK, RouteTravelMode.fromString(null));
    }

    @Test
    public void routeTravelMode_allValuesRoundTrip() {
        for (RouteTravelMode m : RouteTravelMode.values()) {
            assertEquals(m, RouteTravelMode.fromString(m.getValue()));
        }
    }

    // ── Direction — null points ───────────────────────────────────────────────

    @Test
    public void direction_nullPoints_noThrow() {
        Direction dir = new Direction("Go", "1 km", "5 mins", RouteTravelMode.WALK, null);
        assertNull(dir.getPoints());
    }

    @Test
    public void direction_setTransitStops_nullValues_noThrow() {
        Direction dir = new Direction("Board", "0 m", "0 mins",
                RouteTravelMode.TRANSIT, java.util.Collections.emptyList());
        dir.setTransitStops(null, null);
        assertNull(dir.getTransitDepartureStop());
        assertNull(dir.getTransitArrivalStop());
    }

    // ── Route — overwrite setters ─────────────────────────────────────────────

    @Test
    public void route_overwriteValues_lastWins() {
        Route route = new Route();
        route.setDuration("5 mins");
        route.setDuration("10 mins");
        assertEquals("10 mins", route.getDuration());
    }

    // ── Step — overwrite travelMode ───────────────────────────────────────────

    @Test
    public void step_overwriteTravelMode_lastWins() {
        Step step = new Step();
        step.setTravelMode(RouteTravelMode.WALK);
        step.setTravelMode(RouteTravelMode.TRANSIT);
        assertEquals(RouteTravelMode.TRANSIT, step.getTravelMode());
    }

    // ── TransitDetails — vehicleType enum values ──────────────────────────────

    @Test
    public void transitDetails_vehicleType_allModes() {
        for (TransitVehicleType t : TransitVehicleType.values()) {
            TransitDetails td = new TransitDetails();
            td.setVehicleType(t);
            assertEquals(t, td.getVehicleType());
        }
    }

    // ── TransitLine ───────────────────────────────────────────────────────────

    @Test
    public void transitLine_settersAndGetters() {
        TransitLine line = new TransitLine();
        line.setName("Green Line");
        line.setNameShort("2");
        line.setColor("#00A651");

        assertEquals("Green Line", line.getName());
        assertEquals("2",          line.getNameShort());
        assertEquals("#00A651",    line.getColor());
    }

    @Test
    public void transitLine_defaultGetters_returnNull() {
        TransitLine line = new TransitLine();
        assertNull(line.getName());
        assertNull(line.getNameShort());
        assertNull(line.getColor());
    }
}
