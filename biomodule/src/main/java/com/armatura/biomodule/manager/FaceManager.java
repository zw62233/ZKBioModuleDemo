package com.armatura.biomodule.manager;

import android.util.Log;

import com.armatura.biomereric.AMTFaceMXMatch;
import com.armatura.biomereric.AMTFaceMatch;
import com.armatura.constant.FaceErrorCode;

import java.nio.charset.StandardCharsets;

public class FaceManager {

    public final static String FACE_VERSION_8_1 = "8.1";
    public final static String FACE_VERSION_60_1 = "60.1";

    private static final String TAG = "FaceManager";

    private static volatile FaceManager singleton;
    private final static long INVALID_CONTEXT = -1L;
    private long mContext = INVALID_CONTEXT;

    private String mFaceAlgoVersion;

    private FaceManager() {
    }

    public static FaceManager getInstance() {
        if (singleton == null) {
            synchronized (FaceManager.class) {
                if (singleton == null) {
                    singleton = new FaceManager();
                }
            }
        }
        return singleton;
    }

    public int init(String faceAlgoVersion) {
        destroy();
        long[] context = new long[1];
        mFaceAlgoVersion = faceAlgoVersion;
        int ret;
        switch (faceAlgoVersion) {
            case FACE_VERSION_8_1:
                ret = AMTFaceMXMatch.init(context);
                break;
            case FACE_VERSION_60_1:
                ret = AMTFaceMatch.init(context);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + faceAlgoVersion);
        }
        if (ret == FaceErrorCode.SUCCESS) {
            mContext = context[0];
            Log.i(TAG, "init: mContext = " + mContext);
        }
        return ret;
    }

    public boolean dbAdd(String userPin, byte[] regTemplate) {
        if (mContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbAdd: failed,not init");
            return false;
        }
        int ret;
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                ret = AMTFaceMXMatch.dbAdd(mContext, userPin.getBytes(StandardCharsets.UTF_8), regTemplate);
                break;
            case FACE_VERSION_60_1:
                ret = AMTFaceMatch.dbAdd(mContext, userPin.getBytes(StandardCharsets.UTF_8), regTemplate);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }
        if (ret != FaceErrorCode.SUCCESS) {
            Log.e(TAG, String.format("dbAdd: failed, %s %d,count = %d", userPin, ret, dbCount()));
        } else {
            Log.i(TAG, String.format("dbAdd: db add %s %d,count = %d", userPin, ret, dbCount()));
        }
        return ret == FaceErrorCode.SUCCESS;
    }

    public boolean dbDel(String userPin) {
        if (mContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbDel: failed,not init");
            return false;
        }
        int ret;
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                ret = AMTFaceMXMatch.dbDel(mContext, userPin.getBytes(StandardCharsets.UTF_8));
                break;
            case FACE_VERSION_60_1:
                ret = AMTFaceMatch.dbDel(mContext, userPin.getBytes(StandardCharsets.UTF_8));
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }
        if (ret != FaceErrorCode.SUCCESS) {
            Log.e(TAG, "dbDel: failed,ret =" + ret);
        }
        return ret == FaceErrorCode.SUCCESS;
    }

    public int dbCount() {
        if (mContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbCount: failed,not init");
            return FaceErrorCode.ERR_NOT_INIT;
        }
        int count;
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                count = AMTFaceMXMatch.dbCount(mContext);
                break;
            case FACE_VERSION_60_1:
                count = AMTFaceMatch.dbCount(mContext);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }
        return count;
    }

    public int dbIdentify(byte[] verTemplate, byte[] id, float[] score) {
        if (mContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbIdentify: failed,not init");
            return FaceErrorCode.ALGORITHM_NOT_INIT;
        }

        int ret;
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                ret = AMTFaceMXMatch.dbIdentify(mContext, verTemplate, id, score);
                break;
            case FACE_VERSION_60_1:
                ret = AMTFaceMatch.dbIdentify(mContext, verTemplate, id, score);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }

        return ret;
    }

    public float dbVerify(byte[] verTemplate1, byte[] verTemplate2) {
        if (mContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbIdentify: failed,not init");
            return FaceErrorCode.ALGORITHM_NOT_INIT;
        }
        float[] score = {-1F};

        int ret;
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                ret = AMTFaceMXMatch.compare(mContext, verTemplate1, verTemplate2, score);
                break;
            case FACE_VERSION_60_1:
                ret = AMTFaceMatch.compare(mContext, verTemplate1, verTemplate2, score);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }
        if (ret == FaceErrorCode.SUCCESS) {
            return score[0];
        }
        return ret;
    }


    public boolean dbClear() {
        if (mContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbClear: failed,not init");
            return false;
        }
        int ret;
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                ret = AMTFaceMXMatch.dbClear(mContext);
                break;
            case FACE_VERSION_60_1:
                ret = AMTFaceMatch.dbClear(mContext);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }
        Log.i(TAG, "dbClear: ret = " + ret + ",count = " + dbCount());
        return ret == FaceErrorCode.SUCCESS;
    }

    public void destroy() {
        if (mContext == INVALID_CONTEXT) {
            return;
        }
        switch (mFaceAlgoVersion) {
            case FACE_VERSION_8_1:
                AMTFaceMXMatch.terminate(mContext);
                break;
            case FACE_VERSION_60_1:
                AMTFaceMatch.terminate(mContext);
                break;
            default:
                throw new RuntimeException("Unknown face algo version," + mFaceAlgoVersion);
        }
        mContext = INVALID_CONTEXT;
    }
}