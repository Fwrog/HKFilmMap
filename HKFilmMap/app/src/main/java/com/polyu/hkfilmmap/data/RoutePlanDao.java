package com.polyu.hkfilmmap.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RoutePlanDao {
    @Insert
    long insertPlan(RoutePlanEntity entity);

    @Update
    void updatePlan(RoutePlanEntity entity);

    @Query("SELECT * FROM route_plans WHERE name = :name LIMIT 1")
    RoutePlanEntity getPlanByName(String name);

    @Query("SELECT COUNT(*) FROM route_plan_stops WHERE planId = :planId")
    int getStopCount(long planId);

    @Query("SELECT COUNT(*) FROM route_plan_stops WHERE planId = :planId AND sceneId = :sceneId")
    int hasScene(long planId, long sceneId);

    @Query("SELECT COUNT(*) FROM route_plan_stops rps JOIN scenes s ON s.sceneId = rps.sceneId WHERE rps.planId = :planId AND s.placeId = :placeId")
    int hasPlace(long planId, long placeId);

    @Query("SELECT COALESCE(MAX(visitOrder), -1) FROM route_plan_stops WHERE planId = :planId")
    int getMaxVisitOrder(long planId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStop(RoutePlanStopEntity entity);

    @Query("DELETE FROM route_plan_stops WHERE planId = :planId AND sceneId = :sceneId")
    void deleteStop(long planId, long sceneId);

    @Query("DELETE FROM route_plan_stops WHERE planId = :planId AND sceneId IN (SELECT sceneId FROM scenes WHERE placeId = :placeId)")
    void deleteStopsForPlace(long planId, long placeId);

    @Query("DELETE FROM route_plan_stops WHERE planId = :planId")
    void clearStops(long planId);

    @Query("SELECT sceneId FROM route_plan_stops WHERE planId = :planId ORDER BY visitOrder ASC, sceneId ASC")
    List<Long> getSceneIds(long planId);

    @Query("SELECT rps.sceneId FROM route_plan_stops rps JOIN scenes s ON s.sceneId = rps.sceneId WHERE rps.planId = :planId AND s.placeId = :placeId ORDER BY rps.visitOrder ASC LIMIT 1")
    Long getSceneIdForPlace(long planId, long placeId);
}
