package com.polyu.hkfilmmap.data;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BrowseDao {
    @Query("SELECT DISTINCT genreGroup FROM movies WHERE genreGroup IS NOT NULL AND genreGroup != '' ORDER BY genreGroup")
    List<String> getGenres();

    @Query("SELECT p.placeId AS placeId, p.nameEn AS nameEn, p.districtEn AS districtEn, p.addressEn AS addressEn, p.latitude AS latitude, p.longitude AS longitude, p.coordStatus AS coordStatus, COUNT(DISTINCT s.movieId) AS movieCount, COUNT(s.sceneId) AS sceneCount, MAX(CASE WHEN m.isFeatured = 1 THEN 1 ELSE 0 END) AS hasFeaturedMovie, COALESCE(MIN(CASE WHEN m.posterAsset != 'poster_placeholder' THEN m.posterAsset END), 'poster_placeholder') AS posterAsset, CASE WHEN EXISTS (SELECT 1 FROM route_plan_stops rps JOIN scenes rs ON rs.sceneId = rps.sceneId WHERE rps.planId = :routePlanId AND rs.placeId = p.placeId) THEN 1 ELSE 0 END AS isInRoute, 0 AS isSelected FROM places p JOIN scenes s ON s.placeId = p.placeId JOIN movies m ON m.movieId = s.movieId WHERE s.isMapVisible = 1 AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND (:genre = 'All' OR m.genreGroup = :genre) GROUP BY p.placeId ORDER BY isInRoute DESC, hasFeaturedMovie DESC, movieCount DESC, p.nameEn ASC")
    List<MapPlaceItem> getMapPlaces(long routePlanId, String genre);

    @Query("SELECT * FROM places WHERE placeId = :placeId LIMIT 1")
    PlaceEntity getPlace(long placeId);

    @Query("SELECT s.sceneId AS sceneId, m.movieId AS movieId, m.titleEn AS movieTitle, m.titleZh AS movieTitleZh, m.year AS year, m.genreGroup AS genreGroup, m.posterAsset AS posterAsset, s.sceneTitleEn AS sceneTitleEn, s.sceneTitleZh AS sceneTitleZh, s.descriptionEn AS descriptionEn, s.descriptionZh AS descriptionZh, CASE WHEN uc.sceneId IS NULL THEN 0 ELSE 1 END AS isCheckedIn, CASE WHEN EXISTS (SELECT 1 FROM route_plan_stops rps JOIN scenes rs ON rs.sceneId = rps.sceneId WHERE rps.planId = :routePlanId AND rs.placeId = s.placeId) THEN 1 ELSE 0 END AS isInRoute, p.latitude AS latitude, p.longitude AS longitude FROM scenes s JOIN movies m ON m.movieId = s.movieId JOIN places p ON p.placeId = s.placeId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE s.placeId = :placeId ORDER BY m.isFeatured DESC, m.titleEn ASC, s.sceneId ASC")
    List<PlaceSceneItem> getPlaceScenes(long placeId, long routePlanId);

    @Query("SELECT s.sceneId AS sceneId, m.movieId AS movieId, m.titleEn AS movieTitle, m.titleZh AS movieTitleZh, m.year AS year, m.genreGroup AS genreGroup, m.posterAsset AS posterAsset, s.sceneTitleEn AS sceneTitleEn, s.sceneTitleZh AS sceneTitleZh, s.descriptionEn AS descriptionEn, s.descriptionZh AS descriptionZh, CASE WHEN uc.sceneId IS NULL THEN 0 ELSE 1 END AS isCheckedIn, CASE WHEN EXISTS (SELECT 1 FROM route_plan_stops rps JOIN scenes rs ON rs.sceneId = rps.sceneId WHERE rps.planId = :routePlanId AND rs.placeId = s.placeId) THEN 1 ELSE 0 END AS isInRoute, p.latitude AS latitude, p.longitude AS longitude FROM scenes s JOIN movies m ON m.movieId = s.movieId JOIN places p ON p.placeId = s.placeId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE s.placeId = :placeId AND (:genre = 'All' OR m.genreGroup = :genre) ORDER BY m.isFeatured DESC, m.titleEn ASC, s.sceneId ASC")
    List<PlaceSceneItem> getPlaceScenesForMap(long placeId, long routePlanId, String genre);

    @Query("SELECT * FROM movies WHERE movieId = :movieId LIMIT 1")
    MovieEntity getMovie(long movieId);

    @Query("SELECT s.sceneId AS sceneId, p.placeId AS placeId, p.nameEn AS placeName, p.districtEn AS districtEn, p.addressEn AS addressEn, p.latitude AS latitude, p.longitude AS longitude, s.sceneTitleEn AS sceneTitleEn, s.sceneTitleZh AS sceneTitleZh, s.descriptionEn AS descriptionEn, s.descriptionZh AS descriptionZh, CASE WHEN uc.sceneId IS NULL THEN 0 ELSE 1 END AS isCheckedIn, CASE WHEN EXISTS (SELECT 1 FROM route_plan_stops rps JOIN scenes rs ON rs.sceneId = rps.sceneId WHERE rps.planId = :routePlanId AND rs.placeId = s.placeId) THEN 1 ELSE 0 END AS isInRoute FROM scenes s JOIN places p ON p.placeId = s.placeId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE s.movieId = :movieId ORDER BY s.isMapVisible DESC, p.nameEn ASC, s.sceneId ASC")
    List<MovieSceneItem> getMovieScenes(long movieId, long routePlanId);

    @Query("SELECT m.movieId AS movieId, m.titleEn AS titleEn, m.titleZh AS titleZh, m.year AS year, m.director AS director, m.genreGroup AS genreGroup, m.posterAsset AS posterAsset, m.isFeatured AS isFeatured, COUNT(s.sceneId) AS sceneCount, SUM(CASE WHEN s.isMapVisible = 1 THEN 1 ELSE 0 END) AS mapVisibleCount FROM movies m LEFT JOIN scenes s ON s.movieId = m.movieId WHERE (:genre = 'All' OR m.genreGroup = :genre) GROUP BY m.movieId ORDER BY m.isFeatured DESC, mapVisibleCount DESC, m.titleEn ASC")
    List<MovieListItem> getMovieCatalog(String genre);

    @Query("SELECT COUNT(*) FROM user_check_ins")
    int getCheckedInCount();

    @Query("SELECT COUNT(*) FROM scenes")
    int getTotalSceneCount();

    @Query("SELECT COUNT(DISTINCT s.movieId) FROM user_check_ins uc JOIN scenes s ON s.sceneId = uc.sceneId")
    int getCheckedInMovieCount();

    @Query("SELECT COUNT(*) FROM movies")
    int getTotalMovieCount();

    @Query("SELECT s.sceneId AS sceneId, m.movieId AS movieId, p.placeId AS placeId, m.titleEn AS movieTitle, m.posterAsset AS posterAsset, m.genreGroup AS genreGroup, p.nameEn AS placeName, p.addressEn AS addressEn, s.sceneTitleEn AS sceneTitleEn, s.descriptionEn AS descriptionEn, p.latitude AS latitude, p.longitude AS longitude, rps.visitOrder AS visitOrder, CASE WHEN uc.sceneId IS NULL THEN 0 ELSE 1 END AS isCheckedIn, 0 AS isSelected FROM route_plan_stops rps JOIN scenes s ON s.sceneId = rps.sceneId JOIN movies m ON m.movieId = s.movieId JOIN places p ON p.placeId = s.placeId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE rps.planId = :planId ORDER BY rps.visitOrder ASC, s.sceneId ASC")
    List<RouteSceneItem> getRouteScenes(long planId);

    @Query("SELECT s.sceneId AS sceneId, m.movieId AS movieId, p.placeId AS placeId, m.titleEn AS movieTitle, m.posterAsset AS posterAsset, m.genreGroup AS genreGroup, p.nameEn AS placeName, p.addressEn AS addressEn, s.sceneTitleEn AS sceneTitleEn, s.descriptionEn AS descriptionEn, p.latitude AS latitude, p.longitude AS longitude, 0 AS visitOrder, CASE WHEN uc.sceneId IS NULL THEN 0 ELSE 1 END AS isCheckedIn, 0 AS isSelected FROM scenes s JOIN movies m ON m.movieId = s.movieId JOIN places p ON p.placeId = s.placeId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE s.isMapVisible = 1 AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL ORDER BY m.isFeatured DESC, m.titleEn ASC, s.sceneId ASC")
    List<RouteSceneItem> getAllMappableRouteScenes();

    @Query("SELECT s.sceneId AS sceneId, m.movieId AS movieId, p.placeId AS placeId, m.titleEn AS movieTitle, m.posterAsset AS posterAsset, m.genreGroup AS genreGroup, p.nameEn AS placeName, p.addressEn AS addressEn, s.sceneTitleEn AS sceneTitleEn, s.descriptionEn AS descriptionEn, p.latitude AS latitude, p.longitude AS longitude, 0 AS visitOrder, CASE WHEN uc.sceneId IS NULL THEN 0 ELSE 1 END AS isCheckedIn, 0 AS isSelected FROM scenes s JOIN movies m ON m.movieId = s.movieId JOIN places p ON p.placeId = s.placeId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE s.sceneId = :sceneId LIMIT 1")
    RouteSceneItem getRouteScene(long sceneId);

    @Query("SELECT MIN(s.sceneId) FROM scenes s JOIN places p ON p.placeId = s.placeId JOIN movies m ON m.movieId = s.movieId WHERE s.placeId = :placeId AND s.isMapVisible = 1 AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL")
    Long getRepresentativeMappableSceneIdForPlace(long placeId);

    @Query("SELECT MIN(s.sceneId) AS sceneId, MIN(m.movieId) AS movieId, p.placeId AS placeId, p.nameEn AS placeName, p.districtEn AS districtEn, p.addressEn AS addressEn, p.latitude AS latitude, p.longitude AS longitude, p.coordStatus AS coordStatus, COUNT(DISTINCT s.movieId) AS movieCount, COUNT(s.sceneId) AS sceneCount, MAX(CASE WHEN m.isFeatured = 1 THEN 1 ELSE 0 END) AS hasFeaturedMovie, COALESCE(MIN(CASE WHEN m.posterAsset != 'poster_placeholder' THEN m.posterAsset END), 'poster_placeholder') AS posterAsset, COALESCE(MIN(CASE WHEN m.isFeatured = 1 THEN m.titleEn END), MIN(m.titleEn)) AS topMovieTitle, COALESCE(MIN(CASE WHEN m.isFeatured = 1 THEN m.titleZh END), MIN(m.titleZh)) AS topMovieTitleZh, COALESCE(MIN(CASE WHEN m.genreGroup = :genre THEN m.genreGroup END), MIN(m.genreGroup)) AS genreGroup, CASE WHEN EXISTS (SELECT 1 FROM route_plan_stops rps JOIN scenes rs ON rs.sceneId = rps.sceneId WHERE rps.planId = :routePlanId AND rs.placeId = p.placeId) THEN 1 ELSE 0 END AS isInRoute, COUNT(DISTINCT uc.sceneId) AS checkedInSceneCount, 0 AS isSelected FROM places p JOIN scenes s ON s.placeId = p.placeId JOIN movies m ON m.movieId = s.movieId LEFT JOIN user_check_ins uc ON uc.sceneId = s.sceneId WHERE s.isMapVisible = 1 AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND (:genre = 'All' OR m.genreGroup = :genre) AND (:query = '' OR p.nameEn LIKE :query OR p.districtEn LIKE :query OR p.addressEn LIKE :query OR m.titleEn LIKE :query OR m.titleZh LIKE :query OR m.genreGroup LIKE :query OR s.sceneTitleEn LIKE :query OR s.sceneTitleZh LIKE :query OR s.descriptionEn LIKE :query OR s.descriptionZh LIKE :query) GROUP BY p.placeId ORDER BY isInRoute DESC, hasFeaturedMovie DESC, movieCount DESC, p.nameEn ASC LIMIT :limit")
    List<RouteCandidateItem> searchRouteCandidates(long routePlanId, String query, String genre, int limit);
}
