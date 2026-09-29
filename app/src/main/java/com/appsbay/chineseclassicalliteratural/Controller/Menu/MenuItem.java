package com.appsbay.chineseclassicalliteratural.Controller.Menu;

import android.graphics.drawable.Drawable;

public class MenuItem {
    public static final int ACTION_FEEDBACK = 1;
    public static final int ACTION_RATE = 2;
    public static final int ACTION_SHARE = 3;
    public static final int ACTION_REMOVE_ADS = 4;
    public static final int ACTION_RESTORE = 5;
    public static final int ACTION_MORE_APPS = 6;
    public static final int ACTION_WATCH_AD_FREE = 7;
    public static final int ACTION_BACKGROUND = 8;
    public static final int ACTION_LANGUAGE = 9;
    public static final int ACTION_SCRIPT = 10;

    private final int action;
    private String itemName;
    private Drawable icon;

    public MenuItem(int action, String itemName, Drawable icon) {
        this.action = action;
        this.itemName = itemName;
        this.icon = icon;
    }

    public int getAction() {
        return action;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
