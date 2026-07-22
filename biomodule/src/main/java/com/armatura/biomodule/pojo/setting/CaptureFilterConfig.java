package com.armatura.biomodule.pojo.setting;

import androidx.annotation.NonNull;

/**
 * @author magic.hu@armatura.com
 * @date 2020/08/13
 * @since 1.0.0
 */
public class CaptureFilterConfig {
    public int blurThreshold;
    public int frontThreshold;
    public int heightMaxValue;
    public int heightMinValue;
    public int pitchMaxValue;
    public int pitchMinValue;
    public int rollMaxValue;
    public int rollMinValue;
    public int scoreThreshold;//quality
    public int widthMaxValue;
    public int widthMinValue;
    public int yawMaxValue;
    public int yawMinValue;

    @NonNull
    @Override
    public String toString() {
        return "CaptureFilterConfig{" +
                "blurThreshold=" + blurThreshold +
                ", frontThreshold=" + frontThreshold +
                ", heightMaxValue=" + heightMaxValue +
                ", heightMinValue=" + heightMinValue +
                ", pitchMaxValue=" + pitchMaxValue +
                ", pitchMinValue=" + pitchMinValue +
                ", rollMaxValue=" + rollMaxValue +
                ", rollMinValue=" + rollMinValue +
                ", scoreThreshold=" + scoreThreshold +
                ", widthMaxValue=" + widthMaxValue +
                ", widthMinValue=" + widthMinValue +
                ", yawMaxValue=" + yawMaxValue +
                ", yawMinValue=" + yawMinValue +
                '}';
    }
}
