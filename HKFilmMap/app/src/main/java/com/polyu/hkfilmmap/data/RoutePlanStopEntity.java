package com.polyu.hkfilmmap.data;

import androidx.room.Entity;

@Entity(tableName = "route_plan_stops", primaryKeys = {"planId", "sceneId"})
public class RoutePlanStopEntity {
    public long planId;
    public long sceneId;
    public int visitOrder;

    public RoutePlanStopEntity(long planId, long sceneId, int visitOrder) {
        this.planId = planId;
        this.sceneId = sceneId;
        this.visitOrder = visitOrder;
    }
}
