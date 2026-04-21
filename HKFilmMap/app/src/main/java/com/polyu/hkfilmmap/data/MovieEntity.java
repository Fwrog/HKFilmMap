package com.polyu.hkfilmmap.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "movies")
public class MovieEntity {
    @PrimaryKey
    public long movieId;

    @NonNull
    public String titleEn;
    public String titleZh;
    public Integer year;
    public String director;
    public String genreRaw;

    @NonNull
    public String genreGroup;

    @NonNull
    public String posterAsset;
    public boolean isFeatured;
}
