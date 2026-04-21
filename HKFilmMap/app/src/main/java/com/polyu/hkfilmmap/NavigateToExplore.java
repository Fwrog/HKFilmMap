package com.polyu.hkfilmmap;

import android.app.Activity;
import android.content.Intent;

/**
 * Bottom navigation currently {@code finish()}es {@link MainActivity} when opening other tabs,
 * so those tab screens can become the task root — pressing Back then exits the app.
 * This helper restores the map (Explore) as the home screen instead.
 */
public final class NavigateToExplore {

    private NavigateToExplore() {
    }

    /**
     * Opens the map home ({@link MainActivity}) and finishes the current activity.
     */
    public static void openMapHomeAndFinish(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
