package com.appsbay.chineseclassicalliteratural.Controller.Menu.MoreApps;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;

import java.util.ArrayList;
import java.util.List;

public class MoreAppsActivity extends AppCompatActivity {

    List<MoreApp> models;
    MoreAppsAdapter adapter;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_apps);
        setTitle(getResources().getString(R.string.MoreApps));

        recyclerView = findViewById(R.id.recyclerVIew_moreApps);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        models = new ArrayList<>();

        models.add(new MoreApp(
                getResources().getString(R.string.NovelsHub),
                getResources().getString(R.string.FictioneBooksLibrary),
                "com.appsbay.novelshub",
                getResources().getDrawable(R.drawable.icon_app_novels_hub)
        ));

//        models.add(new MoreApp(
//                getResources().getString(R.string.ChineseClassicalLiteratural),
//                getResources().getString(R.string.ChineseClassicalLiteraturalDescription),
//                "com.appsbay.chineseclassicalliteratural",
//                getResources().getDrawable(R.drawable.icon_app_chinese_classical_literatural)
//        ));

        models.add(new MoreApp(
                getResources().getString(R.string.FourGreateChineseLiteratural),
                getResources().getString(R.string.FourGreateChineseLiteraturalDescription),
                "com.appsbay.fourgreatclassicalnovels",
                getResources().getDrawable(R.drawable.icon_app_four_great_chinese_novels)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.MinguoLiteratural),
                getResources().getString(R.string.MinguoLiteraturalDescription),
                "com.appsbay.minguoliteratural",
                getResources().getDrawable(R.drawable.icon_app_minguo_literatural)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.RelaxingUp),
                getResources().getString(R.string.MeditationandHealing),
                "com.appsbay.relaxing_up",
                getResources().getDrawable(R.drawable.relaxing_up)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.SudokuLover),
                getResources().getString(R.string.SudokuPuzzles),
                "com.appsbay.sudoku_lovers",
                getResources().getDrawable(R.drawable.sudoku_lover)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.WePlayPiano),
                getResources().getString(R.string.PianoKeyboard),
                "com.appsbay.we_play_piano",
                getResources().getDrawable(R.drawable.we_play_piano)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.SavingAmbulanceSlidingBlock),
                getResources().getString(R.string.SlidingPuzzleWithCars),
                "com.appsbay.saving_ambulance",
                getResources().getDrawable(R.drawable.saving_ambulance)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.MintTranslate),
                getResources().getString(R.string.TextTranslator),
                "com.appsbay.mint_translate",
                getResources().getDrawable(R.drawable.mint_translate)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.InstantFace),
                getResources().getString(R.string.AvatarMaker),
                "com.appsbay.instant_face",
                getResources().getDrawable(R.drawable.instant_face)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.WorldWeatherLiveAllCities),
                getResources().getString(R.string.Weatherradarweatherforecast),
                "com.appsbay.world_weather_live",
                getResources().getDrawable(R.drawable.world_weather_live)
        ));


        models.add(new MoreApp(
                getResources().getString(R.string.FinanceGo),
                getResources().getString(R.string.FinanceGoDescription),
                "com.appsbay.financego",
                getResources().getDrawable(R.drawable.icon_app_finance_go)
        ));

        models.add(new MoreApp(
                getResources().getString(R.string.FinancialRatiosGo),
                getResources().getString(R.string.FinanceGoDescription),
                "com.finance.financialratiosgoapp",
                getResources().getDrawable(R.drawable.icon_app_financial_ratios_go)
        ));

        models.add(new MoreApp("NASA Lover",
                "Stars, Planets & Space",
                "com.AppsBay.nasa_lover",
                getResources().getDrawable(R.drawable.icon_nasa_lover)
        ));

        adapter = new MoreAppsAdapter(models);
        recyclerView.setAdapter(adapter);

        configColor();
    }

    private void configColor() {
        SharedPreferences preferences = this.getSharedPreferences("Color Preference", Context.MODE_PRIVATE);
        String backgroundColorName = preferences.getString("background", "default");

        recyclerView.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, recyclerView);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(MyColor.getActionBarColor(this));

        View decorView = window.getDecorView();
        if (backgroundColorName.equals("dark")) {
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(MyColor.getActionBarColor(this)));

        Spannable text = new SpannableString(actionBar.getTitle());
        text.setSpan(new ForegroundColorSpan(MyColor.getTitleTextColor(this)), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        actionBar.setTitle(text);

        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(new PorterDuffColorFilter(MyColor.getButtonTintColor(this), PorterDuff.Mode.SRC_ATOP));
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
