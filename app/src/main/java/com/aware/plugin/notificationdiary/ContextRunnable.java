package com.aware.plugin.notificationdiary;

import android.content.Context;

/**
 * Created by aku on 02/12/16.
 */

public class ContextRunnable implements Runnable {
    private Context context;
    public ContextRunnable(Context c) {
        context = c;
    }

    @Override
    public void run() {
        // pls override
    }
}
