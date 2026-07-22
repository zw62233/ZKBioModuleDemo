package com.armatura.biomodule.manager;

import android.util.Log;

import com.armatura.constant.PalmErrorCode;
import com.armatura.palm.AMTVLPalmMatch;

public class NIRPalmManager {
    private static final String TAG = "NIRPalmManager";

    private final static long INVALID_CONTEXT = -1L;
    private long mNIRContext = INVALID_CONTEXT;

    private static volatile NIRPalmManager singleton;

    private NIRPalmManager() {
    }

    public static NIRPalmManager getInstance() {
        if (singleton == null) {
            synchronized (NIRPalmManager.class) {
                if (singleton == null) {
                    singleton = new NIRPalmManager();
                }
            }
        }
        return singleton;
    }

    public int init() {
        if (mNIRContext != INVALID_CONTEXT) {
            return PalmErrorCode.SUCCESS;
        }
        long[] context = new long[1];
        int ret = AMTVLPalmMatch.init(context, null);
        if (ret >= 0) {
            mNIRContext = context[0];
        } else {
            Log.e(TAG, "init: Palm Init ret = " + ret);
        }
        return ret;
    }

    public boolean dbAdd(String userPin, byte[] regTemplate) {
        if (mNIRContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbAdd: failed,not init");
            return false;
        }
        int ret = AMTVLPalmMatch.dbADD(mNIRContext, userPin, regTemplate);
        if (ret != PalmErrorCode.SUCCESS) {
            Log.e(TAG, "dbAdd: failed,ret =" + ret);
        }
        Log.i(TAG, String.format("palm dbAdd: ret=%d ,userPin=%s", ret,  userPin));
        return ret == PalmErrorCode.SUCCESS;
    }

    public boolean dbDel(String userPin) {
        if (mNIRContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbDel: failed,not init");
            return false;
        }

        int ret = AMTVLPalmMatch.dbDel(mNIRContext, userPin);
        if (ret < PalmErrorCode.SUCCESS) {
            Log.e(TAG, "dbDel: failed,ret =" + ret + ",id=" + userPin);
        } else {
            Log.i(TAG, "dbDel: success,db count=" + dbCount());
        }
        return ret == PalmErrorCode.SUCCESS;
    }

    public int dbCount() {
        if (mNIRContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbCount: failed,not init");
            return PalmErrorCode.ALGORITHM_NOT_INIT;
        }
        int count = AMTVLPalmMatch.dbCount(mNIRContext);
        return count;
    }

    public int dbIdentify(byte[] verTemplate, byte[] id, float[] score) {
        if (mNIRContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbIdentify: failed,not init");
            return PalmErrorCode.ALGORITHM_NOT_INIT;
        }
        float[] scoreF = new float[1];
        int ret = AMTVLPalmMatch.dbIdentify(mNIRContext, verTemplate, id, scoreF, 0.9F, 0.6F);
        if (ret == 0) {
            score[0] = scoreF[0];
        }
        Log.i(TAG, "palm dbIdentify: ret=" + ret);
        return ret;
    }

    public boolean dbClear() {
        if (mNIRContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbClear: failed,not init");
            return false;
        }
        int ret = AMTVLPalmMatch.dbClear(mNIRContext);
        Log.i(TAG, "dbClear: count=" + dbCount());
        return ret == PalmErrorCode.SUCCESS;
    }

    public boolean dbVerify(byte[] template1, byte[] template2, float[] score) {
        if (mNIRContext == INVALID_CONTEXT) {
            Log.e(TAG, "dbVerify: failed,not init");
            return false;
        }
        int ret = AMTVLPalmMatch.dbVerify(mNIRContext, template1, template2, score);
        return ret == PalmErrorCode.SUCCESS;
    }

    public void destroy() {
        if (mNIRContext == INVALID_CONTEXT) {
            return;
        }
        AMTVLPalmMatch.release(mNIRContext);
        mNIRContext = INVALID_CONTEXT;
    }
}