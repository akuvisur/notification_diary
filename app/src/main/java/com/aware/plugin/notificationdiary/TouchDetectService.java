package com.aware.plugin.notificationdiary;

/**
 * Created by aku on 28/11/16.
 */
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.aware.Applications;

public class TouchDetectService extends Applications {
    private static final String TAG = "TouchDetectService";

    AccessibilityServiceInfo info;
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "acc service connected");
        info = getServiceInfo();
        // Set the type of events that this service wants to listen to.  Others
        // won't be passed to this service.
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED | AccessibilityEvent.TYPE_TOUCH_INTERACTION_END;

        info.flags = AccessibilityServiceInfo.DEFAULT;

        info.notificationTimeout = 100;

        this.setServiceInfo(info);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int something) {
        super.onStartCommand(intent, flags, something);
        Log.d(TAG, "acc service started");
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "source: " + event.getPackageName());
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            Log.d(TAG, "click click");
        } else if (event.getEventType() == AccessibilityEvent.TYPE_TOUCH_INTERACTION_END) {
            Log.d(TAG, "touch touch");
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "acc service interrupted");
    }

}