package com.appsbay.chineseclassicalliteratural.Controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.View.BackgroundThemeAdapter;
import com.google.android.gms.ads.AdView;

import java.util.Arrays;
import java.util.List;

public class ImagesActivity extends AppCompatActivity {

    private static final List<String> THEMES = Arrays.asList(
            "system", "default", "white", "dark", "green",
            "bg", "bg1", "bg2", "bg3", "bg4", "bg5"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);

        View root = findViewById(R.id.images_root);
        AdView adView = findViewById(R.id.adViewBanner);
        View adContainer = findViewById(R.id.ad_container);
        RecyclerView grid = findViewById(R.id.images_grid);

        ScreenChrome.setup(this, root, adContainer);
        setTitle(R.string.Background);

        AdsHelper.bindBanner(adView);

        SharedPreferences preferences = getSharedPreferences("Color Preference", Context.MODE_PRIVATE);
        String selected = preferences.getString("background", "system");

        grid.setLayoutManager(new GridLayoutManager(this, 3));
        grid.setHasFixedSize(true);
        grid.setAdapter(new BackgroundThemeAdapter(THEMES, selected, this::onThemeSelected));

        root.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, root);
        grid.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }

    private void onThemeSelected(String themeId) {
        SharedPreferences preferences = getSharedPreferences("Color Preference", Context.MODE_PRIVATE);
        preferences.edit().putString("background", themeId).apply();
        setResult(RESULT_OK, new Intent());
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("NotificationBackgroundChange"));
        finish();
    }
}
