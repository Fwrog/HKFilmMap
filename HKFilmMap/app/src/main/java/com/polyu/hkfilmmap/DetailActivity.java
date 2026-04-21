package com.polyu.hkfilmmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.polyu.hkfilmmap.data.AppRepository;
import com.polyu.hkfilmmap.data.PlaceDetail;

public class DetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private AppRepository repository;
    private long placeId;
    private PlaceSceneAdapter adapter;
    private GoogleMap map;
    private TextView tvPlaceName;
    private TextView tvPlaceNameZh;
    private TextView tvPlaceMeta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        placeId = getIntent().getLongExtra("place_id", -1L);
        if (placeId == -1L) {
            finish();
            return;
        }

        repository = AppRepository.getInstance(this);
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvPlaceNameZh = findViewById(R.id.tvPlaceNameZh);
        tvPlaceMeta = findViewById(R.id.tvPlaceMeta);

        RecyclerView recyclerView = findViewById(R.id.recyclerPlaceScenes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceSceneAdapter(new PlaceSceneAdapter.Listener() {
            @Override
            public void onToggleCheckIn(long sceneId) {
                repository.toggleCheckIn(sceneId);
                bindData();
            }

            @Override
            public void onToggleRoute(long sceneId) {
                repository.toggleSceneInRoute(sceneId);
                bindData();
            }

            @Override
            public void onOpenMovie(long movieId) {
                Intent intent = new Intent(DetailActivity.this, MovieDetailActivity.class);
                intent.putExtra("movie_id", movieId);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnOpenRoutePlanner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DetailActivity.this, RoutePlannerActivity.class));
            }
        });

        findViewById(R.id.btnBackToMap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_explore);
        bottomNav.setOnItemSelectedListener(new com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_explore) {
                    Intent intent = new Intent(DetailActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_route) {
                    Intent intent = new Intent(DetailActivity.this, RoutePlannerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_movies) {
                    Intent intent = new Intent(DetailActivity.this, MovieCatalogActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDetail);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        bindData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindData();
    }

    private void bindData() {
        PlaceDetail detail = repository.getPlaceDetail(placeId);
        if (detail.place == null) {
            finish();
            return;
        }
        tvPlaceName.setText(detail.place.nameEn);
        tvPlaceNameZh.setVisibility(detail.place.nameZh == null || detail.place.nameZh.trim().isEmpty() ? View.GONE : View.VISIBLE);
        tvPlaceNameZh.setText(detail.place.nameZh);
        String meta = (detail.place.districtEn == null ? "District pending" : detail.place.districtEn)
                + " | "
                + (detail.place.addressEn == null ? "Address pending" : detail.place.addressEn)
                + " | "
                + detail.scenes.size() + " scenes";
        tvPlaceMeta.setText(meta);
        adapter.submit(detail.scenes);
        renderPlace(detail);
    }

    private void renderPlace(PlaceDetail detail) {
        if (map == null || detail.place == null || detail.place.latitude == null || detail.place.longitude == null) {
            return;
        }
        String posterAsset = detail.scenes != null && !detail.scenes.isEmpty() ? detail.scenes.get(0).posterAsset : null;
        LatLng position = new LatLng(detail.place.latitude, detail.place.longitude);
        map.clear();
        map.addMarker(new MarkerOptions()
                .position(position)
                .title(detail.place.nameEn)
                .icon(PosterUtils.getPosterMarkerIcon(this, posterAsset))
                .anchor(0.5f, 1f));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15.5f));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        bindData();
    }
}
