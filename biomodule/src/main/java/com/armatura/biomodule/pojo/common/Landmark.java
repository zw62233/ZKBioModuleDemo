package com.armatura.biomodule.pojo.common;

import androidx.annotation.NonNull;

/**
 * Created by Magic on 2020/9/28
 */
public class Landmark {
    public int count;               //landmark numbers
    public String data;             //landmark points

    @NonNull
    @Override
    public String toString() {
        return "Landmark{" +
                "count=" + count +
                ", data='" + (data == null) + '\'' +
                '}';
    }
}
