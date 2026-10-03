package com.appsbay.chineseclassicalliteratural.Controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Model.BookCategory;
import com.appsbay.chineseclassicalliteratural.Model.BookCategoryStore;
import com.appsbay.chineseclassicalliteratural.Model.BookGenres;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.BookOpener;
import com.appsbay.chineseclassicalliteratural.Tools.GenreChipsBinder;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchHistory;
import com.appsbay.chineseclassicalliteratural.Tools.SearchPanelBinder;
import com.appsbay.chineseclassicalliteratural.View.BooksGridRecyclerViewAdapter;
import com.appsbay.chineseclassicalliteratural.View.BooksListRecyclerViewAdapter;
import android.widget.FrameLayout;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class BooksListActivity extends AppCompatActivity {

    ArrayList<Book> books;
    RecyclerView booksListRecyclerView;
    private RecyclerView.Adapter<?> booksAdapter;
    private Filterable booksFilterable;

    private FrameLayout mAdContainer;
    private ChipGroup genreChipGroup;
    private View genreChipsScroll;
    private String selectedCategory;
    private Book collectionBook;
    private String pendingSearchQuery = "";
    private boolean genreBrowseMode;
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private boolean searchExpanded;
    private boolean ignoreEmptyQueryCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        mAdContainer = findViewById(R.id.ad_container);
        AdsHelper.bindBanner(mAdContainer);
        ScreenChrome.setup(this, findViewById(R.id.screen_root), findViewById(R.id.ad_container));

        Intent intent = getIntent();
        books = intent.getParcelableArrayListExtra(BookOpener.EXTRA_BOOKS);
        if (books == null) {
            books = new ArrayList<>();
        }
        ArrayList<Book> availableBooks = new ArrayList<>();
        for (Book item : books) {
            if (BookStore.shared.isAvailable(this, item)) availableBooks.add(item);
        }
        books = availableBooks;
        collectionBook = intent.getParcelableExtra(BookOpener.EXTRA_COLLECTION_BOOK);
        if (collectionBook != null && !BookStore.shared.isAvailable(this, collectionBook)) {
            finish();
            return;
        }
        selectedCategory = intent.getStringExtra(BookOpener.EXTRA_CATEGORY_NAME);
        String incomingQuery = intent.getStringExtra(BookOpener.EXTRA_SEARCH_QUERY);
        pendingSearchQuery = incomingQuery == null ? "" : incomingQuery.trim();

        if (collectionBook != null) {
            setTitle(collectionBook.getName());
        } else if (pendingSearchQuery.length() > 0) {
            setTitle(getString(R.string.searching_for_query, pendingSearchQuery));
        } else if (selectedCategory != null && !selectedCategory.isEmpty()) {
            setTitle(BookGenres.displayName(this, selectedCategory));
        } else {
            setTitle(R.string.See_all);
        }

        genreChipsScroll = findViewById(R.id.genre_chips_include);
        genreChipGroup = findViewById(R.id.genre_chip_group);
        booksListRecyclerView = findViewById(R.id.book_list_activity);

        genreBrowseMode = collectionBook == null;
        if (genreBrowseMode) {
            int spanCount = getResources().getInteger(R.integer.book_grid_span);
            booksListRecyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
            BooksGridRecyclerViewAdapter gridAdapter = new BooksGridRecyclerViewAdapter(this, books);
            gridAdapter.setOnFilterPublishedListener(count -> {
                if (!isDestroyed()) {
                    refreshSearchPanel();
                }
            });
            booksAdapter = gridAdapter;
            booksFilterable = gridAdapter;
        } else {
            booksListRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            BooksListRecyclerViewAdapter listAdapter = new BooksListRecyclerViewAdapter(this, books, collectionBook);
            listAdapter.setOnFilterPublishedListener(count -> {
                if (!isDestroyed()) {
                    refreshSearchPanel();
                }
            });
            booksAdapter = listAdapter;
            booksFilterable = listAdapter;
        }
        booksListRecyclerView.setAdapter(booksAdapter);

        if (pendingSearchQuery.length() > 0 && booksFilterable != null) {
            booksFilterable.getFilter().filter(pendingSearchQuery);
            searchExpanded = true;
        }

        if (genreChipsScroll != null) {
            genreChipsScroll.setVisibility(genreBrowseMode ? View.VISIBLE : View.GONE);
            genreChipsScroll.setContentDescription(getString(R.string.genre_filter));
        }
        if (genreBrowseMode && genreChipGroup != null) {
            GenreChipsBinder.bind(genreChipGroup, selectedCategory, this::onGenreSelected);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(offlineDownloadReceiver,
                new IntentFilter(LocalBroadcastHelper.ACTION_OFFLINE_DOWNLOAD_CHANGED));

        configColor();
        refreshSearchPanel();
    }

    private void onGenreSelected(String categoryName) {
        selectedCategory = categoryName;
        if (categoryName == null || categoryName.isEmpty()) {
            setTitle(R.string.genre_all);
            ArrayList<Book> all = new ArrayList<>();
            for (BookCategory category : BookCategoryStore.shared.getCategories(this)) {
                all.addAll(category.getBooks());
            }
            books = all;
        } else {
            setTitle(BookGenres.displayName(this, categoryName));
            books = booksForCategory(categoryName);
        }
        if (booksAdapter instanceof BooksGridRecyclerViewAdapter) {
            ((BooksGridRecyclerViewAdapter) booksAdapter).replaceBooks(books, pendingSearchQuery);
        }
        if (pendingSearchQuery != null && !pendingSearchQuery.isEmpty() && booksFilterable != null) {
            booksFilterable.getFilter().filter(pendingSearchQuery);
        }
        booksListRecyclerView.scrollToPosition(0);
        refreshSearchPanel();
    }

    private ArrayList<Book> booksForCategory(String categoryName) {
        for (BookCategory category : BookCategoryStore.shared.getCategories(this)) {
            if (categoryName.equals(category.getCategoryName())) {
                return new ArrayList<>(category.getBooks());
            }
        }
        return new ArrayList<>();
    }

    private int currentResultCount() {
        if (booksAdapter instanceof BooksGridRecyclerViewAdapter) {
            return ((BooksGridRecyclerViewAdapter) booksAdapter).getFilteredCount();
        }
        if (booksAdapter instanceof BooksListRecyclerViewAdapter) {
            return ((BooksListRecyclerViewAdapter) booksAdapter).getFilteredCount();
        }
        return booksAdapter == null ? 0 : booksAdapter.getItemCount();
    }

    private void refreshSearchPanel() {
        String genreLabel = null;
        if (genreBrowseMode && selectedCategory != null && !selectedCategory.isEmpty()) {
            genreLabel = getString(BookGenres.shortLabelRes(selectedCategory));
        }
        List<String> popular = SearchHistory.popularAuthors(books, 8);
        SearchPanelBinder.update(
                findViewById(R.id.screen_root),
                searchExpanded,
                pendingSearchQuery,
                currentResultCount(),
                genreLabel,
                SearchHistory.get(this),
                popular,
                new SearchPanelBinder.Callbacks() {
                    @Override
                    public void onSuggestionSelected(@NonNull String suggestion) {
                        SearchHistory.add(BooksListActivity.this, suggestion);
                        pendingSearchQuery = suggestion;
                        if (searchView != null) {
                            searchView.setQuery(suggestion, false);
                        }
                        if (booksFilterable != null) {
                            booksFilterable.getFilter().filter(suggestion);
                        }
                        SearchChrome.dismissKeyboard(searchView != null ? searchView : booksListRecyclerView);
                        booksListRecyclerView.post(BooksListActivity.this::refreshSearchPanel);
                    }

                    @Override
                    public void onClearSearch() {
                        pendingSearchQuery = "";
                        if (searchView != null) {
                            searchView.setQuery("", false);
                        }
                        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
                            searchMenuItem.collapseActionView();
                        } else {
                            searchExpanded = false;
                            if (booksFilterable != null) {
                                booksFilterable.getFilter().filter("");
                            }
                            refreshSearchPanel();
                        }
                    }

                    @Override
                    public void onSearchAllGenres() {
                        if (genreBrowseMode) {
                            onGenreSelected(null);
                            if (genreChipGroup != null) {
                                GenreChipsBinder.bind(genreChipGroup, null, BooksListActivity.this::onGenreSelected);
                            }
                        }
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        configColor();
    }

    @Override
    protected void onDestroy() {
        AdsHelper.releaseBanner(mAdContainer);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(offlineDownloadReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver offlineDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (booksAdapter != null) {
                booksAdapter.notifyDataSetChanged();
            }
        }
    };

    private void configColor() {
        booksListRecyclerView.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, booksListRecyclerView);
        if (booksAdapter != null) {
            booksAdapter.notifyDataSetChanged();
        }
        if (genreChipGroup != null && genreBrowseMode) {
            GenreChipsBinder.applyTheme(genreChipGroup);
        }
        SearchPanelBinder.applyTheme(findViewById(R.id.screen_root));
        if (searchView != null) {
            SearchChrome.style(searchView, this);
        }

        ScreenChrome.tint(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.home_menu_action_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_list_menu, menu);

        searchMenuItem = menu.findItem(R.id.home_menu_action_search);
        SearchChrome.tintMenuIcon(searchMenuItem, this);
        searchView = (SearchView) searchMenuItem.getActionView();

        SearchChrome.bind(searchView, this, pendingSearchQuery, new SearchChrome.QueryListener() {
            @Override
            public void onQueryChanged(@NonNull String query) {
                if (ignoreEmptyQueryCallback && query.trim().isEmpty()) {
                    return;
                }
                pendingSearchQuery = query;
                if (booksFilterable != null) {
                    booksFilterable.getFilter().filter(query);
                }
                updateSearchingTitle();
                booksListRecyclerView.post(BooksListActivity.this::refreshSearchPanel);
            }

            @Override
            public void onQuerySubmitted(@NonNull String query) {
                pendingSearchQuery = query;
                if (booksFilterable != null) {
                    booksFilterable.getFilter().filter(query);
                }
                updateSearchingTitle();
                booksListRecyclerView.post(BooksListActivity.this::refreshSearchPanel);
            }

            @Override
            public void onClosed() {
                pendingSearchQuery = "";
                if (booksFilterable != null) {
                    booksFilterable.getFilter().filter("");
                }
                updateSearchingTitle();
                refreshSearchPanel();
            }
        });

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchExpanded = true;
                if (searchView != null) {
                    SearchChrome.style(searchView, BooksListActivity.this);
                }
                refreshSearchPanel();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchExpanded = false;
                pendingSearchQuery = "";
                if (booksFilterable != null) {
                    booksFilterable.getFilter().filter("");
                }
                SearchChrome.tintMenuIcon(item, BooksListActivity.this);
                updateSearchingTitle();
                refreshSearchPanel();
                return true;
            }
        });

        if (pendingSearchQuery != null && !pendingSearchQuery.isEmpty()) {
            final String launchQuery = pendingSearchQuery;
            ignoreEmptyQueryCallback = true;
            searchMenuItem.expandActionView();
            searchView.post(() -> {
                pendingSearchQuery = launchQuery;
                SearchChrome.restoreQuery(searchView, launchQuery);
                if (booksFilterable != null) {
                    booksFilterable.getFilter().filter(launchQuery);
                }
                searchExpanded = true;
                ignoreEmptyQueryCallback = false;
                updateSearchingTitle();
                refreshSearchPanel();
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void updateSearchingTitle() {
        if (collectionBook != null) {
            return;
        }
        if (pendingSearchQuery != null && !pendingSearchQuery.trim().isEmpty()) {
            setTitle(getString(R.string.searching_for_query, pendingSearchQuery.trim()));
        } else if (selectedCategory != null && !selectedCategory.isEmpty()) {
            setTitle(BookGenres.displayName(this, selectedCategory));
        } else {
            setTitle(R.string.genre_all);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (searchView != null) {
            SearchChrome.restoreQuery(searchView, pendingSearchQuery);
            SearchChrome.style(searchView, this);
        }
        return super.onPrepareOptionsMenu(menu);
    }
}
