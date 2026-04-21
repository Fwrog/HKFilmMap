package com.polyu.hkfilmmap;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the email used for {@link com.google.firebase.auth.FirebaseAuth#sendSignInLinkToEmail}
 * so {@link com.google.firebase.auth.FirebaseAuth#signInWithEmailLink} can use the same address.
 */
final class AuthEmailLinkStore {
    private static final String PREFS_NAME = "auth_email_link";
    private static final String KEY_EMAIL = "pending_email";

    private AuthEmailLinkStore() {
    }

    static void saveEmail(Context context, String email) {
        prefs(context).edit().putString(KEY_EMAIL, email).apply();
    }

    static String getEmail(Context context) {
        return prefs(context).getString(KEY_EMAIL, "");
    }

    static void clear(Context context) {
        prefs(context).edit().remove(KEY_EMAIL).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
