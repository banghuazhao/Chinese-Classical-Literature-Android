package com.appsbay.chineseclassicalliteratural.Controller;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.View.BookChapterRecyclerViewAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BookChapterActivity extends AppCompatActivity {
    Book book;
    ArrayList<BookChapter> bookChapters = new ArrayList<>();

    RecyclerView recyclerView;
    BookChapterRecyclerViewAdapter adapter;

    Context mContext;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_chapter);

        mAdView = findViewById(R.id.adViewBanner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Intent intent = getIntent();
        book = intent.getExtras().getParcelable("book");;

        setTitle(book.getName());

        mContext = this;

        fetchBookChapters();
        
        adapter = new BookChapterRecyclerViewAdapter(this, bookChapters, book);

        recyclerView = findViewById(R.id.book_chapter_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        configColor();
    }

    private void fetchBookChapters() {
        try {
            JSONObject obj = new JSONObject( loadJSONFromAsset() );
            JSONArray m_jArry = obj.getJSONArray( book.getName() );

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject( i );

                String chapterNumberName;
                String chapterName;
                String chapterText;

                if (jo_inside.has("章节")) {
                    chapterNumberName = jo_inside.getString("章节");
                } else {
                    chapterNumberName = jo_inside.getString("章節");
                }

                if (jo_inside.has("章节名称")) {
                    chapterName = jo_inside.getString("章节名称");
                } else {
                    chapterName = jo_inside.getString("章節名稱");
                }

                if (jo_inside.has("章节内容")) {
                    chapterText = jo_inside.getString("章节内容");
                } else {
                    chapterText = jo_inside.getString("章節內容");
                }

                BookChapter bookChapter = new BookChapter( chapterNumberName, chapterName, chapterText);
                bookChapters.add( bookChapter );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open( "files/" + book.getFileName() + ".json" );
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read( buffer );
            is.close();
            json = new String( buffer, "UTF-8" );
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
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
            case R.id.nav_book_chapter_bookmark:
                SharedPreferences preferences = this.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE);
                int bookmarkNum = preferences.getInt(book.getName(), 0);
                recyclerView.smoothScrollToPosition(bookmarkNum);
        }
        return super.onOptionsItemSelected(item);
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.bookmark_menu, menu);

        menu.getItem(0).setIcon(MyImage.changeDrawableColor(this, R.drawable.nav_bookmark, MyColor.getButtonTintColor(this)));


        return super.onCreateOptionsMenu(menu);
    }
}
