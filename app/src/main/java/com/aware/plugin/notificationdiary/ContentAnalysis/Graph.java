package com.aware.plugin.notificationdiary.ContentAnalysis;

import android.content.Context;
import android.util.Log;

import com.aware.plugin.notificationdiary.Providers.UnsyncedData;
import com.aware.plugin.notificationdiary.R;
import com.aware.plugin.notificationdiary.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Created by aku on 30/11/16.
 */

public class Graph {

    private final String TAG = "SemanticCore";

    private ArrayList<UnsyncedData.NotificationText> notificationContents;

    private Context context;

    private ArrayList<Node> generatedNodes;
    private ArrayList<String> stopWordsEng;
    private ArrayList<String> stopWordsFin;


    public Graph(Context c) {
        this.context = c;

        UnsyncedData helper = new UnsyncedData(context);
        notificationContents = helper.getAllNotificationText();

        generatedNodes = new ArrayList<>();
        stopWordsEng = new ArrayList<>(Arrays.asList(Utils.readStopWords(c, R.raw.english)));
        stopWordsFin = new ArrayList<>(Arrays.asList(Utils.readStopWords(c, R.raw.finnish)));

        // shuffle the starting element
        long seed = System.nanoTime();
        Collections.shuffle(notificationContents, new Random(seed));
        Collections.shuffle(notificationContents, new Random(seed));

        String word_1;

        long start_second = System.currentTimeMillis()/100;
        Edge edge;
        Node node;
        int count = 0;

        for (UnsyncedData.NotificationText n : notificationContents) {
            String[] words = Utils.Randomize((n.contents + n.title).split(" "));
            if (words.length == 1) continue;
            count++;

            for (int i1 = 0; i1 < words.length; i1++) {
                word_1 = words[i1];

                if (word_1.length() < 3) continue;
                if (stopWordsEng.contains(word_1) | stopWordsFin.contains(word_1)) continue;

                // create new node if not included yet
                node = new Node(word_1);
                if (!generatedNodes.contains(node)) generatedNodes.add(node);

                for (int i2 = i1; i2 < words.length; i2++) {
                    if (i2 == i1) continue;
                    if (word_1.length() < 3 | words[i2].length() < 3) continue;
                    if (stopWordsEng.contains(words[i2]) | stopWordsFin.contains(words[i2])) continue;

                    edge = new Edge(word_1, words[i2]);
                    generatedNodes.get(generatedNodes.indexOf(node)).addEdge(edge);
                }
            }
        }
    }

    public ArrayList<Node> getNodes() {
        return generatedNodes;
    }

}
