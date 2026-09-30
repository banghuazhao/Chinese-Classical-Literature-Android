package com.appsbay.chineseclassicalliteratural.Controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Layout;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdCoordinator;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderOptionsSheet;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderContent;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderMarks;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderSpeech;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.viewmodel.NovelsHubViewModelFactory;
import com.appsbay.chineseclassicalliteratural.viewmodel.ReaderViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.Slider;
import android.speech.tts.Voice;

import java.util.List;
import java.io.IOException;

public class BookPagerActivity extends AppCompatActivity {
    Book book;
    BookChapter bookChapter;

    ScrollView scrollView;
    TextView titleTextView;
    TextView textView;
    TextView prevChapterButton;
    TextView nextChapterButton;
    TextView chapterProgressLabel;
    View pageContent;
    View readerChrome;
    LinearProgressIndicator readingProgress;

    Context mContext;

    private ReaderSpeech speechController;
    private ReaderMarks marks;
    private BackgroundColorSpan speechHighlight;
    private MaterialButton speechPlayButton;
    private int searchFocusOffset = -1;
    private int searchFocusLength;
    private static final int ACTION_HIGHLIGHT = 92101;
    private static final int ACTION_NOTE = 92102;
    private long readingStartedAt;
    private int restoredScrollY;
    private boolean didScroll;
    private boolean leavingWithAd;
    private int chapterIndex = -1;
    private int totalChapters = 0;
    private boolean chapterTransitionRunning;
    private OnBackPressedCallback backPressedCallback;
    private ReaderViewModel readerViewModel;

