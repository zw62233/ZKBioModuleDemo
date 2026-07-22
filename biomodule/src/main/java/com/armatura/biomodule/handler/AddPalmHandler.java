package com.armatura.biomodule.handler;

import static com.armatura.biomodule.databases.BioDataUtil.getValidLength;
import static com.armatura.biomodule.register.RegisterStatus.PALM_REGISTERED;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.armatura.biomodule.R;
import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.common.RegisterOperate;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.databases.BioDataUtil;
import com.armatura.biomodule.manager.NIRPalmManager;
import com.armatura.biomodule.manager.PalmManager;
import com.armatura.biomodule.pojo.palm.PalmFeature;
import com.armatura.biomodule.pojo.palm.PalmRect;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.pojo.setting.VLPalmSetting;
import com.armatura.biomodule.register.RegisterStatus;
import com.armatura.biomodule.register.RegisterStatusCallback;
import com.armatura.biomodule.util.BitmapUtil;
import com.armatura.biomodule.util.FileUtils;
import com.armatura.biomodule.util.HidHelper;
import com.armatura.biomodule.util.JSONUtil;
import com.armatura.biomodule.util.SpeakerHelper;
import com.armatura.constant.ErrorCode;
import com.armatura.constant.SnapType;
import com.armatura.translib.AMTHidManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Magic on 2020/9/17
 */
public class AddPalmHandler extends BaseAddHandler {
    private static final String TAG = "AddPalmHandler";

    public static final int MSG_ADD_PALM_FORM_LOCAL = 2003;
    public static final int MSG_ADD_PALM_FORM_INTERNAL = 2004;
    public static final int MSG_ADD_PALM_FORM_SNAP_SHOT = 2005;
    public static final int MSG_ADD_VL_PALM_FORM_INTERNAL = 2006;

    public static final int MSG_ENROLL_PALM_BY_FEATURE = 2007;

    private final Rect enrollArea = new Rect();

    private final RegisterStatusCallback callback;
    private volatile boolean startEnrollPalm = false;
    private final static int MAX_CACHE_SIZE = 5;

    private final List<PalmRecognizeData> mCachePalmRecognizeDataList = new ArrayList<>(MAX_CACHE_SIZE);

    private final Rect mCacheRect = new Rect();
    private final static int MAX_STILL_COUNT = 3;
    private int mPalmStillFrameCount = 0;

