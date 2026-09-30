package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.LiteraryCopy;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.data.LibraryRepository;
import com.appsbay.chineseclassicalliteratural.Tools.BookDetailHeader;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.ReadingProgressHelper;
import com.appsbay.chineseclassicalliteratural.Tools.SearchHighlight;
import com.sackcentury.shinebuttonlib.ShineButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BooksListRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    public interface OnFilterPublishedListener {
        void onFilterPublished(int count);
    }

    private static final int TYPE_COLLECTION_HEADER = 0;
    private static final int TYPE_BOOK = 1;

    Context context;
    public ArrayList<Book> books;
    private List<Book> filteredBooks;
    private final Book collectionBook;
    String searchString;
    private OnFilterPublishedListener filterPublishedListener;

    public BooksListRecyclerViewAdapter(Context context, ArrayList<Book> books) {
        this(context, books, null);
    }

    public BooksListRecyclerViewAdapter(Context context, ArrayList<Book> books, Book collectionBook) {
        this.context = context;
        this.books = books != null ? books : new ArrayList<Book>();
        this.collectionBook = collectionBook;
        filteredBooks = new ArrayList<>(this.books);
    }

    public void setOnFilterPublishedListener(OnFilterPublishedListener listener) {
        this.filterPublishedListener = listener;
    }

    private int headerCount() {
        return showingHeader() ? 1 : 0;
    }

    private boolean showingHeader() {
        return collectionBook != null && (searchString == null || searchString.isEmpty());
    }

    @Override
    public int getItemViewType(int position) {
        if (showingHeader() && position == 0) {
            return TYPE_COLLECTION_HEADER;
        }
        return TYPE_BOOK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_COLLECTION_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_book_detail, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_book, parent, false);
        BookMotion.attachPressEffect(view);
        return new BooksListRecyclerViewAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder rawHolder, int position) {
        if (rawHolder instanceof HeaderViewHolder) {
            BookDetailHeader.bind(rawHolder.itemView, collectionBook, R.string.Volumes);
            return;
        }

        BooksListRecyclerViewAdapterViewHolder holder = (BooksListRecyclerViewAdapterViewHolder) rawHolder;
        Book book = filteredBooks.get(position - headerCount());

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
            // get input stream
            InputStream ims = context.getAssets().open("covers/" + book.getBookCover() + ".png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            holder.bookImage.setImageDrawable(d);
            ims.close();
        } catch (IOException ex) {
            holder.bookImage.setImageResource(R.drawable.cover_placeholder);
        }

        holder.bookImage.setContentDescription(book.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookOpener.open(context, book);
            }
        });

        holder.itemView.setOnLongClickListener(null);

        boolean saved = LibraryRepository.getInstance(holder.itemView.getContext()).isFavorite(book);
        holder.shineButton.setClickable(false);
        holder.shineButton.setFocusable(false);
        holder.shineButton.setChecked(saved);
        holder.likeHost.setContentDescription(holder.itemView.getContext().getString(
                saved ? R.string.remove_from_library : R.string.save_to_library) + ": " + book.getName());
        holder.likeHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !holder.shineButton.isChecked();
                holder.shineButton.setChecked(next, true);
                holder.likeHost.setContentDescription(holder.itemView.getContext().getString(
                        next ? R.string.remove_from_library : R.string.save_to_library) + ": " + book.getName());
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
            }
        });
    }

    public int getFilteredCount() {
        return filteredBooks.size();
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

    @Override
    public int getItemCount() {
        return filteredBooks.size() + headerCount();
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

    private Filter booksFilter = new Filter() {
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

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class BooksListRecyclerViewAdapterViewHolder extends RecyclerView.ViewHolder {

        TextView bookName;
        TextView bookAuthor;
        TextView matchReason;
        TextView progressText;
        TextView offlineBadge;
        ProgressBar progressBar;
        ImageView bookImage;
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
            separator = itemView.findViewById(R.id.books_list_separator);
            shineButton = itemView.findViewById(R.id.like);
            likeHost = itemView.findViewById(R.id.like_host);
        }
    }
}
