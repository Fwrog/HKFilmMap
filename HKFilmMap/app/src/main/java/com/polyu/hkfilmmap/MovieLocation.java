package com.polyu.hkfilmmap;

public class MovieLocation {
    private int id;
    private String movieTitle;
    private String movieTitleZh;
    private int year;
    private String director;
    private String locationName;
    private String locationNameZh;
    private double latitude;
    private double longitude;
    private String address;
    private String district;
    private String sceneDescription;
    private String sceneDescriptionZh;
    private String genre;
    private boolean isCheckedIn;

    public int getId() { return id; }
    public String getMovieTitle() { return movieTitle; }
    public String getMovieTitleZh() { return movieTitleZh; }
    public int getYear() { return year; }
    public String getDirector() { return director; }
    public String getLocationName() { return locationName; }
    public String getLocationNameZh() { return locationNameZh; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getDistrict() { return district; }
    public String getSceneDescription() { return sceneDescription; }
    public String getSceneDescriptionZh() { return sceneDescriptionZh; }
    public String getGenre() { return genre; }
    public boolean isCheckedIn() { return isCheckedIn; }
    public void setCheckedIn(boolean c) { isCheckedIn = c; }
}
