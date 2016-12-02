package com.aware.plugin.notificationdiary.ContentAnalysis;

import java.util.ArrayList;

/**
 * Created by aku on 01/12/16.
 */
public class Node {
    protected String value;

    protected ArrayList<Edge> edges;

    public Node(String value) {
        this.value = value;
        edges = new ArrayList<>();
    }

    public void addEdge(Edge e) {
        if (edges.contains(e)) edges.get(edges.indexOf(e)).incrementWeight();
        else edges.add(e);
    }

    public boolean hasSharedEdge(Node n) {
        for (Edge e : n.edges) {
            if (edges.contains(e)) return true;
        }
        return false;
    }

    // never call without hasSharedEdgeFirst
    public Edge getSharedEdge(Node n) {
        for (Edge e : n.edges) {
            if (edges.contains(e)) return e;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node) {
            return (value.equals(((Node) obj).value));
        }
        return false;
    }

    public String getValue() {return value;}

    @Override
    public String toString() {
        return "Node: " + value + " (" + edges.size() + ")";
    }

}
