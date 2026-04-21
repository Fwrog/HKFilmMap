package com.polyu.hkfilmmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationBarView;
import com.polyu.hkfilmmap.data.AppRepository;
import com.polyu.hkfilmmap.data.MovieListItem;

import java.util.ArrayList;
import java.util.List;

public class MovieCatalogActivity extends AppCompatActivity {
    private AppRepository repository;
    private MovieCatalogAdapter adapter;
    private TextView emptyView;
    private String selectedGenre = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_catalog);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavigateToExplore.openMapHomeAndFinish(MovieCatalogActivity.this);
            }
        });

        repository = AppRepository.getInstance(this);
        emptyView = findViewById(R.id.tvMovieEmpty);

        RecyclerView recyclerView = findViewById(R.id.recyclerMovies);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MovieCatalogAdapter(new MovieCatalogAdapter.Listener() {
            @Override
            public void onMovieClick(long movieId) {
                Intent intent = new Intent(MovieCatalogActivity.this, MovieDetailActivity.class);
                intent.putExtra("movie_id", movieId);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBackMovies).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigateToExplore.openMapHomeAndFinish(MovieCatalogActivity.this);
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_movies);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_explore) {
                    Intent intent = new Intent(MovieCatalogActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_route) {
                    Intent intent = new Intent(MovieCatalogActivity.this, RoutePlannerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_movies) {
                    return true;
                }
                return false;
            }
        });

        setupGenreChips();
        applyFilter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFilter();
    }

    private void setupGenreChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupMovieGenre);
        final List<String> genres = new ArrayList<>();
        genres.add(getString(R.string.label_all_genres));
        genres.addAll(repository.getGenres());
        chipGroup.removeAllViews();
        for (int i = 0; i < genres.size(); i++) {
            final String genre = genres.get(i);
            Chip chip = new Chip(this);
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chip.setChipBackgroundColorResource(R.color.surface);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        selectedGenre = genre;
                        applyFilter();
                    }
                }
            });
            chipGroup.addView(chip);
        }
    }

    private void applyFilter() {
        List<MovieListItem> items = repository.getMovieCatalog(selectedGenre);
        adapter.submit(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
