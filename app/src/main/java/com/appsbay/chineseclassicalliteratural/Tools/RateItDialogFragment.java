package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.appsbay.chineseclassicalliteratural.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RateItDialogFragment extends DialogFragment {
    private static final String PREF_NAME = "APP_RATER";
    private static final String LAUNCHES = "LAUNCHES";
    private static final String DISABLED = "DISABLED";
    private static final String TAG = "RateItDialog";

    public static void show(Context context, FragmentManager fragmentManager) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (sharedPreferences.getBoolean(DISABLED, false)) {
            return;
        }
        int launches = sharedPreferences.getInt(LAUNCHES, 0) + 1;
        sharedPreferences.edit().putInt(LAUNCHES, launches).apply();
        if (launches == 5 || launches == 25 || launches == 100) {
            if (fragmentManager.findFragmentByTag(TAG) != null) {
                return;
            }
            new RateItDialogFragment().show(fragmentManager, TAG);
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String appName = HelperFunctions.getApplicationName(requireContext());
        MaterialAlertDialogBuilder builder = DialogChrome.alert(requireContext())
                .setTitle(getString(R.string.rate_dialog_title, appName))
                .setMessage(R.string.rate_dialog_message)
                .setNegativeButton(R.string.rate_dialog_not_now, (dialog, which) -> dismiss())
                .setPositiveButton(R.string.rate_dialog_rate, (dialog, which) -> {
                    openPlayStore();
                    getSharedPreferences(requireContext()).edit().putBoolean(DISABLED, true).apply();
                    dismiss();
                })
                .setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> DialogChrome.styleAlertDialog(dialog, requireContext()));
        return dialog;
    }

    private void openPlayStore() {
        String packageName = requireActivity().getPackageName();
        Uri marketUri = Uri.parse("market://details?id=" + packageName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, marketUri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}
