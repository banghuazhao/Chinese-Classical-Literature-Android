package com.appsbay.chineseclassicalliteratural.Controller.Menu.MoreApps;

import androidx.annotation.DrawableRes;

public final class MoreApp {
    private final String name;
    private final String description;
    private final String packageName;
    @DrawableRes
    private final int iconRes;

    public MoreApp(String name, String description, String packageName, @DrawableRes int iconRes) {
        this.name = name;
        this.description = description;
        this.packageName = packageName;
        this.iconRes = iconRes;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPackageName() {
        return packageName;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }
}
