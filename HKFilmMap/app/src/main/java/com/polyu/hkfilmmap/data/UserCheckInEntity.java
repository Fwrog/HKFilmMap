package com.polyu.hkfilmmap.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_check_ins")
public class UserCheckInEntity {
    @PrimaryKey
    public long sceneId;
    public long checkedInAt;

    public UserCheckInEntity(long sceneId, long checkedInAt) {
        this.sceneId = sceneId;
        this.checkedInAt = checkedInAt;
    }
}
