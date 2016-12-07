package com.aware.plugin.notificationdiary.Providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aware.plugin.notificationdiary.ContentAnalysis.EvaluationResult;

import weka.core.converters.C45Loader;

/**
 * Created by aku on 07/12/16.
 */

public class J48Classifiers extends SQLiteOpenHelper {
    private static final String TAG = "J48Classifiers.class";

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "J48_classifiers";

    public J48Classifiers(Context context)  {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    SQLiteDatabase database;
    public void init() {
        if (database == null) {database = this.getWritableDatabase();}
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating");
        db.execSQL(CLASSIFIERS_CREATE_TABLE);
        Log.d(TAG, "Created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "updating");
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        onCreate(db);
    }

    public long insertRecord(ContentValues values) {
        init();
        long id = database.insert(DATABASE_NAME, null, values);
        database.close();
        return id;
    }

    public EvaluationResult getCurrentClassifier() {
        init();
        EvaluationResult result = new EvaluationResult(0.0,0.0,1.0,1.0,0.0,15);;
        Cursor cursor = database.query(DATABASE_NAME,
                null,
                null,
                null,
                null,
                null,
                Classifiers_Table.generate_timestamp + " DESC LIMIT 1");
        if (cursor.moveToFirst()) {
            result = new EvaluationResult(
                    cursor.getDouble(cursor.getColumnIndex(Classifiers_Table.accuracy)),
                    cursor.getDouble(cursor.getColumnIndex(Classifiers_Table.roc_area)),
                    cursor.getDouble(cursor.getColumnIndex(Classifiers_Table.show_false_positive)),
                    cursor.getDouble(cursor.getColumnIndex(Classifiers_Table.hide_false_positive)),
                    cursor.getDouble(cursor.getColumnIndex(Classifiers_Table.kappa)),
                    cursor.getInt(cursor.getColumnIndex(Classifiers_Table.num_clusters))
            );
            cursor.close();
        }
        database.close();
        return result;
    }

    public static final class Classifiers_Table implements Provider.AWAREColumns {
        public static String classifier_id = "classifier_id";
        public static String generate_timestamp = "generate_timestamp";
        public static String num_instances = "num_instances";
        public static String accuracy = "accuracy";
        public static String roc_area = "roc_area";
        public static String show_false_positive = "show_false_positive";
        public static String hide_false_positive = "hide_false_positive";
        public static String kappa = "kappa";
        public static String num_clusters = "num_clusters";
    }

    private static final String CLASSIFIERS_CREATE_TABLE =
        "CREATE TABLE " + DATABASE_NAME + "  (" +
            Classifiers_Table._ID + " integer primary key autoincrement, " +
            Classifiers_Table.classifier_id + " integer, " +
            Classifiers_Table.generate_timestamp + " real, " +
            Classifiers_Table.num_instances + " integer, " +
            Classifiers_Table.accuracy + " real, " +
            Classifiers_Table.roc_area + " real, " +
            Classifiers_Table.show_false_positive + " real, " +
            Classifiers_Table.hide_false_positive + " real, " +
            Classifiers_Table.kappa + " real, " +
            Classifiers_Table.num_clusters + " integer" +
            ");";

}