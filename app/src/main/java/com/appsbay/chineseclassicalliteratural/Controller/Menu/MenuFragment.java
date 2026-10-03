package com.appsbay.chineseclassicalliteratural.Controller.Menu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.appsbay.chineseclassicalliteratural.Tools.BillingManager;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;
import com.appsbay.chineseclassicalliteratural.Tools.LocaleHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.PrivacyManager;
import com.appsbay.chineseclassicalliteratural.Tools.RewardedAdHelper;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.Tools.TemporaryAdFree;
import android.widget.FrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MenuFragment extends Fragment {

    private final ArrayList<MenuItem> menuItems = new ArrayList<>();
    private RecyclerView recyclerView;
    private MenuItemRecyclerViewAdapter adapter;
    private FrameLayout mAdContainer;
    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        mContext = requireContext();

        mAdContainer = view.findViewById(R.id.ad_container);
        AdsHelper.bindBanner(mAdContainer);

        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        buildMenuItems();
        adapter = new MenuItemRecyclerViewAdapter(mContext, menuItems);
        recyclerView = view.findViewById(R.id.recycler_view_menu);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        if (AgeGate.isAdult(mContext)) {
            BillingManager.get(mContext).setPurchaseListener(new BillingManager.PurchaseListener() {
            @Override
            public void onPurchaseCompleted(boolean restored) {
                View anchor = recyclerView != null ? recyclerView : view;
                DialogChrome.snack(anchor,
                        getString(restored ? R.string.purchase_restored : R.string.ad_free_active));
                refreshAfterAdFreeChange();
            }

            @Override
            public void onPurchaseFailed(String message) {
                if (message == null || message.isEmpty()) {
                    return;
                }
                View anchor = recyclerView != null ? recyclerView : view;
                DialogChrome.snack(anchor, message);
            }

            @Override
            public void onProductsUpdated() {
                buildMenuItems();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
            });
        }

        LocalBroadcastManager.getInstance(mContext).registerReceiver(adFreeReceiver,
                new IntentFilter(LocalBroadcastHelper.ACTION_AD_FREE_CHANGED));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(backgroundReceiver,
                new IntentFilter("NotificationBackgroundChange"));

        configColor();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        AdsHelper.bindBanner(mAdContainer);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        buildMenuItems();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        configColor();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        FrameLayout container = mAdContainer;
        if (container != null) {
            if (hidden) {
                AdsHelper.releaseBanner(container);
            } else {
                container.post(() -> AdsHelper.bindBanner(container));
            }
        }
        if (!hidden && mContext != null) {
            buildMenuItems();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        AdsHelper.releaseBanner(mAdContainer);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(adFreeReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(backgroundReceiver);
        if (AgeGate.isAdult(mContext)) {
            BillingManager.get(mContext).setPurchaseListener(null);
        }
        super.onDestroyView();
    }

    private void buildMenuItems() {
        menuItems.clear();
        int tint = MyColor.getButtonTintColor(mContext);
        menuItems.add(new MenuItem(MenuItem.ACTION_BACKGROUND,
                getString(R.string.Background),
                MyImage.changeDrawableColor(mContext, R.drawable.nav_sun, tint)));
        menuItems.add(new MenuItem(MenuItem.ACTION_LANGUAGE,
                getString(R.string.Language) + " · " + LocaleHelper.getSelectedLabel(mContext),
                MyImage.changeDrawableColor(mContext, R.drawable.ic_language, tint)));
        menuItems.add(new MenuItem(MenuItem.ACTION_SCRIPT,
                getString(R.string.book_script) + " · " + getString(BookStore.shared.usesTraditional(mContext)
                        ? R.string.book_script_traditional : R.string.book_script_simplified),
                MyImage.changeDrawableColor(mContext, R.drawable.ic_language, tint)));
        menuItems.add(new MenuItem(MenuItem.ACTION_FEEDBACK,
                getString(R.string.Feedback),
                MyImage.changeDrawableColor(mContext, R.drawable.icon_feedback, tint)));
        menuItems.add(new MenuItem(MenuItem.ACTION_RATE,
                getString(R.string.Rate),
                MyImage.changeDrawableColor(mContext, R.drawable.icon_rate, tint)));
        menuItems.add(new MenuItem(MenuItem.ACTION_SHARE,
                getString(R.string.Share),
                MyImage.changeDrawableColor(mContext, R.drawable.icon_share2, tint)));
        menuItems.add(new MenuItem(MenuItem.ACTION_PRIVACY_POLICY,
                getString(R.string.privacy_policy),
                MyImage.changeDrawableColor(mContext, R.drawable.nav_info, tint)));
        int ageGroup = AgeGate.getGroup(mContext);
        String ageLabel = ageGroup == AgeGate.UNKNOWN ? getString(R.string.age_group)
                : getString(R.string.age_group) + " · " + getString(
                        ageGroup == AgeGate.ADULT ? R.string.age_gate_adult : R.string.age_gate_under_18);
        menuItems.add(new MenuItem(MenuItem.ACTION_AGE_GROUP, ageLabel,
                MyImage.changeDrawableColor(mContext, R.drawable.nav_note, tint)));
        if (AgeGate.isAdult(mContext)
                && PrivacyManager.get(mContext).isPrivacyOptionsRequired()) {
            menuItems.add(new MenuItem(MenuItem.ACTION_PRIVACY_SETTINGS,
                    getString(R.string.privacy_settings),
                    MyImage.changeDrawableColor(mContext, R.drawable.nav_note, tint)));
        }
        if (AgeGate.isAdult(mContext)
                && (BillingManager.get(mContext).isProductAvailable()
                || BillingManager.get(mContext).isAdFree())) {
            menuItems.add(new MenuItem(MenuItem.ACTION_REMOVE_ADS,
                    BillingManager.get(mContext).getRemoveAdsTitle(mContext),
                    MyImage.changeDrawableColor(mContext, R.drawable.nav_bookmark, tint)));
        }
        if (AgeGate.isAdult(mContext) && !BillingManager.get(mContext).isAdFree()) {
            if (!getString(R.string.adRewardedID).isEmpty() || TemporaryAdFree.isActive(mContext)) {
                String watchTitle = TemporaryAdFree.isActive(mContext)
                        ? getString(R.string.temp_ad_free_active)
                        : getString(R.string.watch_ad_for_24h);
                menuItems.add(new MenuItem(MenuItem.ACTION_WATCH_AD_FREE,
                        watchTitle,
                        MyImage.changeDrawableColor(mContext, R.drawable.nav_speaker, tint)));
                RewardedAdHelper.get(mContext).preload();
            }
            if (BillingManager.get(mContext).isProductAvailable()) {
                menuItems.add(new MenuItem(MenuItem.ACTION_RESTORE,
                        getString(R.string.restore_purchases),
                        MyImage.changeDrawableColor(mContext, R.drawable.nav_bookmark_circle, tint)));
            }
        }
        if (AgeGate.isAdult(mContext)) {
            menuItems.add(new MenuItem(MenuItem.ACTION_MORE_APPS,
                    getString(R.string.MoreApps),
                    MyImage.changeDrawableColor(mContext, R.drawable.ic_tab_more, tint)));
        }
    }

    private void refreshAfterAdFreeChange() {
        buildMenuItems();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        AdsHelper.bindBanner(mAdContainer);
    }

    private final BroadcastReceiver adFreeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAfterAdFreeChange();
        }
    };

    private final BroadcastReceiver backgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            configColor();
            if (adapter != null) {
                buildMenuItems();
                adapter.notifyDataSetChanged();
            }
        }
    };

    private void configColor() {
        if (recyclerView == null || mContext == null) {
            return;
        }
        recyclerView.setBackgroundColor(MyColor.getBackgroundColor(mContext));
        MyImage.setBackgroundImage(mContext, recyclerView);
        ScreenChrome.tintHomeChrome((AppCompatActivity) requireActivity());
        BottomNavigationView navigation = requireActivity().findViewById(R.id.bottom_navigation_main);
        MyColor.applyBottomNavigation(mContext, navigation);
    }

    public void scrollToTop() {
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }
    }
}
