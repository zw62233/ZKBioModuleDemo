package com.armatura.biomodule.handler;

import static com.armatura.biomodule.databases.BioDataUtil.getValidLength;
import static com.armatura.biomodule.register.RegisterStatus.FACE_BLUR_TOO_HIGH;
import static com.armatura.biomodule.register.RegisterStatus.FACE_QUALITY_TOO_BAD;
import static com.armatura.biomodule.register.RegisterStatus.FACE_REGISTERED;
import static com.armatura.biomodule.register.RegisterStatus.FACE_ROLL_TOO_BIG;
import static com.armatura.biomodule.register.RegisterStatus.FACE_TOO_SMALL;
import static com.armatura.biomodule.register.RegisterStatus.FACE_YAW_TOO_BIG;
import static com.armatura.biomodule.register.RegisterStatus.JSON_FAILED;
import static com.armatura.biomodule.register.RegisterStatus.NO_DETECT_FACE;
import static com.armatura.biomodule.register.RegisterStatus.SEND_FAILED;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.armatura.biomodule.R;
import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.common.RegisterOperate;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.databases.BioDataUtil;
import com.armatura.biomodule.manager.FaceManager;
import com.armatura.biomodule.pojo.common.CommonConfigData;
import com.armatura.biomodule.pojo.face.FaceFeature;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.face.register.DetectFaceRequest;
import com.armatura.biomodule.pojo.face.register.DetectFaceResponse;
import com.armatura.biomodule.pojo.face.register.Face;
import com.armatura.biomodule.pojo.face.register.FaceInfo;
import com.armatura.biomodule.register.RegisterHelper;
import com.armatura.biomodule.register.RegisterStatus;
import com.armatura.biomodule.register.RegisterStatusCallback;
import com.armatura.biomodule.util.FileUtils;
import com.armatura.biomodule.util.JSONUtil;
import com.armatura.biomodule.util.SpeakerHelper;
import com.armatura.constant.ErrorCode;
import com.armatura.constant.SnapType;
import com.armatura.constant.StatusCode;
import com.armatura.translib.AMTHidManager;
import com.armatura.uvclib.util.AMTUtil;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
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
public class AddFaceHandler extends BaseAddHandler {
    private static final String TAG = AddFaceHandler.class.getSimpleName();
    public static final int MSG_ADD_FACE_FORM_VIDEO = 1001;
    public static final int MSG_ADD_FACE_FORM_LOCAL = 1002;
    public static final int MSG_ADD_FACE_FORM_INTERNAL = 1003;
    public static final int MSG_ADD_FACE_FORM_SNAP_SHOT = 1004;
    public static final int MSG_RESET_TRACK_ID = 1006;
    public static final int MSG_DISABLE_TRACK_MODE = 1007;

    private volatile boolean startEnrollFace = false;
    private final static int MAX_CACHE_SIZE = 5;
    private final Rect enrollArea = new Rect();
    private final List<DetectFaceResponse> mCacheFaceRecognizeDataList = new ArrayList<>(MAX_CACHE_SIZE);

    private final Rect mCacheRect = new Rect();
    private final static int MAX_STILL_COUNT = 3;
    private int mFaceStillFrameCount = 0;

    private final Gson gson = new Gson();

    private final RegisterStatusCallback callback;

    private final static int IMAGE_WIDTH = 720;
    private final static int IMAGE_HEIGHT = 1280;

    public AddFaceHandler(Looper looper, @NotNull RegisterStatusCallback callback) {
        super(looper);
        this.callback = callback;
    }

