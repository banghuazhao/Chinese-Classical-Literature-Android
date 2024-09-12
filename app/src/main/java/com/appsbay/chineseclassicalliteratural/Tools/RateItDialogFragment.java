package com.appsbay.chineseclassicalliteratural.Tools;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.appsbay.chineseclassicalliteratural.R;


public class RateItDialogFragment extends DialogFragment {
    private static final String PREF_NAME = "APP_RATER";
    private static final String LAUNCHES = "LAUNCHES";
    private static final String DISABLED = "DISABLED";

    public static void show(Context context, FragmentManager fragmentManager) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!sharedPreferences.getBoolean(DISABLED, false)) {
            int launches = sharedPreferences.getInt(LAUNCHES, 0) + 1;
            if ((launches == 5) || (launches == 25) || (launches == 200)) {
                new RateItDialogFragment().show(fragmentManager, null);
            }
            editor.putInt(LAUNCHES, launches);
            editor.commit();
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.Enjoy) + " " + HelperFunctions.getApplicationName(getContext()) + "?")
                .setMessage(R.string.Rate)
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StoreHelper.goToMarket(getContext(), getContext().getPackageName());
                        getSharedPreferences(getActivity()).edit().putBoolean(DISABLED, true).commit();
                        dismiss();
                    }
                }).create();
    }
}