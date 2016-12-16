package com.aware.plugin.notificationdiary.Providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;
import com.aware.plugin.notificationdiary.ContentAnalysis.Node;

import java.util.ArrayList;
import java.util.HashMap;

import weka.core.Instances;

/**
 * Created by aku on 02/12/16.
 */

public class WordBins extends SQLiteOpenHelper {
    private static final String TAG = "WordBins.class";

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "wordbins";

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "Creating");
        sqLiteDatabase.execSQL(NOTIFICATIONS_CREATE_TABLE);
        Log.d(TAG, "Created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(TAG, "updating");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(sqLiteDatabase);
    }

    public static final class Bins_Table implements BaseColumns {
        //Note: integers and strings don't need a type prefix_
        public static String WORD = "word";
        public static String DISTANCE = "distance";
        public static String BIN_ID = "bin_id";

    }

    //Define each database table fields
    private static final String NOTIFICATIONS_CREATE_TABLE =
            "CREATE TABLE " + DATABASE_NAME + "  (" +
                    WordBins.Bins_Table._ID + " INTEGER primary key autoincrement, " +
                    Bins_Table.BIN_ID + " INTEGER, " +
                    Bins_Table.WORD + " TEXT, " +
                    Bins_Table.DISTANCE + " INTEGER)";

    public WordBins(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    SQLiteDatabase database;
    public void init() {
        if (database == null) {
            database = this.getWritableDatabase();
        }
        else if (!database.isOpen()) {
            this.getWritableDatabase();
        }
    }

    public ArrayList<Cluster> extractAllClusters(Context context, boolean closeAfter) {
        init();
        HashMap<Integer, Cluster> result = new HashMap<>();

        Cursor cursor = database.query(DATABASE_NAME, null, null, null, null, null, null);
        int rowcount = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                rowcount++;
                if (!result.keySet().contains(cursor.getInt(cursor.getColumnIndex(Bins_Table.BIN_ID)))) {
                    Cluster c = new Cluster(new Node("post_created"));
                    result.put(cursor.getInt(cursor.getColumnIndex(Bins_Table.BIN_ID)), c);
                }
                result.get(cursor.getInt(cursor.getColumnIndex(Bins_Table.BIN_ID))).addNode(
                        cursor.getInt(cursor.getColumnIndex(Bins_Table.DISTANCE)),
                        new Node(cursor.getString(cursor.getColumnIndex(Bins_Table.WORD)))
                );
            }
            cursor.close();
        }

        ArrayList<Cluster> result_list = new ArrayList<>();
        for (Integer key : result.keySet()) {
            result_list.add(result.get(key));
        }
        if (closeAfter) database.close();

        return result_list;
    }

    public ArrayList<Integer> extractClusterIds(boolean closeAfter) {
        init();
        ArrayList<Integer> result = new ArrayList<>();
        Cursor cursor = database.query(
                DATABASE_NAME, new String[]{"DISTINCT("+Bins_Table.BIN_ID+")"},
                null,null,null,null,null
                );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(cursor.getInt(0));
            }
        }
        if (closeAfter) database.close();
        return result;
    }

    public Cluster extractCluster(int bin_id, boolean closeAfter) {
        Cluster result = new Cluster();
        // TODO FINISH THIS
        Cursor cursor = database.query(
                DATABASE_NAME, null,
                Bins_Table.BIN_ID + " =?",new String[]{String.valueOf(bin_id)},null,null,null
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.addNode(
                    cursor.getInt(cursor.getColumnIndex(Bins_Table.DISTANCE)),
                    new Node(cursor.getString(cursor.getColumnIndex(Bins_Table.WORD)))
                );
            }
        }
        if (closeAfter) database.close();
        return result;
    }

    public void storeClusters(ArrayList<Cluster> clusters, boolean closeAfter) {
        init();
        // always replace
        database.delete(DATABASE_NAME, null, null);

        database.beginTransaction();
        int cur_cluster = 0;
        try {
            ContentValues values = new ContentValues();
            for (Cluster cluster : clusters) {
                for (int d = 0; d <= cluster.max_depth; d++) {
                    for (Node node : cluster.getNodes(d)) {
                        values.put(Bins_Table.BIN_ID, cur_cluster);
                        values.put(Bins_Table.WORD, node.getValue());
                        values.put(Bins_Table.DISTANCE, d);
                        database.insert(DATABASE_NAME, null, values);
                    }
                }
                cur_cluster++;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        if (closeAfter) database.close();
    }
}
