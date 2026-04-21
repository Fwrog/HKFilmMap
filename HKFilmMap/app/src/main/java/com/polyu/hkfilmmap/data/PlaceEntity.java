package com.polyu.hkfilmmap.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "places")
public class PlaceEntity {
    @PrimaryKey
    public long placeId;
    public String nameEn;
    public String nameZh;
    public String districtEn;
    public String districtZh;
    public String addressEn;
    public String addressZh;
    public Double latitude;
    public Double longitude;
    public String coordStatus;
}
