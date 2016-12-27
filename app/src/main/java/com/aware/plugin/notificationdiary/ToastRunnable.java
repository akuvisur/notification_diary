package com.aware.plugin.notificationdiary;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

/**
 * Created by aku on 21/12/16.
 */
public class ToastRunnable implements Runnable {
    Context context;
    String message;
    int length;

    public ToastRunnable(Context c, @NonNull String message, @NonNull int length) {
        context = c;
        this.message = message;
        this.length = length;
    }

    @Override
    public void run() {
        Toast.makeText(context, message, length).show();
    }
}
