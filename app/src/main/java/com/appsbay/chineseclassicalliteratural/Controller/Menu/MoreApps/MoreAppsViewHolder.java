package com.appsbay.chineseclassicalliteratural.Controller.Menu.MoreApps;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.StoreHelper;

public class MoreAppsViewHolder extends RecyclerView.ViewHolder {

    private final TextView name;
    private final TextView description;
    private final ImageView appIcon;
    private final ImageView chevron;

    public MoreAppsViewHolder(@NonNull View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.textView_appName);
        description = itemView.findViewById(R.id.textView_appDescription);
        appIcon = itemView.findViewById(R.id.imageView_appIcon);
        chevron = itemView.findViewById(R.id.imageView2);
    }

    public void bind(final MoreApp moreApp) {
        name.setText(moreApp.getName());
        name.setTextColor(MyColor.getTitleTextColor(itemView.getContext()));

        description.setText(moreApp.getDescription());
        description.setTextColor(MyColor.getDetailTextColor(itemView.getContext()));

        appIcon.setImageResource(moreApp.getIconRes());
        chevron.setColorFilter(MyColor.getButtonTintColor(itemView.getContext()));

        itemView.setOnClickListener(v ->
                StoreHelper.goToMarket(itemView.getContext(), moreApp.getPackageName()));
    }
}
