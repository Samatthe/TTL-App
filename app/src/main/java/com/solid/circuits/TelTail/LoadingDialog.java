package com.solid.circuits.TelTail;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

class LoadingDialog {
    Activity activity;
    AlertDialog dialog;

    ProgressBar fwProgressBar;
    TextView fwProgressText;
    View view;

    LoadingDialog(Activity myActivity){
        activity = myActivity;
    }

    void startLoadingDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        view = inflater.inflate(R.layout.loading_dialog, null);
        builder.setView(view);
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        fwProgressBar = view.findViewById(R.id.fwProgressBar);
        fwProgressText = view.findViewById(R.id.fwProgressText);
    }

    void dismissDialog(){
        dialog.dismiss();
    }

    boolean DialogIsDismissed(){
        return !(dialog.isShowing());
    }
}
