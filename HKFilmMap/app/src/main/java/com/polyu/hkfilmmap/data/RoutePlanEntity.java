package com.polyu.hkfilmmap.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "route_plans")
public class RoutePlanEntity {
    @PrimaryKey(autoGenerate = true)
    public long planId;
    public String name;
    public long createdAt;
    public Double originLat;
    public Double originLng;
}
