package com.polyu.hkfilmmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.AppRepository;
import com.polyu.hkfilmmap.data.MovieDetail;

public class MovieDetailActivity extends AppCompatActivity {
    private AppRepository repository;
    private long movieId;
    private MovieSceneAdapter adapter;
    private ImageView poster;
    private TextView title;
    private TextView titleZh;
    private TextView meta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        movieId = getIntent().getLongExtra("movie_id", -1L);
        if (movieId == -1L) {
            finish();
            return;
        }

        repository = AppRepository.getInstance(this);
        poster = findViewById(R.id.ivMoviePoster);
        title = findViewById(R.id.tvMovieTitle);
        titleZh = findViewById(R.id.tvMovieTitleZh);
        meta = findViewById(R.id.tvMovieMeta);

        RecyclerView recyclerView = findViewById(R.id.recyclerMovieScenes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MovieSceneAdapter(new MovieSceneAdapter.Listener() {
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
            public void onOpenPlace(long placeId) {
                Intent intent = new Intent(MovieDetailActivity.this, DetailActivity.class);
                intent.putExtra("place_id", placeId);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnMovieDetailRoute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MovieDetailActivity.this, RoutePlannerActivity.class));
            }
        });

        findViewById(R.id.btnBackMovieCatalog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_movies);
        bottomNav.setOnItemSelectedListener(new com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_explore) {
                    Intent intent = new Intent(MovieDetailActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_route) {
                    Intent intent = new Intent(MovieDetailActivity.this, RoutePlannerActivity.class);
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

        bindData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindData();
    }

    private void bindData() {
        MovieDetail detail = repository.getMovieDetail(movieId);
        if (detail.movie == null) {
            finish();
            return;
        }
        poster.setImageResource(PosterUtils.getPosterRes(this, detail.movie.posterAsset));
        title.setText(detail.movie.titleEn);
        titleZh.setVisibility(detail.movie.titleZh == null || detail.movie.titleZh.trim().isEmpty() ? View.GONE : View.VISIBLE);
        titleZh.setText(detail.movie.titleZh);
        String year = detail.movie.year == null ? "Year TBD" : String.valueOf(detail.movie.year);
        String director = detail.movie.director == null ? "Director TBD" : detail.movie.director;
        meta.setText(year + " | " + director + " | " + detail.movie.genreGroup + " | " + detail.scenes.size() + " scenes");
        adapter.submit(detail.scenes);
    }
}

