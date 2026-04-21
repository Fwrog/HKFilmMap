package com.polyu.hkfilmmap;

public class AchievementItem {
    public final String id;
    public final String title;
    public final String description;
    public final int iconResId;
    public final int current;
    public final int target;
    public final boolean unlocked;

    public AchievementItem(String id, String title, String description, int iconResId, int current, int target, boolean persistedUnlocked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.current = current;
        this.target = target;
        this.unlocked = persistedUnlocked || current >= target;
    }
}
