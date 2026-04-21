package com.polyu.hkfilmmap;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Transparent {@code statusBarColor} in the app theme lets the window draw under system bars.
 * This helper disables framework "fits system windows" and applies {@link WindowInsetsCompat}
 * padding so headers and lists clear the status bar, cutout, and navigation bar.
 */
public final class SystemBarInsetsHelper {

    private SystemBarInsetsHelper() {
    }

    /**
     * @param rootViewId id on the activity content root (e.g. {@code R.id.contentRoot})
     */
    public static void applyContentInsets(Activity activity, int rootViewId) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        View root = activity.findViewById(rootViewId);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets cut = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    Math.max(sys.left, cut.left),
                    Math.max(sys.top, cut.top),
                    Math.max(sys.right, cut.right),
                    Math.max(sys.bottom, cut.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
