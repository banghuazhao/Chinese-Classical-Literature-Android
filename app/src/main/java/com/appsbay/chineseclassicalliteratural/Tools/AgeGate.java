package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.button.MaterialButton;

/** Stores only the age group needed to decide whether advertising is allowed. */
public final class AgeGate {
    public static final int UNKNOWN = 0;
    public static final int UNDER_18 = 1;
    public static final int ADULT = 2;

    private static final String PREFS = "Age Group";
    private static final String KEY_AGE_GROUP = "ageGroup";

    private AgeGate() {
    }

    public static int getGroup(@NonNull Context context) {
        try {
            int group = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getInt(KEY_AGE_GROUP, UNKNOWN);
            return group == UNDER_18 || group == ADULT ? group : UNKNOWN;
        } catch (ClassCastException ignored) {
            return UNKNOWN;
        }
    }

    public static boolean isAdult(@NonNull Context context) {
        return getGroup(context) == ADULT;
    }

    public static boolean saveGroup(@NonNull Context context, int group) {
        if (group != UNDER_18 && group != ADULT) {
            throw new IllegalArgumentException("Choose an age group");
        }
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return preferences.edit().putInt(KEY_AGE_GROUP, group).commit();
    }

    public interface OnChoice {
        void onChoice(int group);
    }

    /** Equal-weight choices, without a default selection. */
    public static void showChoice(@NonNull Activity activity, boolean canCancel,
                                  @NonNull OnChoice onChoice) {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_age_choice, null);
        TextView messageView = content.findViewById(R.id.age_gate_message);
        MaterialButton under18Button = content.findViewById(R.id.age_gate_under_18);
        MaterialButton adultButton = content.findViewById(R.id.age_gate_adult);
        String message = activity.getString(R.string.age_gate_message);
        if (isAdult(activity)) {
            message += "\n\n" + activity.getString(R.string.age_gate_close_notice);
        }
        messageView.setText(message);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.age_gate_title)
                .setView(content)
                .setCancelable(canCancel)
                .create();
        under18Button.setOnClickListener(view -> {
            dialog.dismiss();
            onChoice.onChoice(UNDER_18);
        });
        adultButton.setOnClickListener(view -> {
            dialog.dismiss();
            onChoice.onChoice(ADULT);
        });
        dialog.show();
    }
}
