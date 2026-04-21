package com.polyu.hkfilmmap.data;

public class NearbyFoodItem {
    public String name;
    public String nameZh;
    public String category;
    public String district;
    public double latitude;
    public double longitude;
    public String description;
    public String priceRange;

    /** Transient fields are calculated per query and are not stored in assets. */
    public transient double distanceKm;
    public transient double recommendationScore;
    public transient int walkMinutes;
    public transient int rank;
    public transient int districtMatch;
    public transient String recommendationReason;
}
