package com.polyu.hkfilmmap;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataProvider {
    private static final String PREFS = "hkfilm_prefs";
    private static final String KEY = "checked_ids";
    private static List<MovieLocation> cache = null;

    public static List<MovieLocation> getLocations(Context ctx) {
        if (cache == null) {
            try {
                BufferedReader r = new BufferedReader(
                        new InputStreamReader(ctx.getAssets().open("movie_locations.json")));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                Type t = new TypeToken<List<MovieLocation>>() {}.getType();
                cache = new Gson().fromJson(sb.toString(), t);
                Set<Integer> ids = getCheckedIds(ctx);
                for (MovieLocation loc : cache) {
                    if (ids.contains(loc.getId())) loc.setCheckedIn(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                cache = new ArrayList<>();
            }
        }
        return cache;
    }

    public static MovieLocation getById(Context ctx, int id) {
        for (MovieLocation loc : getLocations(ctx)) {
            if (loc.getId() == id) return loc;
        }
        return null;
    }

    public static List<String> getGenres(Context ctx) {
        List<String> g = new ArrayList<>();
        for (MovieLocation loc : getLocations(ctx)) {
            if (!g.contains(loc.getGenre())) g.add(loc.getGenre());
        }
        return g;
    }

    public static void checkIn(Context ctx, int id) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> s = new HashSet<>(p.getStringSet(KEY, new HashSet<>()));
        s.add(String.valueOf(id));
        p.edit().putStringSet(KEY, s).apply();
        if (cache != null) for (MovieLocation loc : cache) if (loc.getId() == id) loc.setCheckedIn(true);
    }

    public static void uncheckIn(Context ctx, int id) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> s = new HashSet<>(p.getStringSet(KEY, new HashSet<>()));
        s.remove(String.valueOf(id));
        p.edit().putStringSet(KEY, s).apply();
        if (cache != null) for (MovieLocation loc : cache) if (loc.getId() == id) loc.setCheckedIn(false);
    }

    public static int getCheckedCount(Context ctx) { return getCheckedIds(ctx).size(); }

    private static Set<Integer> getCheckedIds(Context ctx) {
        Set<String> ss = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY, new HashSet<>());
        Set<Integer> r = new HashSet<>();
        for (String s : ss) { try { r.add(Integer.parseInt(s)); } catch (Exception ignored) {} }
        return r;
    }
}
