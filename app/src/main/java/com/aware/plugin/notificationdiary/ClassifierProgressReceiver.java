package com.aware.plugin.notificationdiary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;

/**
 * Created by aku on 02/12/16.
 */
public class ClassifierProgressReceiver extends BroadcastReceiver {
    public static final String ACTION = "CLASSIFIER_PROGRESS_CHANGED";
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_LABEL = "progress_label";

    RoundCornerProgressBar bar;
    TextView text;
    Context context;
    Button generate_new;

    public int progress = 0;

    public ClassifierProgressReceiver(RoundCornerProgressBar bar, TextView text, Context c, Button generate_new) {
        this.bar = bar;
        this.text = text;
        this.context = c;
        this.generate_new = generate_new;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        progress = intent.getExtras().getInt(PROGRESS);
        bar.setProgress(intent.getExtras().getInt(PROGRESS));
        bar.setSecondaryProgress(intent.getExtras().getInt(PROGRESS)+10 > 100 ? 100 : intent.getExtras().getInt(PROGRESS));
        text.setText(intent.getExtras().getString(PROGRESS_LABEL));
        bar.invalidate();
        text.invalidate();
        if (generate_new != null) generate_new.setEnabled(progress == 100);
    }
}
