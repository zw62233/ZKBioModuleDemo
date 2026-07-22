package com.armatura.biomodule.pojo.setting;

import androidx.annotation.NonNull;

/**
 * @author magic.hu@armatura.com
 * @date 2020/08/11
 * @since 1.0.0
 */
public class MotionDetectSetting {
    public int brightnessThreshold;
    public int idleTimeOutMS;
    public boolean motionDetectFunOn;
    public int sensitivityThreshold;

    @NonNull
    @Override
    public String toString() {
        return "MotionDetectSetting{" +
                "brightnessThreshold=" + brightnessThreshold +
                ", idleTimeOutMS=" + idleTimeOutMS +
                ", motionDetectFunOn=" + motionDetectFunOn +
                ", sensitivityThreshold=" + sensitivityThreshold +
                '}';
    }
}
