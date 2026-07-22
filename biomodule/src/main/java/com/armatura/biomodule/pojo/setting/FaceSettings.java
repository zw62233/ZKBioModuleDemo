package com.armatura.biomodule.pojo.setting;

/**
 * 2024-12-10 11:19:40
 * FaceSettings pojo class
 */
public class FaceSettings {
    public final static String KEY = "FACESetting";
    public final static int LIVENESS_MODE_DISABLE = 0;
    public final static int LIVENESS_MODE_SINGLE_LENS = 1;
    public final static int LIVENESS_MODE_DUAL_LENS = 2;
    public final static int LIVENESS_MODE_OTHER = 3;

    public boolean faceAEEnabled;
    public boolean isTrackingMatchMode;
    public boolean maxFaceEnable;//true means only recognize single face else recognize multi face
    /**
     * [New item]
     * liveness mode:
     * 0:disable
     * 1:singleLensLiveness
     * 2:dualLensLiveness
     * 3:other
     */
    public int livenessMode;
    public float dualLensLivenessThreshold;
    public float singleLensLivenessThreshold;
    public boolean attributeRecog;
    public int attrInterval;
    public float recogThreshold;
    public int recogInterval;
    public float verifyThreshold;
}