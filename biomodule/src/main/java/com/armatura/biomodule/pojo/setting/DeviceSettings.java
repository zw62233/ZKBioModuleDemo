package com.armatura.biomodule.pojo.setting;

/**
 * Auto-generated: 2020-06-22 15:27:40
 * CommonSetting pojo class
 * Deprecated at V16 Version.
 * Now it
 */
public class DeviceSettings {
    public final static String KEY = "DEVSetting";
    public final static String STAND_BY_MODE_KEY = "standbyMode";
    public final static int VIDEO_STREAM_MODE_DISABLE = 0;
    public final static int VIDEO_STREAM_MODE_VL = 1;
    public final static int VIDEO_STREAM_MODE_IR = 2;
    public final static int VIDEO_STREAM_MODE_ALL = 3;
    public int attendInterval;
    public boolean enableStoreAttendLog;//enable attendance log save in Module
    public boolean enableStoreStrangerAttLog;//enable stranger attendance log save in Module
    public int sensorFrameRate;//frame rate,must one of [15,25,30]
    /**
     * enable or disable standby mode
     */
    public boolean standbyMode;
    /**
     * If enabled, when pushing features, the corresponding photos will be pushed at the same time.
     */
    public boolean pushPhotoEnable;
    /**
     * 0.disable all
     * 1.enable vl stream
     * 2.enable ir stream
     * 3.enable all
     */
    public int videoStreamMode = 1;


    /**
     * When enabled, the push detection distance value will be pushed from CustomData.
     *
     * @since 0.6.8UN-sdkv2-20241014T143431
     */
    public boolean pushDetectionDistance;

    /**
     * 0:disable
     * 1:blue
     * 2:white
     * 3:green
     */
    public int ledBreathType;


    /**
     * Indicator led control mode
     * 0. control by module
     * 1. control by host,Palms and faces can be continuously recognized
     */
    public int ledControlMode;

    public int illuminationTriggerMode;//0 MODE_PROXIMITY_TRIGGER  1 MODE_PALM_TRIGGER


    public int minDetectionDistance;
    public int maxDetectionDistance;
}