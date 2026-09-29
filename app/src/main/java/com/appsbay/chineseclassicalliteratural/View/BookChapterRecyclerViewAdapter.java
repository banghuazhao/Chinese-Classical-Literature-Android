package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Controller.BookPagerActivity;
import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookChapter;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.BookDetailHeader;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ReadingProgressHelper;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class BookChapterRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHAPTER = 1;

    private static final Pattern CHAPTER_LABEL_PREFIX = Pattern.compile(
            "^\\s*(?:chapters?|chap\\.?|parts?|books?|letters?|sections?)\\s*[.:]?\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,:;]+$");

    Context context;
    ArrayList<BookChapter> bookChapters;
    Book book;

    public BookChapterRecyclerViewAdapter(Context context, ArrayList<BookChapter> bookChapters, Book book) {
        this.context = context;
        this.bookChapters = bookChapters;
        this.book = book;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_CHAPTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_book_detail, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_book_chapter, parent, false);
        BookMotion.attachPressEffect(view);
        return new BookChapterRecyclerViewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            BookDetailHeader.bind(holder.itemView, book, R.string.Contents, v -> continueReading());
            // Nothing to open until the chapters are loaded (or downloaded).
            View readButton = holder.itemView.findViewById(R.id.book_detail_continue);
            if (readButton != null) {
                boolean hasChapters = !bookChapters.isEmpty();
                readButton.setEnabled(hasChapters);
                readButton.setAlpha(hasChapters ? 1f : 0.4f);
            }
            return;
        }

        int chapterIndex = position - 1;
        BookChapter bookChapter = bookChapters.get(chapterIndex);
        BookChapterRecyclerViewViewHolder chapterHolder = (BookChapterRecyclerViewViewHolder) holder;

        chapterHolder.bookNumber.setText(chapterBadgeLabel(bookChapter, chapterIndex));
        chapterHolder.bookNumber.setTextColor(MyColor.getAccentColor(context));
        if (chapterHolder.bookNumber.getBackground() != null) {
            chapterHolder.bookNumber.getBackground().mutate()
                    .setTint(MyColor.getAccentSurfaceColor(context));
        }
        chapterHolder.bookName.setText(bookChapter.getChapterName());
        chapterHolder.bookName.setTextColor(MyColor.getTitleTextColor(context));
        if (chapterHolder.separator != null) {
            chapterHolder.separator.setBackgroundColor(MyColor.getSeparatorColor(context));
        }

        SharedPreferences preferences = context.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE);
        int bookmarkNumber = preferences.getInt(book.getName(), 0);
        if (chapterIndex == bookmarkNumber) {
            chapterHolder.bookmark.setImageDrawable(MyImage.changeDrawableColor(context, R.drawable.nav_bookmark, MyColor.getAccentColor(context)));
        } else {
            chapterHolder.bookmark.setImageDrawable(null);
        }

        chapterHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChapter(chapterIndex);
            }
        });
    }

    /**
     * The index badge is only 72dp wide, so "CHAPTER XVIII" would ellipsize to
     * "CHAPTE...". Strip the redundant leading word and keep just the numeral,
     * falling back to the running position when the label is not a numeral.
     */
    private static String chapterBadgeLabel(BookChapter chapter, int chapterIndex) {
        String position = String.valueOf(chapterIndex + 1);
        String label = chapter.getChapterNumberName();
        if (label == null || label.equals(chapter.getChapterName())) {
            return position;
        }
        String numeral = CHAPTER_LABEL_PREFIX.matcher(label).replaceFirst("").trim();
        numeral = TRAILING_PUNCTUATION.matcher(numeral).replaceAll("");
        if (numeral.isEmpty() || numeral.length() > 7) {
            return position;
        }
        return numeral;
    }

    void continueReading() {
        if (bookChapters.isEmpty()) {
            return;
        }
        int chapterIndex = ReadingProgressHelper.getChapterIndex(context, book.getName());
        if (chapterIndex < 0 || chapterIndex >= bookChapters.size()) {
            chapterIndex = 0;
        }
        openChapter(chapterIndex);
    }

    private void openChapter(int chapterIndex) {
        if (chapterIndex < 0 || chapterIndex >= bookChapters.size()) {
            return;
        }
        BookChapter bookChapter = bookChapters.get(chapterIndex);
        ReadingProgressHelper.markChapterOpened(context, book,
                chapterIndex,
                bookChapter.getChapterNumberName(),
                bookChapter.getChapterName(),
                bookChapters.size());
        LocalBroadcastHelper.sendContinueReadingChanged(context);
        notifyDataSetChanged();

        Intent intent = new Intent(context, BookPagerActivity.class);
        intent.putExtra("bookChapter", bookChapter);
        intent.putExtra("book", book);
        intent.putExtra("chapterIndex", chapterIndex);
        intent.putExtra("totalChapters", bookChapters.size());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return bookChapters.size() + 1;
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class BookChapterRecyclerViewViewHolder extends RecyclerView.ViewHolder {

        TextView bookNumber;
        TextView bookName;
        ImageView bookmark;
        View separator;

        public BookChapterRecyclerViewViewHolder(@NonNull View itemView) {
            super(itemView);
            bookNumber = itemView.findViewById(R.id.book_chapter_number);
            bookName = itemView.findViewById(R.id.book_chapter_name);
            bookmark = itemView.findViewById(R.id.book_chapter_bookmark);
            separator = itemView.findViewById(R.id.row_book_chapter_separator);
        }
    }

}
