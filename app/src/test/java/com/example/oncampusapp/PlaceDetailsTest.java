package com.example.oncampusapp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure-Java tests for PlaceDetails POJO — covers all setters/getters including accessibility.
 */
public class PlaceDetailsTest {

    @Test
    public void defaultConstructor_allFieldsNull() {
        PlaceDetails pd = new PlaceDetails();
        assertNull(pd.getName());
        assertNull(pd.getAddress());
        assertNull(pd.getImgUri());
        assertFalse(pd.getAccessibility());
    }

    @Test
    public void setName_andGetName_roundTrip() {
        PlaceDetails pd = new PlaceDetails();
        pd.setName("Hall Building");
        assertEquals("Hall Building", pd.getName());
    }

    @Test
    public void setAddress_andGetAddress_roundTrip() {
        PlaceDetails pd = new PlaceDetails();
        pd.setAddress("1455 De Maisonneuve Blvd W");
        assertEquals("1455 De Maisonneuve Blvd W", pd.getAddress());
    }

    @Test
    public void setImgUri_andGetImgUri_roundTrip() {
        PlaceDetails pd = new PlaceDetails();
        pd.setImgUri("https://example.com/hall.jpg");
        assertEquals("https://example.com/hall.jpg", pd.getImgUri());
    }

    @Test
    public void setAccessibility_true_returnsTrue() {
        PlaceDetails pd = new PlaceDetails();
        pd.setAccessibility(true);
        assertTrue(pd.getAccessibility());
    }

    @Test
    public void setAccessibility_false_returnsFalse() {
        PlaceDetails pd = new PlaceDetails();
        pd.setAccessibility(true);
        pd.setAccessibility(false);
        assertFalse(pd.getAccessibility());
    }

    @Test
    public void setName_null_returnsNull() {
        PlaceDetails pd = new PlaceDetails();
        pd.setName("Something");
        pd.setName(null);
        assertNull(pd.getName());
    }

    @Test
    public void allFieldsSet_roundTrip() {
        PlaceDetails pd = new PlaceDetails();
        pd.setName("EV Building");
        pd.setAddress("1515 St. Catherine W");
        pd.setImgUri("https://example.com/ev.jpg");
        pd.setAccessibility(true);

        assertEquals("EV Building",             pd.getName());
        assertEquals("1515 St. Catherine W",    pd.getAddress());
        assertEquals("https://example.com/ev.jpg", pd.getImgUri());
        assertTrue(pd.getAccessibility());
    }
}
