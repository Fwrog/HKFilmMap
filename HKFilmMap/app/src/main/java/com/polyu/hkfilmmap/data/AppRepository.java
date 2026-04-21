package com.polyu.hkfilmmap.data;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class AppRepository {
    private static final String DRAFT_PLAN_NAME = "My Route";
    private static volatile AppRepository instance;

    private final BrowseDao browseDao;
    private final CheckInDao checkInDao;
    private final RoutePlanDao routePlanDao;
    private final long draftPlanId;

    private AppRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        browseDao = db.browseDao();
        checkInDao = db.checkInDao();
        routePlanDao = db.routePlanDao();
        draftPlanId = ensureDraftPlan();
    }

    public static AppRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private long ensureDraftPlan() {
        RoutePlanEntity plan = routePlanDao.getPlanByName(DRAFT_PLAN_NAME);
        if (plan != null) {
            return plan.planId;
        }
        RoutePlanEntity entity = new RoutePlanEntity();
        entity.name = DRAFT_PLAN_NAME;
        entity.createdAt = System.currentTimeMillis();
        entity.originLat = null;
        entity.originLng = null;
        return routePlanDao.insertPlan(entity);
    }

    public List<String> getGenres() {
        return browseDao.getGenres();
    }

    public List<MapPlaceItem> getMapPlaces(String genre) {
        return getMapPlaces(genre, -1L);
    }

    public List<MapPlaceItem> getMapPlaces(String genre, long selectedPlaceId) {
        List<MapPlaceItem> places = browseDao.getMapPlaces(draftPlanId, normalizeGenreFilter(genre));
        for (MapPlaceItem place : places) {
            place.isSelected = place.placeId == selectedPlaceId ? 1 : 0;
        }
        return places;
    }

    public List<MovieListItem> getMovieCatalog(String genre) {
        return browseDao.getMovieCatalog(normalizeGenreFilter(genre));
    }

    public PlaceDetail getPlaceDetail(long placeId) {
        PlaceDetail detail = new PlaceDetail();
        detail.place = browseDao.getPlace(placeId);
        detail.scenes = browseDao.getPlaceScenes(placeId, draftPlanId);
        return detail;
    }

    public PlaceDetail getPlaceDetailForGenre(long placeId, String genre) {
        PlaceDetail detail = new PlaceDetail();
        detail.place = browseDao.getPlace(placeId);
        detail.scenes = browseDao.getPlaceScenesForMap(placeId, draftPlanId, normalizeGenreFilter(genre));
        return detail;
    }

    public List<PlaceSceneItem> getPlaceScenesForMap(long placeId, String genre) {
        return browseDao.getPlaceScenesForMap(placeId, draftPlanId, normalizeGenreFilter(genre));
    }

    public MovieDetail getMovieDetail(long movieId) {
        MovieDetail detail = new MovieDetail();
        detail.movie = browseDao.getMovie(movieId);
        detail.scenes = browseDao.getMovieScenes(movieId, draftPlanId);
        return detail;
    }

    public int getCheckedInCount() {
        return browseDao.getCheckedInCount();
    }

    public int getTotalSceneCount() {
        return browseDao.getTotalSceneCount();
    }

    public int getCheckedInMovieCount() {
        return browseDao.getCheckedInMovieCount();
    }

    public int getTotalMovieCount() {
        return browseDao.getTotalMovieCount();
    }

    public int getDraftRouteStopCount() {
        return routePlanDao.getStopCount(draftPlanId);
    }

    public List<RouteCandidateItem> searchRouteCandidates(String query, String genre, int limit) {
        String normalizedQuery = query == null ? "" : query.trim();
        String likeQuery = normalizedQuery.isEmpty() ? "" : "%" + normalizedQuery + "%";
        int safeLimit = limit <= 0 ? 30 : limit;
        return browseDao.searchRouteCandidates(draftPlanId, likeQuery, normalizeGenreFilter(genre), safeLimit);
    }

    public boolean addPlaceToDraftRoute(long placeId) {
        if (routePlanDao.hasPlace(draftPlanId, placeId) > 0) {
            return false;
        }
        Long sceneId = browseDao.getRepresentativeMappableSceneIdForPlace(placeId);
        if (sceneId == null || sceneId <= 0) {
            return false;
        }
        int nextOrder = routePlanDao.getMaxVisitOrder(draftPlanId) + 1;
        routePlanDao.insertStop(new RoutePlanStopEntity(draftPlanId, sceneId, nextOrder));
        return true;
    }

    public boolean removePlaceFromDraftRoute(long placeId) {
        if (routePlanDao.hasPlace(draftPlanId, placeId) <= 0) {
            return false;
        }
        routePlanDao.deleteStopsForPlace(draftPlanId, placeId);
        return true;
    }

    public boolean togglePlaceInRoute(long placeId) {
        if (routePlanDao.hasPlace(draftPlanId, placeId) > 0) {
            routePlanDao.deleteStopsForPlace(draftPlanId, placeId);
            return false;
        }
        addPlaceToDraftRoute(placeId);
        return true;
    }

    public Long getDraftRouteSceneIdForPlace(long placeId) {
        return routePlanDao.getSceneIdForPlace(draftPlanId, placeId);
    }

    public List<RouteSceneItem> generateDraftRouteFromMovie(long movieId, int maxStops) {
        MovieDetail detail = getMovieDetail(movieId);
        if (detail == null || detail.scenes == null || detail.scenes.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashMap<Long, MovieSceneItem> uniqueByPlace = new LinkedHashMap<>();
        for (MovieSceneItem scene : detail.scenes) {
            if (scene.latitude == null || scene.longitude == null) {
                continue;
            }
            if (!uniqueByPlace.containsKey(scene.placeId)) {
                uniqueByPlace.put(scene.placeId, scene);
            }
            if (maxStops > 0 && uniqueByPlace.size() >= maxStops) {
                break;
            }
        }
        if (uniqueByPlace.isEmpty()) {
            return Collections.emptyList();
        }

        routePlanDao.clearStops(draftPlanId);
        int order = 0;
        for (MovieSceneItem scene : uniqueByPlace.values()) {
            routePlanDao.insertStop(new RoutePlanStopEntity(draftPlanId, scene.sceneId, order));
            order++;
        }
        return browseDao.getRouteScenes(draftPlanId);
    }

    public List<RouteSceneItem> generateHalfDayRouteDraft(LatLng origin, String generationMode, String genre, int durationHours, int minStops, int maxStops) {
        List<Long> previousSceneIds = routePlanDao.getSceneIds(draftPlanId);
        routePlanDao.clearStops(draftPlanId);
        String normalizedMode = generationMode == null ? "Nearby" : generationMode.trim();
        String normalizedGenre = normalizeGenreFilter(genre);
        boolean preferNearby = !"More Random".equals(normalizedMode);
        int targetMinutes = Math.max(180, Math.min(300, durationHours * 60));
        String candidateGenre = ("Genre".equals(normalizedMode) || "Movie Theme".equals(normalizedMode)) ? normalizedGenre : "All";
        List<RouteCandidateItem> allCandidates = searchRouteCandidates("", candidateGenre, 240);
        if (allCandidates.isEmpty()) {
            restoreDraftRoute(previousSceneIds);
            return Collections.emptyList();
        }
        final Random random = new Random(System.currentTimeMillis());
        List<RouteCandidateScore> scored = new ArrayList<>();
        for (RouteCandidateItem candidate : allCandidates) {
            if (candidate.latitude == null || candidate.longitude == null) {
                continue;
            }
            if (candidate.isInRoute == 1) {
                continue;
            }
            scored.add(new RouteCandidateScore(candidate, scoreHalfDayCandidate(origin, candidate, normalizedMode, normalizedGenre, preferNearby, random)));
        }
        scored.sort(new Comparator<RouteCandidateScore>() {
            @Override
            public int compare(RouteCandidateScore a, RouteCandidateScore b) {
                return Double.compare(b.score, a.score);
            }
        });
        int boundedMin = Math.max(2, minStops);
        int boundedMax = Math.max(boundedMin, maxStops);
        List<RouteCandidateItem> selected = chooseHalfDayCandidates(origin, scored, targetMinutes, boundedMin, boundedMax);
        if (selected.isEmpty()) {
            restoreDraftRoute(previousSceneIds);
            return Collections.emptyList();
        }
        selected = trimHalfDaySelection(origin, selected, scored, targetMinutes, boundedMin);
        if (selected.isEmpty()) {
            restoreDraftRoute(previousSceneIds);
            return Collections.emptyList();
        }
        List<RouteSceneItem> orderedSelection = optimizeSelectedCandidates(origin, selected);
        for (int i = 0; i < orderedSelection.size(); i++) {
            routePlanDao.insertStop(new RoutePlanStopEntity(draftPlanId, orderedSelection.get(i).sceneId, i));
        }
        return browseDao.getRouteScenes(draftPlanId);
    }

    public List<RouteSceneItem> generateRandomHalfDayRouteNearOrigin(LatLng origin, int minStops, int maxStops, boolean preferNearby) {
        return generateHalfDayRouteDraft(origin, preferNearby ? "Nearby" : "More Random", "All", 4, minStops, maxStops);
    }

    private double scoreHalfDayCandidate(LatLng origin, RouteCandidateItem item, String generationMode, String genre, boolean preferNearby, Random random) {
        double distance = distanceKm(origin, item.latitude, item.longitude);
        double distanceScore = preferNearby
                ? Math.max(0d, 120d - distance * 8d)
                : Math.max(0d, 70d - distance * 2.5d);
        double featuredScore = item.hasFeaturedMovie == 1 ? 28d : 0d;
        double richnessScore = item.movieCount * 12d + Math.min(item.sceneCount, 8) * 3d;
        double coordScore = "verified".equals(item.coordStatus) ? 12d : 6d;
        double freshnessScore = item.checkedInSceneCount > 0 ? -18d : 8d;
        double randomness = preferNearby ? random.nextDouble() * 12d : random.nextDouble() * 48d;
        double genreScore = !"All".equals(genre) && genre.equals(item.genreGroup) ? 30d : 0d;
        double themeScore = "Movie Theme".equals(generationMode) && item.hasFeaturedMovie == 1 ? 22d : 0d;
        return distanceScore + featuredScore + richnessScore + coordScore + freshnessScore + randomness + genreScore + themeScore;
    }

    private List<RouteCandidateItem> chooseHalfDayCandidates(LatLng origin, List<RouteCandidateScore> scored, int targetMinutes, int minStops, int maxStops) {
        List<RouteCandidateItem> selected = new ArrayList<>();
        List<RouteCandidateScore> remaining = new ArrayList<>(scored);
        int totalMinutes = 0;
        while (!remaining.isEmpty() && selected.size() < maxStops) {
            RouteCandidateScore best = null;
            int bestMarginalMinutes = Integer.MAX_VALUE;
            for (RouteCandidateScore entry : remaining) {
                int marginalMinutes = estimateMarginalMinutes(origin, selected, entry.candidate);
                boolean canFit = totalMinutes + marginalMinutes <= targetMinutes;
                boolean shouldStretch = selected.size() < minStops && totalMinutes + marginalMinutes <= targetMinutes + 45;
                if (!canFit && !shouldStretch) {
                    continue;
                }
                if (best == null || entry.score > best.score || (entry.score == best.score && marginalMinutes < bestMarginalMinutes)) {
                    best = entry;
                    bestMarginalMinutes = marginalMinutes;
                }
            }
            if (best == null) {
                break;
            }
            selected.add(best.candidate);
            totalMinutes += bestMarginalMinutes;
            remaining.remove(best);
        }
        return selected;
    }

    private List<RouteCandidateItem> trimHalfDaySelection(LatLng origin, List<RouteCandidateItem> selected, List<RouteCandidateScore> scored, int targetMinutes, int minStops) {
        List<RouteCandidateItem> trimmed = new ArrayList<>(selected);
        while (trimmed.size() > minStops && estimateRouteMinutes(origin, trimmed) > targetMinutes) {
            RouteCandidateItem weakest = null;
            double weakestScore = Double.MAX_VALUE;
            for (RouteCandidateItem item : trimmed) {
                double itemScore = 0d;
                for (RouteCandidateScore score : scored) {
                    if (score.candidate.placeId == item.placeId) {
                        itemScore = score.score;
                        break;
                    }
                }
                if (weakest == null || itemScore < weakestScore) {
                    weakest = item;
                    weakestScore = itemScore;
                }
            }
            if (weakest == null) {
                break;
            }
            trimmed.remove(weakest);
        }
        return trimmed;
    }

    private int estimateMarginalMinutes(LatLng origin, List<RouteCandidateItem> selected, RouteCandidateItem candidate) {
        int dwellMinutes = 25;
        if (selected.isEmpty()) {
            return dwellMinutes + estimateTransferMinutes(distanceKm(origin, candidate.latitude, candidate.longitude));
        }
        double nearestKm = Double.MAX_VALUE;
        for (RouteCandidateItem item : selected) {
            nearestKm = Math.min(nearestKm, distanceKm(item.latitude, item.longitude, candidate.latitude, candidate.longitude));
        }
        return dwellMinutes + estimateTransferMinutes(nearestKm);
    }

    private int estimateRouteMinutes(LatLng origin, List<RouteCandidateItem> selected) {
        if (selected.isEmpty()) {
            return 0;
        }
        List<RouteSceneItem> ordered = optimizeSelectedCandidates(origin, selected);
        int totalMinutes = ordered.size() * 25;
        LatLng previous = origin;
        for (RouteSceneItem item : ordered) {
            totalMinutes += estimateTransferMinutes(distanceKm(previous.latitude, previous.longitude, item.latitude, item.longitude));
            previous = new LatLng(item.latitude, item.longitude);
        }
        return totalMinutes;
    }

    private List<RouteSceneItem> optimizeSelectedCandidates(LatLng origin, List<RouteCandidateItem> selected) {
        List<RouteSceneItem> routeScenes = new ArrayList<>();
        for (RouteCandidateItem item : selected) {
            RouteSceneItem scene = new RouteSceneItem();
            scene.sceneId = item.sceneId;
            scene.placeId = item.placeId;
            scene.placeName = item.placeName;
            scene.movieTitle = item.topMovieTitle;
            scene.posterAsset = item.posterAsset;
            scene.genreGroup = item.genreGroup;
            scene.addressEn = item.addressEn;
            scene.sceneTitleEn = item.topMovieTitle;
            scene.latitude = item.latitude;
            scene.longitude = item.longitude;
            routeScenes.add(scene);
        }
        return RouteOptimizer.optimize(origin.latitude, origin.longitude, routeScenes);
    }

    private int estimateTransferMinutes(double distanceKm) {
        if (distanceKm <= 1.2d) {
            return (int) Math.max(6d, Math.round((distanceKm / 4.5d) * 60d));
        }
        double transitMinutes = 12d + ((distanceKm / 18d) * 60d);
        return (int) Math.max(16d, Math.round(transitMinutes));
    }

    private double distanceKm(LatLng origin, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return Double.MAX_VALUE;
        }
        return distanceKm(origin.latitude, origin.longitude, latitude, longitude);
    }

    private double distanceKm(Double fromLatitude, Double fromLongitude, Double toLatitude, Double toLongitude) {
        if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(toLatitude - fromLatitude);
        double dLon = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(fromLatitude)) * Math.cos(Math.toRadians(toLatitude))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return 6371d * c;
    }

    public boolean toggleCheckIn(long sceneId) {
        if (checkInDao.countBySceneId(sceneId) > 0) {
            checkInDao.delete(sceneId);
            return false;
        }
        checkInDao.insert(new UserCheckInEntity(sceneId, System.currentTimeMillis()));
        return true;
    }

    public boolean toggleSceneInRoute(long sceneId) {
        RouteSceneItem scene = browseDao.getRouteScene(sceneId);
        if (scene == null) {
            return false;
        }
        return togglePlaceInRoute(scene.placeId);
    }

    public void clearDraftRoute() {
        routePlanDao.clearStops(draftPlanId);
    }

    public List<RouteSceneItem> getDraftRouteScenes() {
        return browseDao.getRouteScenes(draftPlanId);
    }

    public List<RouteSceneItem> optimizeDraftRoute(LatLng origin) {
        List<RouteSceneItem> routeScenes = browseDao.getRouteScenes(draftPlanId);
        if (routeScenes.isEmpty()) {
            return Collections.emptyList();
        }
        List<RouteSceneItem> ordered = RouteOptimizer.optimize(origin.latitude, origin.longitude, routeScenes);
        routePlanDao.clearStops(draftPlanId);
        for (int i = 0; i < ordered.size(); i++) {
            routePlanDao.insertStop(new RoutePlanStopEntity(draftPlanId, ordered.get(i).sceneId, i));
        }
        RoutePlanEntity plan = routePlanDao.getPlanByName(DRAFT_PLAN_NAME);
        if (plan != null) {
            plan.originLat = origin.latitude;
            plan.originLng = origin.longitude;
            routePlanDao.updatePlan(plan);
        }
        return browseDao.getRouteScenes(draftPlanId);
    }

    public List<RouteSceneItem> reorderDraftRouteSceneIds(List<Long> sceneIds) {
        if (sceneIds == null || sceneIds.isEmpty()) {
            return browseDao.getRouteScenes(draftPlanId);
        }
        routePlanDao.clearStops(draftPlanId);
        for (int i = 0; i < sceneIds.size(); i++) {
            routePlanDao.insertStop(new RoutePlanStopEntity(draftPlanId, sceneIds.get(i), i));
        }
        return browseDao.getRouteScenes(draftPlanId);
    }

    public long getDraftPlanId() {
        return draftPlanId;
    }

    private void restoreDraftRoute(List<Long> previousSceneIds) {
        routePlanDao.clearStops(draftPlanId);
        if (previousSceneIds == null) {
            return;
        }
        for (int i = 0; i < previousSceneIds.size(); i++) {
            routePlanDao.insertStop(new RoutePlanStopEntity(draftPlanId, previousSceneIds.get(i), i));
        }
    }

    private String normalizeGenreFilter(String genre) {
        return genre == null || genre.trim().isEmpty() ? "All" : genre;
    }

    private static class RouteCandidateScore {
        final RouteCandidateItem candidate;
        final double score;

        RouteCandidateScore(RouteCandidateItem candidate, double score) {
            this.candidate = candidate;
            this.score = score;
        }
    }
}
