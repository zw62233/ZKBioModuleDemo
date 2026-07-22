package com.armatura.biomodule.pojo.setting;

import androidx.annotation.NonNull;

/**
 * date 2022/11/1
 *
 * @since HID Version 2.0.0
 */
public class VLPalmSetting {
    public static final int PALM_TEMPLATE_MODE_VL = 0;
    public static final int PALM_TEMPLATE_MODE_IR = 1;
    public static final int PALM_TEMPLATE_MODE_VL_AND_IR = 2;
    public static final int PALM_INTERNAL = 3;

    public int imageQualityThreshold;
    public boolean palmFunOn;
    public boolean palmAE;
    public int palmIdentifyInterval;
    public int palmIdentifyThreshold;
    public boolean palmLiveness;
    public int palmLivenessThreshold;
    public int palmMinSize;


    /**
     * 0:vl template
     * 1:nir template
     * 2:vl and nir template
     */
    public int palmTemplateMode;

    @NonNull
    @Override
    public String toString() {
        return "VLPalmSetting{" +
                "imageQualityThreshold=" + imageQualityThreshold +
                ", palmFunOn=" + palmFunOn +
                ", palmAE=" + palmAE +
                ", palmIdentifyInterval=" + palmIdentifyInterval +
                ", palmIdentifyThreshold=" + palmIdentifyThreshold +
                ", palmLiveness=" + palmLiveness +
                ", palmLivenessThreshold=" + palmLivenessThreshold +
                ", palmMinSize=" + palmMinSize +
                '}';
    }
}
