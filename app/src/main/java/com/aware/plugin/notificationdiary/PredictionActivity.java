package com.aware.plugin.notificationdiary;

import android.content.ContentValues;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.plugin.notificationdiary.Providers.Provider;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;

import java.util.ArrayList;

public class PredictionActivity extends AppCompatActivity {

    private static final String TAG = "PredictionActivity";

    private ArrayList<UnsyncedData.Prediction> predictions;

    TextView num_predictions;
    ListView prediction_list;
    Button accept_all;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        predictions = new ArrayList<>();

        setContentView(R.layout.activity_prediction);

        setTitle("Predictions");

        prediction_list = (ListView) findViewById(R.id.predact_prediction_list);
        num_predictions = (TextView) findViewById(R.id.num_predictions);

        accept_all = (Button) findViewById(R.id.predact_acceptall);
        accept_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "clicked accept all");
                ArrayList<Long> ids = new ArrayList();

                for (UnsyncedData.Prediction p : predictions) {
                    ContentValues c = new ContentValues();
                    c.put(UnsyncedData.Notifications_Table.prediction_correct, 1);
                    c.put(UnsyncedData.Notifications_Table.synced, "1");

                    UnsyncedData ud = new UnsyncedData(context);
                    ud.updateEntry(context, (int) p.sqlite_id, c, false);
                    ids.add(p.sqlite_id);
                    ud.close();
                }

                for (Long id : ids) {
                    UnsyncedData ud = new UnsyncedData(context);
                    UnsyncedNotification n = ud.get(id, false);
                    if (n.interaction_type.equals(AppManagement.INTERACTION_TYPE_DISMISS) && n.labeled)
                        getContentResolver().insert(Provider.Notifications_Data.CONTENT_URI, n.toSyncableContentValues(context));
                    else if (!n.interaction_type.equals(AppManagement.INTERACTION_TYPE_DISMISS))
                        getContentResolver().insert(Provider.Notifications_Data.CONTENT_URI, n.toSyncableContentValues(context));
                    ud.close();
                }

                predictions.clear();
                prediction_list.invalidate();
                adapter.notifyDataSetChanged();
                updateNumPredictions();
            }
        });
    }

    PredictionListAdapter adapter;
    @Override
    public void onResume() {
        super.onResume();
        UnsyncedData ud = new UnsyncedData(this);
        predictions = ud.getPredictions(context);
        adapter = new PredictionListAdapter(this, R.layout.prediction_listitem, predictions, prediction_list);
        prediction_list.setAdapter(adapter);
        ud.close();

        updateNumPredictions();
    }

    private void updateNumPredictions() {
        num_predictions.setText("Total of " + predictions.size() + " predictions");
    }

    class PredictionListAdapter extends ArrayAdapter<UnsyncedData.Prediction> {
        ArrayList<UnsyncedData.Prediction> items;
        private Context context;
        private ListView parentView;

        public PredictionListAdapter(Context c, int layoutResId, ArrayList<UnsyncedData.Prediction> items, ListView parentView) {
            super(c, layoutResId, items);
            this.items = items;
            this.parentView = parentView;
            context = c;
        }

        TextView app_name;
        TextView title;
        TextView message;
        TextView time;
        Button accept_button;
        Button reject_button;

        RelativeLayout header;
        TextView header_text;

        @Override
        public View getView(final int i, final View view, ViewGroup viewGroup) {
            final View item_view = getLayoutInflater().inflate(R.layout.prediction_listitem, viewGroup, false);

            app_name = (TextView) item_view.findViewById(R.id.application);
            title = (TextView) item_view.findViewById(R.id.title);
            message = (TextView) item_view.findViewById(R.id.contents);
            time = (TextView) item_view.findViewById(R.id.timestamp);
            accept_button = (Button) item_view.findViewById(R.id.accept);
            reject_button = (Button) item_view.findViewById(R.id.incorrect);
            header = (RelativeLayout) item_view.findViewById(R.id.listview_header);
            header_text = (TextView) item_view.findViewById(R.id.listview_header_text);

            app_name.setText(AppManagement.getApplicationNameFromPackage(context, items.get(i).package_name));
            title.setText(items.get(i).title);
            message.setText(items.get(i).text);
            time.setText(AppManagement.getDate(context, items.get(i).timestamp));

            accept_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ContentValues c = new ContentValues();
                    c.put(UnsyncedData.Notifications_Table.prediction_correct, 1);
                    UnsyncedData ud = new UnsyncedData(context);
                    ud.updateEntry(context, (int) items.get(i).sqlite_id, c, true);

                    item_view.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            UnsyncedData ud = new UnsyncedData(context);
                            if (items.get(i).interaction_type.equals(AppManagement.INTERACTION_TYPE_DISMISS) && items.get(i).labeled > 0)
                                getContentResolver().insert(Provider.Notifications_Data.CONTENT_URI, ud.get(items.get(i).sqlite_id, false).toSyncableContentValues(context));
                            else if (!items.get(i).interaction_type.equals(AppManagement.INTERACTION_TYPE_DISMISS))
                                getContentResolver().insert(Provider.Notifications_Data.CONTENT_URI, ud.get(items.get(i).sqlite_id, false).toSyncableContentValues(context));
                            ContentValues synced = new ContentValues();
                            synced.put(UnsyncedData.Notifications_Table.synced, "1");
                            ud.updateEntry(context, (int) items.get(i).sqlite_id, synced, false);

                            items.remove(i);
                            notifyDataSetChanged();
                            updateNumPredictions();
                        }
                    },400);
                }
            });

            reject_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContentValues c = new ContentValues();
                    c.put(UnsyncedData.Notifications_Table.prediction_correct, 0);
                    UnsyncedData ud = new UnsyncedData(context);
                    ud.updateEntry(context, (int) items.get(i).sqlite_id, c, true);

                    item_view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_out_left));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            UnsyncedData ud = new UnsyncedData(context);
                            if (items.get(i).interaction_type.equals(AppManagement.INTERACTION_TYPE_DISMISS) && items.get(i).labeled > 0)
                                getContentResolver().insert(Provider.Notifications_Data.CONTENT_URI, ud.get(items.get(i).sqlite_id, false).toSyncableContentValues(context));
                            else if (!items.get(i).interaction_type.equals(AppManagement.INTERACTION_TYPE_DISMISS))
                                getContentResolver().insert(Provider.Notifications_Data.CONTENT_URI, ud.get(items.get(i).sqlite_id, false).toSyncableContentValues(context));

                            ContentValues synced = new ContentValues();
                            synced.put(UnsyncedData.Notifications_Table.synced, "1");
                            ud.updateEntry(context, (int) items.get(i).sqlite_id, synced, false);

                            items.remove(i);
                            notifyDataSetChanged();
                            updateNumPredictions();
                        }
                    },400);
                }
            });

            if (items.get(i).predicted_as_show == 0) {
                header.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_yellow));
                header_text.setText("HIDDEN NOTIFICATION");
            }

            return item_view;
        }
    }



}
