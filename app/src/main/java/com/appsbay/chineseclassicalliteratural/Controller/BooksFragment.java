package com.appsbay.chineseclassicalliteratural.Controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.Model.BookCategory;
import com.appsbay.chineseclassicalliteratural.Model.BookGenres;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.GenreChipsBinder;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchHistory;
import com.appsbay.chineseclassicalliteratural.Tools.SearchPanelBinder;
import com.appsbay.chineseclassicalliteratural.View.BooksListRecyclerViewAdapter;
import com.appsbay.chineseclassicalliteratural.View.BooksVerticalRecyclerViewAdapter;
import com.appsbay.chineseclassicalliteratural.viewmodel.HomeViewModel;
import com.appsbay.chineseclassicalliteratural.viewmodel.NovelsHubViewModelFactory;
import android.widget.FrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class BooksFragment extends Fragment {

    ViewFlipper viewFlipper;
    RecyclerView booksVerticalRecyclerView;
    RecyclerView booksListRecyclerView;
    BooksVerticalRecyclerViewAdapter booksVerticalRecyclerViewAdapter;
    BooksListRecyclerViewAdapter booksListRecyclerViewAdapter;
    Integer viewFlipperChild;
    private Menu menu;
    private FrameLayout mAdContainer;
    Context mContext;
    private HomeViewModel homeViewModel;
    private ChipGroup genreChipGroup;
    private View genreChipsScroll;
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private boolean searchExpanded;
    private int lastResultCount;
    private String lastQuery = "";

    public BooksFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        homeViewModel = new ViewModelProvider(
                this,
                new NovelsHubViewModelFactory(requireActivity().getApplication())
        ).get(HomeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_books, container, false);
        mContext = getContext();

        mAdContainer = view.findViewById(R.id.ad_container);
        AdsHelper.bindBanner(mAdContainer);

        genreChipsScroll = view.findViewById(R.id.genre_chips_include);
        genreChipGroup = view.findViewById(R.id.genre_chip_group);
        if (genreChipsScroll != null) {
            genreChipsScroll.setContentDescription(getString(R.string.genre_filter));
        }

        viewFlipper = view.findViewById(R.id.main_view_flipper);

        booksVerticalRecyclerView = view.findViewById(R.id.main_category_recycler_view);
        booksVerticalRecyclerViewAdapter = new BooksVerticalRecyclerViewAdapter(mContext, new ArrayList<BookCategory>());
        booksVerticalRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        booksVerticalRecyclerView.setAdapter(booksVerticalRecyclerViewAdapter);
        booksVerticalRecyclerView.setHasFixedSize(false);
        booksVerticalRecyclerView.setItemViewCacheSize(8);
        RecyclerView.ItemAnimator categoryAnimator = booksVerticalRecyclerView.getItemAnimator();
        if (categoryAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) categoryAnimator).setSupportsChangeAnimations(false);
        }

        booksListRecyclerView = view.findViewById(R.id.main_list_recycler_view);
        booksListRecyclerViewAdapter = new BooksListRecyclerViewAdapter(mContext, new ArrayList<>());
        booksListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        booksListRecyclerView.setAdapter(booksListRecyclerViewAdapter);
        booksListRecyclerView.setHasFixedSize(true);
        RecyclerView.ItemAnimator listAnimator = booksListRecyclerView.getItemAnimator();
        if (listAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) listAnimator).setSupportsChangeAnimations(false);
        }

        viewFlipperChild = 0;
        viewFlipper.setDisplayedChild(viewFlipperChild);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        bindGenreChips(homeViewModel.getSelectedCategoryName().getValue());

        homeViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (booksVerticalRecyclerViewAdapter != null) {
                booksVerticalRecyclerViewAdapter.setSearchQuery(homeViewModel.getSearchQuery().getValue());
                booksVerticalRecyclerViewAdapter.replaceCategories(categories);
                booksVerticalRecyclerView.scrollToPosition(0);
            }
            if (viewFlipper != null && viewFlipper.getDisplayedChild() == 0) {
                lastResultCount = countBooksInCategories(categories);
                refreshSearchPanel();
            }
        });

        homeViewModel.getAllBooks().observe(getViewLifecycleOwner(), books -> {
            String query = homeViewModel.getSearchQuery().getValue();
            lastQuery = query == null ? "" : query;
            booksListRecyclerViewAdapter.replaceBooks(new ArrayList<>(books));
            booksListRecyclerViewAdapter.setSearchQuery(lastQuery);
            if (viewFlipper == null || viewFlipper.getDisplayedChild() != 0) {
                lastResultCount = books == null ? 0 : books.size();
                refreshSearchPanel();
            }
        });

        homeViewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            lastQuery = query == null ? "" : query;
            booksListRecyclerViewAdapter.setSearchQuery(lastQuery);
            if (booksVerticalRecyclerViewAdapter != null) {
                booksVerticalRecyclerViewAdapter.setSearchQuery(lastQuery);
            }
            lastResultCount = currentVisibleResultCount();
            refreshSearchPanel();
        });

        configColor(view);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("NotificationBackgroundChange"));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(adFreeReceiver,
                new IntentFilter(LocalBroadcastHelper.ACTION_AD_FREE_CHANGED));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(offlineDownloadReceiver,
                new IntentFilter(LocalBroadcastHelper.ACTION_OFFLINE_DOWNLOAD_CHANGED));
        return view;
    }

    private void bindGenreChips(@Nullable String selectedCategory) {
        if (genreChipGroup == null) {
            return;
        }
        GenreChipsBinder.bind(genreChipGroup, selectedCategory, categoryName -> {
            homeViewModel.setSelectedCategory(categoryName);
            if (booksVerticalRecyclerView != null) {
                booksVerticalRecyclerView.scrollToPosition(0);
            }
            if (booksListRecyclerView != null) {
                booksListRecyclerView.scrollToPosition(0);
            }
            refreshSearchPanel();
        });
    }

    private int countBooksInCategories(@Nullable List<BookCategory> categories) {
        if (categories == null) {
            return 0;
        }
        int total = 0;
        for (BookCategory category : categories) {
            if (category.getBooks() != null) {
                total += category.getBooks().size();
            }
        }
        return total;
    }

    private int currentVisibleResultCount() {
        if (viewFlipper != null && viewFlipper.getDisplayedChild() == 0) {
            return countBooksInCategories(homeViewModel.getCategories().getValue());
        }
        List<Book> books = homeViewModel.getAllBooks().getValue();
        return books == null ? 0 : books.size();
    }

    private void refreshSearchPanel() {
        if (mContext == null || getView() == null) {
            return;
        }
        String selected = homeViewModel.getSelectedCategoryName().getValue();
        String genreLabel = null;
        if (selected != null && !selected.isEmpty()) {
            genreLabel = mContext.getString(BookGenres.shortLabelRes(selected));
        }
        SearchPanelBinder.update(
                requireView(),
                searchExpanded,
                lastQuery,
                lastResultCount,
                genreLabel,
                SearchHistory.get(mContext),
                homeViewModel.popularAuthors(8),
                new SearchPanelBinder.Callbacks() {
                    @Override
                    public void onSuggestionSelected(@NonNull String suggestion) {
                        applySuggestion(suggestion);
                    }

                    @Override
                    public void onClearSearch() {
                        clearSearchUi();
                    }

                    @Override
                    public void onSearchAllGenres() {
                        homeViewModel.setSelectedCategory(null);
                        bindGenreChips(null);
                        refreshSearchPanel();
                    }
                }
        );
    }

    private void applySuggestion(@NonNull String suggestion) {
        SearchHistory.add(mContext, suggestion);
        homeViewModel.setSearchQuery(suggestion);
        if (searchView != null) {
            searchView.setQuery(suggestion, false);
        }
        SearchChrome.dismissKeyboard(searchView != null ? searchView : requireView());
    }

    private void clearSearchUi() {
        homeViewModel.setSearchQuery("");
        if (searchView != null) {
            searchView.setQuery("", false);
        }
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        } else {
            searchExpanded = false;
            refreshSearchPanel();
        }
    }

    @Override
    public void onDestroyView() {
        AdsHelper.releaseBanner(mAdContainer);
        mAdContainer = null;
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(adFreeReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(offlineDownloadReceiver);
        super.onDestroyView();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            configColor();
            if (booksVerticalRecyclerViewAdapter != null) {
                booksVerticalRecyclerViewAdapter.notifyDataSetChanged();
            }
            if (booksListRecyclerViewAdapter != null) {
                booksListRecyclerViewAdapter.notifyDataSetChanged();
            }
        }
    };

    private BroadcastReceiver adFreeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AdsHelper.bindBanner(mAdContainer);
        }
    };

    private BroadcastReceiver offlineDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (booksVerticalRecyclerViewAdapter != null) {
                booksVerticalRecyclerViewAdapter.notifyDataSetChanged();
            }
            if (booksListRecyclerViewAdapter != null) {
                booksListRecyclerViewAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        FrameLayout container = mAdContainer;
        if (container == null) return;
        if (hidden) {
            AdsHelper.releaseBanner(container);
        } else {
            container.post(() -> AdsHelper.bindBanner(container));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AdsHelper.bindBanner(mAdContainer);
        homeViewModel.loadHomeData();
    }

    private void configColor() {
        View root = getView();
        if (root != null) {
            configColor(root);
        }
    }

    private void configColor(@NonNull View root) {
        viewFlipper.setBackgroundColor(MyColor.getBackgroundColor(mContext));
        MyImage.setBackgroundImage(mContext, viewFlipper);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            MyColor.flattenActionBar(actionBar);
            CharSequence title = actionBar.getTitle();
            if (title != null) {
                Spannable text = new SpannableString(title);
                text.setSpan(new ForegroundColorSpan(MyColor.getTitleTextColor(mContext)), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                actionBar.setTitle(text);
            }
        }
        ScreenChrome.tintHomeChrome((AppCompatActivity) getActivity());

        if (genreChipGroup != null) {
            GenreChipsBinder.applyTheme(genreChipGroup);
        }
        SearchPanelBinder.applyTheme(root);
        if (searchView != null) {
            SearchChrome.style(searchView, mContext);
        }

        if (menu != null) {
            MenuItem searchItem = menu.findItem(R.id.home_menu_action_search);
            if (searchItem != null) {
                SearchChrome.tintMenuIcon(searchItem, mContext);
            }
            updateLayoutToggleIcon(menu);
        }

        BottomNavigationView navigation = (BottomNavigationView) getActivity().findViewById(R.id.bottom_navigation_main);
        MyColor.applyBottomNavigation(mContext, navigation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.home_menu_action_change) {
            if (viewFlipper.getDisplayedChild() == 0) {
                item.setIcon(changeDrawableColor(mContext, R.drawable.nav_grid22, MyColor.getTitleTextColor(mContext)));
                viewFlipperChild = 1;
            } else {
                item.setIcon(changeDrawableColor(mContext, R.drawable.nav_list_bullet, MyColor.getTitleTextColor(mContext)));
                viewFlipperChild = 0;
            }
            viewFlipper.showNext();
            lastResultCount = currentVisibleResultCount();
            refreshSearchPanel();
            return true;
        } else if (id == R.id.home_menu_action_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static Drawable changeDrawableColor(Context context, int icon, int newColor) {
        Drawable mDrawable = ContextCompat.getDrawable(context, icon).mutate();
        mDrawable.setColorFilter(new PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN));
        return mDrawable;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        this.menu = menu;

        searchMenuItem = menu.findItem(R.id.home_menu_action_search);
        SearchChrome.tintMenuIcon(searchMenuItem, mContext);
        searchView = (SearchView) searchMenuItem.getActionView();

        String initial = homeViewModel.getSearchQuery().getValue();
        if (initial == null) {
            initial = "";
        }

        SearchChrome.bind(searchView, mContext, initial, new SearchChrome.QueryListener() {
            @Override
            public void onQueryChanged(@NonNull String query) {
                homeViewModel.setSearchQuery(query);
            }

            @Override
            public void onQuerySubmitted(@NonNull String query) {
                homeViewModel.setSearchQuery(query);
            }

            @Override
            public void onClosed() {
                homeViewModel.setSearchQuery("");
            }
        });

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchExpanded = true;
                if (searchView != null) {
                    SearchChrome.style(searchView, mContext);
                }
                refreshSearchPanel();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchExpanded = false;
                homeViewModel.setSearchQuery("");
                SearchChrome.tintMenuIcon(item, mContext);
                refreshSearchPanel();
                return true;
            }
        });

        updateLayoutToggleIcon(menu);

        if (!initial.isEmpty()) {
            searchMenuItem.expandActionView();
            SearchChrome.restoreQuery(searchView, initial);
            searchExpanded = true;
            refreshSearchPanel();
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        String query = homeViewModel.getSearchQuery().getValue();
        if (query == null) {
            query = "";
        }
        if (searchView != null) {
            SearchChrome.restoreQuery(searchView, query);
            SearchChrome.style(searchView, mContext);
        }
        if (!query.isEmpty() && searchMenuItem != null && !searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.expandActionView();
            searchExpanded = true;
            SearchChrome.restoreQuery(searchView, query);
        }
        refreshSearchPanel();
    }

    public void scrollToTop() {
        RecyclerView current = viewFlipper != null && viewFlipper.getDisplayedChild() == 1
                ? booksListRecyclerView : booksVerticalRecyclerView;
        if (current != null) {
            current.smoothScrollToPosition(0);
        }
    }

    private void updateLayoutToggleIcon(@Nullable Menu targetMenu) {
        if (targetMenu == null || viewFlipper == null) {
            return;
        }
        // A theme broadcast can reach Home while another tab owns the menu.
        MenuItem layoutItem = targetMenu.findItem(R.id.home_menu_action_change);
        if (layoutItem == null) {
            return;
        }
        int icon = viewFlipper.getDisplayedChild() == 0
                ? R.drawable.nav_list_bullet : R.drawable.nav_grid22;
        layoutItem.setIcon(changeDrawableColor(mContext, icon, MyColor.getTitleTextColor(mContext)));
    }
}
