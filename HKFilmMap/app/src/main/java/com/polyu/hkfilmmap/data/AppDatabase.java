package com.polyu.hkfilmmap.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                MovieEntity.class,
                PlaceEntity.class,
                SceneEntity.class,
                UserCheckInEntity.class,
                RoutePlanEntity.class,
                RoutePlanStopEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract BrowseDao browseDao();
    public abstract CheckInDao checkInDao();
    public abstract RoutePlanDao routePlanDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "hkfilmmap.db")
                            .createFromAsset("hkfilmmap_seed.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
