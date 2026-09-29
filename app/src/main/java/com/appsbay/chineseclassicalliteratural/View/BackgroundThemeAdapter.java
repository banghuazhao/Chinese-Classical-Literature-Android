package com.appsbay.chineseclassicalliteratural.View;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class BackgroundThemeAdapter extends RecyclerView.Adapter<BackgroundThemeAdapter.Holder> {

    public interface OnThemeSelectedListener {
        void onThemeSelected(String themeId);
    }

    private static final float STROKE_SELECTED_DP = 2f;

    private final List<String> themes;
    private final String selectedTheme;
    private final OnThemeSelectedListener listener;

    public BackgroundThemeAdapter(List<String> themes,
                                  String selectedTheme,
                                  OnThemeSelectedListener listener) {
        this.themes = themes;
        this.selectedTheme = selectedTheme;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(themes.get(position), selectedTheme, listener);
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView image;
        private final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.background_cardView);
            image = itemView.findViewById(R.id.background_imageView);
            label = itemView.findViewById(R.id.background_image_name);
        }

        void bind(String themeId, String selectedTheme, OnThemeSelectedListener listener) {
            image.setImageDrawable(null);
            image.setBackgroundColor(Color.TRANSPARENT);
            label.setText("");
            label.setTextColor(Color.parseColor("#1C1917"));

            switch (themeId) {
                case "system":
                    image.setBackgroundColor(Color.parseColor("#E8E4DC"));
                    label.setText(R.string.BackgroundSystem);
                    break;
                case "default":
                    image.setBackgroundColor(Color.parseColor("#FAF6EE"));
                    label.setText(R.string.BackgroundDefault);
                    break;
                case "white":
                    image.setBackgroundColor(Color.parseColor("#FFFEFF"));
                    break;
                case "green":
                    image.setBackgroundColor(Color.parseColor("#E7F3E8"));
                    break;
                case "dark":
                    image.setBackgroundColor(Color.parseColor("#16120E"));
                    label.setText(R.string.BackgroundDarkMode);
                    label.setTextColor(Color.WHITE);
                    break;
                case "bg":
                    image.setImageResource(R.drawable.bg);
                    break;
                case "bg1":
                    image.setImageResource(R.drawable.bg1);
                    break;
                case "bg2":
                    image.setImageResource(R.drawable.bg2);
                    break;
                case "bg3":
                    image.setImageResource(R.drawable.bg3);
                    break;
                case "bg4":
                    image.setImageResource(R.drawable.bg4);
                    break;
                case "bg5":
                    image.setImageResource(R.drawable.bg5);
                    break;
                default:
                    image.setBackgroundColor(Color.parseColor("#FAF6EE"));
                    break;
            }

            boolean selected = themeId.equals(selectedTheme);
            int stroke = selected
                    ? Math.round(STROKE_SELECTED_DP * itemView.getResources().getDisplayMetrics().density)
                    : 0;
            card.setStrokeWidth(stroke);
            card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThemeSelected(themeId);
                }
            });
        }
    }
}
