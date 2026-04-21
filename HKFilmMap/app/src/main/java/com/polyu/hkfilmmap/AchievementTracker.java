package com.polyu.hkfilmmap;

import android.content.Context;
import android.content.SharedPreferences;

public final class AchievementTracker {
    private static final String PREFS_NAME = "achievements_prefs";

    private AchievementTracker() {
    }

    public static boolean isUnlocked(Context context, String achievementId) {
        return prefs(context).getBoolean(achievementId, false);
    }

    public static void markUnlocked(Context context, String achievementId) {
        prefs(context).edit().putBoolean(achievementId, true).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
