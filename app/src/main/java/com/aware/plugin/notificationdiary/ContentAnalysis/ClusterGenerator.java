package com.aware.plugin.notificationdiary.ContentAnalysis;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by aku on 30/11/16.
 */

public class ClusterGenerator {
    private final String TAG = "ClusterGenerator";

    private ArrayList<Node> nodes;
    private ArrayList<Cluster> clusters;
    private ArrayList<Node> centroids;

    private int CUR_DEPTH = 1;
    private final int MAX_DEPTH = 10;

    HashMap<Node, ArrayList<Cluster>> potentialClusters;
    ArrayList<Cluster> potentials;
    HashMap<ClusterNodeTuple, Integer> weights;
    ArrayList<Node> addedNodes;
    ArrayList<Node> removeNodes;

    public ClusterGenerator(ArrayList<Node> n1, int numClusters) {
        Log.d(TAG, "Calculating clusters");
        clusters = new ArrayList<>();
        centroids = new ArrayList<>();

        this.nodes = n1;

        // remove nodes with no associations (edges)
        removeNodes = new ArrayList<>();
        for (Node n : nodes) {
            if (n.edges.size() == 0) removeNodes.add(n);
        }
        nodes.removeAll(removeNodes);

        // order by number of associations (edges)
        orderNodes();

        int clusterCount = 0;

        // create cluster centroids based on nodes with most associated words and minimum distance of 1 between
        Cluster cluster;
        allnodes:
        for (int i = nodes.size()-1; i >= 0; i--) {
            //Log.d(TAG, "clustering for node: " + nodes.getString(i).toString());
            for (Cluster c : clusters) {
                // check if current node shares edge with existing centroid
                for (Edge e : c.centroid.edges) {
                    // if they do, they are too nearby to form another centroid
                    if (nodes.get(i).edges.contains(e)) continue allnodes;
                }
            }
            cluster = new Cluster(nodes.get(i));
            centroids.add(nodes.get(i));
            clusters.add(cluster);
            clusterCount++;
            if (clusterCount == numClusters) break;
        }
        // place nodes in clusters based on their distance from centroid

        // in case of ties, place nodes based on edge weight

        int highest_weight = 0;
        ClusterNodeTuple cluster_highest_weight = null;
        // added nodes
        addedNodes = new ArrayList<>();

        // getString nodes surrounding centroids.. then nodes surrounding those nodes, etc. etc. etc.
        while (CUR_DEPTH < MAX_DEPTH) {
            // refresh potential clusters for each level
            potentialClusters = new HashMap<>();
            for (Cluster c : clusters) {
                // if particular cluster has no nodes at previous depth
                if ((CUR_DEPTH-1) > c.max_depth) continue;

                //Log.d(TAG, "placing for " + c.toString());
                // fetch all nodes from previous distance of the cluster
                for (Node source_node : c.getNodes(CUR_DEPTH-1)) {
                    // then find nodes that share edges with those nodes, and add to potential clusters
                    for (Node n : nodes) {
                        if (n.hasSharedEdge(source_node)) {
                            // if shared, add this cluster to potential clusters, calculate where to put the node
                            // later based on weight (number of times words were associated)
                            if (!potentialClusters.containsKey(n)) potentialClusters.put(n, new ArrayList<Cluster>());
                            potentialClusters.get(n).add(c);
                        }
                    }
                }
                // place nodes based on weight to a cluster
                for (Node node_to_place : potentialClusters.keySet()) {
                    weights = new HashMap<>();
                    potentials = potentialClusters.get(node_to_place);
                    // if there is only a single potential, choice is easy
                    if (potentials.size() == 1) {
                        c.addNode(CUR_DEPTH, node_to_place);
                        addedNodes.add(node_to_place);
                    }
                    // otherwise its hard -_-
                    else {
                        // TODO take into account levels and calculate mean weight?
                        // search for weights
                        for (Cluster potential : potentials) {
                            for (Node node_to_match : potential.getNodes(CUR_DEPTH)) {
                                if (node_to_place.hasSharedEdge(node_to_match)) {
                                    weights.put(new ClusterNodeTuple(potential, node_to_place), node_to_match.getSharedEdge(node_to_place).getWeight());
                                }
                            }
                        }
                        for (ClusterNodeTuple potential : weights.keySet()) {
                            if (weights.get(potential) > highest_weight) {
                                cluster_highest_weight = potential;
                                highest_weight = weights.get(potential);
                            }
                        }
                        // insert if there is a cluster with highest weight
                        if (cluster_highest_weight != null) {
                            c.addNode(CUR_DEPTH, cluster_highest_weight.node);
                            addedNodes.add((cluster_highest_weight.node));
                        }
                    }
                }
            }

            CUR_DEPTH++;
            nodes.removeAll(addedNodes);
            if (nodes.isEmpty()) break;
        }

    }

    private void orderNodes() {
        Collections.sort(nodes, new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                if (n1.edges.size() > n2.edges.size())
                    return 1;
                if (n1.edges.size() < n2.edges.size())
                    return -1;
                return 0;
            }
        });
    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
    }
}
