package com.appsbay.chineseclassicalliteratural.Controller.Menu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.AdsHelper;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.appsbay.chineseclassicalliteratural.Tools.BillingManager;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.LocalBroadcastHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.PrivacyManager;
import com.appsbay.chineseclassicalliteratural.Tools.RewardedAdHelper;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.appsbay.chineseclassicalliteratural.Tools.TemporaryAdFree;
import android.widget.FrameLayout;

import java.util.ArrayList;

public class MenuActivity extends AppCompatActivity {

    ArrayList<MenuItem> menuItems = new ArrayList<>();

    RecyclerView recyclerView;
    MenuItemRecyclerViewAdapter adapter;

    Context mContext;

    private FrameLayout mAdContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_menu);

        mAdContainer = findViewById(R.id.ad_container);
        AdsHelper.bindBanner(mAdContainer);
        ScreenChrome.setup(this, findViewById(R.id.screen_root), findViewById(R.id.ad_container));

        setTitle(R.string.Menu);

        mContext = this;

        buildMenuItems();

        adapter = new MenuItemRecyclerViewAdapter(this, menuItems);

        recyclerView = findViewById(R.id.recycler_view_menu);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        if (AgeGate.isAdult(this)) {
            BillingManager.get(this).setPurchaseListener(new BillingManager.PurchaseListener() {
            @Override
            public void onPurchaseCompleted(boolean restored) {
                View anchor = recyclerView != null ? recyclerView : findViewById(android.R.id.content);
                DialogChrome.snack(anchor,
                        getString(restored ? R.string.purchase_restored : R.string.ad_free_active));
                refreshAfterAdFreeChange();
            }

            @Override
            public void onPurchaseFailed(String message) {
                if (message == null || message.isEmpty()) {
                    return;
                }
                View anchor = recyclerView != null ? recyclerView : findViewById(android.R.id.content);
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

        LocalBroadcastManager.getInstance(this).registerReceiver(adFreeReceiver,
                new IntentFilter(LocalBroadcastHelper.ACTION_AD_FREE_CHANGED));

        configColor();
    }

    private void buildMenuItems() {
        menuItems.clear();
        int tint = MyColor.getButtonTintColor(mContext);
        menuItems.add(new MenuItem(MenuItem.ACTION_BACKGROUND,
                getString(R.string.Background),
                MyImage.changeDrawableColor(mContext, R.drawable.nav_sun, tint)));
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
        int ageGroup = AgeGate.getGroup(this);
        String ageLabel = ageGroup == AgeGate.UNKNOWN ? getString(R.string.age_group)
                : getString(R.string.age_group) + " · " + getString(
                        ageGroup == AgeGate.ADULT ? R.string.age_gate_adult : R.string.age_gate_under_18);
        menuItems.add(new MenuItem(MenuItem.ACTION_AGE_GROUP, ageLabel,
                MyImage.changeDrawableColor(mContext, R.drawable.nav_note, tint)));
        if (AgeGate.isAdult(this) && PrivacyManager.get(this).isPrivacyOptionsRequired()) {
            menuItems.add(new MenuItem(MenuItem.ACTION_PRIVACY_SETTINGS,
                    getString(R.string.privacy_settings),
                    MyImage.changeDrawableColor(mContext, R.drawable.nav_note, tint)));
        }
        if (AgeGate.isAdult(this)) {
            menuItems.add(new MenuItem(MenuItem.ACTION_REMOVE_ADS,
                    BillingManager.get(this).getRemoveAdsTitle(this),
                    MyImage.changeDrawableColor(mContext, R.drawable.nav_bookmark, tint)));
        }
        if (AgeGate.isAdult(this) && !BillingManager.get(this).isAdFree()) {
            String watchTitle = TemporaryAdFree.isActive(this)
                    ? getString(R.string.temp_ad_free_active)
                    : getString(R.string.watch_ad_for_24h);
            menuItems.add(new MenuItem(MenuItem.ACTION_WATCH_AD_FREE,
                    watchTitle,
                    MyImage.changeDrawableColor(mContext, R.drawable.nav_speaker, tint)));
            menuItems.add(new MenuItem(MenuItem.ACTION_RESTORE,
                    getString(R.string.restore_purchases),
                    MyImage.changeDrawableColor(mContext, R.drawable.nav_bookmark_circle, tint)));
            RewardedAdHelper.get(this).preload();
        }
        if (AgeGate.isAdult(this)) {
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

    @Override
    protected void onDestroy() {
        AdsHelper.releaseBanner(mAdContainer);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(adFreeReceiver);
        if (AgeGate.isAdult(this)) {
            BillingManager.get(this).setPurchaseListener(null);
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void configColor() {
        recyclerView.setBackgroundColor(MyColor.getBackgroundColor(this));
        MyImage.setBackgroundImage(this, recyclerView);
        ScreenChrome.tint(this);
    }
}
