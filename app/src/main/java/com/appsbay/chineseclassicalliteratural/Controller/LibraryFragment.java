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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Model.Book;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchChrome;
import com.appsbay.chineseclassicalliteratural.Tools.SearchHistory;
import com.appsbay.chineseclassicalliteratural.Tools.SearchPanelBinder;
import com.appsbay.chineseclassicalliteratural.View.BooksLibraryListRecyclerViewAdapter;
import com.appsbay.chineseclassicalliteratural.viewmodel.LibraryViewModel;
import com.appsbay.chineseclassicalliteratural.viewmodel.NovelsHubViewModelFactory;
import android.widget.FrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class LibraryFragment extends Fragment implements BooksLibraryListRecyclerViewAdapter.OnStartDragListener {

    RecyclerView booksListRecyclerView;
    BooksLibraryListRecyclerViewAdapter booksListRecyclerViewAdapter;
    private Menu menu;
    private FrameLayout mAdContainer;
    Context mContext;
    private LibraryViewModel libraryViewModel;
    private ItemTouchHelper itemTouchHelper;
    private boolean dragEnabled = true;

    private TextView textView;
    private TextView emptySubtitle;
    private View emptyLibrary;
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private boolean searchExpanded;
    private String lastQuery = "";
    private int lastResultCount;

    public LibraryFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        libraryViewModel = new ViewModelProvider(
                this,
                new NovelsHubViewModelFactory(requireActivity().getApplication())
        ).get(LibraryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        mContext = getContext();

        mAdContainer = view.findViewById(R.id.ad_container);
        AdsHelper.bindBanner(mAdContainer);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        booksListRecyclerView = view.findViewById(R.id.library_list_recycler_view);
        emptyLibrary = view.findViewById(R.id.empty_library);
        textView = view.findViewById(R.id.textView_nodata);
        emptySubtitle = view.findViewById(R.id.textView_nodata_subtitle);

        booksListRecyclerViewAdapter = new BooksLibraryListRecyclerViewAdapter(mContext, new ArrayList<>(), this);
        booksListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        booksListRecyclerView.setAdapter(booksListRecyclerViewAdapter);
        attachDragAndDrop();

        libraryViewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            String query = libraryViewModel.getSearchQuery().getValue();
            lastQuery = query == null ? "" : query;
            booksListRecyclerViewAdapter.replaceBooks(new ArrayList<>(books));
            booksListRecyclerViewAdapter.setSearchQuery(lastQuery);
            dragEnabled = lastQuery.trim().isEmpty();
            booksListRecyclerViewAdapter.setDragEnabled(dragEnabled);
            lastResultCount = books == null ? 0 : books.size();
            updateEmptyState(lastResultCount);
            refreshSearchPanel();
        });

        libraryViewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            lastQuery = query == null ? "" : query;
            booksListRecyclerViewAdapter.setSearchQuery(lastQuery);
            lastResultCount = booksListRecyclerViewAdapter.getItemCount();
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

    private void refreshSearchPanel() {
        if (mContext == null || getView() == null) {
            return;
        }
        SearchPanelBinder.update(
                requireView(),
                searchExpanded,
                lastQuery,
                lastResultCount,
                null,
                SearchHistory.get(mContext),
                libraryViewModel.popularAuthors(8),
                new SearchPanelBinder.Callbacks() {
                    @Override
                    public void onSuggestionSelected(@NonNull String suggestion) {
                        SearchHistory.add(mContext, suggestion);
                        libraryViewModel.setSearchQuery(suggestion);
                        if (searchView != null) {
                            searchView.setQuery(suggestion, false);
                        }
                        SearchChrome.dismissKeyboard(searchView != null ? searchView : requireView());
                    }

                    @Override
                    public void onClearSearch() {
                        clearSearchUi();
                    }

                    @Override
                    public void onSearchAllGenres() {
                        // Library has no genre filter.
                    }
                }
        );
    }

    private void clearSearchUi() {
        libraryViewModel.setSearchQuery("");
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

    private void attachDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (!dragEnabled) {
                    return false;
                }
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION || from == to) {
                    return false;
                }
                Book moved = booksListRecyclerViewAdapter.books.remove(from);
                booksListRecyclerViewAdapter.books.add(to, moved);
                booksListRecyclerViewAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (dragEnabled) {
                    booksListRecyclerViewAdapter.syncBooksFull();
                    libraryViewModel.reorderFavorites(new ArrayList<>(booksListRecyclerViewAdapter.books));
                }
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(booksListRecyclerView);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (dragEnabled && itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
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
        libraryViewModel.refreshLibrary();
    }

    private void configColor() {
        View root = getView();
        if (root != null) {
            configColor(root);
        }
    }

    private void configColor(@NonNull View root) {
        booksListRecyclerView.setBackgroundColor(MyColor.getBackgroundColor(mContext));
        MyImage.setBackgroundImage(mContext, booksListRecyclerView);

        textView.setTextColor(MyColor.getTitleTextColor(mContext));
        if (emptySubtitle != null) {
            emptySubtitle.setTextColor(MyColor.getDetailTextColor(mContext));
        }

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

        SearchPanelBinder.applyTheme(root);
        if (searchView != null) {
            SearchChrome.style(searchView, mContext);
        }
        if (menu != null && menu.size() > 0) {
            MenuItem searchItem = menu.findItem(R.id.library_menu_action_search);
            if (searchItem != null) {
                SearchChrome.tintMenuIcon(searchItem, mContext);
            }
        }

        BottomNavigationView navigation = (BottomNavigationView) getActivity().findViewById(R.id.bottom_navigation_main);
        MyColor.applyBottomNavigation(mContext, navigation);
    }

    private void updateEmptyState(int count) {
        if (emptyLibrary == null) {
            return;
        }
        boolean searching = libraryViewModel.isSearchActive() || searchExpanded && !lastQuery.isEmpty();
        if (searching) {
            emptyLibrary.setVisibility(View.INVISIBLE);
            return;
        }
        if (count == 0) {
            emptyLibrary.setVisibility(View.VISIBLE);
            BookMotion.revealOnce(emptyLibrary);
        } else {
            emptyLibrary.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.library_menu_search_text) {
            startActivity(new Intent(requireContext(), ReaderCollectionActivity.class));
            return true;
        }
        if (id == R.id.library_menu_saved_passages) {
            startActivity(new Intent(requireContext(), ReaderCollectionActivity.class)
                    .putExtra(ReaderCollectionActivity.EXTRA_MARKS, true));
            return true;
        }
        if (id == R.id.library_menu_action_search) {
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
        inflater.inflate(R.menu.library_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        this.menu = menu;

        searchMenuItem = menu.findItem(R.id.library_menu_action_search);
        SearchChrome.tintMenuIcon(searchMenuItem, mContext);
        searchView = (SearchView) searchMenuItem.getActionView();

        String initial = libraryViewModel.getSearchQuery().getValue();
        if (initial == null) {
            initial = "";
        }

        SearchChrome.bind(searchView, mContext, initial, new SearchChrome.QueryListener() {
            @Override
            public void onQueryChanged(@NonNull String query) {
                dragEnabled = query.trim().isEmpty();
                booksListRecyclerViewAdapter.setDragEnabled(dragEnabled);
                libraryViewModel.setSearchQuery(query);
            }

            @Override
            public void onQuerySubmitted(@NonNull String query) {
                libraryViewModel.setSearchQuery(query);
            }

            @Override
            public void onClosed() {
                dragEnabled = true;
                booksListRecyclerViewAdapter.setDragEnabled(true);
                libraryViewModel.setSearchQuery("");
            }
        });

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchExpanded = true;
                refreshSearchPanel();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchExpanded = false;
                dragEnabled = true;
                booksListRecyclerViewAdapter.setDragEnabled(true);
                libraryViewModel.setSearchQuery("");
                SearchChrome.tintMenuIcon(item, mContext);
                refreshSearchPanel();
                updateEmptyState(lastResultCount);
                return true;
            }
        });

        menu.getItem(0).setIcon(changeDrawableColor(mContext, R.drawable.ic_search, MyColor.getTitleTextColor(mContext)));

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
        String query = libraryViewModel.getSearchQuery().getValue();
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
        if (booksListRecyclerView != null) {
            booksListRecyclerView.smoothScrollToPosition(0);
        }
    }
}
