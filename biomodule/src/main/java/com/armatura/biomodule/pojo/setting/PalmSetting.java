package com.armatura.biomodule.pojo.setting;

import androidx.annotation.NonNull;

/**
 * @author magic.hu@armatura.com
 * @date 2020/08/13
 * @since 1.0.0
 */
public class PalmSetting {
    public final static String KEY = "PALMSetting";
    public int imageQualityThreshold;
    public boolean palmFunOn;
    public String palmRunState;
    public int palmIdentifyThreshold;
    public int palmSupportHeight;
    public int palmSupportWidth;
    public int templateQualityThreshold;


    @NonNull
    @Override
    public String toString() {
        return "PalmSetting{" +
                "imageQualityThreshold=" + imageQualityThreshold +
                ", palmFunOn='" + palmFunOn + '\'' +
                ", palmRunState='" + palmRunState + '\'' +
                ", palmIdentifyThreshold=" + palmIdentifyThreshold +
                ", palmSupportHeight=" + palmSupportHeight +
                ", palmSupportWidth=" + palmSupportWidth +
                ", templateQualityThreshold=" + templateQualityThreshold +
                '}';
    }
}
