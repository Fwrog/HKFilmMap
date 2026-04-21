package com.polyu.hkfilmmap.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserCheckInEntity entity);

    @Query("DELETE FROM user_check_ins WHERE sceneId = :sceneId")
    void delete(long sceneId);

    @Query("SELECT COUNT(*) FROM user_check_ins WHERE sceneId = :sceneId")
    int countBySceneId(long sceneId);
}
