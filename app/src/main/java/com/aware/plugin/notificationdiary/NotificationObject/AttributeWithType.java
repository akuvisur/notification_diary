package com.aware.plugin.notificationdiary.NotificationObject;

/**
 * Created by aku on 06/12/16.
 */
public class AttributeWithType {
    public String name;
    public String type;

    public AttributeWithType(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return name;
    }
}
