package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;
import com.aware.plugin.notificationdiary.ContentAnalysis.ClusterGenerator;
import com.aware.plugin.notificationdiary.ContentAnalysis.Graph;
import com.aware.plugin.notificationdiary.ContentAnalysis.Node;
import com.aware.plugin.notificationdiary.NotificationObject.DiaryNotification;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.plugin.notificationdiary.Providers.WordBins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ContentAnalysisService extends Service {
    private final String TAG = "AnalysisService";
    public static final int SHOW_NOTIFICATION = 1;
    public static final int HIDE_NOTIFICATION = 0;
    public static final String UNKNOWN = "UNKNOWN";

    final int NUM_CLUSTERS = 20;

    private Context context;

    private ArrayList<String> stopWordsEng;
    private ArrayList<String> stopWordsFin;

    private J48 tree;
    private NaiveBayes bayes;

    public ContentAnalysisService() {
        context = this;
    }

    @Override
    public int onStartCommand(Intent i, int i1, int it2) {
        Log.d(TAG, "started");
        stopWordsEng = new ArrayList<>(Arrays.asList(Utils.readStopWords(this, R.raw.english)));
        stopWordsFin = new ArrayList<>(Arrays.asList(Utils.readStopWords(this, R.raw.finnish)));

        new CoreRunnable(context).execute(context);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class CoreRunnable extends AsyncTask<Context, Integer, Long> {
        Context context;

        public CoreRunnable(Context c1) {
            this.context = c1;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            Intent step = new Intent();
            step.setAction(ClassifierProgressReceiver.ACTION);
            step.putExtra(ClassifierProgressReceiver.PROGRESS, progress[0]);
            step.putExtra(ClassifierProgressReceiver.PROGRESS_LABEL, progress[0] + "% complete");
            sendBroadcast(step);
        }

        @Override
        protected Long doInBackground(Context... params) {
            context = params[0];
            Log.d(TAG, "running core runnable");
            publishProgress(0);

            ClusterGenerator gen = new ClusterGenerator(new Graph(context).getNodes(), NUM_CLUSTERS);
            publishProgress(20);

            WordBins helper = new WordBins(context);

            publishProgress(40);

            helper.storeClusters(gen.getClusters());

            publishProgress(50);

            Instances data = buildClassifier(context);

            publishProgress(70);

            try {
                Evaluation eval = new Evaluation(data);
                eval.crossValidateModel(tree, data, 10, new Random(System.nanoTime()));
                Log.d(TAG, "J48 Cross validation: " + eval.toSummaryString());
                Log.d(TAG, "J48 ROC: " + eval.areaUnderROC(data.classIndex()));

                eval = new Evaluation(data);
                eval.crossValidateModel(bayes, data, 10, new Random(System.nanoTime()));
                Log.d(TAG, "NB Cross validation: " + eval.toSummaryString());
                Log.d(TAG, "NB ROC: " + eval.areaUnderROC(data.classIndex()));
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Error when cross-validating");
            }

            publishProgress(100);

            Intent stopIntent = new Intent(context, ContentAnalysisService.class);
            stopService(stopIntent);

            long value = 1;
            return value;
        }

        private Instances buildClassifier(Context c) {
            // unsupervised training
            String[] options = new String[1];
            options[0] = "-U";

            tree = new J48();
            bayes = new NaiveBayes();
            try {
                tree.setOptions(options);
                bayes.setOptions(options);
            }
            catch (Exception e) {e.printStackTrace();}

            UnsyncedData helper = new UnsyncedData(c);
            ArrayList<UnsyncedNotification> notifications = helper.getLabeledNotifications();
            helper.close();

            Log.d(TAG, "buildClassifier: Extracting");
            WordBins helper2 = new WordBins(c);
            ArrayList<Cluster> clusters = helper2.extractClusters(c);

            // attributes are notification context + voice bins + class attribute
            ArrayList<Attribute> attributes = new ArrayList<>(DiaryNotification.CONTEXT_ATTRIBUTE_COUNT+NUM_CLUSTERS+1);

            List<String> classValues = new ArrayList<>();
            classValues.add("hide");
            classValues.add("show");
            // TODO decide later if you want to use this label
            //classValues.add("defer");
            Attribute labelAttr = new Attribute("label", classValues);
            attributes.add(labelAttr);

            attributes.add(new Attribute("application_package"));
            attributes.add(new Attribute("notification_category"));
            attributes.add(new Attribute("location"));
            attributes.add(new Attribute("activity"));
            attributes.add(new Attribute("headphone_jack"));
            attributes.add(new Attribute("screen_mode"));
            attributes.add(new Attribute("ringer_mode"));
            attributes.add(new Attribute("battery_level"));
            attributes.add(new Attribute("network_availability"));
            attributes.add(new Attribute("wifi_availability"));
            attributes.add(new Attribute("foreground_application_package"));
            for (int i = 0; i < NUM_CLUSTERS; i++) {
                attributes.add(new Attribute("wordbin" + i));
            }
            Instances training_data = new Instances("labeled_notifications", attributes, notifications.size());
            training_data.setClassIndex(0);

            double word_count;
            ArrayList<String> words;
            // add training data as instances
            for (UnsyncedNotification n : notifications) {
                Instance instance = new DenseInstance(DiaryNotification.CONTEXT_ATTRIBUTE_COUNT+NUM_CLUSTERS+1);
                // CLASS LABELS
                // click means always show
                if (n.interaction_type.equals(AppManagement.INTERACTION_TYPE_CLICK)) {
                    //instance.setValue(attributes.get(0), attributes.get(0).addStringValue(SHOW_NOTIFICATION));
                    instance.setValue(attributes.get(0), 1);
                }
                // if both are null, result is unknown so should SHOW
                else if (n.timing_value == null & n.content_importance_value == null) {
                    instance.setValue(attributes.get(0), 1);
                }
                // if timing is unsure but contents are important SHOW
                else if (n.timing_value == null & n.content_importance_value != null && n.content_importance_value >= 3) {
                    instance.setValue(attributes.get(0), 1);
                }
                // if timing is unsure but contents are irrelevant HIDE
                else if (n.timing_value == null & n.content_importance_value != null && n.content_importance_value < 3) {
                    //instance.setValue(attributes.get(0), attributes.get(0).addStringValue(HIDE_NOTIFICATION));
                    instance.setValue(attributes.get(0), 0);
                }
                // if content is unsure but timing was approriate SHOW
                else if ((n.content_importance_value == null) && (n.timing_value >= 3)) {
                    instance.setValue(attributes.get(0), 1);
                }
                else if ((n.content_importance_value == null) && (n.timing_value < 3)) {
                    instance.setValue(attributes.get(0), 1);
                }
                else if (n.content_importance_value != null && n.timing_value != null && (n.content_importance_value * 0.7)+(n.timing_value * 0.3) > 3) {
                    instance.setValue(attributes.get(0), 1);
                }
                else {
                    instance.setValue(attributes.get(0), 0);
                }

                // add remaining raw values
                if (training_data.attribute(1).indexOfValue(n.application_package) < 0) { training_data.attribute(1).addStringValue(n.application_package);}
                instance.setValue(attributes.get(1), training_data.attribute(1).indexOfValue(n.application_package));

                if (training_data.attribute(2).indexOfValue(n.notification_category) < 0) { training_data.attribute(2).addStringValue(n.notification_category);}
                instance.setValue(attributes.get(2), attributes.get(2).addStringValue(n.notification_category));

                if (training_data.attribute(3).indexOfValue(n.location) < 0) { training_data.attribute(3).addStringValue(n.location);}
                instance.setValue(attributes.get(3), attributes.get(3).addStringValue(n.location));

                if (training_data.attribute(4).indexOfValue(n.activity) < 0) { training_data.attribute(4).addStringValue(n.activity);}
                instance.setValue(attributes.get(4), attributes.get(4).addStringValue(n.activity));

                if (training_data.attribute(5).indexOfValue(n.headphone_jack) < 0) { training_data.attribute(5).addStringValue(n.headphone_jack);}
                instance.setValue(attributes.get(5), attributes.get(5).addStringValue(n.headphone_jack));

                if (training_data.attribute(6).indexOfValue(n.screen_mode) < 0) { training_data.attribute(6).addStringValue(n.screen_mode);}
                instance.setValue(attributes.get(6), attributes.get(6).addStringValue(n.screen_mode));

                if (training_data.attribute(7).indexOfValue(n.ringer_mode) < 0) { training_data.attribute(7).addStringValue(n.ringer_mode);}
                instance.setValue(attributes.get(7), attributes.get(7).addStringValue(n.ringer_mode));

                if (training_data.attribute(8).indexOfValue(n.battery_level) < 0) { training_data.attribute(8).addStringValue(n.battery_level);}
                instance.setValue(attributes.get(8), (Double.valueOf(n.battery_level)));

                if (training_data.attribute(9).indexOfValue(n.network_availability) < 0) { training_data.attribute(9).addStringValue(n.network_availability);}
                instance.setValue(attributes.get(9), attributes.get(9).addStringValue(n.network_availability));

                if (training_data.attribute(10).indexOfValue(n.wifi_availability) < 0) { training_data.attribute(10).addStringValue(n.wifi_availability);}
                instance.setValue(attributes.get(10), attributes.get(10).addStringValue(n.wifi_availability));

                if (training_data.attribute(11).indexOfValue(n.foreground_application_package) < 0) { training_data.attribute(11).addStringValue(n.foreground_application_package);}
                instance.setValue(attributes.get(11), attributes.get(11).addStringValue(n.foreground_application_package));

                // word bins
                for (int i = 0; i < NUM_CLUSTERS; i++) {
                    word_count = 0;
                    Cluster cluster = clusters.get(i);
                    words = strip(n.title, n.message);
                    for (Node node : cluster.getNodes()) {
                        if (words.contains(node.getValue())) word_count++;
                    }
                    instance.setValue(attributes.get(i+DiaryNotification.CONTEXT_ATTRIBUTE_COUNT+1), word_count);
                }

                training_data.add(instance);
            }
            Log.d(TAG, "hep");
            Log.d(TAG, "num: " + training_data.size());
            Log.d(TAG, "class: " + training_data.classAttribute().toString() + " (" + training_data.classIndex() + ") / " + training_data.numAttributes());
            Log.d(TAG, training_data.toSummaryString());

            try {
                tree.buildClassifier(training_data);
                bayes.buildClassifier(training_data);
            } catch (Exception e) {
                Log.d(TAG, "Error building J48 classifier");
                e.printStackTrace();
            }

            return training_data;
        }
    }

    public ArrayList<String> strip(String title, String contents) {
        String a = title + " " + contents;
        a = a.toLowerCase().replaceAll("^[a-zA-Z0-9äöüÄÖÜ]", " ");
        ArrayList<String> words = new ArrayList<>(Arrays.asList(a.split(" ")));
        words.removeAll(stopWordsEng);
        words.removeAll(stopWordsFin);
        return words;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ContentAnalysisService finished and destroyed");
    }

}
