package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.ActionBar;

public class MyColor {

    static String resolveThemeId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("Color Preference", Context.MODE_PRIVATE);
        String backgroundColorName = preferences.getString("background", "default");
        if ("system".equals(backgroundColorName)) {
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES ? "dark" : "default";
        }
        return backgroundColorName;
    }

    static public int getBackgroundColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        switch (backgroundColorName) {
            case "default":
                return Color.parseColor("#FAF6EE");
            case "white":
                return Color.parseColor("#FFFEFF");
            case "green":
                return Color.parseColor("#E7F3E8");
            case "dark":
                return Color.parseColor("#16120E");
            default:
                return Color.parseColor("#FAF6EE");
        }

    }

    static public int getActionBarColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        switch (backgroundColorName) {
            case "default":
                return Color.parseColor("#FAF6EE");
            case "white":
                return Color.parseColor("#FFFEFF");
            case "green":
                return Color.parseColor("#E7F3E8");
            case "dark":
                return Color.parseColor("#16120E");
            case "bg":
                return Color.parseColor("#FEE4D9");
            case "bg1":
                return Color.parseColor("#FBE0D7");
            case "bg2":
                return Color.parseColor("#F59D97");
            case "bg3":
                return Color.parseColor("#8DA9DC");
            case "bg4":
                return Color.parseColor("#EDCCCE");
            case "bg5":
                return Color.parseColor("#E6D0B4");
            default:
                return Color.parseColor("#FAF6EE");
        }
    }

    static public int getBottomBarColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        switch (backgroundColorName) {
            case "default":
                return Color.parseColor("#FAF6EE");
            case "white":
                return Color.parseColor("#FFFEFF");
            case "green":
                return Color.parseColor("#E7F3E8");
            case "dark":
                return Color.parseColor("#1C1814");
            case "bg":
                return Color.parseColor("#E1BA96");
            case "bg1":
                return Color.parseColor("#E1BA96");
            case "bg2":
                return Color.parseColor("#F59D97");
            case "bg3":
                return Color.parseColor("#E8BDDC");
            case "bg4":
                return Color.parseColor("#9FBDC5");
            case "bg5":
                return Color.parseColor("#F0C878");
            default:
                return Color.parseColor("#FAF6EE");
        }
    }

    static public int getTitleTextColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        if (backgroundColorName.equals("dark")) {
            return Color.parseColor("#F4EDE4");
        } else {
            return Color.parseColor("#1C1917");
        }
    }

    static public int getDetailTextColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        if (backgroundColorName.equals("dark")) {
            return Color.parseColor("#A39B90");
        } else {
            return Color.parseColor("#6B635B");
        }
    }

    static public int getButtonTintColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        if (backgroundColorName.equals("dark")) {
            return Color.parseColor("#C4B8A8");
        } else {
            return Color.parseColor("#5C534A");
        }
    }

    static public int getSeparatorColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        if (backgroundColorName.equals("dark")) {
            return Color.parseColor("#2C2824");
        } else {
            return Color.parseColor("#E6DCCE");
        }
    }

    static public int getAccentColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        if (backgroundColorName.equals("dark")) {
            return Color.parseColor("#E8C36A");
        } else {
            return Color.parseColor("#A16207");
        }
    }

    static public int getElevatedSurfaceColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        switch (backgroundColorName) {
            case "white":
                return Color.parseColor("#F7F4EF");
            case "green":
                return Color.parseColor("#DCEADB");
            case "dark":
                return Color.parseColor("#211C17");
            default:
                return Color.parseColor("#F3EDE1");
        }
    }

    static public int getAccentSurfaceColor(Context context) {
        String backgroundColorName = resolveThemeId(context);
        if ("dark".equals(backgroundColorName)) {
            return Color.parseColor("#3D3322");
        }
        if ("green".equals(backgroundColorName)) {
            return Color.parseColor("#D7E2C5");
        }
        return Color.parseColor("#E8D7B5");
    }

    static public boolean isDark(Context context) {
        return "dark".equals(resolveThemeId(context));
    }

    static public int getShelfLipColor(Context context) {
        if (isDark(context)) {
            return Color.parseColor("#5A4E42");
        }
        return Color.parseColor("#D7C4A8");
    }

    static public int getShelfGrooveColor(Context context) {
        if (isDark(context)) {
            return Color.parseColor("#2A241E");
        }
        return Color.parseColor("#8F6F52");
    }

    static public int getShelfFaceColor(Context context) {
        if (isDark(context)) {
            return Color.parseColor("#3E342C");
        }
        return Color.parseColor("#B08968");
    }

    static public int getShelfFaceHighlightColor(Context context) {
        if (isDark(context)) {
            return Color.parseColor("#5A4E42");
        }
        return Color.parseColor("#C9A57F");
    }

    static public int getShelfFaceShadowColor(Context context) {
        if (isDark(context)) {
            return Color.parseColor("#2A241E");
        }
        return Color.parseColor("#8A6244");
    }

    static public int getShelfUnderColor(Context context) {
        if (isDark(context)) {
            return Color.parseColor("#99100E0C");
        }
        return Color.parseColor("#401C1917");
    }

    static public void applyBottomNavigation(Context context, BottomNavigationView navigation) {
        if (navigation == null) {
            return;
        }
        navigation.setBackgroundColor(getBottomBarColor(context));
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                getAccentColor(context),
                getDetailTextColor(context)
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);
        navigation.setItemIconTintList(colorStateList);
        navigation.setItemTextColor(colorStateList);
    }

    static public void applySystemBars(Window window, Context context, int navigationBarColor) {
        if (window == null || context == null) {
            return;
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getActionBarColor(context));
        window.setNavigationBarColor(navigationBarColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(navigationBarColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        SharedPreferences preferences = context.getSharedPreferences("Color Preference", Context.MODE_PRIVATE);
        String backgroundColorName = preferences.getString("background", "default");
        boolean dark = isDark(context);
        View decorView = window.getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (dark) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        decorView.setSystemUiVisibility(flags);
    }

    static public void flattenActionBar(ActionBar actionBar) {
        if (actionBar != null) {
            actionBar.setElevation(0);
        }
    }
}
