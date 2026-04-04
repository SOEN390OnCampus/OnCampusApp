package com.example.oncampusapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PoiActivity extends AppCompatActivity {

    private RecyclerView rvPois;
    private PoiAdapter adapter;
    private final List<Poi> allPois = new ArrayList<>();
    private final List<Poi> visiblePois = new ArrayList<>();

    private String currentCategory = "Restaurants";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(PoiActivity.this, MapsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(PoiActivity.this, GoogleCalendarAuthActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(PoiActivity.this, SettingsActivity.class));
                finish();
                return true;
            }

            return false;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        EditText etSearch = findViewById(R.id.et_search_poi);
        TextView tabRestaurants = findViewById(R.id.tab_restaurants);
        TextView tabBookstores = findViewById(R.id.tab_bookstores);
        TextView tabShopping = findViewById(R.id.tab_shopping);
        Button btnViewMore = findViewById(R.id.btn_view_more);
        rvPois = findViewById(R.id.rv_pois);

        rvPois.setLayoutManager(new LinearLayoutManager(this));

        loadSamplePois();
        filterPois();

        adapter = new PoiAdapter(visiblePois, poi -> {
            Intent intent = new Intent(PoiActivity.this, MapsActivity.class);
            intent.putExtra("POI_NAME", poi.getName());
            intent.putExtra("POI_LAT", poi.getLatitude());
            intent.putExtra("POI_LNG", poi.getLongitude());
            intent.putExtra("OPEN_POI_ROUTE", true);
            startActivity(intent);
            finish();
        });

        rvPois.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        tabRestaurants.setOnClickListener(v -> {
            currentCategory = "Restaurants";
            filterPois();
        });

        tabBookstores.setOnClickListener(v -> {
            currentCategory = "Bookstores";
            filterPois();
        });

        tabShopping.setOnClickListener(v -> {
            currentCategory = "Shopping Centers";
            filterPois();
        });

        btnViewMore.setOnClickListener(v -> {
            // for now this button does nothing so remove it if not needed.
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPois(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // Temporary list but could be implemented with Google.
    private void loadSamplePois() {
        // Example SGW-ish coordinates
        allPois.add(new Poi("Cafe Van Houtte", "Restaurants", 45.4958, -73.5785, 0.54));
        allPois.add(new Poi("Ganadara", "Restaurants", 45.4961, -73.5791, 0.78));
        allPois.add(new Poi("McDonalds", "Restaurants", 45.4970, -73.5795, 0.86));
        allPois.add(new Poi("Kinton Ramen", "Restaurants", 45.4975, -73.5801, 1.02));
        allPois.add(new Poi("Subway", "Restaurants", 45.4950, -73.5768, 1.22));

        allPois.add(new Poi("Paragraphe Bookstore", "Bookstores", 45.4974, -73.5799, 0.65));
        allPois.add(new Poi("Indigo", "Bookstores", 45.5000, -73.5710, 2.40));

        allPois.add(new Poi("Eaton Centre", "Shopping Centers", 45.5038, -73.5703, 1.90));
        allPois.add(new Poi("Alexis Nihon", "Shopping Centers", 45.4898, -73.5819, 2.10));
    }

    private void filterPois() {
        filterPois("");
    }

    private void filterPois(String query) {
        visiblePois.clear();

        for (Poi poi : allPois) {
            boolean sameCategory = poi.getCategory().equalsIgnoreCase(currentCategory);
            boolean matchesSearch = poi.getName().toLowerCase(Locale.getDefault())
                    .contains(query.toLowerCase(Locale.getDefault()));

            if (sameCategory && matchesSearch) {
                visiblePois.add(poi);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}