package com.polyu.hkfilmmap;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.polyu.hkfilmmap.data.AppRepository;

import java.util.ArrayList;
import java.util.List;

public class AchievementActivity extends AppCompatActivity {
    private static final String A_FIRST_STEP = "first_step";
    private static final String A_SCENE_ROOKIE = "scene_rookie";
    private static final String A_SCENE_HUNTER = "scene_hunter";
    private static final String A_HALF_MAP = "half_map";
    private static final String A_FULL_MAP = "full_map";
    private static final String A_MOVIE_COLLECTOR = "movie_collector";
    private static final String A_ROUTE_ROOKIE = "route_rookie";
    private static final String A_ROUTE_MASTER = "route_master";

    private AppRepository repository;
    private AchievementAdapter adapter;
    private TextView tvAchievementSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMain();
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbarAchievement);
        toolbar.setNavigationOnClickListener(v -> navigateToMain());

        repository = AppRepository.getInstance(this);
        tvAchievementSummary = findViewById(R.id.tvAchievementSummary);
        RecyclerView recyclerView = findViewById(R.id.recyclerAchievements);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AchievementAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindAchievements();
    }

    private void bindAchievements() {
        int checkedInScenes = repository.getCheckedInCount();
        int totalScenes = Math.max(repository.getTotalSceneCount(), 1);
        int checkedInMovies = repository.getCheckedInMovieCount();
        int totalMovies = Math.max(repository.getTotalMovieCount(), 1);
        int routeStops = repository.getDraftRouteStopCount();
        int progressPercent = Math.round((checkedInScenes * 100f) / totalScenes);

        List<AchievementItem> items = new ArrayList<>();
        items.add(new AchievementItem(
                A_FIRST_STEP,
                getString(R.string.achievement_first_step_title),
                getString(R.string.achievement_first_step_desc),
                android.R.drawable.star_big_on,
                checkedInScenes,
                1,
                AchievementTracker.isUnlocked(this, A_FIRST_STEP)
        ));
        items.add(new AchievementItem(
                A_SCENE_ROOKIE,
                getString(R.string.achievement_scene_rookie_title),
                getString(R.string.achievement_scene_rookie_desc),
                android.R.drawable.star_big_on,
                checkedInScenes,
                5,
                AchievementTracker.isUnlocked(this, A_SCENE_ROOKIE)
        ));
        items.add(new AchievementItem(
                A_SCENE_HUNTER,
                getString(R.string.achievement_scene_hunter_title),
                getString(R.string.achievement_scene_hunter_desc),
                android.R.drawable.star_big_on,
                checkedInScenes,
                15,
                AchievementTracker.isUnlocked(this, A_SCENE_HUNTER)
        ));
        items.add(new AchievementItem(
                A_HALF_MAP,
                getString(R.string.achievement_half_map_title),
                getString(R.string.achievement_half_map_desc),
                android.R.drawable.ic_menu_mapmode,
                progressPercent,
                50,
                AchievementTracker.isUnlocked(this, A_HALF_MAP)
        ));
        items.add(new AchievementItem(
                A_FULL_MAP,
                getString(R.string.achievement_full_map_title),
                getString(R.string.achievement_full_map_desc),
                android.R.drawable.ic_menu_compass,
                progressPercent,
                100,
                AchievementTracker.isUnlocked(this, A_FULL_MAP)
        ));
        items.add(new AchievementItem(
                A_MOVIE_COLLECTOR,
                getString(R.string.achievement_movie_collector_title),
                getString(R.string.achievement_movie_collector_desc),
                android.R.drawable.ic_menu_myplaces,
                checkedInMovies,
                Math.min(totalMovies, 3),
                AchievementTracker.isUnlocked(this, A_MOVIE_COLLECTOR)
        ));
        items.add(new AchievementItem(
                A_ROUTE_ROOKIE,
                getString(R.string.achievement_route_rookie_title),
                getString(R.string.achievement_route_rookie_desc),
                android.R.drawable.ic_menu_directions,
                routeStops,
                3,
                AchievementTracker.isUnlocked(this, A_ROUTE_ROOKIE)
        ));
        items.add(new AchievementItem(
                A_ROUTE_MASTER,
                getString(R.string.achievement_route_master_title),
                getString(R.string.achievement_route_master_desc),
                android.R.drawable.ic_menu_directions,
                routeStops,
                8,
                AchievementTracker.isUnlocked(this, A_ROUTE_MASTER)
        ));

        int unlockedCount = 0;
        for (AchievementItem item : items) {
            if (item.unlocked) {
                unlockedCount += 1;
            }
        }
        tvAchievementSummary.setText(unlockedCount + "/" + items.size() + " achievements unlocked");
        adapter.submit(items);
    }

    private void navigateToMain() {
        NavigateToExplore.openMapHomeAndFinish(this);
    }
}
