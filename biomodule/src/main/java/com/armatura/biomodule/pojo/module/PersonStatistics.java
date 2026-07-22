package com.armatura.biomodule.pojo.module;

import androidx.annotation.NonNull;

/**
 * Created by Magic on 2020/9/12
 */
public class PersonStatistics {
    public static final String KEY  = "data";

    public int personCount;
    public int faceCount;
    public int palmCount;
    public int databaseSize;


    @NonNull
    @Override
    public String toString() {
        return "PersonStatistics{" +
                "personCount=" + personCount +
                ", faceCount=" + faceCount +
                ", palmCount=" + palmCount +
                ", databaseSize=" + databaseSize +
                '}';
    }
}
