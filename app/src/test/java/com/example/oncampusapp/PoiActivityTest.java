package com.example.oncampusapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class PoiActivityTest {

    @Test
    public void onCreate_setsUpRecyclerViewAndAdapter() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        assertNotNull(recyclerView);
        assertNotNull(recyclerView.getLayoutManager());
        assertNotNull(recyclerView.getAdapter());
    }

    @Test
    public void onCreate_showsRestaurantsByDefault() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        // Restaurants loaded by default: Cafe Van Houtte, Ganadara, McDonalds, Kinton Ramen, Subway
        assertEquals(5, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void clickingBookstoresTab_filtersListToBookstores() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        TextView tabBookstores = activity.findViewById(R.id.tab_bookstores);
        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        tabBookstores.performClick();

        // Bookstores: Paragraphe Bookstore, Indigo
        assertEquals(2, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void clickingShoppingTab_filtersListToShoppingCenters() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        TextView tabShopping = activity.findViewById(R.id.tab_shopping);
        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        tabShopping.performClick();

        // Shopping Centers: Eaton Centre, Alexis Nihon
        assertEquals(2, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void typingSearch_filtersVisiblePoisWithinCurrentCategory() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        EditText etSearch = activity.findViewById(R.id.et_search_poi);
        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        etSearch.setText("gan");

        // Should match only "Ganadara" in Restaurants
        assertEquals(1, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void searchIsCaseInsensitive() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        EditText etSearch = activity.findViewById(R.id.et_search_poi);
        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        etSearch.setText("MCDONALDS");

        assertEquals(1, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void searchWorksAfterChangingCategory() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        TextView tabShopping = activity.findViewById(R.id.tab_shopping);
        EditText etSearch = activity.findViewById(R.id.et_search_poi);
        RecyclerView recyclerView = activity.findViewById(R.id.rv_pois);

        tabShopping.performClick();
        etSearch.setText("Alexis");

        assertEquals(1, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void bottomNavigation_homeStartsMapsActivity() {
        ActivityController<PoiActivity> controller =
                Robolectric.buildActivity(PoiActivity.class).setup();
        PoiActivity activity = controller.get();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        assertEquals(MapsActivity.class.getName(),
                Shadows.shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }
}