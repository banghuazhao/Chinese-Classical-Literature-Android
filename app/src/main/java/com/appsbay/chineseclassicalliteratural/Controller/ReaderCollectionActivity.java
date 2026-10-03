package com.appsbay.chineseclassicalliteratural.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderContent;
import com.appsbay.chineseclassicalliteratural.Tools.ReaderMarks;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline corpus search and the reader's saved passages. */
public class ReaderCollectionActivity extends AppCompatActivity {
    public static final String EXTRA_MARKS = "marks";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private LinearLayout results;
    private ProgressBar progress;
    private TextView message;
    private ReaderMarks marks;
    private int generation;
    private boolean marksMode;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        marksMode = getIntent().getBooleanExtra(EXTRA_MARKS, false);
        marks = new ReaderMarks(this);
        setTitle(marksMode ? R.string.saved_passages : R.string.search_chapters);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(MyColor.getBackgroundColor(this));
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setId(R.id.toolbar);
        toolbar.setMinimumHeight(dp(56));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(12), dp(16), 0);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        if (!marksMode) {
            EditText search = new EditText(this);
            search.setId(R.id.reader_search_input);
            search.setSingleLine(true);
            search.setHint(R.string.search_chapters_hint);
            search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            body.addView(search, new LinearLayout.LayoutParams(-1, dp(56)));
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                    int next = ++generation;
                    String query = s.toString().trim();
                    main.postDelayed(() -> { if (next == generation) search(query, next); }, 300);
                }
                @Override public void afterTextChanged(Editable e) {}
            });
        }
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        body.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
        message = new TextView(this);
        message.setVisibility(View.GONE);
        message.setTextColor(MyColor.getDetailTextColor(this));
        message.setPadding(dp(8), dp(20), dp(8), dp(16));
        body.addView(message);
        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setId(R.id.reader_search_results);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        ScreenChrome.setup(this, root, root);
        if (!marksMode && state != null) {
            EditText search = findViewById(R.id.reader_search_input);
            search.setText(state.getString("query", ""));
        }
        if (marksMode) showMarks();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (!marksMode) {
            EditText search = findViewById(R.id.reader_search_input);
            state.putString("query", search.getText().toString());
        }
        super.onSaveInstanceState(state);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    @Override protected void onResume() { super.onResume(); if (marksMode && results != null) showMarks(); }
    @Override protected void onDestroy() {
        generation++;
        worker.shutdownNow();
        marks.close();
        super.onDestroy();
    }

    private void search(String query, int request) {
        results.removeAllViews();
        if (query.isEmpty()) {
            progress.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            return;
        }
        progress.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        message.setText(R.string.searching_chapters);
        List<Book> books = BookStore.shared.getBooks(this);
        worker.execute(() -> {
            List<ReaderContent.Match> found = ReaderContent.search(
                    getApplicationContext(), books, query, 150,
                    () -> request != generation || Thread.currentThread().isInterrupted());
            main.post(() -> {
                if (request != generation || isFinishing() || isDestroyed()) return;
                progress.setVisibility(View.GONE);
                message.setText(found.isEmpty() ? getString(R.string.no_chapter_matches)
                        : found.size() + (found.size() >= 150 ? "+" : "") + " · "
                        + getString(R.string.search_results_hint));
                for (ReaderContent.Match match : found) {
                    addResult(match.book.getName() + " · " + match.chapterName,
                            match.snippet,
                            () -> open(match.book, match.chapterIndex, match.offset, -1f, query.length()));
                }
            });
        });
    }

    private void showMarks() {
        results.removeAllViews();
        message.setVisibility(View.VISIBLE);
        List<ReaderMarks.Mark> saved = new ArrayList<>();
        for (ReaderMarks.Mark mark : marks.all()) {
            if (BookStore.shared.isAvailableId(this, mark.bookId)) saved.add(mark);
        }
        message.setText(saved.isEmpty() ? R.string.no_saved_passages : R.string.saved_passages_hint);
        for (ReaderMarks.Mark mark : saved) {
            String detail = mark.quote.isEmpty() ? getString(R.string.position_bookmark)
                    : ReaderContent.snippet(mark.quote, 0, Math.min(80, mark.quote.length()));
            if (!mark.note.isEmpty()) detail += "\n" + mark.note;
            TextView row = addResult(mark.bookName + " · " + getString(R.string.chapter_number_short,
                            mark.chapterIndex + 1), detail, () -> openMark(mark));
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setItems(new String[]{getString(R.string.edit_note),
                        getString(R.string.delete_mark)}, (dialog, which) -> {
                    if (which == 0) editNote(mark); else new AlertDialog.Builder(this)
                            .setMessage(R.string.delete_mark_confirm)
                            .setNegativeButton(R.string.Cancel, null)
                            .setPositiveButton(R.string.delete_mark, (d, w) -> {
                                marks.delete(mark.id); showMarks();
                            }).show();
                }).show();
                return true;
            });
        }
    }

    private void editNote(ReaderMarks.Mark mark) {
        EditText input = new EditText(this);
        input.setText(mark.note);
        input.setMinLines(3);
        new AlertDialog.Builder(this).setTitle(R.string.edit_note).setView(input)
                .setNegativeButton(R.string.Cancel, null)
                .setPositiveButton(R.string.OK, (d, w) -> {
                    marks.updateNote(mark.id, input.getText().toString().trim()); showMarks();
                }).show();
    }

    private void openMark(ReaderMarks.Mark mark) {
        if (!BookStore.shared.isAvailableId(this, mark.bookId)) return;
        Book target = null;
        for (Book variant : BookStore.shared.variantsForId(mark.bookId)) {
            if (BookStore.shared.isTraditional(variant) == mark.traditional) target = variant;
        }
        if (target != null) open(target, mark.chapterIndex, mark.start, mark.scrollFraction, 0);
    }

    private void open(Book book, int index, int offset, float fraction, int length) {
        if (!BookStore.shared.isAvailable(this, book)) return;
        progress.setVisibility(View.VISIBLE);
        worker.execute(() -> {
            try {
                List<BookChapter> chapters = ReaderContent.load(getApplicationContext(), book);
                main.post(() -> {
                    progress.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;
                    if (index < 0 || index >= chapters.size()) {
                        Toast.makeText(this, R.string.chapter_unavailable, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(this, BookPagerActivity.class);
                    intent.putExtra("book", book);
                    intent.putExtra("bookChapter", chapters.get(index));
                    intent.putExtra("chapterIndex", index);
                    intent.putExtra("totalChapters", chapters.size());
                    intent.putExtra("focusOffset", offset);
                    intent.putExtra("focusLength", length);
                    intent.putExtra("focusFraction", fraction);
                    startActivity(intent);
                });
            } catch (IOException e) {
                main.post(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.chapter_unavailable, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private TextView addResult(String title, String detail, Runnable action) {
        TextView row = new TextView(this);
        row.setText(title + "\n" + detail);
        row.setTextSize(16);
        row.setLineSpacing(dp(4), 1f);
        row.setTextColor(MyColor.getTitleTextColor(this));
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(v -> action.run());
        results.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
