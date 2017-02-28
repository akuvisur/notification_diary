package com.aware.plugin.notificationdiary;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Screen;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

/**
 * Created by aku on 24/11/16.
 */
public class ActivityApiClient implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "ActivityApiClient";

    private static GoogleApiClient mApiClient;

    private Context context;

    public ActivityApiClient(Context c) {
        this.context = c;
        mApiClient = new GoogleApiClient.Builder(c)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "connected");
        if (NotificationListener.SCREEN_STATE.equals(Screen.ACTION_AWARE_SCREEN_OFF)) screenOff(context);
        else screenOn(context);
    }

    static void screenOff(Context c) {
        try {
            Intent intent = new Intent(c, ActivityService.class);
            PendingIntent pendingIntent = PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 600000, pendingIntent);
        }
        catch (Exception e) {
            Log.d(TAG, "GoogleApiClient not connected");
        }
    }

    static void screenOn(Context c) {
        try {
            Intent intent = new Intent(c, ActivityService.class );
            PendingIntent pendingIntent = PendingIntent.getService(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( mApiClient, 30000, pendingIntent );
        }
        catch (Exception e) {
            Log.d(TAG, "GoogleApiClient not connected");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "conncetion failed");
    }
}
