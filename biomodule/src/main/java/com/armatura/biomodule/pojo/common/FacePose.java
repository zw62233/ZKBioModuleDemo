package com.armatura.biomodule.pojo.common;

import androidx.annotation.NonNull;

/**
 * Created by Magic on 2020/9/28
 */
public class FacePose {
    public float yaw;
    public float pitch;
    public float roll;

    @NonNull
    @Override
    public String toString() {
        return "FacePose{" +
                "yaw=" + yaw +
                ", pitch=" + pitch +
                ", roll=" + roll +
                '}';
    }
}
