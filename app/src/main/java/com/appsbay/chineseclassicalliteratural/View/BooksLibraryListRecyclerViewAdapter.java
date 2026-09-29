package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.data.LibraryRepository;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.ReadingProgressHelper;
import com.appsbay.chineseclassicalliteratural.Tools.SearchHighlight;
import com.sackcentury.shinebuttonlib.ShineButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BooksLibraryListRecyclerViewAdapter extends RecyclerView.Adapter<BooksLibraryListRecyclerViewAdapter.BooksListRecyclerViewAdapterViewHolder> implements Filterable {

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    Context context;
    public ArrayList<Book> books;
    public List<Book> booksFull;
    String searchString;
    private final OnStartDragListener dragListener;
    private boolean dragEnabled = true;

    public BooksLibraryListRecyclerViewAdapter(Context context, ArrayList<Book> books) {
        this(context, books, null);
    }

    public BooksLibraryListRecyclerViewAdapter(Context context, ArrayList<Book> books,
                                               OnStartDragListener dragListener) {
        this.context = context;
        this.books = books;
        this.booksFull = new ArrayList<>(books);
        this.dragListener = dragListener;
    }

    public void setDragEnabled(boolean enabled) {
        if (this.dragEnabled == enabled) {
            return;
        }
        this.dragEnabled = enabled;
        notifyDataSetChanged();
    }

    public void syncBooksFull() {
        booksFull = new ArrayList<>(books);
    }

    @NonNull
    @Override
    public BooksListRecyclerViewAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_book, parent, false);
        BookMotion.attachPressEffect(view);
        return new BooksListRecyclerViewAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BooksListRecyclerViewAdapterViewHolder holder, int position) {
        Book book = books.get(position);

        SearchHighlight.bindListRow(
                context,
                book,
                searchString,
                holder.bookName,
                holder.bookAuthor,
                holder.matchReason
        );
        holder.separator.setBackgroundColor(MyColor.getSeparatorColor(context));

        ReadingProgressHelper.bindProgressRow(context, book,
                holder.progressText, holder.progressBar);
        ReadingProgressHelper.bindOfflineBadge(context, book, holder.offlineBadge);

        try {
            InputStream ims = context.getAssets().open("covers/" + book.getBookCover() + ".png");
            Drawable d = Drawable.createFromStream(ims, null);
            holder.bookImage.setImageDrawable(d);
            ims.close();
        } catch (IOException ex) {
            holder.bookImage.setImageResource(R.drawable.cover_placeholder);
        }

        holder.bookImage.setContentDescription(book.getName());

        holder.itemView.setOnClickListener(v -> BookOpener.open(context, book));

        holder.itemView.setOnLongClickListener(null);

        boolean showDrag = dragEnabled && dragListener != null;
        holder.dragHandle.setVisibility(showDrag ? View.VISIBLE : View.GONE);
        if (showDrag) {
            Drawable handle = holder.dragHandle.getDrawable();
            if (handle != null) {
                handle = DrawableCompat.wrap(handle.mutate());
                DrawableCompat.setTint(handle, MyColor.getButtonTintColor(context));
                holder.dragHandle.setImageDrawable(handle);
            }
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    dragListener.onStartDrag(holder);
                }
                return false;
            });
        } else {
            holder.dragHandle.setOnTouchListener(null);
        }

        boolean saved = LibraryRepository.getInstance(holder.itemView.getContext()).isFavorite(book);
        holder.shineButton.setClickable(false);
        holder.shineButton.setFocusable(false);
        holder.shineButton.setChecked(saved);
        holder.likeHost.setContentDescription(holder.itemView.getContext().getString(
                saved ? R.string.remove_from_library : R.string.save_to_library));
        holder.likeHost.setOnClickListener(v -> {
            boolean next = !holder.shineButton.isChecked();
            holder.shineButton.setChecked(next, true);
            holder.likeHost.setContentDescription(holder.itemView.getContext().getString(
                    next ? R.string.remove_from_library : R.string.save_to_library));
            LibraryRepository libraryRepository = LibraryRepository.getInstance(holder.itemView.getContext());
            if (next) {
                libraryRepository.addFavorite(book);

                String addedString = holder.itemView.getContext().getResources().getString(R.string.Library_added);
                    DialogChrome.snack(holder.itemView, addedString + ": " + book.getName());
            } else {
                libraryRepository.removeFavorite(book);

                String removedString = holder.itemView.getContext().getResources().getString(R.string.Library_removed);
                    DialogChrome.snack(holder.itemView, removedString + ": " + book.getName());
            }
        });
    }

    public void replaceBooks(ArrayList<Book> newBooks) {
        this.books = newBooks != null ? newBooks : new ArrayList<>();
        this.booksFull = new ArrayList<>(this.books);
        notifyDataSetChanged();
    }

    public void setSearchQuery(String query) {
        this.searchString = query == null ? "" : query.trim();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    @Override
    public Filter getFilter() {
        return booksFilter;
    }

    private final Filter booksFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Book> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(booksFull);
                searchString = "";
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                searchString = filterPattern;

                for (Book book : booksFull) {
                    if (LiteraryCopy.shared.matchesSearch(book, filterPattern)) {
                        filteredList.add(book);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            books.clear();
            books.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class BooksListRecyclerViewAdapterViewHolder extends RecyclerView.ViewHolder {

        TextView bookName;
        TextView bookAuthor;
        TextView matchReason;
        TextView progressText;
        TextView offlineBadge;
        ProgressBar progressBar;
        ImageView bookImage;
        ImageView dragHandle;
        View separator;
        ShineButton shineButton;
        View likeHost;

        public BooksListRecyclerViewAdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            bookName = itemView.findViewById(R.id.row_book_name);
            bookAuthor = itemView.findViewById(R.id.row_book_author);
            matchReason = itemView.findViewById(R.id.row_book_match_reason);
            progressText = itemView.findViewById(R.id.row_book_progress_text);
            offlineBadge = itemView.findViewById(R.id.row_book_offline_badge);
            progressBar = itemView.findViewById(R.id.row_book_progress);
            bookImage = itemView.findViewById(R.id.row_book_image_view);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            separator = itemView.findViewById(R.id.books_list_separator);
            shineButton = itemView.findViewById(R.id.like);
            likeHost = itemView.findViewById(R.id.like_host);
        }
    }
}