    private static final int CHROME_SCROLL_THRESHOLD_PX = 12;
    private static final long CHROME_ANIM_MS = 220L;
    private boolean chromeVisible = true;
    private boolean chromeLaidOut;
    private boolean suppressChromeScroll;
    private int lastScrollY;
    private int chromeTopPadding;
    private ValueAnimator chromePaddingAnimator;
    private GestureDetector chromeTapDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_pager);

        scrollView = findViewById(R.id.book_pager_scroll);
        ScreenChrome.setup(this, findViewById(R.id.screen_root), scrollView);

        Intent intent = getIntent();
        bookChapter = intent.getParcelableExtra("bookChapter");
        book = intent.getParcelableExtra("book");
        if (book == null || bookChapter == null) {
            // Nothing to read without a chapter (e.g. a stale or restored intent).
            finish();
            return;
        }
        chapterIndex = intent.getIntExtra("chapterIndex", -1);
        totalChapters = intent.getIntExtra("totalChapters", 0);
        searchFocusOffset = intent.getIntExtra("focusOffset", -1);
        searchFocusLength = intent.getIntExtra("focusLength", 0);

        readerViewModel = new ViewModelProvider(
                this,
                new NovelsHubViewModelFactory.ReaderFactory(
                        getApplication(),
                        book,
                        bookChapter,
                        chapterIndex,
                        totalChapters
                )
        ).get(ReaderViewModel.class);

        setTitle(bookChapter.getChapterNumberName());

        mContext = this;
        readingStartedAt = System.currentTimeMillis();
        AdCoordinator.get(this).preloadInterstitial();

        titleTextView = findViewById(R.id.book_pager_title);
        textView = findViewById(R.id.book_pager_text);
        prevChapterButton = findViewById(R.id.book_pager_prev_chapter);
        nextChapterButton = findViewById(R.id.book_pager_next_chapter);
        chapterProgressLabel = findViewById(R.id.book_pager_chapter_progress_label);
        pageContent = findViewById(R.id.book_pager_page_content);
        readerChrome = findViewById(R.id.book_pager_chrome);
        readingProgress = findViewById(R.id.book_pager_reading_progress);

        marks = new ReaderMarks(this);
        speechController = new ReaderSpeech(this, new ReaderSpeech.Listener() {
            @Override public void onPosition(boolean playing, int start, int end) {
                showSpokenRange(playing ? start : -1, playing ? end : -1);
                if (speechPlayButton != null) {
                    speechPlayButton.setText(playing ? R.string.pause_reading
                            : speechController.hasSession() ? R.string.resume_reading
                            : R.string.Begin_Reading);
                }
            }
            @Override public void onUnavailable() {
                Toast.makeText(BookPagerActivity.this, R.string.speech_unavailable, Toast.LENGTH_SHORT).show();
            }
        });

        titleTextView.setText(bookChapter.getChapterName());
        renderChapterText();
        setupSelectionActions();
        titleTextView.setTextColor(MyColor.getTitleTextColor(mContext));
        textView.setTextColor(MyColor.getTitleTextColor(mContext));
        updateChapterProgressLabel();
        tintChapterNavButtons();
        setupReaderChrome();

        Integer textSize = readerViewModel.getTextSize().getValue();
        textView.setTextSize(textSize != null ? textSize : 18);

        readerViewModel.getTextSize().observe(this, size -> {
            if (size != null) {
                textView.setTextSize(size);
            }
        });

        readerViewModel.getCanGoPrevious().observe(this, enabled -> {
            boolean canGo = Boolean.TRUE.equals(enabled);
            prevChapterButton.setEnabled(canGo);
            prevChapterButton.setAlpha(canGo ? 1f : 0.28f);
        });
        readerViewModel.getCanGoNext().observe(this, enabled -> {
            boolean canGo = Boolean.TRUE.equals(enabled);
            nextChapterButton.setEnabled(canGo);
            nextChapterButton.setAlpha(canGo ? 1f : 0.28f);
        });

        prevChapterButton.setOnClickListener(v -> openAdjacentChapter(true));
        nextChapterButton.setOnClickListener(v -> openAdjacentChapter(false));
        BookMotion.attachPressEffect(prevChapterButton);
        BookMotion.attachPressEffect(nextChapterButton);
        BookMotion.revealOnce(pageContent);

        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleReaderBack();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        configColor();

        LocalBroadcastManager.getInstance(this).registerReceiver(backgroundReceiver,
                new IntentFilter("NotificationBackgroundChange"));

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                float gotoPostion = intent.getFloatExtra("focusFraction", -1f);
                if (gotoPostion < 0) gotoPostion = readerViewModel.getSavedScrollFraction();
                int y = (int) (gotoPostion * scrollView.getChildAt(0).getHeight());
                scrollView.scrollTo(0, y);
                int focusOffset = intent.getIntExtra("focusOffset", -1);
                if (focusOffset >= 0) scrollToTextOffset(focusOffset, true);
                restoredScrollY = scrollView.getScrollY();
                lastScrollY = restoredScrollY;
                updateReadingProgressIndicator();
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        restoredScrollY = scrollView.getScrollY();
                        lastScrollY = restoredScrollY;
                        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                            int scrollY = scrollView.getScrollY();
                            if (Math.abs(scrollY - restoredScrollY) > 120) {
                                didScroll = true;
                            }
                            updateReadingProgressIndicator();
                            handleReaderScroll(scrollY);
                        });
                    }
                });
            }
        });

    }

    private void setupReaderChrome() {
        chromeTapDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleReaderChrome();
                return true;
            }
        });
        View.OnTouchListener tapListener = (v, event) -> {
            chromeTapDetector.onTouchEvent(event);
            return false;
        };
        scrollView.setOnTouchListener(tapListener);
        pageContent.setOnTouchListener(tapListener);
        titleTextView.setOnTouchListener(tapListener);
        textView.setOnTouchListener(tapListener);

        readerChrome.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (height <= 0 || height == chromeTopPadding) {
                return;
            }
            chromeTopPadding = height;
            chromeLaidOut = true;
            if (chromeVisible) {
                applyScrollTopPadding(height, false);
            }
        });
        readerChrome.post(() -> {
            if (readerChrome.getHeight() > 0) {
                chromeTopPadding = readerChrome.getHeight();
                chromeLaidOut = true;
                if (chromeVisible) {
                    applyScrollTopPadding(chromeTopPadding, false);
                }
            }
        });
    }

    private void handleReaderScroll(int scrollY) {
        if (suppressChromeScroll) {
            lastScrollY = scrollY;
            return;
        }
        int dy = scrollY - lastScrollY;
        lastScrollY = scrollY;
        if (Math.abs(dy) < CHROME_SCROLL_THRESHOLD_PX) {
            return;
        }
        if (dy > 0) {
            setReaderChromeVisible(false, true);
        } else {
            setReaderChromeVisible(true, true);
        }
    }

    private void toggleReaderChrome() {
        setReaderChromeVisible(!chromeVisible, true);
    }

    private void setReaderChromeVisible(boolean visible, boolean animate) {
        if (chromeVisible == visible) {
            return;
        }
        if (!chromeLaidOut || readerChrome.getHeight() <= 0) {
            readerChrome.post(() -> setReaderChromeVisible(visible, animate));
            return;
        }
        chromeVisible = visible;
        int chromeHeight = readerChrome.getHeight();
        chromeTopPadding = chromeHeight;
        int hiddenPadding = statusBarInset();
        int targetPadding = visible ? chromeHeight : hiddenPadding;
        float targetTranslation = visible ? 0f : -chromeHeight;

        if (!animate || !BookMotion.animationsEnabled(this)) {
            readerChrome.animate().cancel();
            if (chromePaddingAnimator != null) {
                chromePaddingAnimator.cancel();
            }
            readerChrome.setTranslationY(targetTranslation);
            readerChrome.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            applyScrollTopPadding(targetPadding, true);
            return;
        }

        readerChrome.setVisibility(View.VISIBLE);
        readerChrome.animate().cancel();
        readerChrome.animate()
                .translationY(targetTranslation)
                .setDuration(CHROME_ANIM_MS)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        readerChrome.setVisibility(chromeVisible ? View.VISIBLE : View.INVISIBLE);
                        readerChrome.animate().setListener(null);
                    }
                })
                .start();
        applyScrollTopPadding(targetPadding, true);
    }

    private void applyScrollTopPadding(int targetTop, boolean compensateScroll) {
        int currentTop = scrollView.getPaddingTop();
        if (currentTop == targetTop) {
            return;
        }
        if (chromePaddingAnimator != null) {
            chromePaddingAnimator.cancel();
        }
        if (!compensateScroll || !BookMotion.animationsEnabled(this)) {
            int delta = targetTop - currentTop;
            suppressChromeScroll = true;
            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    targetTop,
                    scrollView.getPaddingRight(),
                    scrollView.getPaddingBottom());
            if (compensateScroll && delta != 0) {
                scrollView.scrollBy(0, delta);
            }
            lastScrollY = scrollView.getScrollY();
            suppressChromeScroll = false;
            return;
        }

        final int startTop = currentTop;
        suppressChromeScroll = true;
        chromePaddingAnimator = ValueAnimator.ofInt(startTop, targetTop);
        chromePaddingAnimator.setDuration(CHROME_ANIM_MS);
        chromePaddingAnimator.setInterpolator(new DecelerateInterpolator(1.6f));
        chromePaddingAnimator.addUpdateListener(animation -> {
            int nextTop = (int) animation.getAnimatedValue();
            int previousTop = scrollView.getPaddingTop();
            if (nextTop == previousTop) {
                return;
            }
            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    nextTop,
                    scrollView.getPaddingRight(),
                    scrollView.getPaddingBottom());
            scrollView.scrollBy(0, nextTop - previousTop);
            lastScrollY = scrollView.getScrollY();
        });
        chromePaddingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                suppressChromeScroll = false;
                lastScrollY = scrollView.getScrollY();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                suppressChromeScroll = false;
                lastScrollY = scrollView.getScrollY();
            }
        });
        chromePaddingAnimator.start();
    }

    private int statusBarInset() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(scrollView);
        if (insets != null) {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            if (bars.top > 0) {
                return bars.top;
            }
        }
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return Math.round(24f * getResources().getDisplayMetrics().density);
    }

    private void openAdjacentChapter(boolean previous) {
        if (chapterTransitionRunning) {
            return;
        }
        saveReadingProgress();
        stopSpeaking();
        BookChapter adjacent = previous
                ? readerViewModel.goToPreviousChapter()
                : readerViewModel.goToNextChapter();
        if (adjacent == null) {
            return;
        }
        chapterTransitionRunning = true;
        BookMotion.swapPage(pageContent, previous, () -> {
            bookChapter = adjacent;
            chapterIndex = readerViewModel.getChapterIndex();
            searchFocusOffset = -1;
            setTitle(bookChapter.getChapterNumberName());
            titleTextView.setText(bookChapter.getChapterName());
            renderChapterText();
            updateChapterProgressLabel();
            didScroll = false;
            setReaderChromeVisible(true, false);
            scrollView.post(() -> {
                suppressChromeScroll = true;
                scrollView.scrollTo(0, 0);
                restoredScrollY = 0;
                lastScrollY = 0;
                updateReadingProgressIndicator();
                suppressChromeScroll = false;
            });
            LocalBroadcastHelper.sendContinueReadingChanged(this);
            pageContent.announceForAccessibility(bookChapter.getChapterName());
        });
        pageContent.postDelayed(
                () -> chapterTransitionRunning = false,
                BookMotion.animationsEnabled(this) ? 380L : 0L
        );
    }

    private void tintChapterNavButtons() {
        int accent = MyColor.getAccentColor(mContext);
        int surface = MyColor.getElevatedSurfaceColor(mContext);
        int ripple = MyColor.getSeparatorColor(mContext);
        prevChapterButton.setTextColor(accent);
        nextChapterButton.setTextColor(accent);
        if (prevChapterButton instanceof MaterialButton) {
            MaterialButton previous = (MaterialButton) prevChapterButton;
            previous.setStrokeColor(ColorStateList.valueOf(accent));
            previous.setBackgroundTintList(ColorStateList.valueOf(surface));
            previous.setRippleColor(ColorStateList.valueOf(ripple));
        }
        if (nextChapterButton instanceof MaterialButton) {
            MaterialButton next = (MaterialButton) nextChapterButton;
            next.setStrokeColor(ColorStateList.valueOf(accent));
            next.setBackgroundTintList(ColorStateList.valueOf(surface));
            next.setRippleColor(ColorStateList.valueOf(ripple));
        }
        if (chapterProgressLabel != null) {
            chapterProgressLabel.setTextColor(MyColor.getDetailTextColor(mContext));
        }
        if (readingProgress != null) {
            readingProgress.setIndicatorColor(accent);
            readingProgress.setTrackColor(MyColor.getSeparatorColor(mContext));
        }
    }

    private void updateChapterProgressLabel() {
        updateChapterProgressLabel(currentChapterScrollPercent());
    }

    private void updateChapterProgressLabel(int chapterPercent) {
        if (chapterProgressLabel == null) {
            return;
        }
        if (chapterIndex >= 0 && totalChapters > 0) {
            chapterProgressLabel.setVisibility(View.VISIBLE);
            chapterProgressLabel.setText(getString(
                    R.string.chapter_progress_with_percent,
                    chapterPercent,
                    chapterIndex + 1,
                    totalChapters));
        } else {
            chapterProgressLabel.setVisibility(View.GONE);
        }
    }

    private int currentChapterScrollPercent() {
        if (scrollView == null || scrollView.getChildCount() == 0) {
            return 0;
        }
        int maxScroll = Math.max(0, scrollView.getChildAt(0).getHeight() - scrollView.getHeight());
        if (maxScroll == 0) {
            return 100;
        }
        return Math.max(0, Math.min(100,
                Math.round((scrollView.getScrollY() / (float) maxScroll) * 100f)));
    }

    private void updateReadingProgressIndicator() {
        if (readingProgress == null) {
            return;
        }
        int percent = currentChapterScrollPercent();
        readingProgress.setProgressCompat(percent, false);
        readingProgress.setContentDescription(
                getString(R.string.chapter_percent_of_chapter, percent));
        updateChapterProgressLabel(percent);
    }

    private CharSequence formatChapterText(String chapterText) {
        if (chapterText == null || chapterText.isEmpty()) {
            return "";
        }
        SpannableString styled = new SpannableString(chapterText);
        int firstLetter = -1;
        for (int i = 0; i < chapterText.length(); i++) {
            if (Character.isLetter(chapterText.charAt(i))) {
                firstLetter = i;
                break;
            }
        }
        if (firstLetter >= 0) {
            int end = firstLetter + 1;
            styled.setSpan(new RelativeSizeSpan(1.75f), firstLetter, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new StyleSpan(Typeface.BOLD), firstLetter, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new ForegroundColorSpan(MyColor.getAccentColor(this)), firstLetter, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return styled;
    }

    @Override
    protected void onPause() {
        saveReadingProgress();
        if (speechController != null && speechController.isPlaying()) speechController.pause();
        super.onPause();
    }

    private void saveReadingProgress() {
        if (book == null || bookChapter == null || scrollView == null) {
            return;
        }
        if (scrollView.getChildCount() == 0 || scrollView.getChildAt(0).getHeight() == 0) {
            return;
        }
        readerViewModel.saveReadingProgress(scrollFraction());
    }

    private void handleReaderBack() {
        if (leavingWithAd) {
            return;
        }
        stopSpeaking();
        leavingWithAd = AdCoordinator.get(this).maybeShowInterstitialAfterReading(
                this,
                readingStartedAt,
                didScroll,
                this::completeBackNavigation);
        if (!leavingWithAd) {
            completeBackNavigation();
        }
    }

    private void completeBackNavigation() {
        if (backPressedCallback != null) {
            backPressedCallback.setEnabled(false);
        }
        getOnBackPressedDispatcher().onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(backgroundReceiver);
        stopSpeaking();
        if (marks != null) marks.close();
        super.onDestroy();
    }

    private final BroadcastReceiver backgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (titleTextView == null || textView == null) {
                return;
            }
            titleTextView.setTextColor(MyColor.getTitleTextColor(mContext));
            textView.setTextColor(MyColor.getTitleTextColor(mContext));
            configColor();
            invalidateOptionsMenu();
        }
    };

    private void stopSpeaking() {
        if (speechController != null) speechController.stop();
    }

    private void configColor() {
        if (scrollView == null || bookChapter == null) {
            // onCreate bailed out early (no chapter in the intent).
            return;
        }
        scrollView.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, scrollView);

        ScreenChrome.tint(this);
        tintChapterNavButtons();
        renderChapterText();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.nav_book_pager_more) {
            showReaderOptions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showReaderOptions() {
        ReaderOptionsSheet.showMenu(this, new ReaderOptionsSheet.MenuCallbacks() {
            @Override
            public void onFontSizeSelected() {
                showFontSizeSheet();
            }

            @Override
            public void onBackground() {
                startActivity(new Intent(BookPagerActivity.this, ImagesActivity.class));
            }

            @Override
            public void onShare() {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                String str = bookChapter.getChapterNumberName() + "\n\n"
                        + bookChapter.getChapterName() + "\n\n"
                        + bookChapter.getText();
                sendIntent.putExtra(Intent.EXTRA_TEXT, str);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, null));
            }

            @Override
            public void onSpeechControls() {
                showSpeechControls();
            }

            @Override public void onSearch() {
                startActivity(new Intent(BookPagerActivity.this, ReaderCollectionActivity.class));
            }
            @Override public void onBookmark() { bookmarkPosition(); }
            @Override public void onSavedPassages() {
                startActivity(new Intent(BookPagerActivity.this, ReaderCollectionActivity.class)
                        .putExtra(ReaderCollectionActivity.EXTRA_MARKS, true));
            }
            @Override public void onSwitchScript() { switchScript(); }
        });
    }

    private void showFontSizeSheet() {
        ReaderOptionsSheet.showFontSize(this, new ReaderOptionsSheet.FontSizeCallbacks() {
            @Override
            public int currentFontSize() {
                Integer size = readerViewModel.getTextSize().getValue();
                return size != null ? size : 18;
            }

            @Override
            public void onFontSizeChanged(int size) {
                readerViewModel.setTextSize(size);
                textView.setTextSize(size);
            }
        });
    }

    private float scrollFraction() {
        if (scrollView.getChildCount() == 0) {
            return 0f;
        }
        int height = scrollView.getChildAt(0).getHeight();
        if (height <= 0) {
            return 0f;
        }
        return (float) scrollView.getScrollY() / height;
    }

    private void renderChapterText() {
        if (textView == null || bookChapter == null) return;
        SpannableStringBuilder styled = new SpannableStringBuilder(formatChapterText(bookChapter.getText()));
        if (marks != null && chapterIndex >= 0) {
            for (ReaderMarks.Mark mark : marks.forChapter(book,
                    BookStore.shared.isTraditional(book), chapterIndex)) {
                if (ReaderMarks.HIGHLIGHT.equals(mark.type) && mark.start >= 0
                        && mark.end <= styled.length() && mark.start < mark.end) {
                    styled.setSpan(new BackgroundColorSpan(0x66D8A346), mark.start, mark.end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        if (searchFocusOffset >= 0 && searchFocusLength > 0
                && searchFocusOffset + searchFocusLength <= styled.length()) {
            styled.setSpan(new BackgroundColorSpan(0x88D8A346), searchFocusOffset,
                    searchFocusOffset + searchFocusLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(styled, TextView.BufferType.SPANNABLE);
        speechHighlight = null;
    }

    private void setupSelectionActions() {
        textView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(0, ACTION_HIGHLIGHT, 10, R.string.highlight_passage);
                menu.add(0, ACTION_NOTE, 11, R.string.add_note);
                return true;
            }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() != ACTION_HIGHLIGHT && item.getItemId() != ACTION_NOTE) return false;
                int start = Math.min(textView.getSelectionStart(), textView.getSelectionEnd());
                int end = Math.max(textView.getSelectionStart(), textView.getSelectionEnd());
                if (start < 0 || end <= start || end > bookChapter.getText().length()) return true;
                String quote = bookChapter.getText().substring(start, Math.min(end, start + 500))
                        + (end - start > 500 ? "…" : "");
                if (item.getItemId() == ACTION_NOTE) {
                    EditText note = new EditText(BookPagerActivity.this);
                    note.setMinLines(3);
                    note.setHint(R.string.note_hint);
                    new AlertDialog.Builder(BookPagerActivity.this).setTitle(R.string.add_note)
                            .setView(note).setNegativeButton(R.string.Cancel, null)
                            .setPositiveButton(R.string.OK, (d, w) -> saveHighlight(start, end, quote,
                                    note.getText().toString().trim())).show();
                } else saveHighlight(start, end, quote, "");
                mode.finish();
                return true;
            }
            @Override public void onDestroyActionMode(ActionMode mode) {}
        });
    }

    private void saveHighlight(int start, int end, String quote, String note) {
        marks.add(book, BookStore.shared.isTraditional(book), chapterIndex,
                start, end, scrollFraction(), ReaderMarks.HIGHLIGHT, quote, note);
        renderChapterText();
        Toast.makeText(this, R.string.passage_saved, Toast.LENGTH_SHORT).show();
    }

    private void bookmarkPosition() {
        marks.add(book, BookStore.shared.isTraditional(book), chapterIndex,
                -1, -1, scrollFraction(), ReaderMarks.BOOKMARK, "", "");
        Toast.makeText(this, R.string.bookmark_saved, Toast.LENGTH_SHORT).show();
    }

    private void switchScript() {
        if (chapterTransitionRunning) return;
        boolean targetTraditional = !BookStore.shared.isTraditional(book);
        Book target = null;
        for (Book variant : BookStore.shared.variantsForId(book.getId())) {
            if (BookStore.shared.isTraditional(variant) == targetTraditional) target = variant;
        }
        if (target == null) {
            Toast.makeText(this, R.string.chapter_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        saveReadingProgress();
        chapterTransitionRunning = true;
        Book finalTarget = target;
        int mapped = BookStore.shared.chapterIndexFor(target, chapterIndex,
                BookStore.shared.isTraditional(book));
        float fraction = scrollFraction();
        new Thread(() -> {
            try {
                List<BookChapter> chapters = ReaderContent.load(getApplicationContext(), finalTarget);
                runOnUiThread(() -> {
                    chapterTransitionRunning = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (mapped < 0 || mapped >= chapters.size()) {
                        Toast.makeText(this, R.string.chapter_unavailable, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    BookStore.shared.setTraditional(this, targetTraditional);
                    stopSpeaking();
                    Intent intent = new Intent(this, BookPagerActivity.class);
                    intent.putExtra("book", finalTarget);
                    intent.putExtra("bookChapter", chapters.get(mapped));
                    intent.putExtra("chapterIndex", mapped);
                    intent.putExtra("totalChapters", chapters.size());
                    intent.putExtra("focusFraction", fraction);
                    startActivity(intent);
                    finish();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    chapterTransitionRunning = false;
                    Toast.makeText(this, R.string.chapter_unavailable, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private int visibleTextOffset() {
        Layout layout = textView.getLayout();
        if (layout == null) return 0;
        int y = Math.max(0, scrollView.getScrollY() - textView.getTop());
        return layout.getLineStart(layout.getLineForVertical(y));
    }

    private void scrollToTextOffset(int offset, boolean force) {
        Layout layout = textView.getLayout();
        if (layout == null || textView.length() == 0) return;
        int safe = Math.max(0, Math.min(offset, textView.length() - 1));
        int y = textView.getTop() + layout.getLineTop(layout.getLineForOffset(safe));
        if (force || y < scrollView.getScrollY() || y > scrollView.getScrollY() + scrollView.getHeight() - dp(80)) {
            scrollView.scrollTo(0, Math.max(0, y - dp(100)));
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void showSpokenRange(int start, int end) {
        if (textView == null || !(textView.getText() instanceof Spannable)) return;
        Spannable styled = (Spannable) textView.getText();
        if (speechHighlight != null) styled.removeSpan(speechHighlight);
        speechHighlight = null;
        if (start >= 0 && end > start && end <= styled.length()) {
            speechHighlight = new BackgroundColorSpan(0x775FAEA9);
            styled.setSpan(speechHighlight, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            scrollToTextOffset(start, false);
        }
    }

    private void showSpeechControls() {
        BottomSheetDialog dialog = DialogChrome.bottomSheet(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(24));
        TextView title = new TextView(this);
        title.setText(R.string.speech_controls);
        title.setTextSize(20);
        title.setTextColor(MyColor.getTitleTextColor(this));
        content.addView(title);
        MaterialButton play = new MaterialButton(this);
        speechPlayButton = play;
        play.setText(speechController.isPlaying() ? R.string.pause_reading :
                speechController.hasSession() ? R.string.resume_reading : R.string.Begin_Reading);
        content.addView(play);
        play.setOnClickListener(v -> {
            if (speechController.isPlaying()) speechController.pause();
            else if (speechController.hasSession()) speechController.resume();
            else speechController.start(bookChapter.getText(), BookStore.shared.isTraditional(book),
                        visibleTextOffset());
            play.setText(speechController.isPlaying() ? R.string.pause_reading : R.string.resume_reading);
        });
        MaterialButton stop = new MaterialButton(this);
        stop.setText(R.string.Stop_Reading);
        content.addView(stop);
        stop.setOnClickListener(v -> { stopSpeaking(); play.setText(R.string.Begin_Reading); });
        TextView speedLabel = new TextView(this);
        speedLabel.setText(getString(R.string.speech_speed, speechController.speed()));
        speedLabel.setTextColor(MyColor.getTitleTextColor(this));
        content.addView(speedLabel);
        Slider speed = new Slider(this);
        speed.setValueFrom(0.5f);
        speed.setValueTo(2f);
        speed.setStepSize(0.25f);
        speed.setValue(speechController.speed());
        speed.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) {
                speechController.setSpeed(value);
                speedLabel.setText(getString(R.string.speech_speed, value));
            }
        });
        content.addView(speed);
        MaterialButton voice = new MaterialButton(this);
        voice.setText(R.string.choose_voice);
        content.addView(voice);
        voice.setOnClickListener(v -> {
            if (!speechController.isReady()) {
                Toast.makeText(this, R.string.start_reading_for_voices, Toast.LENGTH_SHORT).show();
                return;
            }
            List<Voice> voices = speechController.voices();
            if (voices.isEmpty()) {
                Toast.makeText(this, R.string.no_offline_voices, Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[voices.size()];
            for (int i = 0; i < voices.size(); i++) {
                Voice candidate = voices.get(i);
                names[i] = candidate.getName()
                        + (candidate.isNetworkConnectionRequired()
                        ? " · " + getString(R.string.online_voice) : "");
            }
            new AlertDialog.Builder(this).setTitle(R.string.choose_voice).setItems(names,
                    (d, which) -> speechController.setVoice(voices.get(which))).show();
        });
        DialogChrome.prepareSheet(dialog, content, this);
        dialog.setOnDismissListener(d -> speechPlayButton = null);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_pager_menu, menu);

        menu.getItem(0).setIcon(MyImage.changeDrawableColor(this, R.drawable.ic_tab_more, MyColor.getButtonTintColor(this)));

        return super.onCreateOptionsMenu(menu);
    }
}
