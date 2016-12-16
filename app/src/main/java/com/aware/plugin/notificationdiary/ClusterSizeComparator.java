package com.aware.plugin.notificationdiary;

import com.aware.plugin.notificationdiary.ContentAnalysis.Cluster;

import java.util.Comparator;

/**
 * Created by aku on 09/12/16.
 */

public class ClusterSizeComparator implements Comparator {
    @Override
    public int compare(Object o1, Object o2) {
        if (! (o1 instanceof Cluster && o2 instanceof Cluster) ) return 0;
        Cluster c1 = (Cluster) o1;
        Cluster c2 = (Cluster) o2;
        if (c1.getNodes().size() < c2.getNodes().size()) {
            return 1;
        }
        else if (c1.getNodes().size() > c2.getNodes().size()) {
            return -1;
        }
        return 0;
    }
}
