package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.aware.plugin.notificationdiary.Providers.UnsyncedData;

public class ProviderSyncService extends Service {
    private Context context;
    public ProviderSyncService() {
        context = this;
    }

    @Override
    public int onStartCommand(Intent i, int i1, int it2) {
        UnsyncedData h = new UnsyncedData(this);

        h.syncAlltoProvider(this);
        h.close();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
