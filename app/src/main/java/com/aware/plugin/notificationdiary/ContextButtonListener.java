package com.aware.plugin.notificationdiary;

import android.content.Context;
import android.view.View;

/**
 * Created by aku on 02/12/16.
 */

public class ContextButtonListener implements View.OnClickListener {
    private final Context context;
    public ContextButtonListener(Context c) {
        this.context = c;
    }
    @Override
    public void onClick(View v) {
        // override pls
    }
}
