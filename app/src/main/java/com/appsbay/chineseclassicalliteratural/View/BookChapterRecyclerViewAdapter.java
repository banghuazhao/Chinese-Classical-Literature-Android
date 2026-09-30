package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.content.Intent;
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

public class BookChapterRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHAPTER = 1;

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

        chapterHolder.bookNumber.setText(String.valueOf(chapterIndex + 1));
        chapterHolder.bookNumber.setTextColor(MyColor.getAccentColor(context));
        if (chapterHolder.bookNumber.getBackground() != null) {
            chapterHolder.bookNumber.getBackground().mutate()
                    .setTint(MyColor.getAccentSurfaceColor(context));
        }
        chapterHolder.bookName.setText(bookChapter.getChapterName());
        chapterHolder.itemView.setContentDescription(
                context.getString(R.string.reading_chapter_only, chapterIndex + 1)
                        + ": " + bookChapter.getChapterName());
        chapterHolder.bookName.setTextColor(MyColor.getTitleTextColor(context));
        if (chapterHolder.separator != null) {
            chapterHolder.separator.setBackgroundColor(MyColor.getSeparatorColor(context));
        }

        int bookmarkNumber = ReadingProgressHelper.getChapterIndex(context, book);
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

    void continueReading() {
        if (bookChapters.isEmpty()) {
            return;
        }
        int chapterIndex = ReadingProgressHelper.getChapterIndex(context, book);
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
