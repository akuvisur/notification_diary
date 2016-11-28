package com.aware.plugin.notificationdiary;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by aku on 24/11/16.
 */
public class ActivityService extends IntentService{

    public static final String UNCERTAIN = "uncertain";
    public static final String IN_VEHICLE = "in_vehicle";
    public static final String ON_BICYCLE = "on_bicycle";
    public static final String ON_FOOT = "on_foot";
    public static final String RUNNING = "running";
    public static final String STILL = "still";
    public static final String TILTING = "tilting";
    public static final String WALKING = "walking";
    public static final String UNKNOWN = "unknown";

    public ActivityService() {
        super("ActivityRecognizedService");
    }

    public ActivityService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities( result.getProbableActivities() );
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        for( DetectedActivity activity : probableActivities ) {
            if (activity.getConfidence() < 75) {
                NotificationListener.ACTIVITY = UNCERTAIN;
            }
            else {
                switch( activity.getType() ) {
                    case DetectedActivity.IN_VEHICLE: {
                        Log.e( "ActivityRecogition", "In Vehicle: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = IN_VEHICLE;
                        break;
                    }
                    case DetectedActivity.ON_BICYCLE: {
                        Log.e( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = ON_BICYCLE;
                        break;
                    }
                    case DetectedActivity.ON_FOOT: {
                        Log.e( "ActivityRecogition", "On Foot: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = ON_FOOT;
                        break;
                    }
                    case DetectedActivity.RUNNING: {
                        Log.e( "ActivityRecogition", "Running: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = RUNNING;
                        break;
                    }
                    case DetectedActivity.STILL: {
                        Log.e( "ActivityRecogition", "Still: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = STILL;
                        break;
                    }
                    case DetectedActivity.TILTING: {
                        Log.e( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = TILTING;
                        break;
                    }
                    case DetectedActivity.WALKING: {
                        Log.e( "ActivityRecogition", "Walking: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = WALKING;
                        break;
                    }
                    case DetectedActivity.UNKNOWN: {
                        Log.e( "ActivityRecogition", "Unknown: " + activity.getConfidence() );
                        NotificationListener.ACTIVITY = NotificationListener.UNKNOWN;
                        break;
                    }
                }
            }
        }
    }
}
