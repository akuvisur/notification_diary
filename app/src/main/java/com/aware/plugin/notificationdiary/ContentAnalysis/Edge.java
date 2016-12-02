package com.aware.plugin.notificationdiary.ContentAnalysis;

/**
 * Created by aku on 01/12/16.
 */
public class Edge {
    private int weight = 1;

    String edge_1;
    String edge_2;

    Edge(String e1, String e2) {
        // order edges
        if (e1.compareToIgnoreCase(e2) < 0) {
            this.edge_1 = e1;
            this.edge_2 = e2;
        } else {
            this.edge_2 = e1;
            this.edge_1 = e2;
        }
    }

    int incrementWeight() {
        weight++;
        return weight;
    }

    int getWeight() {
        return weight;
    }

    @Override
    public int hashCode() {
        return (edge_1 + edge_2).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Edge) {
            Edge n = (Edge) obj;
            return (
                n.edge_1.equals(edge_1) | n.edge_2.equals(edge_2)
            );
        }
        return false;
    }

    @Override
    public String toString() {
        return edge_1 + " - " + edge_2 + ": " + weight;
    }

    public boolean contains(Node n) {
        return (n.value.equals(edge_1) | n.value.equals(edge_2));
    }
}
