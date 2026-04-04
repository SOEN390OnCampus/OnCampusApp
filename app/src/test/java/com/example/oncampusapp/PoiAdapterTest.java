package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
public class PoiAdapterTest {

    private ViewGroup parent;

    @Before
    public void setUp() {
        parent = new FrameLayout(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void getItemCount_returnsCorrectSize() {
        List<Poi> pois = new ArrayList<>();
        pois.add(new Poi("A", "Restaurants", 1, 1, 0.5, "Open"));
        pois.add(new Poi("B", "Restaurants", 2, 2, 1.0, "Closed"));

        PoiAdapter adapter = new PoiAdapter(pois, poi -> {});

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void onCreateViewHolder_inflatesViewHolderCorrectly() {
        PoiAdapter adapter = new PoiAdapter(new ArrayList<>(), poi -> {});

        PoiAdapter.PoiViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        assertNotNull(holder);
        assertNotNull(holder.txtName);
        assertNotNull(holder.txtDistance);
        assertNotNull(holder.statusDot);
    }

    @Test
    public void onBindViewHolder_setsNameDistanceAndGreenColor_forOpen() {
        List<Poi> pois = new ArrayList<>();
        pois.add(new Poi("Cafe Van Houtte", "Restaurants", 45.4958, -73.5785, 0.54, "Open"));

        PoiAdapter adapter = new PoiAdapter(pois, poi -> {});
        PoiAdapter.PoiViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Cafe Van Houtte", holder.txtName.getText().toString());
        assertEquals(String.format(Locale.getDefault(), "%.2f KM", 0.54),
                holder.txtDistance.getText().toString());

        ColorStateList tint = holder.statusDot.getBackgroundTintList();
        assertNotNull(tint);
        assertEquals(Color.parseColor("#4CAF50"), tint.getDefaultColor());
    }

    @Test
    public void onBindViewHolder_setsYellowColor_forAlmostClosed() {
        List<Poi> pois = new ArrayList<>();
        pois.add(new Poi("Alexis Nihon", "Shopping Centers", 45.4898, -73.5819, 2.10, "Almost_closed"));

        PoiAdapter adapter = new PoiAdapter(pois, poi -> {});
        PoiAdapter.PoiViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ColorStateList tint = holder.statusDot.getBackgroundTintList();
        assertNotNull(tint);
        assertEquals(Color.parseColor("#FFC107"), tint.getDefaultColor());
    }

    @Test
    public void onBindViewHolder_setsRedColor_forClosed() {
        List<Poi> pois = new ArrayList<>();
        pois.add(new Poi("McDonalds", "Restaurants", 45.4970, -73.5795, 0.86, "Closed"));

        PoiAdapter adapter = new PoiAdapter(pois, poi -> {});
        PoiAdapter.PoiViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ColorStateList tint = holder.statusDot.getBackgroundTintList();
        assertNotNull(tint);
        assertEquals(Color.parseColor("#F44336"), tint.getDefaultColor());
    }

    @Test
    public void onBindViewHolder_setsGrayColor_forUnknownStatus() {
        List<Poi> pois = new ArrayList<>();
        pois.add(new Poi("Unknown Place", "Restaurants", 0, 0, 1.0, "SomethingElse"));

        PoiAdapter adapter = new PoiAdapter(pois, poi -> {});
        PoiAdapter.PoiViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        ColorStateList tint = holder.statusDot.getBackgroundTintList();
        assertNotNull(tint);
        assertEquals(Color.GRAY, tint.getDefaultColor());
    }

    @Test
    public void clickingItem_callsListener() {
        List<Poi> pois = new ArrayList<>();
        Poi expectedPoi = new Poi("Ganadara", "Restaurants", 45.4961, -73.5791, 0.78, "Open");
        pois.add(expectedPoi);

        AtomicBoolean clicked = new AtomicBoolean(false);

        PoiAdapter adapter = new PoiAdapter(pois, poi -> {
            assertEquals(expectedPoi.getName(), poi.getName());
            clicked.set(true);
        });

        PoiAdapter.PoiViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();

        assertTrue(clicked.get());
    }
}