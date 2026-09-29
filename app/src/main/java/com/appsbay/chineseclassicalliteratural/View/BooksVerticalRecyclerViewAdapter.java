package com.appsbay.chineseclassicalliteratural.View;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Controller.BooksListActivity;
import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookGenres;
import com.appsbay.chineseclassicalliteratural.Model.BookCategory;
import com.appsbay.chineseclassicalliteratural.Model.BookCategoryStore;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;

import java.util.ArrayList;
import java.util.List;

public class BooksVerticalRecyclerViewAdapter extends RecyclerView.Adapter<BooksVerticalRecyclerViewAdapter.CategoryViewHolder> {

    private final Context context;
    private final ArrayList<BookCategory> bookCategories;
    private String searchQuery = "";

    public BooksVerticalRecyclerViewAdapter(Context context, ArrayList<BookCategory> bookCategories) {
        this.context = context;
        this.bookCategories = bookCategories != null ? bookCategories : new ArrayList<>();
        setHasStableIds(true);
    }

    public void replaceCategories(List<BookCategory> categories) {
        bookCategories.clear();
        if (categories != null) {
            bookCategories.addAll(categories);
        }
        notifyDataSetChanged();
    }

    public void setSearchQuery(@Nullable String query) {
        String next = query == null ? "" : query.trim();
        if (next.equals(this.searchQuery)) {
            return;
        }
        this.searchQuery = next;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        String name = bookCategories.get(position).getCategoryName();
        return name == null ? position : name.hashCode();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_book_category, parent, false);
        CategoryViewHolder holder = new CategoryViewHolder(view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public boolean isAutoMeasureEnabled() {
                return true;
            }
        };
        layoutManager.setInitialPrefetchItemCount(4);
        layoutManager.setRecycleChildrenOnDetach(true);
        holder.booksRecyclerView.setLayoutManager(layoutManager);
        holder.booksRecyclerView.setNestedScrollingEnabled(false);
        // Do not share a RecycledViewPool across shelves: wrap_content items can keep
        // stale dimensions after search filters change item counts.
        holder.booksRecyclerView.setHasFixedSize(false);
        holder.booksRecyclerView.setWillNotDraw(false);
        holder.booksRecyclerView.setClipToPadding(false);
        holder.booksRecyclerView.setClipChildren(false);
        holder.booksRecyclerView.addItemDecoration(new ShelfBoardDecoration());
        holder.booksRecyclerView.addItemDecoration(new ShelfGapDecoration(
                context.getResources().getDimensionPixelSize(R.dimen.shelf_item_gap)));

        holder.horizontalAdapter = new BooksHorizontalRecyclerViewAdapter(context, new ArrayList<>());
        holder.booksRecyclerView.setAdapter(holder.horizontalAdapter);
        BookMotion.attachPressEffect(holder.moreAction);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder categoryHolder, int position) {
        BookCategory bookCategory = bookCategories.get(position);

        String title = bookCategory.getCategoryName();
        ArrayList<Book> books = bookCategory.getBooks();
        boolean searching = searchQuery != null && !searchQuery.isEmpty();

        String displayTitle = BookGenres.displayName(context, title);

        categoryHolder.chapterTitle.setText(displayTitle);
        categoryHolder.chapterTitle.setTextColor(MyColor.getTitleTextColor(context));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            categoryHolder.chapterTitle.setAccessibilityHeading(true);
        }

        int accent = MyColor.getAccentColor(context);
        categoryHolder.buttonMore.setTextColor(accent);
        categoryHolder.moreChevron.setColorFilter(accent, PorterDuff.Mode.SRC_IN);
        categoryHolder.buttonMore.setText(context.getString(searching ? R.string.see_matches : R.string.See_all));
        categoryHolder.moreAction.setContentDescription(context.getString(
                searching ? R.string.see_matches_for_category : R.string.see_all_for_category, displayTitle));
        categoryHolder.moreAction.setOnClickListener(v -> {
            Intent intent = new Intent(context, BooksListActivity.class);
            ArrayList<Book> toOpen = fullCategoryBooks(title);
            if (toOpen.isEmpty()) {
                toOpen = books != null ? new ArrayList<>(books) : new ArrayList<>();
            }
            intent.putParcelableArrayListExtra(BookOpener.EXTRA_BOOKS, toOpen);
            intent.putExtra(BookOpener.EXTRA_CATEGORY_NAME, title);
            if (searching) {
                intent.putExtra(BookOpener.EXTRA_SEARCH_QUERY, searchQuery);
            }
            context.startActivity(intent);
        });

        categoryHolder.horizontalAdapter.setBooks(books);
        categoryHolder.booksRecyclerView.stopScroll();
        categoryHolder.booksRecyclerView.scrollToPosition(0);
        categoryHolder.booksRecyclerView.post(() -> {
            categoryHolder.booksRecyclerView.requestLayout();
            categoryHolder.itemView.requestLayout();
        });
    }

    @NonNull
    private ArrayList<Book> fullCategoryBooks(@Nullable String title) {
        if (title == null) {
            return new ArrayList<>();
        }
        for (BookCategory category : BookCategoryStore.shared.getCategories(context)) {
            if (title.equals(category.getCategoryName())) {
                return new ArrayList<>(category.getBooks());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public int getItemCount() {
        return bookCategories.size();
    }

    static class ShelfGapDecoration extends RecyclerView.ItemDecoration {
        private final int gap;

        ShelfGapDecoration(int gap) {
            this.gap = gap;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position > 0) {
                if (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    outRect.right = gap;
                } else {
                    outRect.left = gap;
                }
            }
        }
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        TextView chapterTitle;
        RecyclerView booksRecyclerView;
        TextView buttonMore;
        View moreAction;
        ImageView moreChevron;
        BooksHorizontalRecyclerViewAdapter horizontalAdapter;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterTitle = itemView.findViewById(R.id.book_category_title);
            booksRecyclerView = itemView.findViewById(R.id.book_category_recycler_view);
            buttonMore = itemView.findViewById(R.id.book_more);
            moreAction = itemView.findViewById(R.id.book_more_action);
            moreChevron = itemView.findViewById(R.id.book_more_chevron);
        }
    }
}
