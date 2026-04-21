package com.polyu.hkfilmmap;

import android.content.Context;

import com.polyu.hkfilmmap.data.NearbyFoodItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NearbyFoodHelper {

    private static final double[] RECOMMENDATION_RADII_KM = new double[]{1.2d, 2.0d, 3.2d, 4.5d};
    private static List<NearbyFoodItem> cachedItems;

    public static synchronized List<NearbyFoodItem> loadAll(Context context) {
        if (cachedItems != null) {
            return cachedItems;
        }
        List<NearbyFoodItem> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("nearby_food.json"), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                NearbyFoodItem item = new NearbyFoodItem();
                item.name = cleanDisplayText(obj.optString("name", ""));
                item.nameZh = cleanDisplayText(obj.optString("nameZh", ""));
                item.category = cleanDisplayText(obj.optString("category", "food"));
                item.district = cleanDisplayText(obj.optString("district", ""));
                item.latitude = obj.optDouble("latitude", 0d);
                item.longitude = obj.optDouble("longitude", 0d);
                item.description = cleanDisplayText(obj.optString("description", ""));
                item.priceRange = cleanDisplayText(obj.optString("priceRange", "$"));
                if (!item.name.isEmpty() && item.latitude != 0d && item.longitude != 0d) {
                    result.add(item);
                }
            }
        } catch (Exception ignored) {
        }
        cachedItems = result;
        return cachedItems;
    }

    /**
     * Returns food items within {@code radiusKm} of the given point, sorted nearest-first.
     * If fewer than 3 results are found by distance, falls back to district name matching
     * using the provided {@code districtHint}.
     */
    public static List<NearbyFoodItem> findNearby(Context context, double lat, double lng,
                                                  double radiusKm, String districtHint) {
        List<NearbyFoodItem> all = loadAll(context);
        List<NearbyFoodItem> results = new ArrayList<>();
        for (NearbyFoodItem source : all) {
            double distance = haversineKm(lat, lng, source.latitude, source.longitude);
            if (distance <= radiusKm) {
                NearbyFoodItem item = copyItem(source);
                item.distanceKm = distance;
                item.walkMinutes = estimateWalkMinutes(distance);
                item.districtMatch = isDistrictMatch(item.district, districtHint) ? 1 : 0;
                results.add(item);
            }
        }

        if (results.size() < 3 && districtHint != null && !districtHint.trim().isEmpty()) {
            for (NearbyFoodItem source : all) {
                if (!isDistrictMatch(source.district, districtHint) || containsByName(results, source.name)) {
                    continue;
                }
                NearbyFoodItem item = copyItem(source);
                item.distanceKm = haversineKm(lat, lng, source.latitude, source.longitude);
                item.walkMinutes = estimateWalkMinutes(item.distanceKm);
                item.districtMatch = 1;
                results.add(item);
            }
        }

        Collections.sort(results, new Comparator<NearbyFoodItem>() {
            @Override
            public int compare(NearbyFoodItem a, NearbyFoodItem b) {
                int byDistance = Double.compare(a.distanceKm, b.distanceKm);
                if (byDistance != 0) {
                    return byDistance;
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return results;
    }

    public static List<NearbyFoodItem> findRecommended(Context context, double lat, double lng,
                                                       String districtHint, String genreHint, int limit) {
        int safeLimit = Math.max(3, Math.min(limit, 12));
        List<NearbyFoodItem> all = loadAll(context);
        List<NearbyFoodItem> candidates = collectRecommendationCandidates(all, lat, lng, districtHint, safeLimit);
        if (candidates.isEmpty()) {
            return candidates;
        }

        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String normalizedGenre = genreHint == null ? "" : genreHint.trim();
        for (NearbyFoodItem item : candidates) {
            item.walkMinutes = estimateWalkMinutes(item.distanceKm);
            item.districtMatch = isDistrictMatch(item.district, districtHint) ? 1 : 0;
            item.recommendationScore = scoreRecommendation(item, normalizedGenre, hourOfDay);
            item.recommendationReason = buildRecommendationReason(item, normalizedGenre, hourOfDay);
        }

        Collections.sort(candidates, new Comparator<NearbyFoodItem>() {
            @Override
            public int compare(NearbyFoodItem a, NearbyFoodItem b) {
                int byScore = Double.compare(b.recommendationScore, a.recommendationScore);
                if (byScore != 0) {
                    return byScore;
                }
                int byDistrict = Integer.compare(b.districtMatch, a.districtMatch);
                if (byDistrict != 0) {
                    return byDistrict;
                }
                return Double.compare(a.distanceKm, b.distanceKm);
            }
        });

        List<NearbyFoodItem> ranked = diversifyRecommendations(candidates, safeLimit);
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).rank = i + 1;
        }
        return ranked;
    }

    private static List<NearbyFoodItem> collectRecommendationCandidates(List<NearbyFoodItem> all,
                                                                        double lat,
                                                                        double lng,
                                                                        String districtHint,
                                                                        int limit) {
        List<NearbyFoodItem> candidates = new ArrayList<>();
        for (double radiusKm : RECOMMENDATION_RADII_KM) {
            for (NearbyFoodItem source : all) {
                if (containsByName(candidates, source.name)) {
                    continue;
                }
                double distance = haversineKm(lat, lng, source.latitude, source.longitude);
                if (distance > radiusKm) {
                    continue;
                }
                NearbyFoodItem item = copyItem(source);
                item.distanceKm = distance;
                candidates.add(item);
            }
            if (candidates.size() >= limit) {
                break;
            }
        }

        if (candidates.size() < limit && districtHint != null && !districtHint.trim().isEmpty()) {
            for (NearbyFoodItem source : all) {
                if (!isDistrictMatch(source.district, districtHint) || containsByName(candidates, source.name)) {
                    continue;
                }
                NearbyFoodItem item = copyItem(source);
                item.distanceKm = haversineKm(lat, lng, source.latitude, source.longitude);
                candidates.add(item);
                if (candidates.size() >= limit) {
                    break;
                }
            }
        }

        if (candidates.size() < limit) {
            List<NearbyFoodItem> fallback = new ArrayList<>();
            for (NearbyFoodItem source : all) {
                if (containsByName(candidates, source.name)) {
                    continue;
                }
                NearbyFoodItem item = copyItem(source);
                item.distanceKm = haversineKm(lat, lng, source.latitude, source.longitude);
                fallback.add(item);
            }
            Collections.sort(fallback, new Comparator<NearbyFoodItem>() {
                @Override
                public int compare(NearbyFoodItem a, NearbyFoodItem b) {
                    return Double.compare(a.distanceKm, b.distanceKm);
                }
            });
            for (NearbyFoodItem item : fallback) {
                candidates.add(item);
                if (candidates.size() >= limit) {
                    break;
                }
            }
        }

        return candidates;
    }

    private static List<NearbyFoodItem> diversifyRecommendations(List<NearbyFoodItem> sortedCandidates, int limit) {
        List<NearbyFoodItem> remaining = new ArrayList<>(sortedCandidates);
        List<NearbyFoodItem> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < limit) {
            NearbyFoodItem best = null;
            double bestAdjustedScore = -Double.MAX_VALUE;
            for (NearbyFoodItem item : remaining) {
                double adjusted = item.recommendationScore - categoryPenalty(selected, item.category);
                if (best == null || adjusted > bestAdjustedScore
                        || (adjusted == bestAdjustedScore && item.distanceKm < best.distanceKm)) {
                    best = item;
                    bestAdjustedScore = adjusted;
                }
            }
            if (best == null) {
                break;
            }
            selected.add(best);
            remaining.remove(best);
        }
        return selected;
    }

    private static double categoryPenalty(List<NearbyFoodItem> selected, String category) {
        if (selected.isEmpty()) {
            return 0d;
        }
        int duplicateCount = 0;
        for (NearbyFoodItem item : selected) {
            if (sameText(item.category, category)) {
                duplicateCount++;
            }
        }
        double penalty = duplicateCount * 4d;
        NearbyFoodItem latest = selected.get(selected.size() - 1);
        if (sameText(latest.category, category)) {
            penalty += 7d;
        }
        return penalty;
    }

    private static double scoreRecommendation(NearbyFoodItem item, String genreHint, int hourOfDay) {
        return distanceScore(item.distanceKm)
                + (item.districtMatch == 1 ? 16d : 0d)
                + timeOfDayCategoryBonus(item.category, hourOfDay)
                + genreMoodBonus(item.category, genreHint)
                + priceBonus(item.priceRange)
                + editorialBoost(item.description);
    }

    private static double distanceScore(double distanceKm) {
        if (distanceKm <= 0.35d) {
            return 42d;
        }
        if (distanceKm <= 0.8d) {
            return 35d;
        }
        if (distanceKm <= 1.2d) {
            return 28d;
        }
        if (distanceKm <= 2.0d) {
            return 20d;
        }
        if (distanceKm <= 3.2d) {
            return 12d;
        }
        return 5d;
    }

    private static double timeOfDayCategoryBonus(String category, int hourOfDay) {
        boolean morning = hourOfDay >= 6 && hourOfDay < 11;
        boolean lunch = hourOfDay >= 11 && hourOfDay < 14;
        boolean afternoon = hourOfDay >= 14 && hourOfDay < 18;
        boolean evening = hourOfDay >= 18 && hourOfDay < 22;
        if (morning) {
            if ("coffee".equals(category)) {
                return 13d;
            }
            if ("food".equals(category)) {
                return 9d;
            }
            return 4d;
        }
        if (lunch) {
            if ("food".equals(category)) {
                return 15d;
            }
            if ("coffee".equals(category)) {
                return 6d;
            }
            return 4d;
        }
        if (afternoon) {
            if ("coffee".equals(category)) {
                return 11d;
            }
            if ("dessert".equals(category)) {
                return 9d;
            }
            return 7d;
        }
        if (evening) {
            if ("food".equals(category)) {
                return 13d;
            }
            if ("dessert".equals(category)) {
                return 7d;
            }
            return 6d;
        }
        if ("food".equals(category)) {
            return 10d;
        }
        if ("coffee".equals(category)) {
            return 8d;
        }
        return 5d;
    }

    private static double genreMoodBonus(String category, String genreHint) {
        String genre = genreHint == null ? "" : genreHint.toLowerCase(Locale.US);
        if (genre.contains("romance") || genre.contains("drama")) {
            if ("coffee".equals(category)) {
                return 7d;
            }
            if ("dessert".equals(category)) {
                return 6d;
            }
        }
        if (genre.contains("crime") || genre.contains("action") || genre.contains("thriller")) {
            if ("food".equals(category)) {
                return 7d;
            }
            if ("coffee".equals(category)) {
                return 2d;
            }
        }
        if (genre.contains("comedy") || genre.contains("family")) {
            if ("dessert".equals(category)) {
                return 6d;
            }
            if ("food".equals(category)) {
                return 4d;
            }
        }
        if (genre.contains("arthouse") || genre.contains("indie")) {
            if ("coffee".equals(category)) {
                return 5d;
            }
        }
        return 0d;
    }

    private static double priceBonus(String priceRange) {
        if ("$".equals(priceRange)) {
            return 8d;
        }
        if ("$$".equals(priceRange)) {
            return 5d;
        }
        if ("$$$".equals(priceRange)) {
            return 2d;
        }
        return 3d;
    }

    private static double editorialBoost(String description) {
        String lower = description == null ? "" : description.toLowerCase(Locale.US);
        double boost = 0d;
        if (lower.contains("legendary") || lower.contains("iconic")) {
            boost += 8d;
        }
        if (lower.contains("classic") || lower.contains("historic")) {
            boost += 6d;
        }
        if (lower.contains("authentic") || lower.contains("award-winning") || lower.contains("famous")) {
            boost += 5d;
        }
        if (lower.contains("waterfront") || lower.contains("harbour") || lower.contains("sky-high")) {
            boost += 3d;
        }
        return boost;
    }

    private static String buildRecommendationReason(NearbyFoodItem item, String genreHint, int hourOfDay) {
        List<String> reasons = new ArrayList<>();
        String categoryMoment = categoryMomentLabel(item.category, hourOfDay);
        if (!categoryMoment.isEmpty()) {
            reasons.add(categoryMoment);
        }
        String genreMood = genreMoodLabel(item.category, genreHint);
        if (!genreMood.isEmpty()) {
            reasons.add(genreMood);
        }
        String editorial = editorialLabel(item.description);
        if (!editorial.isEmpty()) {
            reasons.add(editorial);
        }
        if (item.districtMatch == 1) {
            reasons.add("same-district option");
        }
        if (reasons.isEmpty()) {
            return "close and easy to add to this stop";
        }
        if (reasons.size() > 3) {
            reasons = reasons.subList(0, 3);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(reasons.get(i));
        }
        return builder.toString();
    }

    private static String categoryMomentLabel(String category, int hourOfDay) {
        boolean morning = hourOfDay >= 6 && hourOfDay < 11;
        boolean lunch = hourOfDay >= 11 && hourOfDay < 14;
        boolean afternoon = hourOfDay >= 14 && hourOfDay < 18;
        boolean evening = hourOfDay >= 18 && hourOfDay < 22;
        if ("coffee".equals(category)) {
            if (morning) {
                return "strong morning coffee pick";
            }
            if (afternoon) {
                return "good afternoon recharge";
            }
            return "easy coffee stop";
        }
        if ("dessert".equals(category)) {
            if (afternoon || evening) {
                return "sweet break between stops";
            }
            return "light dessert detour";
        }
        if (lunch) {
            return "lunch-friendly local meal";
        }
        if (evening) {
            return "good dinner stop";
        }
        return "solid local bite";
    }

    private static String genreMoodLabel(String category, String genreHint) {
        String genre = genreHint == null ? "" : genreHint.toLowerCase(Locale.US);
        if ((genre.contains("romance") || genre.contains("drama"))
                && ("coffee".equals(category) || "dessert".equals(category))) {
            return "fits a softer film-stop mood";
        }
        if ((genre.contains("crime") || genre.contains("action") || genre.contains("thriller"))
                && "food".equals(category)) {
            return "works well as a fast route refuel";
        }
        if ((genre.contains("comedy") || genre.contains("family")) && "dessert".equals(category)) {
            return "playful dessert stop";
        }
        return "";
    }

    private static String editorialLabel(String description) {
        String lower = description == null ? "" : description.toLowerCase(Locale.US);
        if (lower.contains("legendary") || lower.contains("iconic")) {
            return "classic Hong Kong staple";
        }
        if (lower.contains("historic")) {
            return "historic local favourite";
        }
        if (lower.contains("award-winning")) {
            return "award-winning spot";
        }
        if (lower.contains("waterfront") || lower.contains("harbour")) {
            return "extra harbour atmosphere";
        }
        return "";
    }

    private static int estimateWalkMinutes(double distanceKm) {
        return Math.max(1, (int) Math.round((distanceKm / 4.5d) * 60d));
    }

    private static NearbyFoodItem copyItem(NearbyFoodItem source) {
        NearbyFoodItem item = new NearbyFoodItem();
        item.name = source.name;
        item.nameZh = source.nameZh;
        item.category = source.category;
        item.district = source.district;
        item.latitude = source.latitude;
        item.longitude = source.longitude;
        item.description = source.description;
        item.priceRange = source.priceRange;
        return item;
    }

    private static boolean containsByName(List<NearbyFoodItem> items, String name) {
        for (NearbyFoodItem item : items) {
            if (sameText(item.name, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDistrictMatch(String district, String districtHint) {
        if (district == null || districtHint == null) {
            return false;
        }
        return district.trim().equalsIgnoreCase(districtHint.trim());
    }

    private static boolean sameText(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static String cleanDisplayText(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replace('\uFFFD', ' ').trim();
        if (cleaned.contains("?") && !cleaned.contains("%")) {
            return "";
        }
        return cleaned;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return 6371d * c;
    }
}