    public AddPalmHandler(Looper looper, @NotNull RegisterStatusCallback callback) {
        super(looper);
        this.callback = callback;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SNAP_SHOT_RGB:
                callback.onSnapStart();
                exitStandByMode();
                Bitmap rgbBitmap = snapShot(SnapType.SNAP_RGB);
                callback.onSnapFinish(rgbBitmap);
                break;
            case EXIT_STAND_BY_MODE:
                exitStandByMode();
                break;
            case START_REGISTER:
                if (callback != null) {
                    callback.onStatusCallback(RegisterStatus.PREPARE_TO_ENROLL_PALM);
                }
                startEnrollPalm = true;
                break;
            case MSG_ENROLL_PALM_BY_FEATURE: {
                PalmRecognizeData palmRecognizeData = (PalmRecognizeData) msg.obj;
                enrollPalmByFeature(palmRecognizeData);
            }
            break;
            case STOP_REGISTER:
                startEnrollPalm = false;
                break;
            case ADD_USER_INFO: {
                enterStandByMode();
                if (callback == null) {
                    Log.w(TAG, "handleMessage: callback is null,may activity/fragment already onDestroy");
                    return;
                }
                String userId = callback.getUserId();
                UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId);
                userInfo.palmFeature1 = (byte[]) msg.obj;
                BioDataUtil.instance().updateUserInfo(userInfo);
                callback.onRegisterFinish();
                break;
            }
            case MSG_ADD_PALM_FORM_LOCAL:
            case MSG_ADD_PALM_FORM_SNAP_SHOT: {
                Bitmap bitmap = (Bitmap) msg.obj;
                registerPalmByImage(bitmap);
            }
            break;
            default:
                break;
        }
    }

    private void resetPalmStillFrameCount() {
        mPalmStillFrameCount = 0;
    }


    private void registerPalmByImage(Bitmap bitmap) {
        JSONObject registerPalmJson = new JSONObject();
        JSONArray imagesJsonArray = new JSONArray();
        JSONObject imagesJson = new JSONObject();
        try {
            registerPalmJson.put("palmInfo", false);
            registerPalmJson.put("picture", false);
            registerPalmJson.put("feature", true);

            imagesJson.put("bioType", "palm");
            imagesJson.put("data", BitmapUtil.bitmapToBase64(bitmap));
            imagesJson.put("format", "jpeg");
            imagesJson.put("width", bitmap.getWidth());
            imagesJson.put("height", bitmap.getHeight());

            imagesJsonArray.put(imagesJson);

            registerPalmJson.put("images", imagesJsonArray);
        } catch (JSONException e) {
            Log.e(TAG, "registerPalmByImage: ", e);
            callback.onStatusCallback(RegisterStatus.JSON_FAILED);
            return;
        }
        byte[] result = new byte[200 * 1024];
        int[] size = new int[]{result.length};
        int hidRet = AMTHidManager.instance().registerPalm(
                registerPalmJson.toString().getBytes(), result, size);
        if (hidRet == ErrorCode.ERROR_NONE) {
            String resultJson = new String(result, 0, size[0]);
            try {
                JSONObject resultJsonObject = new JSONObject(resultJson);
                if (resultJsonObject.has("status")) {
                    int status = resultJsonObject.getInt("status");
                    if (status == 0) {
                        JSONObject data = resultJsonObject.getJSONObject("data");
                        JSONArray palms = data.getJSONArray("palms");
                        JSONObject firstPalm = palms.getJSONObject(0);
                        JSONObject feature = firstPalm.getJSONObject("feature");
                        String verTemplate = feature.getString("verTemplate");
                        byte[] palmFeature = Base64.decode(verTemplate, Base64.DEFAULT);
                        byte[] id = new byte[40];
                        float[] score = new float[1];
                        PalmManager.getInstance().dbIdentify(palmFeature, id, score);
                        float palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold;
                        Log.d(TAG, "[checkVLFeature]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold);
                        if (score[0] > Config.instance().palmVLIdentifyThreshold) {
                            String identifyUserId = new String(id, 0, getValidLength(id));
                            if (callback.getRegisterOperate() == RegisterOperate.ADD) {
                                callback.onStatusCallback(PALM_REGISTERED, identifyUserId,
                                        Config.instance().palmVLIdentifyThreshold, score[0]);
                                return;
                            }
                            if (callback.getRegisterOperate() == RegisterOperate.UPDATE) {
                                if (!identifyUserId.equals(callback.getUserId())) {
                                    callback.onStatusCallback(PALM_REGISTERED, identifyUserId,
                                            Config.instance().palmVLIdentifyThreshold, score[0]);
                                    return;
                                }
                            }
                        }
                        SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep);
                        callback.onSuccess(palmFeature);
                    } else {
                        String detail = resultJsonObject.getString("detail");
                        callback.onResult(detail);
                    }
                } else {
                    callback.onStatusCallback(RegisterStatus.JSON_FAILED);
                }
            } catch (JSONException e) {
                callback.onStatusCallback(RegisterStatus.JSON_FAILED);
            }
        } else {
            callback.onHidFailed(hidRet);
        }
    }

    private void enrollPalmByFeature(PalmRecognizeData palmRecognizeData) {
        if (!startEnrollPalm || mCachePalmRecognizeDataList.size() >= MAX_CACHE_SIZE) {
            return;
        }
        PalmRect palmRect = palmRecognizeData.getTrackInfo().getRect();
        if (palmRect != null) {
            Rect newRect = palmRect.getRect();
            calEnrollArea(enrollArea, 720, 1280);
            if (!enrollArea.contains(newRect)) {
                callback.onStatusCallback(RegisterStatus.PALM_NOT_IN_ENROLL_AREA);
                resetPalmStillFrameCount();
                return;
            }
            if (mCacheRect.centerX() == 0/*first*/) {
                mCacheRect.set(newRect);
                return;
            }
            if (!checkPalmImageIsStable(newRect, mCacheRect)) {
                callback.onStatusCallback(RegisterStatus.DO_NOT_MOVE_PALM);
                mCacheRect.setEmpty();
                resetPalmStillFrameCount();
                Log.w(TAG, "enrollPalmByFeature: not stable");
                return;
            }
            mPalmStillFrameCount++;
            if (mPalmStillFrameCount < MAX_STILL_COUNT) {
                Log.i(TAG, "stable palm frame count: " + mPalmStillFrameCount);
                return;
            }
            resetPalmStillFrameCount();
        } else {
            callback.onStatusCallback(RegisterStatus.PREPARE_TO_ENROLL_PALM);
            return;
        }


        String userId = callback.getUserId();
        UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId);
        switch (Config.palmTemplateMode) {
            case VLPalmSetting.PALM_TEMPLATE_MODE_IR:
                if (checkIRFeature(palmRecognizeData, userInfo)) {
                    mCachePalmRecognizeDataList.add(palmRecognizeData);
                    SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep);
                    callback.onSuccess(null);
                }
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL:
                if (checkVLFeature(palmRecognizeData, userInfo)) {
                    mCachePalmRecognizeDataList.add(palmRecognizeData);
                    SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep);
                    callback.onSuccess(null);
                }
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR:
                boolean vlCheck = checkVLFeature(palmRecognizeData, userInfo);
                if (!vlCheck) {
                    return;
                }
                boolean irCheck = checkIRFeature(palmRecognizeData, userInfo);
                if (!irCheck) {
                    return;
                }
                mCachePalmRecognizeDataList.add(palmRecognizeData);
                SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep);
                callback.onSuccess(null);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported palmTemplateMode " + Config.palmTemplateMode);
        }
        Log.i(TAG, String.format("enrollPalm: prepare a preTemplate,count =%d", mCachePalmRecognizeDataList.size()));

        Log.i(TAG, String.format("enrollPalmByFeature: prepare a preTemplate,count =%d", mCachePalmRecognizeDataList.size()));
        if (mCachePalmRecognizeDataList.size() >= MAX_CACHE_SIZE) {
            doRegisterPalm(userInfo);
            callback.onSuccess(null);
            callback.onRegisterFinish();
        }
    }


    private void doRegisterPalm(UserInfo userInfo) {
        //find best visible palm template
        Collections.sort(mCachePalmRecognizeDataList, new Comparator<PalmRecognizeData>() {
            @Override
            public int compare(PalmRecognizeData o1, PalmRecognizeData o2) {
                return o2.getTrackInfo().getImageQuality() - o1.getTrackInfo().getImageQuality();
            }
        });
        PalmRecognizeData bestQualityPalm = mCachePalmRecognizeDataList.get(0);

        //best to save in database
        switch (Config.palmTemplateMode) {
            case VLPalmSetting.PALM_TEMPLATE_MODE_IR:
                userInfo.palmFeature2 = bestQualityPalm.getFeatureVein().getByteVerTemplate();
                saveIREnrollImage(bestQualityPalm, userInfo);
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL:
                userInfo.palmFeature1 = bestQualityPalm.getFeature().getByteVerTemplate();
                saveVLEnrollImage(bestQualityPalm, userInfo);
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR:
                userInfo.palmFeature1 = bestQualityPalm.getFeature().getByteVerTemplate();
                saveIREnrollImage(bestQualityPalm, userInfo);
                userInfo.palmFeature2 = bestQualityPalm.getFeatureVein().getByteVerTemplate();
                saveVLEnrollImage(bestQualityPalm, userInfo);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported palmTemplateMode " + Config.palmTemplateMode);
        }

        BioDataUtil.instance().updateUserInfo(userInfo);
    }


    private void saveVLEnrollImage(PalmRecognizeData palmRecognizeData, UserInfo userInfo) {
        String userId = userInfo.userId;
        //save vlPalmImage
        String vlPalmImage = palmRecognizeData.getFeature().getImage();
        if (vlPalmImage != null) {
            byte[] jpegData = Base64.decode(vlPalmImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            saveBimap(bitmap, userId + "_palm_vl", userId);
        }
    }

    private void saveIREnrollImage(PalmRecognizeData palmRecognizeData, UserInfo userInfo) {
        String userId = userInfo.userId;
        String nirPalmImage = palmRecognizeData.getFeatureVein().getImage();
        if (nirPalmImage != null) {
            byte[] jpegData = Base64.decode(nirPalmImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            saveBimap(bitmap, userId + "_palm_ir", userId);
        }
    }

    private boolean checkVLFeature(PalmRecognizeData palmRecognizeData, UserInfo userInfo) {
        PalmFeature palmFeature = palmRecognizeData.getFeature();
        if (palmFeature != null) {
            //if register ,should verify for is registered person
            //do 1:N ,check if repeat
            byte[] verifyTemplate = palmFeature.getByteVerTemplate();
            byte[] id = new byte[40];
            float[] score = new float[1];
            PalmManager.getInstance().dbIdentify(verifyTemplate, id, score);
            float palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold;
            Log.d(TAG, "[checkVLFeature]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold);
            if (score[0] > Config.instance().palmVLIdentifyThreshold) {
                String identifyUserId = new String(id, 0, getValidLength(id));
                if (callback.getRegisterOperate() == RegisterOperate.ADD) {
                    callback.onStatusCallback(PALM_REGISTERED, identifyUserId,
                            Config.instance().palmVLIdentifyThreshold, score[0]);
                    return false;
                }
                if (callback.getRegisterOperate() == RegisterOperate.UPDATE) {
                    if (!identifyUserId.equals(callback.getUserId())) {
                        callback.onStatusCallback(PALM_REGISTERED, identifyUserId,
                                Config.instance().palmVLIdentifyThreshold, score[0]);
                        return false;
                    }
                }
            }

            if (mCachePalmRecognizeDataList.size() > 1) {
                //do 1:1 with first palm feature ,check if it's same palm
                byte[] firstPalmTemplate = mCachePalmRecognizeDataList.get(0).getFeature().getByteVerTemplate();
                float[] scoreArr = new float[1];
                boolean verifyRlt = PalmManager.getInstance().dbVerify(firstPalmTemplate, verifyTemplate, scoreArr);
                if (verifyRlt) {
                    float verifyScore = scoreArr[0];
                    Log.d(TAG, "[checkVLFeature]: dbVerify score=" + verifyScore + ", recognizeThreshold=" + palmVLRecognizeThreshold);
                    if (verifyScore < palmVLRecognizeThreshold) {
                        Log.w(TAG, "[checkVLFeature]: verify failed, score=" + verifyScore + ", threshold=" + palmVLRecognizeThreshold + ", need the same palm to finish enrolling the user");
                        callback.onStatusCallback(RegisterStatus.ENROLL_NEED_SAME_PALM);
                        return false;
                    }
                }
            }
            return true;
        }
        Log.w(TAG, "checkVLFeature: NO VL Feature");
        return false;
    }

    private boolean checkIRFeature(PalmRecognizeData palmRecognizeData, UserInfo userInfo) {
        PalmFeature palmVein = palmRecognizeData.getFeatureVein();
        if (palmVein != null) {
            //if register ,should verify for is registered person
            //do 1:N ,check if repeat
            byte[] verifyTemplate = palmVein.getByteVerTemplate();
            byte[] id = new byte[40];
            float[] score = new float[1];
            NIRPalmManager.getInstance().dbIdentify(verifyTemplate, id, score);
            float palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold;
            Log.d(TAG, "[checkIRFeature]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold);
            if (score[0] > Config.instance().palmVLIdentifyThreshold) {
                String identifyUserId = new String(id, 0, getValidLength(id));
                if (callback.getRegisterOperate() == RegisterOperate.ADD) {
                    callback.onStatusCallback(PALM_REGISTERED, identifyUserId,
                            Config.instance().palmVLIdentifyThreshold, score[0]);
                    return false;
                }
                if (callback.getRegisterOperate() == RegisterOperate.UPDATE) {
                    if (!identifyUserId.equals(callback.getUserId())) {
                        callback.onStatusCallback(PALM_REGISTERED, identifyUserId,
                                Config.instance().palmVLIdentifyThreshold, score[0]);
                        return false;
                    }
                }
            }

            if (mCachePalmRecognizeDataList.size() > 1) {
                //do 1:1 with first palm feature ,check if it's same palm
                byte[] firstPalmTemplate = mCachePalmRecognizeDataList.get(0).getFeatureVein().getByteVerTemplate();
                float[] scoreArr = new float[1];
                boolean verifyRlt = NIRPalmManager.getInstance().dbVerify(firstPalmTemplate, verifyTemplate, scoreArr);
                if (verifyRlt) {
                    float verifyScore = scoreArr[0];
                    Log.d(TAG, "[checkIRFeature]: dbVerify score=" + verifyScore + ", recognizeThreshold=" + palmVLRecognizeThreshold);
                    if (verifyScore < palmVLRecognizeThreshold) {
                        Log.w(TAG, "[checkIRFeature]: verify failed, score=" + verifyScore + ", threshold=" + palmVLRecognizeThreshold + ", need the same palm to finish enrolling the user");
                        callback.onStatusCallback(RegisterStatus.ENROLL_NEED_SAME_PALM);
                        return false;
                    }
                }
            }
            return true;
        }
        Log.w(TAG, "checkIRFeature: NO IR Feature");
        return false;
    }

    private void saveBimap(Bitmap bitmap, String fileName, String pin) {
        if (bitmap == null) {
            Log.e(TAG, "[saveBimap]: save enroll palm pic fail,bitmap is null, pin=" + pin);
            return;
        }
        FileUtils.saveBitmap(bitmap, fileName,
                FileUtils.USER_BIO_PHOTO + File.separator + pin + File.separator);
    }

    private void calEnrollArea(Rect enrollArea, int width, int height) {
        int fixRectLeft = width / 6;
        int fixRectRight = fixRectLeft * 5;
        int fixRectTop = height / 5;
        int fixRectBottom = fixRectTop * 4;
        enrollArea.set(fixRectLeft, fixRectTop, fixRectRight, fixRectBottom);
    }

    private boolean checkPalmImageIsStable(Rect newRect, Rect oldRect) {
        int xMoveDistance = Math.abs(newRect.centerX() - oldRect.centerX());
        int yMoveDistance = Math.abs(newRect.centerY() - oldRect.centerY());
        if (xMoveDistance > Config.palmMoveDistanceThreshold) {
            return false;
        }
        if (yMoveDistance > Config.palmMoveDistanceThreshold) {
            return false;
        }
        Log.i(TAG, "checkPalmImageIsStable: xMoveDistance=" + xMoveDistance + ",yMoveDistance=" + yMoveDistance);
        return true;
    }
}