    public void handleMessage(@NonNull Message msg) {
        if (callback == null) {
            Log.w(TAG, "handleMessage: callback is null,may activity/fragment already onDestroy");
            return;
        }
        Bitmap bitmap = null;
        switch (msg.what) {
            case EXIT_STAND_BY_MODE:
                exitStandByMode();
                break;
            case MSG_ADD_FACE_FORM_SNAP_SHOT:
            case MSG_ADD_FACE_FORM_VIDEO:
                bitmap = (Bitmap) msg.obj;
                RegisterHelper.instance().registerByBitmap(bitmap, callback.getUserId(),
                        callback.getRegisterOperate(), callback, true);
                break;
            case START_REGISTER:
                startEnrollFace = true;
                sendEmptyMessage(MSG_ADD_FACE_FORM_INTERNAL);
                break;
            case MSG_ADD_FACE_FORM_LOCAL:
                bitmap = (Bitmap) msg.obj;
                RegisterHelper.instance().registerByBitmap(bitmap, callback.getUserId(),
                        callback.getRegisterOperate(), callback, false);
                break;
            case MSG_ADD_FACE_FORM_INTERNAL:
                extractFace();
                if (startEnrollFace) {
                    removeMessages(MSG_ADD_FACE_FORM_INTERNAL);
                    sendEmptyMessage(MSG_ADD_FACE_FORM_INTERNAL);
                }
                break;
            case MSG_SNAP_SHOT_RGB:
                callback.onSnapStart();
                exitStandByMode();
                Bitmap rgbBitmap = snapShot(SnapType.SNAP_RGB);
                callback.onSnapFinish(rgbBitmap);
                break;
            case ADD_USER_INFO:
                String userId = callback.getUserId();
                UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId);
                userInfo.faceFeature = (byte[]) msg.obj;
                BioDataUtil.instance().updateUserInfo(userInfo);
                callback.onRegisterFinish();
                break;
            default:
                break;
        }
    }


    private void extractFace() {
        DetectFaceRequest detectFaceRequest = new DetectFaceRequest();
        detectFaceRequest.setIsNeedFaceInfo(true);
        detectFaceRequest.setIsNeedPicture(true);
        detectFaceRequest.setIsNeedFeature(true);
        String data = JSONUtil.getJsonString(detectFaceRequest);
        byte[] result = new byte[400 * 1024/*if you need image of feature,please alloc a lit big*/];
        int[] size = new int[]{result.length};
        int ret = AMTHidManager.instance().registerFace(data.getBytes(), result, size);
        if (ret == 0) {
            String jsonResult = new String(result, 0, size[0]);
            try {
                JSONObject jsonObject = new JSONObject(jsonResult);
                if (jsonObject.has("status")) {
                    int status = jsonObject.getInt("status");
                    if (status == ErrorCode.ERROR_NONE) {
                        JSONObject detectFaceResultJsonObj = jsonObject.getJSONObject("data");
                        DetectFaceResponse detectFaceResponse =
                                gson.fromJson(detectFaceResultJsonObj.toString(), DetectFaceResponse.class);
                        enrollFaceByFeature(detectFaceResponse);
                    } else {
                        if (status == StatusCode.DETECT_NO_FACE) {
                            callback.onStatusCallback(NO_DETECT_FACE);
                        }
                        String detail = jsonObject.getString("detail");
                        callback.onResult(detail);
                        Log.w(TAG, "extractFace: failed,status = " + status + "," + detail);
                    }
                } else {
                    Log.w(TAG, "extractFace: not expect json");
                }
            } catch (JSONException e) {
                Log.e(TAG, "extractFace: ", e);
                callback.onStatusCallback(JSON_FAILED);
            }
        } else {
            callback.onStatusCallback(SEND_FAILED);
        }
    }


    private void enrollFaceByFeature(DetectFaceResponse detectFaceResponse) {
        if (!startEnrollFace || mCacheFaceRecognizeDataList.size() >= MAX_CACHE_SIZE) {
            return;
        }

        if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
            callback.onStatusCallback(NO_DETECT_FACE);
            return;
        }
        if (detectFaceResponse.faces.size() > 1) {
            callback.onStatusCallback(RegisterStatus.DETECT_TOO_MUCH_FACE);
            return;
        }

        //set first face as max face default
        Face biggestFace = detectFaceResponse.faces.get(0);
        //find max face
        for (Face face : detectFaceResponse.faces) {
            int w1 = Math.abs(face.faceInfo.rect.right - face.faceInfo.rect.left);
            int w2 = Math.abs(biggestFace.faceInfo.rect.right - biggestFace.faceInfo.rect.left);
            if (w1 > w2) {
                biggestFace = face;
            }
        }


        Log.i(TAG, "registerByBitmap: " + biggestFace.faceInfo.toString());
        FaceInfo faceInfo = biggestFace.faceInfo;

        Rect faceRect = faceInfo.rect;
        calEnrollArea(enrollArea, IMAGE_WIDTH, IMAGE_HEIGHT);
        boolean isBigFace = faceRect.width() > enrollArea.width() && faceRect.width() < IMAGE_WIDTH
                && faceRect.top >= enrollArea.top && faceRect.bottom <= IMAGE_HEIGHT
                && faceRect.right > 0 && faceRect.right < IMAGE_WIDTH;
        if (!isBigFace && !enrollArea.contains(faceRect)) {
            //not big face and not in enroll area
            callback.onStatusCallback(RegisterStatus.FACE_NOT_IN_ENROLL_AREA);
            return;
        }

        //face size filter
        int face_w = faceRect.right - faceRect.left;
        if (face_w < Config.instance().faceWidthMinSize) {
            callback.onStatusCallbackEx(FACE_TOO_SMALL, Config.instance().faceWidthMinSize, face_w);
            return;
        }

        int face_h = faceRect.bottom - faceRect.top;
        if (face_h < Config.instance().faceHeightMinSize) {
            callback.onStatusCallbackEx(FACE_TOO_SMALL, Config.instance().faceHeightMinSize, face_h);
            return;
        }

        if (faceInfo.score < Config.instance().faceRegistrationQuality) {
            callback.onStatusCallbackEx(FACE_QUALITY_TOO_BAD,
                    Config.instance().faceRegistrationQuality, faceInfo.score);
            return;
        }

        if (biggestFace.blur > Config.instance().faceBlurThreshold) {
            callback.onStatusCallbackEx(FACE_BLUR_TOO_HIGH,
                    Config.instance().faceBlurThreshold, biggestFace.blur);
            return;
        }

        if (faceInfo.pose.yaw > Config.instance().faceYawMaxThreshold
                || faceInfo.pose.yaw < Config.instance().faceYawMinThreshold) {
            callback.onStatusCallbackEx(FACE_YAW_TOO_BIG,
                    Config.instance().faceYawMaxThreshold, faceInfo.pose.yaw);
            return;
        }

        if (faceInfo.pose.roll > Config.instance().faceRollMaxThreshold
                || faceInfo.pose.roll < Config.instance().faceRollMinThreshold) {
            callback.onStatusCallbackEx(FACE_ROLL_TOO_BIG,
                    Config.instance().faceRollMaxThreshold, faceInfo.pose.roll);
            return;
        }

        String userId = callback.getUserId();
        FaceFeature faceFeature = biggestFace.feature;
        byte[] feature = faceFeature.getFeature();
        byte[] id = new byte[40];
        float[] score = new float[1];
        if (FaceManager.getInstance().dbIdentify(feature, id, score) == 0) {
            String identifyUserId = new String(id, 0, getValidLength(id));
            if (score[0] > Config.instance().faceIdentifyThreshold) {
                if (callback.getRegisterOperate() == RegisterOperate.ADD) {
                    callback.onStatusCallback(FACE_REGISTERED, identifyUserId, Config.instance().faceIdentifyThreshold, score[0]);
                    return;
                }
                if (callback.getRegisterOperate() == RegisterOperate.UPDATE) {
                    if (!identifyUserId.equals(userId)) {
                        callback.onStatusCallback(FACE_REGISTERED, identifyUserId, Config.instance().faceIdentifyThreshold, score[0]);
                        return;
                    }
                }
            }
        }

        UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId);
        if (checkVLFeature(faceFeature, userInfo)) {
            mCacheFaceRecognizeDataList.add(detectFaceResponse);
            //face need reset track id than it can be generate new feature
            callback.onSuccess(null);
            SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep);
        }
        Log.i(TAG, String.format("enrollFaceByFeature: prepare a preTemplate,count =%d", mCacheFaceRecognizeDataList.size()));
        if (mCacheFaceRecognizeDataList.size() >= MAX_CACHE_SIZE) {
            startEnrollFace = false;
            doRegisterFace(userInfo);
            callback.onSuccess(null);
            callback.onRegisterFinish();
        }
    }


    private void enrollFaceByDetectFaceResponse(DetectFaceResponse detectFaceResponse){
        if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
            callback.onStatusCallback(NO_DETECT_FACE);
            return;
        }
        if (detectFaceResponse.faces.size() > 1) {
            callback.onStatusCallback(RegisterStatus.DETECT_TOO_MUCH_FACE);
            return;
        }

        //set first face as max face default
        Face biggestFace = detectFaceResponse.faces.get(0);
        //find max face
        for (Face face : detectFaceResponse.faces) {
            int w1 = Math.abs(face.faceInfo.rect.right - face.faceInfo.rect.left);
            int w2 = Math.abs(biggestFace.faceInfo.rect.right - biggestFace.faceInfo.rect.left);
            if (w1 > w2) {
                biggestFace = face;
            }
        }


        Log.i(TAG, "registerByBitmap: " + biggestFace.faceInfo.toString());
        FaceInfo faceInfo = biggestFace.faceInfo;

        Rect faceRect = faceInfo.rect;
        calEnrollArea(enrollArea, IMAGE_WIDTH, IMAGE_HEIGHT);
        boolean isBigFace = faceRect.width() > enrollArea.width() && faceRect.width() < IMAGE_WIDTH
                && faceRect.top >= enrollArea.top && faceRect.bottom <= IMAGE_HEIGHT
                && faceRect.right > 0 && faceRect.right < IMAGE_WIDTH;
        if (!isBigFace && !enrollArea.contains(faceRect)) {
            //not big face and not in enroll area
            callback.onStatusCallback(RegisterStatus.FACE_NOT_IN_ENROLL_AREA);
            return;
        }

        //face size filter
        int face_w = faceRect.right - faceRect.left;
        if (face_w < Config.instance().faceWidthMinSize) {
            callback.onStatusCallbackEx(FACE_TOO_SMALL, Config.instance().faceWidthMinSize, face_w);
            return;
        }

        int face_h = faceRect.bottom - faceRect.top;
        if (face_h < Config.instance().faceHeightMinSize) {
            callback.onStatusCallbackEx(FACE_TOO_SMALL, Config.instance().faceHeightMinSize, face_h);
            return;
        }

        if (faceInfo.score < Config.instance().faceRegistrationQuality) {
            callback.onStatusCallbackEx(FACE_QUALITY_TOO_BAD,
                    Config.instance().faceRegistrationQuality, faceInfo.score);
            return;
        }

        if (biggestFace.blur > Config.instance().faceBlurThreshold) {
            callback.onStatusCallbackEx(FACE_BLUR_TOO_HIGH,
                    Config.instance().faceBlurThreshold, biggestFace.blur);
            return;
        }

        if (faceInfo.pose.yaw > Config.instance().faceYawMaxThreshold
                || faceInfo.pose.yaw < Config.instance().faceYawMinThreshold) {
            callback.onStatusCallbackEx(FACE_YAW_TOO_BIG,
                    Config.instance().faceYawMaxThreshold, faceInfo.pose.yaw);
            return;
        }

        if (faceInfo.pose.roll > Config.instance().faceRollMaxThreshold
                || faceInfo.pose.roll < Config.instance().faceRollMinThreshold) {
            callback.onStatusCallbackEx(FACE_ROLL_TOO_BIG,
                    Config.instance().faceRollMaxThreshold, faceInfo.pose.roll);
            return;
        }

        String userId = callback.getUserId();
        FaceFeature faceFeature = biggestFace.feature;
        byte[] feature = faceFeature.getFeature();
        byte[] id = new byte[40];
        float[] score = new float[1];
        if (FaceManager.getInstance().dbIdentify(feature, id, score) == 0) {
            String identifyUserId = new String(id, 0, getValidLength(id));
            if (score[0] > Config.instance().faceIdentifyThreshold) {
                if (callback.getRegisterOperate() == RegisterOperate.ADD) {
                    callback.onStatusCallback(FACE_REGISTERED, identifyUserId, Config.instance().faceIdentifyThreshold, score[0]);
                    return;
                }
                if (callback.getRegisterOperate() == RegisterOperate.UPDATE) {
                    if (!identifyUserId.equals(userId)) {
                        callback.onStatusCallback(FACE_REGISTERED, identifyUserId, Config.instance().faceIdentifyThreshold, score[0]);
                        return;
                    }
                }
            }
        }

        UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId);
        if (checkVLFeature(faceFeature, userInfo)) {

            callback.onSuccess(null);
            SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep);
            String faceFeatureBase64Str = faceFeature.data;
            userInfo.faceFeature = Base64.decode(faceFeatureBase64Str, Base64.DEFAULT);

            //save in database
            BioDataUtil.instance().updateUserInfo(userInfo);

            saveVLEnrollImage(detectFaceResponse, userInfo);
        }
    }


    private boolean checkVLFeature(FaceFeature faceFeature, UserInfo userInfo) {
        if (faceFeature != null) {
            //if register ,should verify for is registered person
            //do 1:N ,check if repeat
            byte[] verifyTemplate = faceFeature.getFeature();
            byte[] id = new byte[40];
            float[] score = new float[1];
            FaceManager.getInstance().dbIdentify(verifyTemplate, id, score);
            float faceVLRecognizeThreshold = Config.instance().faceIdentifyThreshold;
            Log.d(TAG, "[checkVLFeature]: dbIdentify score=" + score[0] + ", threshold=" + faceVLRecognizeThreshold);
            if (score[0] > faceVLRecognizeThreshold) {
                String identifyUserId = new String(id, 0, getValidLength(id));
                if (callback.getRegisterOperate() == RegisterOperate.ADD) {
                    callback.onStatusCallback(FACE_REGISTERED, identifyUserId,
                            faceVLRecognizeThreshold, score[0]);
                    return false;
                }
                if (callback.getRegisterOperate() == RegisterOperate.UPDATE) {
                    if (!identifyUserId.equals(userInfo.userId)) {
                        callback.onStatusCallback(FACE_REGISTERED, identifyUserId,
                                faceVLRecognizeThreshold, score[0]);
                        return false;
                    }
                }
            }

            if (mCacheFaceRecognizeDataList.size() > 1) {
                //do 1:1 with first palm feature ,check if it's same palm
                byte[] firstFaceTemplate = mCacheFaceRecognizeDataList.get(0).faces.get(0).feature.getFeature();
                float similarity = FaceManager.getInstance().dbVerify(firstFaceTemplate, verifyTemplate);
                Log.d(TAG, "[checkVLFeature]: dbVerify score=" + similarity + ", recognizeThreshold=" + faceVLRecognizeThreshold);
                if (similarity < faceVLRecognizeThreshold) {
                    Log.w(TAG, "[checkVLFeature]: verify failed, score=" + similarity + ", threshold=" + faceVLRecognizeThreshold + ", need the same palm to finish enrolling the user");
                    callback.onStatusCallback(RegisterStatus.ENROLL_NEED_SAME_FACE);
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }


    private void calEnrollArea(Rect enrollArea, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        int fixRectLeft = width / 6;
        int fixRectRight = fixRectLeft * 5;
        int fixRectTop = centerY - centerX;
        int fixRectBottom = centerY + centerX;
        enrollArea.set(fixRectLeft, fixRectTop, fixRectRight, fixRectBottom);
    }

    private void doRegisterFace(UserInfo userInfo) {
        //find best visible palm template
        Collections.sort(mCacheFaceRecognizeDataList, new Comparator<DetectFaceResponse>() {
            @Override
            public int compare(DetectFaceResponse o1, DetectFaceResponse o2) {
                return (int) ((o2.faces.get(0).faceInfo.score - o1.faces.get(0).faceInfo.score) * 100);
            }
        });
        DetectFaceResponse detectFaceResponse = mCacheFaceRecognizeDataList.get(0);

        String faceFeatureBase64Str = detectFaceResponse.faces.get(0).feature.data;
        userInfo.faceFeature = Base64.decode(faceFeatureBase64Str, Base64.DEFAULT);
        //best to save in database
        BioDataUtil.instance().updateUserInfo(userInfo);

        saveVLEnrollImage(detectFaceResponse, userInfo);
    }

    private void saveVLEnrollImage(DetectFaceResponse detectFaceResponse, UserInfo userInfo) {
        String userId = userInfo.userId;
        //save vlFaceImage
        String vlFaceImage = detectFaceResponse.faces.get(0).picture.data;
        if (vlFaceImage != null) {
            byte[] jpegData = Base64.decode(vlFaceImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            saveBimap(bitmap, userId + "_face_vl", userId);
        }
    }


    private void saveBimap(Bitmap bitmap, String fileName, String pin) {
        if (bitmap == null) {
            Log.e(TAG, "[saveBimap]: save enroll palm pic fail,bitmap is null, pin=" + pin);
            return;
        }
        FileUtils.saveBitmap(bitmap, fileName,
                FileUtils.USER_BIO_PHOTO + File.separator + pin + File.separator);
    }

}
