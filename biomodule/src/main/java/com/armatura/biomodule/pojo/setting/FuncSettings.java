package com.armatura.biomodule.pojo.setting;

/**
 * Auto-generated: 2024-08-08 15:21:57
 */
public class FuncSettings {

    public static class SensorType {
        public final static int SENSOR_TYPE_RGB_AND_NIR = 0;
        public final static int SENSOR_TYPE_RGB = 1;
        public final static int SENSOR_TYPE_NIR = 2;
    }

    public final static String KEY = "FuncSettings";
    private int RFIDMaxCount;
    private int faceMaxCount;
    private boolean isSupportFace;
    private boolean isSupportInfraredDetector;
    private boolean isSupportLed;
    private boolean isSupportPalm;
    private boolean isSupportRFID;
    private boolean isSupportStoreInModule;
    private int palmMaxCount;
    private int sensorType = SensorType.SENSOR_TYPE_RGB_AND_NIR;

    public void setRFIDMaxCount(int RFIDMaxCount) {
        this.RFIDMaxCount = RFIDMaxCount;
    }

    public int getRFIDMaxCount() {
        return RFIDMaxCount;
    }

    public void setFaceMaxCount(int faceMaxCount) {
        this.faceMaxCount = faceMaxCount;
    }

    public int getFaceMaxCount() {
        return faceMaxCount;
    }

    public void setIsSupportFace(boolean isSupportFace) {
        this.isSupportFace = isSupportFace;
    }

    public boolean getIsSupportFace() {
        return isSupportFace;
    }

    public void setIsSupportInfraredDetector(boolean isSupportInfraredDetector) {
        this.isSupportInfraredDetector = isSupportInfraredDetector;
    }

    public boolean getIsSupportInfraredDetector() {
        return isSupportInfraredDetector;
    }

    public void setIsSupportLed(boolean isSupportLed) {
        this.isSupportLed = isSupportLed;
    }

    public boolean getIsSupportLed() {
        return isSupportLed;
    }

    public void setIsSupportPalm(boolean isSupportPalm) {
        this.isSupportPalm = isSupportPalm;
    }

    public boolean getIsSupportPalm() {
        return isSupportPalm;
    }

    public void setIsSupportRFID(boolean isSupportRFID) {
        this.isSupportRFID = isSupportRFID;
    }

    public boolean getIsSupportRFID() {
        return isSupportRFID;
    }

    public void setIsSupportStoreInModule(boolean isSupportStoreInModule) {
        this.isSupportStoreInModule = isSupportStoreInModule;
    }

    public boolean getIsSupportStoreInModule() {
        return isSupportStoreInModule;
    }

    public void setPalmMaxCount(int palmMaxCount) {
        this.palmMaxCount = palmMaxCount;
    }

    public int getPalmMaxCount() {
        return palmMaxCount;
    }

    public int getSensorType() {
        return sensorType;
    }
}
