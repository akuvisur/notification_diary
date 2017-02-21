package com.aware.plugin.notificationdiary;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aware.plugin.notificationdiary.Providers.UnsyncedData;

import java.util.ArrayList;

public class PredictionActivity extends AppCompatActivity {

    private static final String TAG = "PredictionActivity";

    private ArrayList<UnsyncedData.Prediction> predictions;

    TextView num_predictions;
    ListView prediction_list;
    Button accept_all;
    Button reject_all;

    private Context context;
    private UnsyncedData helper = null;

    private void closeDbConnection() {
        if (helper != null) {
            helper.close();
            helper = null;
        }
    }

    private void initDbConnection() {
        closeDbConnection();
        helper = new UnsyncedData(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        initDbConnection();

        predictions = new ArrayList<>();

        setContentView(R.layout.activity_prediction);

        setTitle("Predictions");

        prediction_list = (ListView) findViewById(R.id.predact_prediction_list);
        num_predictions = (TextView) findViewById(R.id.num_predictions);

        accept_all = (Button) findViewById(R.id.predact_acceptall);
        accept_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDbConnection();
                helper.batchUpdatePredictions(context, predictions, 1);
                predictions.clear();
                prediction_list.invalidate();
                adapter.notifyDataSetChanged();
                updateNumPredictions();
                closeDbConnection();

                finish();
            }
        });

        reject_all = (Button) findViewById(R.id.predact_rejectall);
        reject_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Reject all predictions");
                builder.setMessage("This will label all predictions as 'incorrect'. Are you sure?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        initDbConnection();
                        helper.batchUpdatePredictions(context, predictions, 0);
                        predictions.clear();
                        prediction_list.invalidate();
                        adapter.notifyDataSetChanged();
                        updateNumPredictions();
                        closeDbConnection();
                        dialog.cancel();
                        finish();
                    }
                });
                builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setIcon(R.mipmap.ic_launcher);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    PredictionListAdapter adapter;
    @Override
    public void onResume() {
        super.onResume();
        initDbConnection();
        predictions = helper.getPredictions(context);
        adapter = new PredictionListAdapter(this, R.layout.prediction_listitem, predictions, prediction_list);
        prediction_list.setAdapter(adapter);

        updateNumPredictions();
        closeDbConnection();
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
                    initDbConnection();
                    ContentValues c = new ContentValues();
                    c.put(UnsyncedData.Notifications_Table.prediction_correct, 1);
                    helper.updateEntry(context, (int) items.get(i).sqlite_id, c);

                    item_view.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            items.remove(i);
                            notifyDataSetChanged();
                            updateNumPredictions();
                        }
                    },400);
                    closeDbConnection();
                }
            });

            reject_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    initDbConnection();
                    ContentValues c = new ContentValues();
                    c.put(UnsyncedData.Notifications_Table.prediction_correct, 0);
                    helper.updateEntry(context, (int) items.get(i).sqlite_id, c);

                    item_view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_out_left));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            items.remove(i);
                            notifyDataSetChanged();
                            updateNumPredictions();
                        }
                    },400);
                    closeDbConnection();
                }
            });

            if (items.get(i).predicted_as_show == 0) {
                header.setBackgroundColor(ContextCompat.getColor(context, R.color.accent_yellow));
                header_text.setText("HIDDEN NOTIFICATION");
            }

            return item_view;
        }
    }

    @Override
    public void onDestroy() {
        initDbConnection();
        int count = (helper.getPredictions(context).size() + helper.countUnlabeledNotifications());
        if (count > 0) BadgeUtils.setBadge(context, count);
        else BadgeUtils.clearBadge(context);
        closeDbConnection();
        super.onDestroy();
    }

}
