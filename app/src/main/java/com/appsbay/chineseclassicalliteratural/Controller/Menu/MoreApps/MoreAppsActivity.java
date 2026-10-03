package com.appsbay.chineseclassicalliteratural.Controller.Menu.MoreApps;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;

import java.util.ArrayList;
import java.util.List;

public class MoreAppsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AgeGate.isAdult(this)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_more_apps);

        View root = findViewById(R.id.more_apps_root);
        RecyclerView recyclerView = findViewById(R.id.recyclerVIew_moreApps);

        ScreenChrome.setup(this, root, recyclerView);
        setTitle(R.string.MoreApps);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(new MoreAppsAdapter(buildApps()));

        root.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, root);
        recyclerView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }

    private List<MoreApp> buildApps() {
        List<MoreApp> apps = new ArrayList<>();
        apps.add(new MoreApp(
                getString(R.string.MintTranslate),
                getString(R.string.TextTranslator),
                "com.appsbay.mint_translate",
                R.drawable.mint_translate
        ));
        apps.add(new MoreApp(
                getString(R.string.ClassicMemoryGame),
                getString(R.string.ClassicMemoryGameDescription),
                "com.appsbay.classic_memory_game",
                R.drawable.icon_classic_memory_game
        ));
        apps.add(new MoreApp(
                getString(R.string.RelaxingUp),
                getString(R.string.MeditationandHealing),
                "com.appsbay.relaxing_up",
                R.drawable.relaxing_up
        ));
        apps.add(new MoreApp(
                getString(R.string.SudokuLover),
                getString(R.string.SudokuPuzzles),
                "com.appsbay.sudoku_lovers",
                R.drawable.sudoku_lover
        ));
        apps.add(new MoreApp(
                getString(R.string.WePlayPiano),
                getString(R.string.PianoKeyboard),
                "com.appsbay.we_play_piano",
                R.drawable.we_play_piano
        ));
        apps.add(new MoreApp(
                getString(R.string.SavingAmbulanceSlidingBlock),
                getString(R.string.SlidingPuzzleWithCars),
                "com.appsbay.saving_ambulance",
                R.drawable.saving_ambulance
        ));
        apps.add(new MoreApp(
                getString(R.string.WorldWeatherLiveAllCities),
                getString(R.string.Weatherradarweatherforecast),
                "com.appsbay.world_weather_live",
                R.drawable.world_weather_live
        ));
        apps.add(new MoreApp(
                getString(R.string.MetronomeGo),
                getString(R.string.tempo),
                "com.appsbay.metronome_go",
                R.drawable.metronome_go
        ));
        apps.add(new MoreApp(
                getString(R.string.SimpleCalculator),
                getString(R.string.MathCalculator),
                "com.appsbay.simple_calculator",
                R.drawable.simple_calculator
        ));
        apps.add(new MoreApp(
                "Mechanical Engineering Toolkit",
                "Engineering Equations, Formulas",
                "com.appsbay.mechanical_engineering_toolkit",
                R.drawable.icon_mechanical_engineering_toolkit
        ));
        return apps;
    }
}
