package com.aware.plugin.notificationdiary.ContentAnalysis;

import android.util.Log;

import static com.google.android.gms.wearable.DataMap.TAG;

/**
 * Created by aku on 07/12/16.
 */

public class EvaluationResult {

    private static final String TAG = "EvaluationResult";

    public double accuracy;
    public double roc_area;
    public double show_false_positive;
    public double hide_false_positive;
    public double kappa;
    public int num_clusters;

    public EvaluationResult(
            double accuracy,
            double roc_area,
            double show_false_positive,
            double hide_false_positive,
            double kappa,
            int num_clusters
    ) {
        this.accuracy = accuracy;
        this.roc_area = roc_area;
        this.show_false_positive = show_false_positive;
        this.hide_false_positive = hide_false_positive;
        this.kappa = kappa;
        this.num_clusters = num_clusters;
    }

    public boolean isBetterThan(EvaluationResult e) {
        if (e == null) return true;
        return getOverallAccuracy() > e.getOverallAccuracy();
    }

    private double getOverallAccuracy() {
        Log.d(TAG, this.hashCode() + ": overall accuracy" + ((accuracy+roc_area)/2+((1-show_false_positive)+2*(1-hide_false_positive))/3+kappa)/3);
        return (
                ((accuracy+roc_area)/2+((1-show_false_positive)+2*(1-hide_false_positive))/3+kappa)/3
        );
    }

    @Override
    public String toString() {
        return "Accuracy: " + accuracy + "% ROC_AREA: " + roc_area + " SHOW fp: " + show_false_positive + " HIDE fp: " + hide_false_positive + " kappa: " + kappa + " clusters: " + num_clusters;
    }
}
