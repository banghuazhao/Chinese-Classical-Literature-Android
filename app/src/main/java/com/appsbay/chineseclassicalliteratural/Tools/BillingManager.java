package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.appsbay.chineseclassicalliteratural.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One-time "Remove ads" purchase via Google Play Billing.
 * Product ID in Play Console must be {@link #PRODUCT_REMOVE_ADS}.
 */
public final class BillingManager implements PurchasesUpdatedListener {

    private static final String LOG_TAG = "BillingManager";
    private static final String PREFS = "Billing";
    private static final String KEY_AD_FREE = "adFree";

    /** Must match the managed product ID created in Google Play Console. */
    public static final String PRODUCT_REMOVE_ADS = "remove_ads";

    private static BillingManager instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    private BillingClient billingClient;
    @Nullable
    private ProductDetails removeAdsProduct;
    @Nullable
    private PurchaseListener purchaseListener;
    private final List<Runnable> pendingAfterConnect = new ArrayList<>();

    public interface PurchaseListener {
        void onPurchaseCompleted(boolean restored);

        void onPurchaseFailed(@Nullable String message);

        void onProductsUpdated();
    }

    public static synchronized BillingManager get(Context context) {
        if (instance == null) {
            instance = new BillingManager(context.getApplicationContext());
        }
        return instance;
    }

    private BillingManager(Context appContext) {
        this.appContext = appContext;
        this.prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureConnected(null);
    }

    public boolean isAdFree() {
        return prefs.getBoolean(KEY_AD_FREE, false);
    }

    public boolean isProductAvailable() {
        return removeAdsProduct != null;
    }

    public void setPurchaseListener(@Nullable PurchaseListener listener) {
        this.purchaseListener = listener;
    }

    public void launchRemoveAdsPurchase(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (isAdFree()) {
            notifyCompleted(false);
            return;
        }
        ensureConnected(() -> {
            if (removeAdsProduct == null) {
                queryProductDetails(() -> {
                    if (removeAdsProduct == null) {
                        notifyFailed(appContext.getString(R.string.purchase_unavailable));
                        return;
                    }
                    startBillingFlow(activity);
                });
                return;
            }
            startBillingFlow(activity);
        });
    }

    public void restorePurchases() {
        ensureConnected(() -> billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchases) -> mainHandler.post(() -> {
                    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        notifyFailed(billingResult.getDebugMessage());
                        return;
                    }
                    boolean found = handlePurchases(purchases, true);
                    if (!found && purchaseListener != null) {
                        purchaseListener.onPurchaseFailed(
                                appContext.getString(R.string.purchase_nothing_to_restore));
                    }
                })));
    }

    @Nullable
    public String getRemoveAdsPrice() {
        if (removeAdsProduct == null || removeAdsProduct.getOneTimePurchaseOfferDetails() == null) {
            return null;
        }
        return removeAdsProduct.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    public String getRemoveAdsTitle(Context context) {
        if (isAdFree()) {
            return context.getString(R.string.ad_free_active);
        }
        String price = getRemoveAdsPrice();
        if (price != null) {
            return context.getString(R.string.remove_ads_price, price);
        }
        return context.getString(R.string.remove_ads);
    }

    private void startBillingFlow(Activity activity) {
        if (billingClient == null || !billingClient.isReady() || removeAdsProduct == null) {
            notifyFailed(appContext.getString(R.string.purchase_unavailable));
            return;
        }
        List<BillingFlowParams.ProductDetailsParams> productParams = Collections.singletonList(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(removeAdsProduct)
                        .build());
        BillingResult result = billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productParams)
                        .build());
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            notifyFailed(result.getDebugMessage());
        }
    }

    private void ensureConnected(@Nullable Runnable onReady) {
        if (billingClient != null && billingClient.isReady()) {
            if (onReady != null) {
                onReady.run();
            }
            return;
        }
        if (onReady != null) {
            synchronized (pendingAfterConnect) {
                pendingAfterConnect.add(onReady);
            }
        }
        if (!connecting.compareAndSet(false, true)) {
            return;
        }
        if (billingClient != null) {
            try {
                billingClient.endConnection();
            } catch (Exception ignored) {
            }
        }
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                connecting.set(false);
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Log.w(LOG_TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    drainPending(false);
                    return;
                }
                queryProductDetails(null);
                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                        (result, purchases) -> mainHandler.post(() -> {
                            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                handlePurchases(purchases, false);
                            }
                        }));
                drainPending(true);
            }

            @Override
            public void onBillingServiceDisconnected() {
                connecting.set(false);
                Log.i(LOG_TAG, "Billing service disconnected");
            }
        });
    }

    private void drainPending(boolean ready) {
        List<Runnable> copy;
        synchronized (pendingAfterConnect) {
            copy = new ArrayList<>(pendingAfterConnect);
            pendingAfterConnect.clear();
        }
        if (!ready) {
            return;
        }
        for (Runnable runnable : copy) {
            mainHandler.post(runnable);
        }
    }

    private void queryProductDetails(@Nullable Runnable onComplete) {
        if (billingClient == null || !billingClient.isReady()) {
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PRODUCT_REMOVE_ADS)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()))
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, result) ->
                mainHandler.post(() -> {
                    List<ProductDetails> details =
                            result == null ? null : result.getProductDetailsList();
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && details != null && !details.isEmpty()) {
                        removeAdsProduct = details.get(0);
                        if (purchaseListener != null) {
                            purchaseListener.onProductsUpdated();
                        }
                    } else {
                        Log.w(LOG_TAG, "Product query failed: " + billingResult.getDebugMessage());
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }));
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        mainHandler.post(() -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                handlePurchases(purchases, false);
                return;
            }
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                return;
            }
            notifyFailed(billingResult.getDebugMessage());
        });
    }

    /**
     * @return true if a remove-ads purchase was found and applied
     */
    private boolean handlePurchases(@Nullable List<Purchase> purchases, boolean fromRestore) {
        if (purchases == null || purchases.isEmpty()) {
            return false;
        }
        boolean found = false;
        for (Purchase purchase : purchases) {
            if (!purchase.getProducts().contains(PRODUCT_REMOVE_ADS)) {
                continue;
            }
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Log.i(LOG_TAG, "Remove-ads purchase is pending");
                continue;
            }
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
                continue;
            }
            found = true;
            acknowledgeIfNeeded(purchase);
            boolean wasAdFree = isAdFree();
            setAdFree(true);
            if (!wasAdFree || fromRestore) {
                notifyCompleted(fromRestore || wasAdFree);
            }
        }
        return found;
    }

    private void acknowledgeIfNeeded(Purchase purchase) {
        if (purchase.isAcknowledged() || billingClient == null) {
            return;
        }
        billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build(),
                billingResult -> Log.d(LOG_TAG, "Ack: " + billingResult.getResponseCode()));
    }

    private void setAdFree(boolean adFree) {
        if (prefs.getBoolean(KEY_AD_FREE, false) == adFree) {
            return;
        }
        prefs.edit().putBoolean(KEY_AD_FREE, adFree).apply();
        LocalBroadcastHelper.sendAdFreeChanged(appContext);
    }

    private void notifyCompleted(boolean restored) {
        if (purchaseListener != null) {
            purchaseListener.onPurchaseCompleted(restored);
        }
    }

    private void notifyFailed(@Nullable String message) {
        if (purchaseListener != null) {
            purchaseListener.onPurchaseFailed(message);
        }
    }
}
