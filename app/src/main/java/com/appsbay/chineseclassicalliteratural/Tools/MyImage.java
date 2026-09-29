package com.appsbay.chineseclassicalliteratural.Tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.appsbay.chineseclassicalliteratural.R;

public class MyImage {

    public static Drawable changeDrawableColor(Context context, int icon, int newColor) {
        Drawable mDrawable = ContextCompat.getDrawable(context, icon).mutate();
        mDrawable.setColorFilter(new PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN));
        return mDrawable;
    }

    public static void setBackgroundImage(Context context, View view) {
        if (view == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences("Color Preference", Context.MODE_PRIVATE);
        String backgroundColorName = preferences.getString("background", "default");
        switch (backgroundColorName) {
            case "bg":
                view.setBackgroundResource(R.drawable.bg);
                break;
            case "bg1":
                view.setBackgroundResource(R.drawable.bg1);
                break;
            case "bg2":
                view.setBackgroundResource(R.drawable.bg2);
                break;
            case "bg3":
                view.setBackgroundResource(R.drawable.bg3);
                break;
            case "bg4":
                view.setBackgroundResource(R.drawable.bg4);
                break;
            case "bg5":
                view.setBackgroundResource(R.drawable.bg5);
                break;
            case "default":
                view.setBackgroundResource(R.drawable.bg_paper);
                break;
            default:
                return;
        }
    }
}
