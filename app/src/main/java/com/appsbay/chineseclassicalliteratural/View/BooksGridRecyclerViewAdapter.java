package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchHighlight;
import com.appsbay.chineseclassicalliteratural.data.LibraryRepository;
import com.sackcentury.shinebuttonlib.ShineButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BooksGridRecyclerViewAdapter extends RecyclerView.Adapter<BooksGridRecyclerViewAdapter.GridViewHolder>
        implements Filterable {

    public interface OnFilterPublishedListener {
        void onFilterPublished(int count);
    }

    private final Context context;
    private ArrayList<Book> books;
    private List<Book> filteredBooks;
    private String searchString = "";
    private OnFilterPublishedListener filterPublishedListener;

    public BooksGridRecyclerViewAdapter(Context context, ArrayList<Book> books) {
        this.context = context;
        this.books = books != null ? books : new ArrayList<>();
        this.filteredBooks = new ArrayList<>(this.books);
    }

    public void setOnFilterPublishedListener(OnFilterPublishedListener listener) {
        this.filterPublishedListener = listener;
    }

    public void replaceBooks(ArrayList<Book> newBooks) {
        this.books = newBooks != null ? newBooks : new ArrayList<>();
        this.filteredBooks = new ArrayList<>(this.books);
        notifyDataSetChanged();
        notifyFilterPublished();
    }

    public void replaceBooks(ArrayList<Book> newBooks, String query) {
        this.books = newBooks != null ? newBooks : new ArrayList<>();
        this.searchString = query == null ? "" : query.trim();
        this.filteredBooks = applySearch(this.books, searchString);
        notifyDataSetChanged();
        notifyFilterPublished();
    }

    public void setSearchQuery(String query) {
        this.searchString = query == null ? "" : query.trim();
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return filteredBooks.size();
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_grid, parent, false);
        BookMotion.attachPressEffect(view);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Book book = filteredBooks.get(position);
        SearchHighlight.bindTitleOnly(context, book, searchString, holder.bookName, holder.matchReason);

        holder.bookImage.setContentDescription(book.getName());
        try {
            InputStream ims = context.getAssets().open("covers/" + book.getBookCover() + ".png");
            Drawable d = Drawable.createFromStream(ims, null);
            holder.bookImage.setImageDrawable(d);
            ims.close();
        } catch (IOException ex) {
            holder.bookImage.setImageResource(R.drawable.cover_placeholder);
        }

        holder.itemView.setOnClickListener(v -> BookOpener.open(context, book));

        holder.itemView.setOnLongClickListener(null);

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

    @Override
    public int getItemCount() {
        return filteredBooks.size();
    }

    @Override
    public Filter getFilter() {
        return booksFilter;
    }

    private List<Book> applySearch(List<Book> source, String query) {
        List<Book> filteredList = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(source);
            return filteredList;
        }
        String filterPattern = query.toLowerCase().trim();
        for (Book book : source) {
            if (LiteraryCopy.shared.matchesSearch(book, filterPattern)) {
                filteredList.add(book);
            }
        }
        return filteredList;
    }

    private void notifyFilterPublished() {
        if (filterPublishedListener != null) {
            filterPublishedListener.onFilterPublished(filteredBooks.size());
        }
    }

    private final Filter booksFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String query = constraint == null ? "" : constraint.toString().trim();
            List<Book> filteredList = applySearch(books, query);
            FilterResults results = new FilterResults();
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            searchString = constraint == null ? "" : constraint.toString().trim();
            filteredBooks.clear();
            if (results != null && results.values instanceof List) {
                //noinspection unchecked
                filteredBooks.addAll((List<Book>) results.values);
            }
            notifyDataSetChanged();
            notifyFilterPublished();
        }
    };

    static class GridViewHolder extends RecyclerView.ViewHolder {
        TextView bookName;
        TextView matchReason;
        ImageView bookImage;
        ShineButton shineButton;
        View likeHost;

        GridViewHolder(@NonNull View itemView) {
            super(itemView);
            bookName = itemView.findViewById(R.id.book_name);
            matchReason = itemView.findViewById(R.id.book_match_reason);
            bookImage = itemView.findViewById(R.id.book_image);
            shineButton = itemView.findViewById(R.id.like);
            likeHost = itemView.findViewById(R.id.like_host);
        }
    }
}
