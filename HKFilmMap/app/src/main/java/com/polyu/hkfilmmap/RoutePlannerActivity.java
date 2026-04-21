package com.polyu.hkfilmmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.maps.android.PolyUtil;
import com.polyu.hkfilmmap.data.AppRepository;
import com.polyu.hkfilmmap.data.NearbyFoodItem;
import com.polyu.hkfilmmap.data.RouteCandidateItem;
import com.polyu.hkfilmmap.data.RouteSceneItem;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RoutePlannerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final LatLng HK_CENTER = new LatLng(22.3193, 114.1694);
    /** Beyond this distance from HK, half-day time estimates reject every stop (origin stays in DB coords). */
    private static final double HK_HALF_DAY_MAX_DISTANCE_KM = 160d;
    private static final int PANEL_ROUTE = 0;
    private static final int PANEL_SEARCH = 1;
    private static final int PANEL_BROWSE = 2;

    private AppRepository repository;
    private RouteSceneAdapter routeAdapter;
    private RouteCandidateAdapter candidateAdapter;
    private TextView tvRouteSummary;
    private TextView tvRouteEmpty;
    private TextView tvMovieRouteHint;
    private TextView tvCandidateEmpty;
    private MaterialButtonToggleGroup rgRandomMode;
    private TabLayout tabLayoutRoute;
    private EditText inputRouteSearch;
    private RecyclerView recyclerRoute;
    private RecyclerView recyclerRouteCandidates;
    private MaterialButton btnStartNavigation;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;

    private int currentPanel = PANEL_ROUTE;
    private List<RouteSceneItem> currentRouteScenes = new ArrayList<>();
    private Marker focusMarker;
    private Marker foodMarker;
    private Polyline foodPolyline;
    private BottomSheetDialog currentFoodSheet;
    private RouteSceneItem foodOriginStop;
    private boolean routeOrderDirty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_planner);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavigateToExplore.openMapHomeAndFinish(RoutePlannerActivity.this);
            }
        });

        repository = AppRepository.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tvRouteSummary = findViewById(R.id.tvRouteSummary);
        tvRouteEmpty = findViewById(R.id.tvRouteEmpty);
        tvMovieRouteHint = findViewById(R.id.tvMovieRouteHint);
        tvCandidateEmpty = findViewById(R.id.tvCandidateEmpty);
        rgRandomMode = findViewById(R.id.rgRandomMode);
        tabLayoutRoute = findViewById(R.id.tabLayoutRoute);
        inputRouteSearch = findViewById(R.id.inputRouteSearch);
        recyclerRoute = findViewById(R.id.recyclerRoute);
        recyclerRouteCandidates = findViewById(R.id.recyclerRouteCandidates);
        btnStartNavigation = findViewById(R.id.btnStartNavigation);

        setupRouteList();
        setupCandidateList();
        setupControls();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapRoute);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setPanel(PANEL_ROUTE);
        loadRoute(HK_CENTER, false);
    }

    private void setupRouteList() {
        recyclerRoute.setLayoutManager(new LinearLayoutManager(this));
        recyclerRoute.setNestedScrollingEnabled(false);
        routeAdapter = new RouteSceneAdapter();
        routeAdapter.setOnFoodClickListener(new RouteSceneAdapter.OnFoodClickListener() {
            @Override
            public void onFoodClick(RouteSceneItem item) {
                showFoodBottomSheet(item);
            }
        });
        routeAdapter.setOnRemoveClickListener(new RouteSceneAdapter.OnRemoveClickListener() {
            @Override
            public void onRemoveClick(RouteSceneItem item) {
                removeRouteStop(item);
            }
        });
        routeAdapter.setOnCheckInClickListener(new RouteSceneAdapter.OnCheckInClickListener() {
            @Override
            public void onCheckInClick(RouteSceneItem item) {
                boolean nowCheckedIn = repository.toggleCheckIn(item.sceneId);
                loadRoute(HK_CENTER, false);
                if (nowCheckedIn) {
                    showFoodBottomSheet(item, true);
                }
            }
        });
        routeAdapter.setOnStopClickListener(new RouteSceneAdapter.OnStopClickListener() {
            @Override
            public void onStopClick(RouteSceneItem item) {
                focusStop(item);
            }
        });
        recyclerRoute.setAdapter(routeAdapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                routeOrderDirty = true;
                routeAdapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (routeOrderDirty) {
                    persistManualRouteOrder();
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        }).attachToRecyclerView(recyclerRoute);
    }

    private void setupCandidateList() {
        recyclerRouteCandidates.setLayoutManager(new LinearLayoutManager(this));
        recyclerRouteCandidates.setNestedScrollingEnabled(false);
        candidateAdapter = new RouteCandidateAdapter(new RouteCandidateAdapter.Listener() {
            @Override
            public void onToggleRoute(RouteCandidateItem item) {
                toggleCandidateRoute(item);
            }

            @Override
            public void onViewDetail(RouteCandidateItem item) {
                openPlaceDetail(item.placeId);
            }

            @Override
            public void onFocus(RouteCandidateItem item) {
                focusCandidate(item);
            }
        });
        recyclerRouteCandidates.setAdapter(candidateAdapter);
    }

    private void setupControls() {
        tabLayoutRoute.addTab(tabLayoutRoute.newTab().setText(R.string.route_tab_my_route));
        tabLayoutRoute.addTab(tabLayoutRoute.newTab().setText(R.string.route_tab_search));
        tabLayoutRoute.addTab(tabLayoutRoute.newTab().setText(R.string.route_tab_browse));
        tabLayoutRoute.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 1) {
                    setPanel(PANEL_SEARCH);
                } else if (pos == 2) {
                    setPanel(PANEL_BROWSE);
                } else {
                    setPanel(PANEL_ROUTE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
        inputRouteSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentPanel == PANEL_SEARCH) {
                    refreshCandidates();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        findViewById(R.id.btnOptimizeRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optimizeRoute();
            }
        });
        findViewById(R.id.btnClearRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repository.clearDraftRoute();
                clearFoodNavigation();
                loadRoute(HK_CENTER, false);
            }
        });
        findViewById(R.id.btnBackRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigateToExplore.openMapHomeAndFinish(RoutePlannerActivity.this);
            }
        });
        findViewById(R.id.btnGenerateMovieRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateRandomHalfDayRoute();
            }
        });
        btnStartNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNavigation();
            }
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_route);
            bottomNav.setOnItemSelectedListener(new com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.nav_explore) {
                        Intent intent = new Intent(RoutePlannerActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    } else if (id == R.id.nav_movies) {
                        Intent intent = new Intent(RoutePlannerActivity.this, MovieCatalogActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    } else if (id == R.id.nav_route) {
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void setPanel(int panel) {
        currentPanel = panel;
        boolean routePanel = panel == PANEL_ROUTE;
        recyclerRoute.setVisibility(routePanel ? View.VISIBLE : View.GONE);
        recyclerRouteCandidates.setVisibility(routePanel ? View.GONE : View.VISIBLE);
        inputRouteSearch.setVisibility(panel == PANEL_SEARCH ? View.VISIBLE : View.GONE);
        tvCandidateEmpty.setVisibility(View.GONE);
        tvRouteEmpty.setVisibility(routePanel && currentRouteScenes.isEmpty() ? View.VISIBLE : View.GONE);
        if (routePanel) {
            routeAdapter.submit(currentRouteScenes);
        } else {
            refreshCandidates();
        }
    }

    private void generateRandomHalfDayRoute() {
        clearFoodNavigation();
        final boolean preferNearby = rgRandomMode.getCheckedButtonId() != R.id.rbMoreRandom;
        resolveOrigin(new OriginCallback() {
            @Override
            public void onResolved(LatLng origin, boolean fromCurrentLocation) {
                boolean useHkCenter = distanceKm(HK_CENTER.latitude, HK_CENTER.longitude, origin.latitude, origin.longitude)
                        > HK_HALF_DAY_MAX_DISTANCE_KM;
                final LatLng routeOrigin = useHkCenter ? HK_CENTER : origin;
                if (useHkCenter) {
                    Toast.makeText(RoutePlannerActivity.this, R.string.route_build_using_hk_center, Toast.LENGTH_LONG).show();
                }
                List<RouteSceneItem> generated = repository.generateRandomHalfDayRouteNearOrigin(routeOrigin, 4, 6, preferNearby);
                if (generated.isEmpty()) {
                    Toast.makeText(RoutePlannerActivity.this, R.string.route_no_candidates, Toast.LENGTH_SHORT).show();
                    return;
                }
                List<RouteSceneItem> optimized = repository.optimizeDraftRoute(routeOrigin);
                TabLayout.Tab routeTab = tabLayoutRoute.getTabAt(PANEL_ROUTE);
                if (routeTab != null) routeTab.select();
                currentRouteScenes = optimized;
                routeAdapter.submit(optimized);
                updateRouteEmptyState();
                drawRoute(routeOrigin, optimized);
                int estimatedMinutes = estimateHalfDayMinutes(routeOrigin, optimized);
                String mtrTip = buildMtrSuggestion(optimized);
                tvMovieRouteHint.setText("Estimated: " + formatDuration(estimatedMinutes)
                        + " | Stops: " + optimized.size()
                        + " | MTR: " + mtrTip
                        + " | " + getString(R.string.route_mode_label) + ": "
                        + (preferNearby ? getString(R.string.route_mode_nearby) : getString(R.string.route_mode_random)));
                boolean nearHk = !useHkCenter;
                tvRouteSummary.setText("Half-day route draft ready from "
                        + (fromCurrentLocation && nearHk ? "your location" : "Hong Kong center") + ".");
                refreshCandidates();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRoute(HK_CENTER, false);
    }

    private void optimizeRoute() {
        clearFoodNavigation();
        final List<RouteSceneItem> current = repository.getDraftRouteScenes();
        if (current.isEmpty()) {
            Toast.makeText(this, R.string.hint_no_route, Toast.LENGTH_SHORT).show();
            return;
        }
        resolveOrigin(new OriginCallback() {
            @Override
            public void onResolved(LatLng origin, boolean fromCurrentLocation) {
                List<RouteSceneItem> optimized = repository.optimizeDraftRoute(origin);
                int routedCount = countResolvedStops(optimized);
                int pendingCount = optimized.size() - routedCount;
                String prefix = fromCurrentLocation ? "current location" : "Hong Kong center";
                tvRouteSummary.setText("Optimized " + routedCount + " routed stops from " + prefix + (pendingCount > 0 ? ". " + pendingCount + " stops are still waiting for coordinates." : "."));
                currentRouteScenes = optimized;
                routeAdapter.submit(optimized);
                updateRouteEmptyState();
                drawRoute(origin, optimized);
                refreshCandidates();
            }
        });
    }

    private void loadRoute(LatLng origin, boolean drawLines) {
        currentRouteScenes = repository.getDraftRouteScenes();
        routeAdapter.submit(currentRouteScenes);
        updateRouteEmptyState();
        if (currentRouteScenes.isEmpty()) {
            tvRouteSummary.setText(getString(R.string.hint_no_route));
        } else {
            int routedCount = countResolvedStops(currentRouteScenes);
            int pendingCount = currentRouteScenes.size() - routedCount;
            tvRouteSummary.setText("Draft route with " + currentRouteScenes.size() + " selected stops" + (pendingCount > 0 ? " (" + pendingCount + " pending coordinates)" : "") + ".");
        }
        if (map != null) {
            if (drawLines) {
                drawRoute(origin, currentRouteScenes);
            } else {
                renderMarkers(origin, currentRouteScenes);
            }
        }
        refreshCandidates();
    }

    private void updateRouteEmptyState() {
        tvRouteEmpty.setVisibility(currentPanel == PANEL_ROUTE && currentRouteScenes.isEmpty() ? View.VISIBLE : View.GONE);
        btnStartNavigation.setEnabled(countResolvedStops(currentRouteScenes) > 0);
        if (currentRouteScenes.isEmpty()) {
            tvMovieRouteHint.setText(getString(R.string.route_random_card_hint));
        } else {
            tvMovieRouteHint.setText("Long-press route cards to reorder. Nearby Food opens a draggable Top 10 around each stop.");
        }
    }

    private void persistManualRouteOrder() {
        if (routeAdapter == null) {
            return;
        }
        currentRouteScenes = repository.reorderDraftRouteSceneIds(routeAdapter.getSceneIds());
        routeAdapter.submit(currentRouteScenes);
        updateRouteEmptyState();
        refreshCandidates();
        if (map != null) {
            drawRoute(HK_CENTER, currentRouteScenes);
        }
        tvRouteSummary.setText("Route order updated. Long-press any stop to refine it again.");
        routeOrderDirty = false;
    }

    private void refreshCandidates() {
        if (candidateAdapter == null || currentPanel == PANEL_ROUTE) {
            return;
        }
        String query = currentPanel == PANEL_SEARCH ? inputRouteSearch.getText().toString() : "";
        int limit = currentPanel == PANEL_SEARCH ? 50 : 35;
        List<RouteCandidateItem> candidates = repository.searchRouteCandidates(query, "All", limit);
        candidateAdapter.submit(candidates);
        tvCandidateEmpty.setVisibility(candidates.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void toggleCandidateRoute(RouteCandidateItem item) {
        clearFoodNavigation();
        boolean nowInRoute;
        if (item.isInRoute == 1) {
            repository.removePlaceFromDraftRoute(item.placeId);
            nowInRoute = false;
        } else {
            nowInRoute = repository.addPlaceToDraftRoute(item.placeId);
        }
        loadRoute(HK_CENTER, false);
        focusCandidate(item);
        Toast.makeText(this,
                (nowInRoute ? "Added " : "Removed ") + safePlaceName(item.placeName),
                Toast.LENGTH_SHORT).show();
    }

    private void removeRouteStop(RouteSceneItem item) {
        clearFoodNavigation();
        repository.removePlaceFromDraftRoute(item.placeId);
        loadRoute(HK_CENTER, false);
        Toast.makeText(this, "Removed " + safePlaceName(item.placeName), Toast.LENGTH_SHORT).show();
    }

    private void openPlaceDetail(long placeId) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("place_id", placeId);
        startActivity(intent);
    }

    private void focusCandidate(RouteCandidateItem item) {
        if (map == null || item.latitude == null || item.longitude == null) {
            return;
        }
        LatLng point = new LatLng(item.latitude, item.longitude);
        if (focusMarker != null) {
            focusMarker.remove();
        }
        focusMarker = map.addMarker(new MarkerOptions()
                .position(point)
                .title(item.placeName)
                .snippet(item.topMovieTitle)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.2f));
        tvRouteSummary.setText(getString(R.string.route_stop_selected, safePlaceName(item.placeName)));
    }

    private void focusStop(RouteSceneItem item) {
        if (map == null || item.latitude == null || item.longitude == null) {
            Toast.makeText(this, "This stop is waiting for coordinates.", Toast.LENGTH_SHORT).show();
            return;
        }
        LatLng point = new LatLng(item.latitude, item.longitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15.2f));
        tvRouteSummary.setText(getString(R.string.route_stop_selected, safePlaceName(item.placeName)));
    }

    private void startNavigation() {
        final List<RouteSceneItem> routableStops = new ArrayList<>();
        for (RouteSceneItem item : currentRouteScenes) {
            if (item.latitude != null && item.longitude != null) {
                routableStops.add(item);
            }
        }
        if (routableStops.isEmpty()) {
            Toast.makeText(this, R.string.hint_no_route, Toast.LENGTH_SHORT).show();
            return;
        }
        resolveOrigin(new OriginCallback() {
            @Override
            public void onResolved(LatLng origin, boolean fromCurrentLocation) {
                drawRoute(origin, routableStops);
                tvRouteSummary.setText("In-app navigation ready from "
                        + (fromCurrentLocation ? "your location" : "Hong Kong center")
                        + ". Follow the numbered stops on this map.");
            }
        });
    }

    private String safePlaceName(String placeName) {
        return placeName == null || placeName.trim().isEmpty() ? "film place" : placeName;
    }

    private void showFoodBottomSheet(final RouteSceneItem stop) {
        showFoodBottomSheet(stop, false);
    }

    private void showFoodBottomSheet(final RouteSceneItem stop, boolean fromCheckIn) {
        if (stop.latitude == null || stop.longitude == null) {
            return;
        }
        if (currentFoodSheet != null) {
            currentFoodSheet.dismiss();
        }
        foodOriginStop = stop;
        currentFoodSheet = NearbyRecommendationSheet.show(
                this,
                safePlaceName(stop.placeName),
                stop.latitude,
                stop.longitude,
                extractDistrict(stop.addressEn),
                stop.genreGroup,
                fromCheckIn,
                new NearbyRecommendationSheet.NavigateListener() {
            @Override
            public void onNavigate(NearbyFoodItem item) {
                navigateToFood(item);
            }
        });
        if (currentFoodSheet == null) {
            foodOriginStop = null;
            return;
        }
        if (fromCheckIn) {
            tvRouteSummary.setText("Checked in at " + safePlaceName(stop.placeName) + ". Top nearby picks are ready.");
        }
    }

    private String extractDistrict(String addressEn) {
        if (addressEn == null) {
            return "";
        }
        String lower = addressEn.toLowerCase(Locale.US);
        String[] knownDistricts = {
                "Tsim Sha Tsui", "Central", "Quarry Bay", "Tuen Mun",
                "Sheung Wan", "Sha Tin", "Yau Ma Tei", "Mong Kok", "Aberdeen"
        };
        for (String d : knownDistricts) {
            if (lower.contains(d.toLowerCase(Locale.US))) {
                return d;
            }
        }
        return "";
    }

    private void navigateToFood(final NearbyFoodItem food) {
        if (currentFoodSheet != null) {
            currentFoodSheet.dismiss();
            currentFoodSheet = null;
        }
        if (foodOriginStop == null || foodOriginStop.latitude == null || foodOriginStop.longitude == null) {
            return;
        }

        clearFoodNavigation();
        final LatLng from = new LatLng(foodOriginStop.latitude, foodOriginStop.longitude);
        final LatLng to = new LatLng(food.latitude, food.longitude);

        if (map != null) {
            foodMarker = map.addMarker(new MarkerOptions()
                    .position(to)
                    .title(food.name)
                    .snippet(food.nameZh + " | " + food.priceRange)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            bounds.include(from);
            bounds.include(to);
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 120));
            } catch (IllegalStateException ignored) {
            }
        }

        tvRouteSummary.setText(getString(R.string.food_navigating, food.name));
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<LatLng> polyline = fetchWalkingPolyline(from, to);
                final int walkMinutes = estimateWalkMinutes(from, to);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (map == null) {
                            return;
                        }
                        List<LatLng> points = polyline;
                        if (points == null || points.isEmpty()) {
                            points = new ArrayList<>();
                            points.add(from);
                            points.add(to);
                        }
                        foodPolyline = map.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .color(Color.parseColor("#FF9800"))
                                .width(10f));
                        String msg = getString(R.string.food_walk_estimate, food.name, walkMinutes);
                        View anchorRoot = findViewById(android.R.id.content);
                        Snackbar snackbar = Snackbar.make(anchorRoot, msg, Snackbar.LENGTH_INDEFINITE);
                        View bottomNav = findViewById(R.id.bottomNavigation);
                        if (bottomNav != null) {
                            snackbar.setAnchorView(bottomNav);
                        }
                        snackbar.setAction(R.string.food_clear_nav, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clearFoodNavigation();
                                tvRouteSummary.setText(getString(R.string.food_nav_cleared));
                            }
                        });
                        snackbar.show();
                    }
                });
            }
        }).start();
    }

    private int estimateWalkMinutes(LatLng from, LatLng to) {
        double km = distanceKm(from.latitude, from.longitude, to.latitude, to.longitude);
        return Math.max(1, (int) Math.round((km / 4.5) * 60));
    }

    private void clearFoodNavigation() {
        if (foodMarker != null) {
            foodMarker.remove();
            foodMarker = null;
        }
        if (foodPolyline != null) {
            foodPolyline.remove();
            foodPolyline = null;
        }
        foodOriginStop = null;
    }

    private int countResolvedStops(List<RouteSceneItem> scenes) {
        int count = 0;
        for (RouteSceneItem item : scenes) {
            if (item.latitude != null && item.longitude != null) {
                count++;
            }
        }
        return count;
    }

    private int estimateHalfDayMinutes(LatLng origin, List<RouteSceneItem> scenes) {
        if (scenes.isEmpty()) {
            return 0;
        }
        double km = 0d;
        LatLng prev = origin;
        for (RouteSceneItem item : scenes) {
            if (item.latitude == null || item.longitude == null) {
                continue;
            }
            LatLng now = new LatLng(item.latitude, item.longitude);
            km += distanceKm(prev.latitude, prev.longitude, now.latitude, now.longitude);
            prev = now;
        }
        int transitMinutes = (int) Math.round((km / 3.6d) * 60d);
        int dwellMinutes = scenes.size() * 38;
        int raw = transitMinutes + dwellMinutes;
        return Math.max(180, Math.min(360, raw));
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours <= 0) {
            return mins + " min";
        }
        return String.format(Locale.US, "%dh %02dm", hours, mins);
    }

    private String buildMtrSuggestion(List<RouteSceneItem> scenes) {
        Set<String> hints = new HashSet<>();
        for (RouteSceneItem item : scenes) {
            String district = item.placeName == null ? "" : item.placeName.toLowerCase(Locale.US);
            String address = item.addressEn == null ? "" : item.addressEn.toLowerCase(Locale.US);
            String merged = district + " " + address;
            if (merged.contains("central") || merged.contains("wan chai") || merged.contains("quarry bay")) {
                hints.add("Island Line");
            }
            if (merged.contains("tsim sha tsui") || merged.contains("mong kok") || merged.contains("jordan")) {
                hints.add("Tsuen Wan Line");
            }
            if (merged.contains("admiralty")) {
                hints.add("Admiralty interchange");
            }
        }
        if (hints.isEmpty()) {
            return "Use nearest MTR exits along the route";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String hint : hints) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(hint);
            i++;
        }
        return sb.toString();
    }

    private void resolveOrigin(final OriginCallback callback) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onResolved(HK_CENTER, false);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location == null) {
                callback.onResolved(HK_CENTER, false);
            } else {
                callback.onResolved(new LatLng(location.getLatitude(), location.getLongitude()), true);
            }
        });
    }

    private void renderMarkers(LatLng origin, List<RouteSceneItem> scenes) {
        if (map == null) {
            return;
        }
        map.clear();
        focusMarker = null;
        foodMarker = null;
        foodPolyline = null;
        map.addMarker(new MarkerOptions().position(origin).title("Route start"));
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        bounds.include(origin);
        for (int i = 0; i < scenes.size(); i++) {
            RouteSceneItem item = scenes.get(i);
            if (item.latitude == null || item.longitude == null) {
                continue;
            }
            LatLng point = new LatLng(item.latitude, item.longitude);
            map.addMarker(new MarkerOptions()
                    .position(point)
                    .title((i + 1) + ". " + item.placeName)
                    .snippet(item.movieTitle)
                    .icon(PosterUtils.getPosterMarkerIcon(this, item.posterAsset))
                    .anchor(0.5f, 1f));
            bounds.include(point);
        }
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
        } catch (IllegalStateException ignored) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 11.5f));
        }
    }

    private void drawRoute(final LatLng origin, final List<RouteSceneItem> scenes) {
        renderMarkers(origin, scenes);
        if (scenes.isEmpty()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<List<LatLng>> polylines = new ArrayList<>();
                LatLng start = origin;
                for (RouteSceneItem item : scenes) {
                    if (item.latitude == null || item.longitude == null) {
                        continue;
                    }
                    LatLng end = new LatLng(item.latitude, item.longitude);
                    List<LatLng> polyline = fetchWalkingPolyline(start, end);
                    if (polyline == null || polyline.isEmpty()) {
                        polyline = new ArrayList<>();
                        polyline.add(start);
                        polyline.add(end);
                    }
                    polylines.add(polyline);
                    start = end;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (map == null) {
                            return;
                        }
                        for (List<LatLng> polyline : polylines) {
                            map.addPolyline(new PolylineOptions()
                                    .addAll(polyline)
                                    .color(ContextCompat.getColor(RoutePlannerActivity.this, R.color.route_color))
                                    .width(8f));
                        }
                    }
                });
            }
        }).start();
    }

    private List<LatLng> fetchWalkingPolyline(LatLng origin, LatLng destination) {
        HttpURLConnection connection = null;
        try {
            String requestUrl = "https://maps.googleapis.com/maps/api/directions/json?origin="
                    + origin.latitude + "," + origin.longitude
                    + "&destination=" + destination.latitude + "," + destination.longitude
                    + "&mode=walking&key=" + getString(R.string.directions_api_key);
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            JSONObject root = new JSONObject(builder.toString());
            if (!"OK".equals(root.optString("status"))) {
                return null;
            }
            String points = root.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points");
            return PolyUtil.decode(points);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        loadRoute(HK_CENTER, false);
    }

    private interface OriginCallback {
        void onResolved(LatLng origin, boolean fromCurrentLocation);
    }
}
