/**
 * Copyright 2020 bejson.com
 */
package com.armatura.biomodule.pojo.info;

/**
 * snap shot data
 */
public class SnapshotData {

    public final static String KEY = "snapshot";

    private int frameId;
    private int height;
    private String data;
    private long timeStamp;
    private int width;
    private String type;

    public void setFrameId(int frameid) {
        this.frameId = frameid;
    }

    public int getFrameId() {
        return frameId;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public String getData() {
        return data;
    }

    public SnapshotData setData(String data) {
        this.data = data;
        return this;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}