package com.appsbay.chineseclassicalliteratural.Controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.AuthorBioSheet;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.View.BookChapterRecyclerViewAdapter;
import com.appsbay.chineseclassicalliteratural.data.BookLoadError;
import com.appsbay.chineseclassicalliteratural.viewmodel.ChapterViewModel;
import com.appsbay.chineseclassicalliteratural.viewmodel.NovelsHubViewModelFactory;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class BookChapterActivity extends AppCompatActivity {
    Book book;
    ArrayList<BookChapter> bookChapters = new ArrayList<>();

    RecyclerView recyclerView;
    BookChapterRecyclerViewAdapter adapter;

    Context mContext;

    View loadingContainer;
    ProgressBar pBar;
    TextView downloadingLabel;
    TextView downloadingHint;
    View errorContainer;
    ImageView errorIcon;
    TextView errorTitle;
    TextView errorMessage;
    MaterialButton errorRetry;

    private AdView mAdView;
    private boolean autoOpenChapter;
    private boolean openedChapter;
    private ChapterViewModel chapterViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_chapter);

        mAdView = findViewById(R.id.adViewBanner);
        AdsHelper.bindBanner(mAdView);
        ScreenChrome.setup(this, findViewById(R.id.screen_root), findViewById(R.id.ad_container));

        loadingContainer = findViewById(R.id.book_chapter_loading);
        pBar = findViewById(R.id.pBar);
        downloadingLabel = findViewById(R.id.textView_downloading);
        downloadingHint = findViewById(R.id.textView_downloading_hint);
        errorContainer = findViewById(R.id.book_load_error);
        errorIcon = findViewById(R.id.book_load_error_icon);
        errorTitle = findViewById(R.id.book_load_error_title);
        errorMessage = findViewById(R.id.book_load_error_message);
        errorRetry = findViewById(R.id.book_load_error_retry);
        loadingContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);

        Intent intent = getIntent();
        book = intent.getParcelableExtra(BookOpener.EXTRA_BOOK);
        if (book == null) {
            book = intent.getParcelableExtra("book");
        }
        if (book == null) {
            // Nothing to show without a book (e.g. a stale shortcut or restored intent).
            finish();
            return;
        }
        autoOpenChapter = intent.getBooleanExtra(BookOpener.EXTRA_AUTO_OPEN_CHAPTER, false);

        setTitle(book.getName());

        mContext = this;

        chapterViewModel = new ViewModelProvider(
                this,
                new NovelsHubViewModelFactory.ChapterFactory(getApplication(), book)
        ).get(ChapterViewModel.class);

        adapter = new BookChapterRecyclerViewAdapter(this, bookChapters, book);

        recyclerView = findViewById(R.id.book_chapter_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        errorRetry.setOnClickListener(v -> chapterViewModel.loadChapters());

        chapterViewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            loadingContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                errorContainer.setVisibility(View.GONE);
                // The list (and its book header) would collide with the centred
                // state view on short screens, so only one of them is on screen.
                recyclerView.setVisibility(View.GONE);
            }
        });

        chapterViewModel.getDownloading().observe(this, downloading -> {
            int visibility = Boolean.TRUE.equals(downloading) ? View.VISIBLE : View.GONE;
            downloadingLabel.setVisibility(visibility);
            downloadingHint.setVisibility(visibility);
        });

        chapterViewModel.getChapterList().observe(this, chapters -> {
            bookChapters.clear();
            if (chapters != null) {
                bookChapters.addAll(chapters);
            }
            adapter.notifyDataSetChanged();
            if (!bookChapters.isEmpty()) {
                recyclerView.setVisibility(View.VISIBLE);
                BookMotion.revealOnce(recyclerView);
            }
            maybeAutoOpenChapter();
        });

        chapterViewModel.getLoadError().observe(this, this::showLoadError);

        configColor();

        LocalBroadcastManager.getInstance(this).registerReceiver(backgroundReceiver,
                new IntentFilter("NotificationBackgroundChange"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Theme may have changed in the reader while this screen was stopped.
        configColor();
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(backgroundReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver backgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            configColor();
            invalidateOptionsMenu();
        }
    };

    private void maybeAutoOpenChapter() {
        if (!autoOpenChapter || openedChapter || bookChapters.isEmpty()) {
            return;
        }
        openedChapter = true;
        int chapterIndex = chapterViewModel.getContinueChapterIndex();
        if (chapterIndex < 0 || chapterIndex >= bookChapters.size()) {
            chapterIndex = chapterViewModel.getChapterBookmarkIndex();
        }
        if (chapterIndex < 0 || chapterIndex >= bookChapters.size()) {
            chapterIndex = 0;
        }
        openChapterAtIndex(chapterIndex);
    }

    private void openChapterAtIndex(int chapterIndex) {
        BookChapter bookChapter = bookChapters.get(chapterIndex);
        chapterViewModel.openChapter(bookChapter, chapterIndex, bookChapters.size());

        Intent intent = new Intent(this, BookPagerActivity.class);
        intent.putExtra("bookChapter", bookChapter);
        intent.putExtra("book", book);
        intent.putExtra("totalChapters", bookChapters.size());
        intent.putExtra("chapterIndex", chapterIndex);
        startActivity(intent);
    }

    private void showLoadError(@Nullable BookLoadError error) {
        if (error == null) {
            errorContainer.setVisibility(View.GONE);
            return;
        }
        recyclerView.setVisibility(View.GONE);
        int titleRes;
        int messageRes;
        switch (error) {
            case NO_CONNECTION:
                titleRes = R.string.book_offline_title;
                messageRes = R.string.book_offline_message;
                break;
            case CONTENT_UNAVAILABLE:
                titleRes = R.string.book_unavailable_title;
                messageRes = R.string.book_unavailable_message;
                break;
            case DOWNLOAD_FAILED:
            default:
                titleRes = R.string.book_download_failed_title;
                messageRes = R.string.book_download_failed_message;
                break;
        }
        errorTitle.setText(titleRes);
        errorMessage.setText(messageRes);
        // Retrying a missing or corrupt title will not help, so only offer it for network failures.
        errorRetry.setVisibility(error == BookLoadError.CONTENT_UNAVAILABLE ? View.GONE : View.VISIBLE);
        errorContainer.setVisibility(View.VISIBLE);
        errorContainer.announceForAccessibility(getString(titleRes));
        tintLoadStates();
    }

    private void tintLoadStates() {
        int accent = MyColor.getAccentColor(this);
        errorTitle.setTextColor(MyColor.getTitleTextColor(this));
        errorMessage.setTextColor(MyColor.getDetailTextColor(this));
        errorIcon.setImageTintList(ColorStateList.valueOf(MyColor.getDetailTextColor(this)));
        errorRetry.setTextColor(accent);
        errorRetry.setStrokeColor(ColorStateList.valueOf(accent));
        errorRetry.setBackgroundTintList(ColorStateList.valueOf(MyColor.getElevatedSurfaceColor(this)));
        errorRetry.setRippleColor(ColorStateList.valueOf(MyColor.getSeparatorColor(this)));
        downloadingLabel.setTextColor(MyColor.getTitleTextColor(this));
        downloadingHint.setTextColor(MyColor.getDetailTextColor(this));
    }

    private void configColor() {
        if (recyclerView == null) {
            // onCreate bailed out early (no book in the intent).
            return;
        }
        recyclerView.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, recyclerView);
        findViewById(R.id.book_chapter_content).setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, findViewById(R.id.book_chapter_content));
        tintLoadStates();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        ScreenChrome.tint(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.nav_book_chapter_info) {
            if (book != null) {
                AuthorBioSheet.showBook(this, book);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bookmark_menu, menu);

        MenuItem infoItem = menu.findItem(R.id.nav_book_chapter_info);
        if (infoItem != null && book != null) {
            infoItem.setIcon(MyImage.changeDrawableColor(this, R.drawable.nav_info, MyColor.getButtonTintColor(this)));
            infoItem.setVisible(AuthorBioSheet.hasBookIntro(book));
        }

        return super.onCreateOptionsMenu(menu);
    }
}
