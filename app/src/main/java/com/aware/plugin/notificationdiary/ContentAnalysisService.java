package com.aware.plugin.notificationdiary;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;
import com.aware.plugin.notificationdiary.ContentAnalysis.ClusterGenerator;
import com.aware.plugin.notificationdiary.ContentAnalysis.EvaluationResult;
import com.aware.plugin.notificationdiary.ContentAnalysis.Graph;
import com.aware.plugin.notificationdiary.ContentAnalysis.Node;
import com.aware.plugin.notificationdiary.NotificationObject.AttributeWithType;
import com.aware.plugin.notificationdiary.NotificationObject.DiaryNotification;
import com.aware.plugin.notificationdiary.NotificationObject.UnsyncedNotification;
import com.aware.plugin.notificationdiary.Providers.J48Classifiers;
import com.aware.plugin.notificationdiary.Providers.Provider;
import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.plugin.notificationdiary.Providers.WordBins;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class ContentAnalysisService extends Service {
    private final String TAG = "AnalysisService";
    public static final String SHOW_NOTIFICATION = "SHOW_NOTIFICATION";
    public static final String HIDE_NOTIFICATION = "HIDE_NOTIFICATION";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String EMPTY_VALUE = "EMPTY_VALUE";

    public static final String RETURN_TO_MAIN = "RETURN_TO_MAIN";

    protected int OPTIMAL_NUM_CLUSTERS = 15;
    private EvaluationResult evaluationResult;
    private EvaluationResult previousEvaluationResult;

    private Context context;

    private ArrayList<String> stopWordsEng;
    private ArrayList<String> stopWordsFin;

    private J48 tree;
    private J48 bestTree;
    private RandomForest rand;

    private ArrayList<Cluster> bestClusters;
    private ArrayList<Cluster> curClusters;
    private Instances data;
    private Instances bestData;

    public ContentAnalysisService() {
        context = this;
    }

    private boolean restartmain = false;

    @Override
    public int onStartCommand(Intent i, int i1, int it2) {
        Log.d(TAG, "started");
        stopWordsEng = new ArrayList<>(Arrays.asList(Utils.readStopWords(this, R.raw.english)));
        stopWordsFin = new ArrayList<>(Arrays.asList(Utils.readStopWords(this, R.raw.finnish)));

        OPTIMAL_NUM_CLUSTERS = AppManagement.getNumClusters(context);

        restartmain = i.hasExtra(RETURN_TO_MAIN);

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

        private J48Classifiers tree_db;
        private WordBins wordBins;

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

        ClusterGenerator gen;
        @Override
        protected Long doInBackground(Context... params) {
            context = params[0];

            wordBins = new WordBins(context);
            bestClusters = wordBins.extractAllClusters(context, true);

            tree_db = new J48Classifiers(context);
            previousEvaluationResult = tree_db.getCurrentClassifier();

            try {
                bestTree = (J48) weka.core.SerializationHelper.read(context.getFilesDir() + "/J48.model");
            }
            catch (Exception e) {
                Log.d(TAG, "error reading classifier from file");
                e.printStackTrace();
            }

            Instances data = null;

            publishProgress(0);
            // iterate through 10,15,20,25,30 clusters to determine best wordbin amount
            for (int num_clusters = 2; num_clusters <= 6; num_clusters++) {
                gen = new ClusterGenerator(new Graph(context).getNodes(), num_clusters*5);
                curClusters = gen.getClusters();
                publishProgress(((num_clusters-1)*20)-15);

                data = buildTrainingData(context, num_clusters*5);
                publishProgress(((num_clusters-1)*20)-10);

                data = balanceTrainingData(data);
                publishProgress(((num_clusters-1)*20)-5);

                buildClassifier(data);

                publishProgress(((num_clusters-1)*20));
                evaluateClassifier(data, num_clusters*5, gen.getClusters());
            }

            if (previousEvaluationResult.isBetterThan(evaluationResult)) {
                Log.d(TAG, "previous result seemed to be better");
            }

            storeClassifier(bestData);

            wordBins.close();
            tree_db.close();


            publishProgress(100);

            if (restartmain) {
                Intent restartMain = new Intent(MainTabs.RESTART_MAIN_ACTIVITY);
                sendBroadcast(restartMain);
            }
            Intent stopIntent = new Intent(context, ContentAnalysisService.class);
            stopService(stopIntent);

            long value = 1;
            return value;
        }

        UnsyncedData helper;
        ArrayList<UnsyncedNotification> notifications;
        ArrayList<Attribute> attributes;
        List<String> classValues;
        HashMap<String, ArrayList<String>> attributeValuesMap;
        Instances training_data;
        ArrayList<String> words;

        private Instances buildTrainingData(Context c, int num_clusters) {
            // unsupervised training
            String[] options = new String[1];
            options[0] = "-C 0.33";
            tree = new J48();
            try {
                tree.setOptions(options);
            }
            catch (Exception e) {e.printStackTrace();}

            helper = new UnsyncedData(c);
            notifications = helper.getLabeledNotifications();
            helper.close();

            // attributes are notification context + voice bins + class attribute
            attributes = new ArrayList<>(DiaryNotification.CONTEXT_ATTRIBUTE_COUNT+num_clusters+1);

            classValues = new ArrayList<>();
            classValues.add(HIDE_NOTIFICATION);
            classValues.add(SHOW_NOTIFICATION);
            // TODO decide later if you want to use this label
            //classValues.add("defer");
            Attribute labelAttr = new Attribute("label", classValues);
            attributes.add(labelAttr);

            // add context variables
            attributeValuesMap = extractAttributeValues(notifications);

            for (AttributeWithType context_variable : UnsyncedNotification.getContextVariables()) {
                if (context_variable.type.equals(DiaryNotification.CONTEXT_VARIABLE_TYPE_STRING)) {
                    if (attributeValuesMap.containsKey(context_variable.name)) attributes.add(new Attribute(context_variable.name, attributeValuesMap.get(context_variable.name)));
                    else attributes.add(new Attribute(context_variable.name, new ArrayList<>(Arrays.asList(EMPTY_VALUE))));
                }
                else if (context_variable.type.equals(DiaryNotification.CONTEXT_VARIABLE_TYPE_DOUBLE)) {
                    attributes.add(new Attribute(context_variable.name));
                }
            }

            for (int i = 0; i < num_clusters; i++) {
                attributes.add(new Attribute("wordbin" + i));
            }

            training_data = new Instances("labeled_notifications", attributes, notifications.size());
            training_data.setClassIndex(0);

            double word_count;
            Instance instance;
            // add training data as instances
            for (UnsyncedNotification n : notifications) {
                instance = new DenseInstance(DiaryNotification.CONTEXT_ATTRIBUTE_COUNT + num_clusters + 1);
                // every instance needs to be associated to a dataset
                instance.setDataset(training_data);

                // if we have user verification on our training data
                if (n.prediction_correct > 0) {
                    if (n.predicted_as_show == 1) {
                        if (n.prediction_correct == 1) instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                        else instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(HIDE_NOTIFICATION));
                    }
                    else {
                        if (n.prediction_correct == 1) instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(HIDE_NOTIFICATION));
                        else instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                    }
                }
                else {
                    // CLASS LABELS
                    // click means always show
                    if (n.interaction_type.equals(AppManagement.INTERACTION_TYPE_CLICK)) {
                        //instance.setValue(attributes.getString(0), attributes.getString(0).addStringValue(SHOW_NOTIFICATION));
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                    }
                    // if both are null, result is unknown so should SHOW
                    else if (n.timing_value == null & n.content_importance_value == null) {
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                    }
                    // if timing is unsure but contents are important SHOW
                    else if (n.timing_value == null & n.content_importance_value != null && n.content_importance_value >= 3) {
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                    }
                    // if timing is unsure but contents are irrelevant HIDE
                    else if (n.timing_value == null & n.content_importance_value != null && n.content_importance_value < 3) {
                        //instance.setValue(attributes.getString(0), attributes.getString(0).addStringValue(HIDE_NOTIFICATION));
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(HIDE_NOTIFICATION));
                    }
                    // if content is unsure but timing was approriate SHOW
                    else if ((n.content_importance_value == null) && (n.timing_value >= 3)) {
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                    }
                    // if timing unapproriate HIDE
                    else if ((n.content_importance_value == null) && (n.timing_value < 3)) {
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(HIDE_NOTIFICATION));
                    }
                    // finally if both exist, calculate weighted average (timing * 0.3 + content * 0.7) SHOW
                    else if (n.content_importance_value != null && n.timing_value != null && (n.content_importance_value * 0.7) + (n.timing_value * 0.3) >= 3) {
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(SHOW_NOTIFICATION));
                    } // HIDE
                    else {
                        instance.setValue(attributes.get(0), training_data.attribute(0).indexOfValue(HIDE_NOTIFICATION));
                    }
                }
                // add remaining raw values
                for (AttributeWithType context_variable : UnsyncedNotification.getContextVariables()) {
                    if (context_variable.type.equals(DiaryNotification.CONTEXT_VARIABLE_TYPE_STRING) & attributeValuesMap.containsKey(context_variable.name)) {
                        instance.setValue(training_data.attribute(context_variable.name), n.getString(context_variable.name));
                    }
                    else if (context_variable.type.equals(DiaryNotification.CONTEXT_VARIABLE_TYPE_DOUBLE)) {
                        instance.setValue(training_data.attribute(context_variable.name), n.getDouble(context_variable.name));
                    }
                }

                // word bins
                for (int i = 0; i < curClusters.size(); i++) {
                    word_count = 0;
                    Cluster cluster = curClusters.get(i);
                    words = strip(n.title, n.message);
                    for (Node node : cluster.getNodes()) {
                        if (words.contains(node.getValue())) word_count++;
                    }
                    instance.setValue(attributes.get(i+DiaryNotification.CONTEXT_ATTRIBUTE_COUNT+1), word_count);
                }

                training_data.add(instance);
            }

            return training_data;
        }

        HashMap<String, ArrayList<String>> result;
        private HashMap<String, ArrayList<String>> extractAttributeValues(ArrayList<UnsyncedNotification> n) {
            result = new HashMap<>();

            for (UnsyncedNotification notification : n) {
                for (AttributeWithType context_variable : UnsyncedNotification.getContextVariables()) {
                    // doubles are numeric so do not need range of values
                    if (context_variable.type.equals(DiaryNotification.CONTEXT_VARIABLE_TYPE_DOUBLE)) continue;
                    // dont add nulls because doesnt do null checks
                    // add new list of possible attribute values if does not exist yet
                    if (!result.keySet().contains(context_variable.name)) {
                        ArrayList<String> empty = new ArrayList<String>();
                        empty.add(EMPTY_VALUE);
                        result.put(context_variable.name, empty);
                    }
                    // if no duplicate exists yet, add the possible value
                    if (notification.getString(context_variable.name) == null) result.get(context_variable.name).add(EMPTY_VALUE);
                    if (!result.get(context_variable.name).contains(notification.getString(context_variable.name))) result.get(context_variable.name).add(notification.getString(context_variable.name));
                }
            }
            return result;
        }

        private Instances balanceTrainingData(Instances d) {
            int show = 0, hide = 0;
            for (Instance inst : d) {
                if (inst.value(inst.classIndex()) == inst.classAttribute().indexOfValue(SHOW_NOTIFICATION)) show++;
                else hide++;
            }
            Collections.shuffle(d, new Random(System.nanoTime()));
            if (show > hide) {
                for (int i = 0; i < ((show-hide)/2); i++) {
                    d.remove(i);
                }
            }
            else if (hide > show) {
                for (int i = 0; i < ((hide-show)/2); i++) {
                    d.remove(i);
                }
            }
            return d;
        }

        private void buildClassifier(Instances data) {
            try {
                tree.buildClassifier(data);
            } catch (Exception e) {
                Log.d(TAG, "Error building J48 classifier");
                e.printStackTrace();
            }
            /*
            try {
                rand.buildClassifier(data);
            } catch (Exception e) {
                Log.d(TAG, "Error building RandomForest classifier");
                e.printStackTrace();
            }
            */
        }

        private void evaluateClassifier(Instances data, int num_clusters, ArrayList<Cluster> clusters) {
            try {
                Evaluation eval = new Evaluation(data);
                eval.crossValidateModel(tree, data, 10, new Random(System.nanoTime()));

                EvaluationResult e = new EvaluationResult(
                        eval.correct()/eval.numInstances(),
                        eval.areaUnderROC(data.classIndex()),
                        eval.falsePositiveRate(data.attribute(data.classIndex()).indexOfValue(SHOW_NOTIFICATION)),
                        eval.falsePositiveRate(data.attribute(data.classIndex()).indexOfValue(HIDE_NOTIFICATION)),
                        eval.kappa(),
                        num_clusters
                );

                if (evaluationResult == null) evaluationResult = e;

                if (e.isBetterThan(evaluationResult)) {
                    evaluationResult = e;
                    bestTree = tree;
                    bestData = data;
                    OPTIMAL_NUM_CLUSTERS = num_clusters;
                    bestClusters = clusters;
                }
                /*
                eval.crossValidateModel(rand, data, 10, new Random(System.nanoTime()));
                e = new EvaluationResult(
                        eval.correct()/eval.numInstances(),
                        eval.areaUnderROC(data.classIndex()),
                        eval.falsePositiveRate(data.attribute(data.classIndex()).indexOfValue(SHOW_NOTIFICATION)),
                        eval.falsePositiveRate(data.attribute(data.classIndex()).indexOfValue(HIDE_NOTIFICATION)),
                        eval.kappa(),
                        num_clusters
                );
                Log.d(TAG, "random forest evaluation: " + e.toString());
                */
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Error when cross-validating");
            }

        }

        private void storeClassifier(Instances data) {
            Log.d(TAG, "storeClassifier");
            if (data == null) return;
            tree_db.close();
            wordBins.close();

            tree_db = new J48Classifiers(context);
            wordBins = new WordBins(context);
            try {
                Log.d(TAG, "writing classifier to file");
                SerializationHelper.write(context.getFilesDir() + "/J48.model", bestTree);

                BufferedWriter writer = new BufferedWriter(
                        new FileWriter(context.getFilesDir() +"/J48.arff", false));
                Log.d(TAG, "storing data: " + bestData.toSummaryString());
                writer.write(bestData.toString());
                writer.newLine();
                writer.flush();
                writer.close();

                ContentValues values = new ContentValues();
                values.put(J48Classifiers.Classifiers_Table.generate_timestamp, System.currentTimeMillis());
                values.put(J48Classifiers.Classifiers_Table.accuracy, evaluationResult.accuracy);
                values.put(J48Classifiers.Classifiers_Table.roc_area, evaluationResult.roc_area);
                values.put(J48Classifiers.Classifiers_Table.show_false_positive, evaluationResult.show_false_positive);
                values.put(J48Classifiers.Classifiers_Table.hide_false_positive, evaluationResult.hide_false_positive);
                values.put(J48Classifiers.Classifiers_Table.kappa, evaluationResult.kappa);
                values.put(J48Classifiers.Classifiers_Table.num_instances, data.size());

                long classifier_id = tree_db.insertRecord(values);

                // sync to server
                values = new ContentValues();
                values.put(Provider.Predictions_Data.TIMESTAMP, System.currentTimeMillis());
                values.put(Provider.Predictions_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
                values.put(Provider.Predictions_Data.classifier_id, classifier_id);
                values.put(Provider.Predictions_Data.generate_timestamp, System.currentTimeMillis());
                values.put(Provider.Predictions_Data.num_clusters, OPTIMAL_NUM_CLUSTERS);
                values.put(Provider.Predictions_Data.num_instances, data.size());
                values.put(Provider.Predictions_Data.accuracy, evaluationResult.accuracy);
                values.put(Provider.Predictions_Data.roc_area, evaluationResult.roc_area);
                values.put(Provider.Predictions_Data.show_false_positive, evaluationResult.show_false_positive);
                values.put(Provider.Predictions_Data.hide_false_positive, evaluationResult.hide_false_positive);
                values.put(Provider.Predictions_Data.kappa, evaluationResult.kappa);

                getContentResolver().insert(Provider.Predictions_Data.CONTENT_URI, values);

                AppManagement.storeNumClusters(OPTIMAL_NUM_CLUSTERS, context);

                wordBins.storeClusters(bestClusters, false);

                Log.d(TAG, "Stored best classifier: " + evaluationResult.toString());
            }
            catch (Exception e) {
                Log.d(TAG, "error writing");
                e.printStackTrace();
            }

            wordBins.close();
            tree_db.close();
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
