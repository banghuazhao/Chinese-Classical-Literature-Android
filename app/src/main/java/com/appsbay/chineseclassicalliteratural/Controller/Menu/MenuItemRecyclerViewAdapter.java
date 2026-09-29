package com.appsbay.chineseclassicalliteratural.Controller.Menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.Controller.ImagesActivity;
import com.appsbay.chineseclassicalliteratural.Controller.Menu.MoreApps.MoreAppsActivity;
import com.appsbay.chineseclassicalliteratural.Model.BookStore;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.BillingManager;
import com.appsbay.chineseclassicalliteratural.Tools.DialogChrome;
import com.appsbay.chineseclassicalliteratural.Tools.HelperFunctions;
import com.appsbay.chineseclassicalliteratural.Tools.LocaleHelper;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.MyImage;
import com.appsbay.chineseclassicalliteratural.Tools.RewardedAdHelper;
import com.appsbay.chineseclassicalliteratural.Tools.StoreHelper;

import java.util.ArrayList;

public class MenuItemRecyclerViewAdapter extends RecyclerView.Adapter<MenuItemRecyclerViewAdapter.MenuItemRecyclerViewViewHolder> {

    Context context;
    ArrayList<MenuItem> menuItems;

    public MenuItemRecyclerViewAdapter(Context context, ArrayList<MenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems;
    }

    @NonNull
    @Override
    public MenuItemRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_menu_item, parent, false);
        return new MenuItemRecyclerViewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemRecyclerViewViewHolder holder, int position) {
        MenuItem menuItem = menuItems.get(position);

        holder.itemText.setText(menuItem.getItemName());
        holder.itemText.setTextColor(MyColor.getTitleTextColor(context));
        holder.menuIcon.setImageDrawable(menuItem.getIcon());
        holder.rightArrow.setImageDrawable(MyImage.changeDrawableColor(
                context, R.drawable.icon_right_arrow, MyColor.getButtonTintColor(context)));

        holder.itemView.setOnClickListener(v -> {
            switch (menuItem.getAction()) {
                case MenuItem.ACTION_FEEDBACK: {
                    Intent email = new Intent(Intent.ACTION_SEND);
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"appsbayarea@gmail.com"});
                    email.putExtra(Intent.EXTRA_SUBJECT,
                            HelperFunctions.getApplicationName(context) + " - "
                                    + context.getResources().getString(R.string.Feedback));
                    email.putExtra(Intent.EXTRA_TEXT, "");
                    email.setType("message/rfc822");
                    context.startActivity(Intent.createChooser(email, "Choose an Email client:"));
                    break;
                }
                case MenuItem.ACTION_RATE:
                    StoreHelper.goToGoogleMarket(context, context.getPackageName());
                    break;
                case MenuItem.ACTION_SHARE:
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                                HelperFunctions.getApplicationName(context));
                        String shareMessage = "https://play.google.com/store/apps/details?id="
                                + context.getPackageName() + "\n";
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        context.startActivity(Intent.createChooser(shareIntent, "choose one"));
                    } catch (Exception ignored) {
                    }
                    break;
                case MenuItem.ACTION_REMOVE_ADS:
                    if (context instanceof Activity) {
                        BillingManager.get(context).launchRemoveAdsPurchase((Activity) context);
                    }
                    break;
                case MenuItem.ACTION_WATCH_AD_FREE:
                    if (context instanceof Activity) {
                        RewardedAdHelper.get(context)
                                .showForTwentyFourHourAdFree((Activity) context);
                    }
                    break;
                case MenuItem.ACTION_RESTORE:
                    BillingManager.get(context).restorePurchases();
                    break;
                case MenuItem.ACTION_MORE_APPS:
                    context.startActivity(new Intent(context, MoreAppsActivity.class));
                    break;
                case MenuItem.ACTION_BACKGROUND:
                    context.startActivity(new Intent(context, ImagesActivity.class));
                    break;
                case MenuItem.ACTION_LANGUAGE:
                    showLanguagePicker();
                    break;
                case MenuItem.ACTION_SCRIPT:
                    showScriptPicker();
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Applies to the app's own strings only - book text ships in assets and
     * stays in its original language.
     */
    private void showLanguagePicker() {
        String[] tags = LocaleHelper.tags();
        int[] checked = {LocaleHelper.getSelectedIndex()};
        androidx.appcompat.app.AlertDialog dialog = DialogChrome.alert(context)
                .setTitle(R.string.Language)
                .setSingleChoiceItems(LocaleHelper.labels(context), checked[0],
                        (d, which) -> checked[0] = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        (d, which) -> LocaleHelper.apply(tags[checked[0]]))
                .create();
        dialog.setOnShowListener(d -> DialogChrome.styleAlertDialog(dialog, context));
        dialog.show();
    }

    private void showScriptPicker() {
        String[] scripts = {
                context.getString(R.string.book_script_simplified),
                context.getString(R.string.book_script_traditional)
        };
        int[] checked = {BookStore.shared.usesTraditional(context) ? 1 : 0};
        androidx.appcompat.app.AlertDialog dialog = DialogChrome.alert(context)
                .setTitle(R.string.book_script)
                .setSingleChoiceItems(scripts, checked[0], (d, which) -> checked[0] = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    BookStore.shared.setTraditional(context, checked[0] == 1);
                    if (context instanceof Activity) ((Activity) context).recreate();
                })
                .create();
        dialog.setOnShowListener(d -> DialogChrome.styleAlertDialog(dialog, context));
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public class MenuItemRecyclerViewViewHolder extends RecyclerView.ViewHolder {

        TextView itemText;
        ImageView menuIcon;
        ImageView rightArrow;

        public MenuItemRecyclerViewViewHolder(@NonNull View itemView) {
            super(itemView);
            itemText = itemView.findViewById(R.id.row_menu_textView);
            menuIcon = itemView.findViewById(R.id.row_menu_icon);
            rightArrow = itemView.findViewById(R.id.row_menu_arrow);
        }
    }
}
