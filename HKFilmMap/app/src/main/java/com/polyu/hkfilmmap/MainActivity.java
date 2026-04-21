package com.polyu.hkfilmmap;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.PolyUtil;
import com.polyu.hkfilmmap.data.AppRepository;
import com.polyu.hkfilmmap.data.MapPlaceItem;
import com.polyu.hkfilmmap.data.NearbyFoodItem;
import com.polyu.hkfilmmap.data.PlaceDetail;
import com.polyu.hkfilmmap.data.PlaceSceneItem;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOC_PERMISSION_REQUEST = 7001;
    private static final LatLng HK_CENTER = new LatLng(22.3193, 114.1694);
    private static final float ZOOM_CLUSTER_THRESHOLD = 12.9f;
    private static final float ZOOM_EXPANDED_THRESHOLD = 14.4f;
    private static final String A_FIRST_STEP = "first_step";
    private static final String A_SCENE_ROOKIE = "scene_rookie";
    private static final String A_SCENE_HUNTER = "scene_hunter";
    private static final String A_HALF_MAP = "half_map";
    private static final String A_FULL_MAP = "full_map";
    private static final String A_MOVIE_COLLECTOR = "movie_collector";
    private static final String A_ROUTE_ROOKIE = "route_rookie";
    private static final String A_ROUTE_MASTER = "route_master";

    private GoogleMap map;
    private AppRepository repository;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private PlaceAdapter placeAdapter;
    private PlaceSceneAdapter placeSceneAdapter;
    private TextView tvStats;
    private ProgressBar progressCheckIn;
    private TextView tvEmptyState;
    private TextView tvSheetTitle;
    private TextView tvSheetSubtitle;
    private ChipGroup chipGroupGenre;
    private RecyclerView recyclerPlaces;
    private RecyclerView recyclerPlaceScenes;
    private View btnSheetBack;
    private View btnOpenPlacePage;
    private View btnPlaceRecommendations;
    private LinearLayout layoutPlaceActions;
    private View layoutStatsHeader;
    private View chipScrollView;
    private String selectedGenre = "All";
    private long selectedPlaceId = -1L;
    private List<MapPlaceItem> currentPlaces = new ArrayList<>();
    private final Map<Long, List<PlaceSceneItem>> placeSceneCache = new HashMap<>();
    private final Map<Marker, MapPlaceItem> markerMap = new HashMap<>();
    private final Map<Marker, ClusterGroup> clusterMarkerMap = new HashMap<>();
    private Marker activeFoodMarker;
    private Polyline activeFoodPolyline;
    private Snackbar activeFoodSnackbar;
    private LatLng activeFoodOrigin;
    private NearbyFoodItem activeFoodDestination;
    private List<LatLng> activeFoodPolylinePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        repository = AppRepository.getInstance(this);
        tvStats = findViewById(R.id.tvStats);
        progressCheckIn = findViewById(R.id.progressCheckIn);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvSheetTitle = findViewById(R.id.tvSheetTitle);
        tvSheetSubtitle = findViewById(R.id.tvSheetSubtitle);
        chipGroupGenre = findViewById(R.id.chipGroupGenre);
        chipScrollView = findViewById(R.id.chipScrollView);
        layoutStatsHeader = findViewById(R.id.layoutStatsHeader);
        layoutPlaceActions = findViewById(R.id.layoutPlaceActions);
        recyclerPlaces = findViewById(R.id.recyclerPlaces);
        recyclerPlaceScenes = findViewById(R.id.recyclerPlaceScenes);
        btnSheetBack = findViewById(R.id.btnSheetBack);
        btnOpenPlacePage = findViewById(R.id.btnOpenPlacePage);
        btnPlaceRecommendations = findViewById(R.id.btnPlaceRecommendations);

        findViewById(R.id.btnSignOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent signOut = new Intent(MainActivity.this, LoginActivity.class);
                signOut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(signOut);
                finish();
            }
        });
        findViewById(R.id.btnAchievements).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AchievementActivity.class));
            }
        });
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_explore);
        bottomNav.setOnItemSelectedListener(new com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_explore) {
                    showPlaceList();
                    return true;
                } else if (id == R.id.nav_route) {
                    Intent intent = new Intent(MainActivity.this, RoutePlannerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_movies) {
                    Intent intent = new Intent(MainActivity.this, MovieCatalogActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        View bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(dpToPx(300));
        bottomSheetBehavior.setFitToContents(false);
        bottomSheetBehavior.setHalfExpandedRatio(0.52f);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        recyclerPlaces.setLayoutManager(new LinearLayoutManager(this));
        placeAdapter = new PlaceAdapter(new PlaceAdapter.Listener() {
            @Override
            public void onPlaceClick(MapPlaceItem item) {
                openPlaceDetail(item.placeId);
            }
        });
        recyclerPlaces.setAdapter(placeAdapter);

        recyclerPlaceScenes.setLayoutManager(new LinearLayoutManager(this));
        placeSceneAdapter = new PlaceSceneAdapter(new PlaceSceneAdapter.Listener() {
            @Override
            public void onToggleCheckIn(long sceneId) {
                boolean nowCheckedIn = repository.toggleCheckIn(sceneId);
                updateStats();
                bindSelectedPlace();
                if (nowCheckedIn) {
                    openPlaceRecommendations();
                }
            }

            @Override
            public void onToggleRoute(long sceneId) {
                repository.toggleSceneInRoute(sceneId);
                updateStats();
                bindSelectedPlace();
                renderMapMarkersForCurrentZoom();
            }

            @Override
            public void onOpenMovie(long movieId) {
                Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
                intent.putExtra("movie_id", movieId);
                startActivity(intent);
            }
        });
        recyclerPlaceScenes.setAdapter(placeSceneAdapter);

        btnSheetBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlaceList();
            }
        });

        btnOpenPlacePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPlaceId != -1L) {
                    openPlacePage(selectedPlaceId);
                }
            }
        });
        btnPlaceRecommendations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPlaceRecommendations();
            }
        });

        // Old bottom buttons logic removed

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupGenreChips();
        requestLocationIfNeeded();
        updateStats();
        applyFilters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        applyFilters();
    }

    @Override
    public void onBackPressed() {
        if (selectedPlaceId != -1L) {
            showPlaceList();
            return;
        }
        super.onBackPressed();
    }

    private void setupGenreChips() {
        final List<String> genres = new ArrayList<>();
        genres.add(getString(R.string.label_all_genres));
        genres.addAll(repository.getGenres());
        chipGroupGenre.removeAllViews();
        for (int i = 0; i < genres.size(); i++) {
            final String genre = genres.get(i);
            Chip chip = new Chip(this);
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chip.setChipBackgroundColorResource(R.color.surface);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        selectedGenre = genre;
                        applyFilters();
                    }
                }
            });
            chipGroupGenre.addView(chip);
        }
    }

    private void applyFilters() {
        currentPlaces = repository.getMapPlaces(selectedGenre);
        placeSceneCache.clear();
        renderMapMarkersForCurrentZoom();
        if (selectedPlaceId == -1L) {
            showPlaceList(currentPlaces);
        } else if (containsPlace(currentPlaces, selectedPlaceId)) {
            bindSelectedPlace();
        } else {
            showPlaceList(currentPlaces);
        }
    }

    private boolean containsPlace(List<MapPlaceItem> places, long placeId) {
        for (MapPlaceItem place : places) {
            if (place.placeId == placeId) {
                return true;
            }
        }
        return false;
    }

    private void updateStats() {
        int checkedIn = repository.getCheckedInCount();
        int totalScenes = repository.getTotalSceneCount();
        int progressPercent = totalScenes <= 0 ? 0 : Math.round((checkedIn * 100f) / totalScenes);

        if (progressCheckIn != null) {
            progressCheckIn.setMax(Math.max(totalScenes, 1));
            progressCheckIn.setProgress(Math.min(checkedIn, Math.max(totalScenes, 1)));
            applyProgressColor(progressPercent);
        }

        maybeShowAchievementUnlockDialog(checkedIn, totalScenes, progressPercent);

        String stats = checkedIn + "/" + totalScenes + " scenes checked in (" + progressPercent + "%) | "
                + repository.getDraftRouteStopCount() + " planned stops";
        tvStats.setText(stats);
    }

    private void applyProgressColor(int progressPercent) {
        int colorResId;
        if (progressPercent < 30) {
            colorResId = R.color.route_color;
        } else if (progressPercent <= 70) {
            colorResId = R.color.colorAccent;
        } else {
            colorResId = R.color.checked_in;
        }
        progressCheckIn.setProgressTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, colorResId)
        ));
    }

    private void maybeShowAchievementUnlockDialog(int checkedInScenes, int totalScenes, int progressPercent) {
        int checkedInMovies = repository.getCheckedInMovieCount();
        int totalMovies = Math.max(repository.getTotalMovieCount(), 1);
        int routeStops = repository.getDraftRouteStopCount();

        AchievementUnlock[] candidates = new AchievementUnlock[]{
                new AchievementUnlock(A_FIRST_STEP, checkedInScenes >= 1, getString(R.string.achievement_first_step_title), getString(R.string.achievement_first_step_desc), android.R.drawable.star_big_on),
                new AchievementUnlock(A_SCENE_ROOKIE, checkedInScenes >= 5, getString(R.string.achievement_scene_rookie_title), getString(R.string.achievement_scene_rookie_desc), android.R.drawable.star_big_on),
                new AchievementUnlock(A_SCENE_HUNTER, checkedInScenes >= 15, getString(R.string.achievement_scene_hunter_title), getString(R.string.achievement_scene_hunter_desc), android.R.drawable.star_big_on),
                new AchievementUnlock(A_HALF_MAP, progressPercent >= 50, getString(R.string.achievement_half_map_title), getString(R.string.achievement_half_map_desc), android.R.drawable.ic_menu_mapmode),
                new AchievementUnlock(A_FULL_MAP, progressPercent >= 100, getString(R.string.achievement_full_map_title), getString(R.string.achievement_full_map_desc), android.R.drawable.ic_menu_compass),
                new AchievementUnlock(A_MOVIE_COLLECTOR, checkedInMovies >= Math.min(totalMovies, 3), getString(R.string.achievement_movie_collector_title), getString(R.string.achievement_movie_collector_desc), android.R.drawable.ic_menu_myplaces),
                new AchievementUnlock(A_ROUTE_ROOKIE, routeStops >= 3, getString(R.string.achievement_route_rookie_title), getString(R.string.achievement_route_rookie_desc), android.R.drawable.ic_menu_directions),
                new AchievementUnlock(A_ROUTE_MASTER, routeStops >= 8, getString(R.string.achievement_route_master_title), getString(R.string.achievement_route_master_desc), android.R.drawable.ic_menu_directions)
        };

        for (AchievementUnlock candidate : candidates) {
            if (candidate.unlockedNow && !AchievementTracker.isUnlocked(this, candidate.id)) {
                AchievementTracker.markUnlocked(this, candidate.id);
                showAchievementUnlockDialog(candidate.title, candidate.description, candidate.iconResId);
                return;
            }
        }
    }

    private void showAchievementUnlockDialog(String title, String description, int iconResId) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_achievement_unlocked, null, false);
        View card = content.findViewById(R.id.layoutUnlockCard);
        ImageView icon = content.findViewById(R.id.ivUnlockIcon);
        TextView tvTitle = content.findViewById(R.id.tvUnlockTitle);
        TextView tvDesc = content.findViewById(R.id.tvUnlockDescription);
        icon.setImageResource(iconResId);
        tvTitle.setText(title);
        tvDesc.setText(description);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton(R.string.action_ok, null)
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.52f);
        }

        int riseDistance = dpToPx(18);
        card.setAlpha(0f);
        card.setTranslationY(riseDistance);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        ObjectAnimator pulseX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.7f, 1.18f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.7f, 1.18f, 1f);
        pulseX.setDuration(550);
        pulseY.setDuration(550);
        pulseX.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseY.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseX.start();
        pulseY.start();
    }

    private void showPlaceList() {
        clearActiveFoodNavigation(false);
        selectedPlaceId = -1L;
        showPlaceList(currentPlaces);
    }

    private void showPlaceList(List<MapPlaceItem> places) {
        selectedPlaceId = -1L;
        tvSheetTitle.setText(R.string.label_verified_places);
        tvSheetSubtitle.setText(buildPlaceListSubtitle(places));
        chipScrollView.setVisibility(View.VISIBLE);
        layoutStatsHeader.setVisibility(View.VISIBLE);
        btnSheetBack.setVisibility(View.GONE);
        layoutPlaceActions.setVisibility(View.GONE);
        recyclerPlaces.setVisibility(View.VISIBLE);
        recyclerPlaceScenes.setVisibility(View.GONE);
        tvEmptyState.setText(R.string.hint_no_places);
        tvEmptyState.setVisibility(places.isEmpty() ? View.VISIBLE : View.GONE);
        placeAdapter.submit(places);
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private String buildPlaceListSubtitle(List<MapPlaceItem> places) {
        int sceneTotal = 0;
        for (MapPlaceItem place : places) {
            sceneTotal += Math.max(place.sceneCount, 0);
        }
        return places.size() + " mapped places | " + sceneTotal + " mapped scenes | zoom in to reveal more posters";
    }

    private void openPlaceDetail(long placeId) {
        clearActiveFoodNavigation(false);
        selectedPlaceId = placeId;
        bindSelectedPlace();
    }

    private void openPlacePage(long placeId) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("place_id", placeId);
        startActivity(intent);
    }

    private void openPlaceRecommendations() {
        if (selectedPlaceId == -1L) {
            return;
        }
        final PlaceDetail detail = repository.getPlaceDetailForGenre(selectedPlaceId, selectedGenre);
        if (detail.place == null || detail.place.latitude == null || detail.place.longitude == null) {
            return;
        }
        NearbyRecommendationSheet.show(
                this,
                detail.place.nameEn == null || detail.place.nameEn.trim().isEmpty()
                        ? getString(R.string.label_verified_places)
                        : detail.place.nameEn,
                detail.place.latitude,
                detail.place.longitude,
                detail.place.districtEn,
                buildGenreHint(detail.scenes),
                false,
                new NearbyRecommendationSheet.NavigateListener() {
                    @Override
                    public void onNavigate(NearbyFoodItem item) {
                        navigateToRecommendedFood(detail, item);
                    }
                });
    }

    private void navigateToRecommendedFood(final PlaceDetail detail, final NearbyFoodItem item) {
        if (detail.place == null || detail.place.latitude == null || detail.place.longitude == null) {
            return;
        }
        final LatLng origin = new LatLng(detail.place.latitude, detail.place.longitude);
        final LatLng destination = new LatLng(item.latitude, item.longitude);
        clearActiveFoodNavigation(false);
        activeFoodOrigin = origin;
        activeFoodDestination = item;
        activeFoodPolylinePoints = new ArrayList<>();
        renderActiveFoodNavigation();
        focusActiveFoodNavigation();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<LatLng> polyline = fetchWalkingPolyline(origin, destination);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activeFoodDestination != item || activeFoodOrigin == null) {
                            return;
                        }
                        if (polyline != null && !polyline.isEmpty()) {
                            activeFoodPolylinePoints = polyline;
                        }
                        renderActiveFoodNavigation();
                        focusActiveFoodNavigation();
                        showActiveFoodSnackbar(item.name, estimateWalkMinutes(origin, destination));
                    }
                });
            }
        }).start();
    }

    private String buildGenreHint(List<PlaceSceneItem> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return selectedGenre;
        }
        Map<String, Integer> counts = new HashMap<>();
        String bestGenre = selectedGenre == null ? "All" : selectedGenre;
        int bestCount = -1;
        for (PlaceSceneItem item : scenes) {
            if (item.genreGroup == null || item.genreGroup.trim().isEmpty()) {
                continue;
            }
            int nextCount = counts.containsKey(item.genreGroup) ? counts.get(item.genreGroup) + 1 : 1;
            counts.put(item.genreGroup, nextCount);
            if (nextCount > bestCount) {
                bestGenre = item.genreGroup;
                bestCount = nextCount;
            }
        }
        return bestGenre;
    }

    private void bindSelectedPlace() {
        if (selectedPlaceId == -1L) {
            return;
        }
        PlaceDetail detail = repository.getPlaceDetailForGenre(selectedPlaceId, selectedGenre);
        if (detail.place == null) {
            showPlaceList();
            return;
        }
        List<PlaceSceneItem> scenes = detail.scenes == null ? new ArrayList<PlaceSceneItem>() : detail.scenes;
        tvSheetTitle.setText(detail.place.nameEn);
        tvSheetSubtitle.setText(buildPlaceSubtitle(detail, scenes));
        chipScrollView.setVisibility(View.GONE);
        layoutStatsHeader.setVisibility(View.GONE);
        btnSheetBack.setVisibility(View.VISIBLE);
        layoutPlaceActions.setVisibility(View.VISIBLE);
        btnPlaceRecommendations.setVisibility(
                detail.place.latitude != null && detail.place.longitude != null ? View.VISIBLE : View.GONE);
        recyclerPlaces.setVisibility(View.GONE);
        recyclerPlaceScenes.setVisibility(View.VISIBLE);
        tvEmptyState.setText(R.string.hint_no_place_scenes);
        tvEmptyState.setVisibility(scenes.isEmpty() ? View.VISIBLE : View.GONE);
        placeSceneAdapter.submit(scenes);
        focusPlace(detail);
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private String buildPlaceSubtitle(PlaceDetail detail, List<PlaceSceneItem> scenes) {
        Set<Long> movieIds = new HashSet<>();
        for (PlaceSceneItem item : scenes) {
            movieIds.add(item.movieId);
        }
        String district = detail.place.districtEn == null ? "District pending" : detail.place.districtEn;
        String address = detail.place.addressEn == null ? "Address pending" : detail.place.addressEn;
        String prefix = movieIds.size() + " movies | " + scenes.size() + " scenes";
        if (detail.place.nameZh != null && !detail.place.nameZh.trim().isEmpty()) {
            prefix = detail.place.nameZh + " | " + prefix;
        }
        return prefix + " | " + district + " | " + address;
    }

    private void focusPlace(PlaceDetail detail) {
        if (map == null || detail.place == null || detail.place.latitude == null || detail.place.longitude == null) {
            return;
        }
        LatLng position = new LatLng(detail.place.latitude, detail.place.longitude);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, Math.max(map.getCameraPosition().zoom, 15.5f)));
    }

    private void renderMapMarkersForCurrentZoom() {
        if (map == null) {
            return;
        }
        map.clear();
        markerMap.clear();
        clusterMarkerMap.clear();
        if (currentPlaces.isEmpty()) {
            return;
        }
        float zoom = map.getCameraPosition() == null ? 11.3f : map.getCameraPosition().zoom;
        if (zoom < ZOOM_CLUSTER_THRESHOLD) {
            addClusterMarkers(currentPlaces, zoom);
        } else if (zoom < ZOOM_EXPANDED_THRESHOLD) {
            addPlaceStackMarkers(currentPlaces);
        } else {
            addExpandedMoviePlaceMarkers(currentPlaces);
        }
        renderActiveFoodNavigation();
    }

    private void addClusterMarkers(List<MapPlaceItem> places, float zoom) {
        List<ClusterGroup> clusters = buildClusters(places, zoom);
        for (ClusterGroup cluster : clusters) {
            if (cluster.places.size() == 1) {
                addPlaceStackMarker(cluster.places.get(0));
                continue;
            }
            List<String> posters = sampleClusterPosters(cluster, 3);
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(cluster.getCenter())
                    .title(cluster.places.size() + " nearby places")
                    .snippet(cluster.movieCount + " movies | " + cluster.sceneCount + " scenes")
                    .icon(PosterUtils.getPosterStackMarkerIcon(this, posters, cluster.movieCount))
                    .anchor(0.5f, 1f));
            if (marker != null) {
                clusterMarkerMap.put(marker, cluster);
            }
        }
    }

    private List<ClusterGroup> buildClusters(List<MapPlaceItem> places, float zoom) {
        Projection projection = map.getProjection();
        int cellSize = zoom < 11.8f ? dpToPx(80) : dpToPx(64);
        Map<String, ClusterGroup> groups = new LinkedHashMap<>();
        for (MapPlaceItem place : places) {
            if (place.latitude == null || place.longitude == null) {
                continue;
            }
            Point point = projection.toScreenLocation(new LatLng(place.latitude, place.longitude));
            String key = Math.floorDiv(point.x, cellSize) + ":" + Math.floorDiv(point.y, cellSize);
            ClusterGroup group = groups.get(key);
            if (group == null) {
                group = new ClusterGroup();
                groups.put(key, group);
            }
            group.add(place);
        }
        return new ArrayList<>(groups.values());
    }

    private List<String> sampleClusterPosters(ClusterGroup cluster, int limit) {
        List<String> posters = new ArrayList<>();
        for (MapPlaceItem place : cluster.places) {
            for (String asset : getPosterAssetsForPlace(place.placeId, 1)) {
                if (!posters.contains(asset)) {
                    posters.add(asset);
                }
                if (posters.size() >= limit) {
                    return posters;
                }
            }
            if ((place.posterAsset != null && !place.posterAsset.trim().isEmpty()) && !posters.contains(place.posterAsset)) {
                posters.add(place.posterAsset);
                if (posters.size() >= limit) {
                    return posters;
                }
            }
        }
        return posters;
    }

    private void addPlaceStackMarkers(List<MapPlaceItem> places) {
        for (MapPlaceItem place : places) {
            addPlaceStackMarker(place);
        }
    }

    private void addPlaceStackMarker(MapPlaceItem place) {
        if (place.latitude == null || place.longitude == null) {
            return;
        }
        List<String> posters = getPosterAssetsForPlace(place.placeId, 3);
        if (posters.isEmpty() && place.posterAsset != null) {
            posters.add(place.posterAsset);
        }
        Marker marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(place.latitude, place.longitude))
                .title(place.nameEn)
                .snippet(place.movieCount + " movies | " + place.sceneCount + " scenes")
                .icon(PosterUtils.getPosterStackMarkerIcon(this, posters, place.movieCount))
                .anchor(0.5f, 1f)
                .zIndex(place.placeId == selectedPlaceId ? 2f : 1f));
        if (marker != null) {
            markerMap.put(marker, place);
        }
    }

    private void addExpandedMoviePlaceMarkers(List<MapPlaceItem> places) {
        for (MapPlaceItem place : places) {
            if (place.latitude == null || place.longitude == null) {
                continue;
            }
            List<PlaceSceneItem> scenes = getUniqueMovieScenesForPlace(place.placeId);
            if (scenes.isEmpty()) {
                addPlaceStackMarker(place);
                continue;
            }
            LatLng center = new LatLng(place.latitude, place.longitude);
            List<LatLng> positions = buildFanOutPositions(center, scenes.size());
            for (int i = 0; i < scenes.size(); i++) {
                PlaceSceneItem scene = scenes.get(i);
                LatLng markerPosition = positions.get(i);
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(markerPosition)
                        .title(scene.movieTitle)
                        .snippet(place.nameEn)
                        .icon(PosterUtils.getPosterMarkerIcon(this, scene.posterAsset))
                        .anchor(0.5f, 1f)
                        .zIndex(place.placeId == selectedPlaceId ? 2.5f : 1.5f));
                if (marker != null) {
                    markerMap.put(marker, place);
                }
            }
        }
    }

    private List<LatLng> buildFanOutPositions(LatLng center, int count) {
        List<LatLng> positions = new ArrayList<>();
        if (count <= 1 || map == null) {
            positions.add(center);
            return positions;
        }

        Projection projection = map.getProjection();
        Point centerPoint = projection.toScreenLocation(center);
        int placed = 0;
        int ring = 0;
        while (placed < count) {
            int ringSize = Math.min(count - placed, ring == 0 ? 6 : 6 + ring * 2);
            int radius = dpToPx(24 + ring * 14);
            for (int i = 0; i < ringSize; i++) {
                double angle = -Math.PI / 2d + ((2d * Math.PI) * i / ringSize);
                int x = (int) Math.round(centerPoint.x + Math.cos(angle) * radius);
                int y = (int) Math.round(centerPoint.y + Math.sin(angle) * radius);
                positions.add(projection.fromScreenLocation(new Point(x, y)));
            }
            placed += ringSize;
            ring += 1;
        }
        return positions;
    }

    private List<PlaceSceneItem> getPlaceScenesForMap(long placeId) {
        List<PlaceSceneItem> cached = placeSceneCache.get(placeId);
        if (cached != null) {
            return cached;
        }
        List<PlaceSceneItem> scenes = repository.getPlaceScenesForMap(placeId, selectedGenre);
        placeSceneCache.put(placeId, scenes);
        return scenes;
    }

    private List<PlaceSceneItem> getUniqueMovieScenesForPlace(long placeId) {
        LinkedHashMap<Long, PlaceSceneItem> unique = new LinkedHashMap<>();
        for (PlaceSceneItem item : getPlaceScenesForMap(placeId)) {
            if (!unique.containsKey(item.movieId)) {
                unique.put(item.movieId, item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private List<String> getPosterAssetsForPlace(long placeId, int limit) {
        List<String> posters = new ArrayList<>();
        for (PlaceSceneItem item : getUniqueMovieScenesForPlace(placeId)) {
            if (item.posterAsset == null || item.posterAsset.trim().isEmpty()) {
                continue;
            }
            if (!posters.contains(item.posterAsset)) {
                posters.add(item.posterAsset);
            }
            if (limit > 0 && posters.size() >= limit) {
                break;
            }
        }
        return posters;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(HK_CENTER, 11.3f));
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                ClusterGroup cluster = clusterMarkerMap.get(marker);
                if (cluster != null) {
                    zoomToCluster(cluster);
                    return true;
                }
                MapPlaceItem item = markerMap.get(marker);
                if (item != null) {
                    openPlaceDetail(item.placeId);
                    return true;
                }
                return false;
            }
        });
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                if (selectedPlaceId != -1L) {
                    showPlaceList();
                }
            }
        });
        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                renderMapMarkersForCurrentZoom();
            }
        });
        enableMyLocationIfGranted();
        applyFilters();
    }

    private void zoomToCluster(ClusterGroup cluster) {
        if (map == null || cluster.places.isEmpty()) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (MapPlaceItem place : cluster.places) {
            if (place.latitude == null || place.longitude == null) {
                continue;
            }
            builder.include(new LatLng(place.latitude, place.longitude));
        }
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), dpToPx(72)));
        } catch (IllegalStateException ignored) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.getCenter(), map.getCameraPosition().zoom + 1.4f));
        }
    }

    private void requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOC_PERMISSION_REQUEST);
        }
    }

    private void enableMyLocationIfGranted() {
        if (map == null) {
            return;
        }
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOC_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationIfGranted();
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private void renderActiveFoodNavigation() {
        if (map == null || activeFoodOrigin == null || activeFoodDestination == null) {
            return;
        }
        activeFoodMarker = map.addMarker(new MarkerOptions()
                .position(new LatLng(activeFoodDestination.latitude, activeFoodDestination.longitude))
                .title(activeFoodDestination.name)
                .snippet(activeFoodDestination.nameZh)
                .zIndex(3.5f));
        List<LatLng> points = new ArrayList<>();
        if (activeFoodPolylinePoints != null && !activeFoodPolylinePoints.isEmpty()) {
            points.addAll(activeFoodPolylinePoints);
        } else {
            points.add(activeFoodOrigin);
            points.add(new LatLng(activeFoodDestination.latitude, activeFoodDestination.longitude));
        }
        activeFoodPolyline = map.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(ContextCompat.getColor(this, R.color.colorAccent))
                .width(10f));
    }

    private void focusActiveFoodNavigation() {
        if (map == null || activeFoodOrigin == null || activeFoodDestination == null) {
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        bounds.include(activeFoodOrigin);
        bounds.include(new LatLng(activeFoodDestination.latitude, activeFoodDestination.longitude));
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), dpToPx(84)));
        } catch (IllegalStateException ignored) {
        }
    }

    private void showActiveFoodSnackbar(String foodName, int walkMinutes) {
        if (activeFoodSnackbar != null) {
            activeFoodSnackbar.dismiss();
        }
        activeFoodSnackbar = Snackbar.make(findViewById(R.id.map),
                getString(R.string.food_walk_estimate, foodName, walkMinutes),
                Snackbar.LENGTH_INDEFINITE);
        activeFoodSnackbar.setAction(R.string.food_clear_nav, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearActiveFoodNavigation(true);
            }
        });
        activeFoodSnackbar.show();
    }

    private void clearActiveFoodNavigation(boolean showFeedback) {
        if (activeFoodMarker != null) {
            activeFoodMarker.remove();
            activeFoodMarker = null;
        }
        if (activeFoodPolyline != null) {
            activeFoodPolyline.remove();
            activeFoodPolyline = null;
        }
        activeFoodOrigin = null;
        activeFoodDestination = null;
        activeFoodPolylinePoints = new ArrayList<>();
        if (activeFoodSnackbar != null) {
            activeFoodSnackbar.dismiss();
            activeFoodSnackbar = null;
        }
        if (showFeedback) {
            Snackbar.make(findViewById(R.id.map), R.string.food_nav_cleared, Snackbar.LENGTH_SHORT).show();
        }
    }

    private int estimateWalkMinutes(LatLng origin, LatLng destination) {
        double km = distanceKm(origin.latitude, origin.longitude, destination.latitude, destination.longitude);
        return Math.max(1, (int) Math.round((km / 4.5d) * 60d));
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return 6371d * c;
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
            String points = root.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points");
            return PolyUtil.decode(points);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static class ClusterGroup {
        final List<MapPlaceItem> places = new ArrayList<>();
        double latSum = 0d;
        double lngSum = 0d;
        int movieCount = 0;
        int sceneCount = 0;

        void add(MapPlaceItem place) {
            places.add(place);
            latSum += place.latitude == null ? 0d : place.latitude;
            lngSum += place.longitude == null ? 0d : place.longitude;
            movieCount += Math.max(place.movieCount, 1);
            sceneCount += Math.max(place.sceneCount, 1);
        }

        LatLng getCenter() {
            return new LatLng(latSum / places.size(), lngSum / places.size());
        }
    }

    private static class AchievementUnlock {
        final String id;
        final boolean unlockedNow;
        final String title;
        final String description;
        final int iconResId;

        AchievementUnlock(String id, boolean unlockedNow, String title, String description, int iconResId) {
            this.id = id;
            this.unlockedNow = unlockedNow;
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
        }
    }
}
