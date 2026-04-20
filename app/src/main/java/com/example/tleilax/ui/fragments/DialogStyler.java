package com.example.tleilax.ui.fragments;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

final class DialogStyler {

    private static final float DIALOG_DIM_AMOUNT = 0.08f;
    private static final int DIALOG_BOTTOM_OFFSET_DP = 28;

    private DialogStyler() {
    }

    @NonNull
    static AlertDialog createBottomDialog(@NonNull Context context, @NonNull View dialogView) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setDimAmount(DIALOG_DIM_AMOUNT);
            dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        }
        return dialog;
    }

    static void showBottomDialog(@NonNull Context context, @NonNull AlertDialog dialog) {
        dialog.show();
        if (dialog.getWindow() == null) {
            return;
        }
        int bottomOffset = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DIALOG_BOTTOM_OFFSET_DP,
                context.getResources().getDisplayMetrics()
        );
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dialog.getWindow().getAttributes().y = bottomOffset;
    }
}
