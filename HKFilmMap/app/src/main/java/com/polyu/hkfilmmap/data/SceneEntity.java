package com.polyu.hkfilmmap.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scenes")
public class SceneEntity {
    @PrimaryKey
    public long sceneId;
    public long movieId;
    public long placeId;
    public String sceneTitleEn;
    public String sceneTitleZh;
    public String descriptionEn;
    public String descriptionZh;
    public int sourceRow;
    public boolean isMapVisible;
}
