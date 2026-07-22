package com.armatura.biomodule.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.camera.biodata.CustomDataListener;
import com.armatura.biomodule.camera.biodata.FaceData;
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache;
import com.armatura.biomodule.common.Common;
import com.armatura.biomodule.common.IdentifyState;
import com.armatura.biomodule.common.WatchDogType;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.databases.BioDataUtil;
import com.armatura.biomodule.fragment.ArmaturaFragment;
import com.armatura.biomodule.manager.CameraWatchDogManager;
import com.armatura.biomodule.manager.NIRPalmManager;
import com.armatura.biomodule.manager.PalmManager;
import com.armatura.biomodule.pojo.common.Attribute;
import com.armatura.biomodule.pojo.common.BioType;
import com.armatura.biomodule.pojo.common.Label;
import com.armatura.biomodule.pojo.common.LiveData;
import com.armatura.biomodule.pojo.common.TrackData;
import com.armatura.biomodule.pojo.face.FaceFeature;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.face.recognize.IdentifyInfo;
import com.armatura.biomodule.pojo.palm.PalmFeature;
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.pojo.setting.VLPalmSetting;
import com.armatura.biomodule.util.CsvHelper;
import com.armatura.biomodule.util.FileUtils;
import com.armatura.biomodule.util.HidHelper;
import com.armatura.biomodule.util.JSONUtil;
import com.armatura.biomodule.util.StringHelper;
import com.armatura.constant.PalmErrorCode;
import com.armatura.internaldata.util.ModuleBioDataUtil;
import com.armatura.uvclib.CameraDataCallback;
import com.armatura.uvclib.model.CustomData;
import com.armatura.uvclib.model.VideoData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraDataModel implements CameraDataCallback, ICameraDataModel {
    private final String TAG = this.getClass().getSimpleName();

    private final RecognizedBioDataCache recognizedBioDataCache;
    private final HandlerThread faceDataHandlerThread;
    private final Handler faceDataHandler;
    private final HandlerThread palmDataHandlerThread;
    private final Handler palmDataHandler;
    private final HandlerThread cardDataHandlerThread;
    private final Handler cardDataHandler;
    private final HandlerThread fpsHandlerThread;
    private final Handler fpsHandler;

    private CustomDataListener customDataListener;
    private final Gson gson = new Gson();
    private int imgFrameCount;
    private int customDataFrameCount;
    private int imgFPS;
    private int customDataFPS;
    private long imgPreTime;
    private long customPreTime;
    private final int[] fpsInfo = new int[2];

    private long lastCaptureTime = 0L;
    private static final int TEST_RECORD_INTERVAL = 100;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
    private final static int MSG_PAUSE_PROCESS_PALM = 0x8001;
    private final static int MSG_RESUME_PROCESS_PALM = 0x8002;

    private final static int MSG_RESET_IMG_FPS = 0x7001;
    private final static int MSG_RESET_CSD_FPS = 0x7002;

    CameraDataModel() {
        fpsHandlerThread = new HandlerThread("fpsThread");
        fpsHandlerThread.start();
        fpsHandler = new Handler(fpsHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_RESET_IMG_FPS:
                        imgFPS = 0;
                        break;
                    case MSG_RESET_CSD_FPS:
                        customDataFPS = 0;
                        break;
                }
                return false;
            }
        });
        faceDataHandlerThread = new HandlerThread("FaceDataHandlerThread");
        faceDataHandlerThread.start();
        //if data is face template,handle it in this callback
        //get face_id from list which recognized
        //if does't exist in recognized list,add it
        //if liveness function is disable,assume it as true
        //no feature or no IdentifyInfo,skip this time
        //match local
        //if you want save frame data
        //save liveness photo
        //get video data if you need
        //save recognize record
        Handler.Callback faceDataRecognizeCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                DrawFaceData drawFaceData = (DrawFaceData) message.obj;

                List<FaceData> faceDataList = drawFaceData.faceDataList;
                for (FaceData faceData : faceDataList) {
                    //get face_id from list which recognized
                    Common.RecognizedFaceData recognizedFaceData = recognizedBioDataCache.getRecognizedFace(faceData.trackData.trackId);
                    //if does't exist in recognized list,add it
                    if (recognizedFaceData == null) {
                        recognizedFaceData = new Common.RecognizedFaceData();
                        recognizedBioDataCache.addRecFace(faceData.trackData.trackId, recognizedFaceData);
                    }
                    recognizedFaceData.ori_w = drawFaceData.width;
                    recognizedFaceData.ori_h = drawFaceData.height;
                    recognizedFaceData.frameindex = drawFaceData.index;

                    if (faceData.bHasFeature) {
                        recognizedFaceData.faceData.bHasFeature = true;
                        recognizedFaceData.faceData.faceFeature = faceData.faceFeature;
                    }

                    if (faceData.bHasLiveScore) {
                        recognizedFaceData.faceData.bHasLiveScore = true;
                        recognizedFaceData.faceData.liveness = faceData.liveness;
                        int liveness = faceData.liveness.liveness;
                        recognizedFaceData.isPassLiveness = (liveness == Common.FaceLiveStatus.PASS.getCode()
                                ||/*if liveness function is disable,assume it as true*/
                                liveness == Common.FaceLiveStatus.DISABLE.getCode());
                    }

                    if (faceData.bHasAttr) {
                        recognizedFaceData.faceData.bHasAttr = true;
                        recognizedFaceData.faceData.attribute = faceData.attribute;
                    }

                    //no feature or no IdentifyInfo,skip this time
                    if (!faceData.bHasFeature && !faceData.bHasIdentifyInfo) {
                        return true;
                    }

                    UserInfo userInfo = null;
                    if (Config.instance().recognizeMode == Config.MULTI_BIO_MODULE_INTERNAL_MODE) {
                        if (faceData.bHasIdentifyInfo) {
                            userInfo = ModuleBioDataUtil.instance().getUserInfoFromFaceData(faceData);
                        }
                    } else {
                        if (faceData.bHasFeature) {
                            userInfo = BioDataUtil.identifyUserInfoByFaceFeature(faceData.faceFeature.getFeature());
                        }
                    }

                    if (userInfo == null) {
                        //identify failed
                        recognizedFaceData.isRecognized = false;
                        customDataListener.onIdentifyFailed(new IdentifyFailedData(BioType.FACE));
                        return true;
                    }

                    //identify success
                    if (faceData.bHasLiveScore) {
                        userInfo.liveData = faceData.liveness;
                    }
                    userInfo.bUpdate = true;
                    recognizedFaceData.isRecognized = true;
                    recognizedFaceData.userFace = userInfo;

                    if (Config.instance().isDisplayLivenessInfo) {
                        if (recognizedFaceData.isPassLiveness) {
                            recognizedBioDataCache.addRecordUserInfo(userInfo);
                        }
                    } else {
                        recognizedBioDataCache.addRecordUserInfo(userInfo);
                    }

                    //save liveness photo
                    if (Config.instance().isTestMode) {
                        saveFaceLiveRecord(drawFaceData, userInfo);
                    }

                    //save recognize record
                    if (Config.instance().isTestMode) {
                        saveFaceIdentifyRecord(drawFaceData, userInfo, faceData);
                    }
                }

                return true;
            }
        };
        faceDataHandler = new Handler(faceDataHandlerThread.getLooper(), faceDataRecognizeCallback);

        palmDataHandlerThread = new HandlerThread("PalmDataHandlerThread");
        palmDataHandlerThread.start();
        //palm recognize process
        Handler.Callback palmDataRecognizeCallback = new Handler.Callback() {
            private final AtomicBoolean pausePalmRecognizeFlag = new AtomicBoolean(false);
            private final static int PAUSE_TIME = 600;
            private int identifyFailedCount = 0;

            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 0 && msg.obj != null) {
                    if (pausePalmRecognizeFlag.get()) {
                        Log.i(TAG, "handleMessage: palm process is pause");
                        return true;
                    }
                    PalmRecognizeData palmRecognizeData = (PalmRecognizeData) msg.obj;
                    PalmInfo palmInfo = palmRecognizeData.getTrackInfo();
                    UserInfo userInfoByUserPin;
                    if (Config.instance().recognizeMode == Config.MULTI_BIO_MODULE_INTERNAL_MODE) {
                        //palm will recognize in MultiBioModule and return identify info
                        boolean isHasIdentifyInfo = false;
                        List<IdentifyInfo> identify = palmRecognizeData.getIdentify();
                        if (identify != null && !identify.isEmpty()) {
                            IdentifyInfo identifyInfo = identify.get(0);
                            isHasIdentifyInfo = (identifyInfo.getUserId() != null && !"".equals(identifyInfo.getUserId()));
                        }
                        if (isHasIdentifyInfo) {
                            IdentifyInfo identifyInfo = identify.get(0);
                            UserInfo userInfo = new UserInfo();
                            userInfo.groupId = identifyInfo.getGroupId();
                            userInfo.personId = identifyInfo.getPersonId();
                            userInfo.name = identifyInfo.getName();
                            userInfo.userId = identifyInfo.getUserId();
                            userInfo.similarity = identifyInfo.getSimilarity();
                            userInfo.avatarIndex = -1;
                            userInfo.setIdentifyMode(VLPalmSetting.PALM_INTERNAL);
                            palmInfo.setUserInfo(userInfo);
                        }
                        onPalmRecognizeComing(palmInfo, palmRecognizeData);
                    } else /*HOST_MODE*/ {
                        //palm will recognize in Host Device.The application get feature
                        // from MultiBioModule and identify by Host.
                        if (palmRecognizeData.getFeature() == null && palmRecognizeData.getFeatureVein() == null) {
                            Log.w(TAG, "handleMessage: either feature or featureVein is null,skip");
                            onPalmRecognizeComing(palmInfo, palmRecognizeData);
                            return false;
                        }
                        userInfoByUserPin = identifyPalm(palmRecognizeData);
                        if (userInfoByUserPin != null) {
                            whenIdentifyPalmSuccess(palmInfo, userInfoByUserPin, palmRecognizeData);
                        } else {
                            whenIdentifyPalmFailed();
                        }
                        onPalmRecognizeComing(palmInfo, palmRecognizeData);
                    }
                } else if (msg.what == MSG_PAUSE_PROCESS_PALM) {
                    pausePalmRecognizeFlag.set(true);
                } else if (msg.what == MSG_RESUME_PROCESS_PALM) {
                    pausePalmRecognizeFlag.set(false);
                }
                return false;
            }

            private void whenIdentifyPalmFailed() {
                //palm identify failed
                ++identifyFailedCount;
                Log.d(TAG, "palm identify failed,count = " + identifyFailedCount);
                if (identifyFailedCount >= Config.instance().maxIdentifyFailedCount) {
                    if (Common.state == IdentifyState.IDENTIFY_CONST
                            || (Common.state == IdentifyState.IDENTIFY_ONCE && !Common.state.isIdentified())) {
                        if (Config.isSupportIndicator && Config.controlLEDByHost) {
                            HidHelper.controlIndicatorRedLED();
                            palmDataHandler.sendEmptyMessage(MSG_PAUSE_PROCESS_PALM);
                            palmDataHandler.sendEmptyMessageDelayed(MSG_RESUME_PROCESS_PALM,
                                    PAUSE_TIME);
                        }
                    }
                    customDataListener.onIdentifyFailed(new IdentifyFailedData(BioType.PALM));
                    identifyFailedCount = 0;
                }
            }

            private void whenIdentifyPalmSuccess(PalmInfo palmInfo, UserInfo userInfoByUserPin
                    , PalmRecognizeData palmRecognizeData) {
                if (palmInfo == null) {
                    Log.w(TAG, "whenIdentifyPalmSuccess: palm info is null,invalid identify result");
                    return;
                }
                identifyFailedCount = 0;
                if (Common.state == IdentifyState.IDENTIFY_CONST
                        || (Common.state == IdentifyState.IDENTIFY_ONCE && !Common.state.isIdentified())) {
                    if (Config.isSupportIndicator && Config.controlLEDByHost) {
                        HidHelper.controlIndicatorGreenLED();
                        pausePalmRecognizeFlag.set(true);
                        palmDataHandler.sendEmptyMessage(MSG_PAUSE_PROCESS_PALM);
                        palmDataHandler.sendEmptyMessageDelayed(MSG_RESUME_PROCESS_PALM,
                                PAUSE_TIME);
                    }
                }
                palmInfo.setUserInfo(userInfoByUserPin);
                saveTestLiveRecord(palmRecognizeData, userInfoByUserPin);
            }
        };
        palmDataHandler = new Handler(palmDataHandlerThread.getLooper(), palmDataRecognizeCallback);
        //card process
        cardDataHandlerThread = new HandlerThread("cardDataHandlerThread");
        cardDataHandlerThread.start();
        Handler.Callback cardDataProcessCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                String card = (String) msg.obj;
                CardInfo cardInfo = new CardInfo();
                cardInfo.rawCard = card;
                UserInfo userInfo = BioDataUtil.identifyUserInfoByCard(card);
                if (userInfo != null) {
                    cardInfo.isIdentifySuccess = true;
                    cardInfo.userInfo = userInfo;
                }
                if (customDataListener != null) {
                    customDataListener.onCardInfo(cardInfo);
                }
                return false;
            }
        };
        cardDataHandler = new Handler(cardDataHandlerThread.getLooper(), cardDataProcessCallback);

        recognizedBioDataCache = RecognizedBioDataCache.instance();
    }

    private void onPalmRecognizeComing(PalmInfo palmInfo, PalmRecognizeData palmRecognizeData) {
        if (palmInfo == null || palmRecognizeData == null) {
            Log.w(TAG, "onPalmRecognizeComing: palm info and palm recognize data is null,skip");
            return;
        }
        if (Config.instance().drawPalmRectWhenIdentify) {
            if (palmRecognizeData.getFeature() == null && palmRecognizeData.getFeatureVein() == null) {
                Log.w(TAG, "onPalmRecognizeComing: either feature or featureVein is null,skip");
                return;
            }
            customDataListener.onPalmRecognizeComing(palmRecognizeData);
        } else {
            customDataListener.onPalmRecognizeComing(palmRecognizeData);
        }
    }


    private UserInfo identifyPalm(PalmRecognizeData palmRecognizeData) {
        UserInfo userInfo = null;
        switch (Config.palmTemplateMode) {
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL:
                userInfo = identifyPalmByVLFeature(palmRecognizeData);
                if (userInfo != null) {
                    userInfo.setIdentifyMode(VLPalmSetting.PALM_TEMPLATE_MODE_VL);
                }
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_IR:
                userInfo = identifyPalmByNIRFeature(palmRecognizeData);
                if (userInfo != null) {
                    userInfo.setIdentifyMode(VLPalmSetting.PALM_TEMPLATE_MODE_IR);
                }
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR:
                UserInfo userInfoByVL = identifyPalmByVLFeature(palmRecognizeData);
                if (userInfoByVL == null) {
                    Log.w(TAG, "identifyPalm: identifyPalmByVLFeature failed");
                    return null;
                }
                UserInfo userInfoByIR = identifyPalmByNIRFeature(palmRecognizeData);
                if (userInfoByIR == null) {
                    Log.w(TAG, "identifyPalm: identifyPalmByNIRFeature failed");
                    return null;
                }
                userInfoByVL.setIdentifyMode(VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR);
                if (userInfoByIR.userId.equals(userInfoByVL.userId)) {
                    userInfo = userInfoByVL;
                    userInfo.nirSimilarity = userInfoByIR.nirSimilarity;
                } else {
                    Log.w(TAG, "identifyPalm: vl identify result different with nir,failed");
                }
                break;
        }
        return userInfo;
    }

    private UserInfo identifyPalmByVLFeature(PalmRecognizeData palmRecognizeData) {
        PalmFeature feature = palmRecognizeData.getFeature();
        if (feature != null) {
            String verTemplate = feature.getVerTemplate();
            byte[] templateBytes = Base64.decode(verTemplate, Base64.NO_WRAP);
            byte[] userPin = new byte[40];
            float[] score = new float[1];
            int ret = PalmManager.getInstance().dbIdentify(templateBytes, userPin, score);
            if (ret == PalmErrorCode.SUCCESS) {
                String userPinStr = new String(userPin, 0, StringHelper.getValidLength(userPin));
                Log.i(TAG, String.format("[identifyPalmByVLFeature]: score=%f,name=%s", score[0], userPinStr));
                if (score[0] >= Config.instance().palmVLIdentifyThreshold) {
                    UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userPinStr);
                    if (userInfo != null) {
                        userInfo.similarity = score[0];
                    }
                    return userInfo;
                } else {
                    saveVLIdentifyFailedRecord(palmRecognizeData, userPinStr, score[0]);
                }
            } else {
                Log.w(TAG, "identifyPalmByVLFeature: ret = " + ret);
            }
        }
        return null;
    }

    private UserInfo identifyPalmByNIRFeature(PalmRecognizeData palmRecognizeData) {
        PalmFeature feature = palmRecognizeData.getFeatureVein();
        if (feature != null) {
            String verTemplate = feature.getVerTemplate();
            byte[] templateBytes = Base64.decode(verTemplate, Base64.NO_WRAP);
            byte[] userPin = new byte[40];
            float[] score = new float[1];
            if (NIRPalmManager.getInstance().dbIdentify(templateBytes, userPin, score) == PalmErrorCode.SUCCESS) {
                String userPinStr = new String(userPin, 0, StringHelper.getValidLength(userPin));
                Log.i(TAG, String.format("[identifyPalmByNIRFeature]: score=%f,name=%s", score[0], userPinStr));
                if (score[0] >= Config.instance().palmVLIdentifyThreshold) {
                    UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userPinStr);
                    if (userInfo != null) {
                        userInfo.nirSimilarity = score[0];
                    }
                    return userInfo;
                } else {
                    saveIRIdentifyFailedRecord(palmRecognizeData, userPinStr, score[0]);
                }
            }
        }
        return null;
    }


    private void saveVLIdentifyFailedRecord(PalmRecognizeData palmRecognizeData, String userId, float score) {
        if (Config.instance().isTestMode) {
            String curUserPin = "unknown";
            if (!TextUtils.isEmpty(ArmaturaFragment.Companion.getCUR_USER_PIN())) {
                curUserPin = ArmaturaFragment.Companion.getCUR_USER_PIN();
            }
            PalmInfo trackInfo = palmRecognizeData.getTrackInfo();
            float liveScore = trackInfo.getLiveScore();
            String dateTimeStr = sdf.format(new Date());
            String vlFileName = String.format("%s_%s_%s_%s_%s",
                    dateTimeStr, userId, curUserPin, score, liveScore);
            PalmFeature palmFeature = palmRecognizeData.getFeature();
            if (palmFeature != null && palmFeature.getImage() != null) {
                savePalmImage(palmFeature.getImage(), vlFileName, "/fail/" + curUserPin);
                CsvHelper.getInstance().appendPalmTestRecord("fail_vl",
                        dateTimeStr, userId, curUserPin, String.valueOf(score), String.valueOf(liveScore));
            }
        }
    }

    private void saveIRIdentifyFailedRecord(PalmRecognizeData palmRecognizeData, String userId, float score) {
        if (Config.instance().isTestMode) {
            String curUserPin = "unknown";
            if (!TextUtils.isEmpty(ArmaturaFragment.Companion.getCUR_USER_PIN())) {
                curUserPin = ArmaturaFragment.Companion.getCUR_USER_PIN();
            }
            PalmInfo trackInfo = palmRecognizeData.getTrackInfo();
            float liveScore = trackInfo.getLiveScore();
            String dateTimeStr = sdf.format(new Date());
            String irFileName = String.format("%s_%s_%s_%s_%s",
                    dateTimeStr, userId, curUserPin, score, liveScore);
            PalmFeature palmVeinFeature = palmRecognizeData.getFeatureVein();
            if (palmVeinFeature != null && palmVeinFeature.getImage() != null) {
                savePalmImage(palmVeinFeature.getImage(), irFileName, "/fail/" + curUserPin);
                CsvHelper.getInstance().appendPalmTestRecord("fail_ir",
                        dateTimeStr, userId, curUserPin, String.valueOf(score), String.valueOf(liveScore));
            }
        }
    }

    private void saveTestLiveRecord(PalmRecognizeData palmRecognizeData, UserInfo identifyUserInfo) {
        if (Config.instance().isTestMode) {
            PalmInfo palmInfo = palmRecognizeData.getTrackInfo();
            float liveScore = palmInfo.getLiveScore();
            Log.d(TAG, "[handleMessage]: live score="
                    + liveScore + ", threshold=" + Config.instance().palmVLLivenessThreshold);
            boolean bPassLiveThreshold = !Config.instance().bPalmLivenessEnable
                    || (!Float.isNaN(liveScore) && liveScore >= Config.instance().palmVLLivenessThreshold);
            String curUserPin = "unknown";
            if (!TextUtils.isEmpty(ArmaturaFragment.Companion.getCUR_USER_PIN())) {
                curUserPin = ArmaturaFragment.Companion.getCUR_USER_PIN();
            }
            String dateTimeStr = sdf.format(new Date());
            String vlFileName = String.format("vl_%s_%s_%s_%s_%s",
                    dateTimeStr, identifyUserInfo.userId, curUserPin, identifyUserInfo.similarity, liveScore);
            String irFileName = String.format("ir_%s_%s_%s_%s_%s",
                    dateTimeStr, identifyUserInfo.userId, curUserPin, identifyUserInfo.nirSimilarity, liveScore);
            if (bPassLiveThreshold) {
                PalmFeature palmFeature = palmRecognizeData.getFeature();
                if (palmFeature != null && palmFeature.getImage() != null) {
                    String fileName = palmFeature.getFileName();
                    if (fileName != null) {
                        byte[] nv21 = HidHelper.getAndSaveFile(fileName);
                        savePalmNV21Image(nv21, vlFileName, "/success/" + curUserPin);
                    }

                    savePalmImage(palmFeature.getImage(), vlFileName, "/success/" + curUserPin);
                    CsvHelper.getInstance().appendPalmTestRecord("success_vl",
                            dateTimeStr, identifyUserInfo.userId, curUserPin, String.valueOf(identifyUserInfo.similarity), String.valueOf(liveScore));
                }
                PalmFeature palmVeinFeature = palmRecognizeData.getFeatureVein();
                if (palmVeinFeature != null && palmVeinFeature.getImage() != null) {
                    String fileName = palmVeinFeature.getFileName();
                    if (fileName != null) {
                        byte[] nv21 = HidHelper.getAndSaveFile(fileName);
                        savePalmNV21Image(nv21, irFileName, "/success/" + curUserPin);
                    }
                    savePalmImage(palmVeinFeature.getImage(), irFileName, "/success/" + curUserPin);
                    CsvHelper.getInstance().appendPalmTestRecord("success_ir",
                            dateTimeStr, identifyUserInfo.userId, curUserPin, String.valueOf(identifyUserInfo.similarity), String.valueOf(liveScore));
                }
            } else {
                PalmFeature palmFeature = palmRecognizeData.getFeature();
                if (palmFeature != null && palmFeature.getImage() != null) {
                    String fileName = palmFeature.getFileName();
                    if (fileName != null) {
                        byte[] nv21 = HidHelper.getAndSaveFile(fileName);
                        savePalmNV21Image(nv21, vlFileName, "/fake/" + curUserPin);
                    }

                    savePalmImage(palmFeature.getImage(), vlFileName, "/fake/" + curUserPin);
                    CsvHelper.getInstance().appendPalmTestRecord("fake_vl",
                            dateTimeStr, identifyUserInfo.userId, curUserPin, String.valueOf(identifyUserInfo.similarity), String.valueOf(liveScore));
                }
                PalmFeature palmVeinFeature = palmRecognizeData.getFeatureVein();
                if (palmVeinFeature != null && palmVeinFeature.getImage() != null) {
                    String fileName = palmVeinFeature.getFileName();
                    if (fileName != null) {
                        byte[] nv21 = HidHelper.getAndSaveFile(fileName);
                        savePalmNV21Image(nv21, irFileName, "/fake/" + curUserPin);
                    }

                    savePalmImage(palmVeinFeature.getImage(), irFileName, "/fake/" + curUserPin);
                    CsvHelper.getInstance().appendPalmTestRecord("fake_ir",
                            dateTimeStr, identifyUserInfo.userId, curUserPin, String.valueOf(identifyUserInfo.similarity), String.valueOf(liveScore));
                }
            }
        }
    }


    @Override
    public void onFrameDataRecv(VideoData videoData) {
        CameraWatchDogManager.getInstance().feedCameraWatchDog(WatchDogType.UVC);
        if (customDataListener != null) {
            customDataListener.onVideoData(videoData);
        }
        updateFrameRate();
    }

    @Override
    public void onCustomDataRecv(CustomData customData) {
        CameraWatchDogManager.getInstance().feedCameraWatchDog(WatchDogType.UVC);
        if (customData != null) {
            updateCustomDataFPS();
            getSegmentData(customData);
        }
    }

    @Override
    public void addBitmap(Bitmap bitmap) {
        if (customDataListener != null) {
            customDataListener.onBitmap(bitmap);
        }
    }


    @Override
    public void setCustomDataListener(CustomDataListener customDataListener) {
        this.customDataListener = customDataListener;
    }

    @Override
    public RecognizedBioDataCache getRecognizedBioDataCache() {
        return recognizedBioDataCache;
    }


    @Override
    public int[] getImgFPS() {
        fpsInfo[0] = imgFPS;
        fpsInfo[1] = customDataFPS;
        return fpsInfo;
    }

    @Override
    public void clearData() {
        recognizedBioDataCache.clearRecFaces();
        imgPreTime = 0;
        customPreTime = 0;
    }

    @Override
    public void onDestroy() {
        if (faceDataHandlerThread != null) {
            faceDataHandlerThread.quit();
            faceDataHandlerThread.interrupt();
        }
        if (palmDataHandlerThread != null) {
            palmDataHandlerThread.quit();
            palmDataHandlerThread.interrupt();
        }
        if (cardDataHandlerThread != null) {
            cardDataHandlerThread.quit();
            cardDataHandlerThread.interrupt();
        }
        clearData();
    }

    private synchronized void updateFrameRate() {
        fpsHandler.removeMessages(MSG_RESET_IMG_FPS);
        fpsHandler.sendEmptyMessageDelayed(MSG_RESET_IMG_FPS, 1000);
        long curtime = System.currentTimeMillis();
        if (imgPreTime == 0) {
            imgPreTime = curtime;
            imgFrameCount = 0;
            return;
        }
        if ((curtime - imgPreTime) > 1000) {
            imgFPS = imgFrameCount;
//            Log.i(TAG, "updateImgFrameRate: " + imgFPS);
            imgFrameCount = 0;
            imgPreTime = curtime;
        } else {
            imgFrameCount++;
        }
    }

    private synchronized void updateCustomDataFPS() {
        fpsHandler.removeMessages(MSG_RESET_CSD_FPS);
        fpsHandler.sendEmptyMessageDelayed(MSG_RESET_CSD_FPS, 1000);
        long curtime = System.currentTimeMillis();
        if (customPreTime == 0) {
            customPreTime = curtime;
            customDataFrameCount = 0;
            return;
        }
        if ((curtime - customPreTime) > 1000) {
            customDataFPS = customDataFrameCount;
//            Log.i(TAG, "updateCustomDataFPS: " + customDataFPS);
            customDataFrameCount = 0;
            customPreTime = curtime;
        } else {
            customDataFrameCount++;
        }
    }

    private void saveFaceIdentifyRecord(DrawFaceData drawFaceData, UserInfo userFace, FaceData faceData) {
        if (drawFaceData != null) {
            Log.i(TAG, "[saveFaceIdentifyRecord]: save record");
            //save photo if identify success
            String imageBase64Str = drawFaceData.faceDataList.get(0).faceFeature.image;
            if (imageBase64Str == null) {
                Log.w(TAG, "saveFaceIdentifyRecord: no feature image ,skip");
                return;
            }
            byte[] jpegData = Base64.decode(imageBase64Str, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
            if (bitmap == null) {
                Log.w(TAG, "saveFaceIdentifyRecord: bitmap invalid");
                return;
            }
            String fileName = (userFace != null ? userFace.name : "null") + "_" + System.currentTimeMillis();
            String path;
            if (faceData.attribute.respirator == 1 && faceData.attribute.respiratorLevel == 1) {
                path = FileUtils.saveBitmap(bitmap, fileName, FileUtils.maskOnFacePhotoPath);
            } else if (faceData.attribute.respirator == 1 && faceData.attribute.respiratorLevel > 1) {
                path = FileUtils.saveBitmap(bitmap, fileName, FileUtils.maskOnWrongPlacePhotoPath);
            } else {
                path = FileUtils.saveBitmap(bitmap, fileName, FileUtils.noMaskOnFacePhotoPath);
            }
            Log.i(TAG, String.format("saveRecord: mask=%d,level=%d", faceData.attribute.respirator, faceData.attribute.respiratorLevel));

            CsvHelper.getInstance().appendFaceAttrAndRecognizeRecord(userFace != null ? userFace.name : "null", String.valueOf(userFace != null ? userFace.similarity : -1), path, faceData.liveness, faceData.attribute);
        } else {
            Log.e(TAG, "[saveRecord]: save failed,frameData is null");
        }
    }

    private void saveFaceLiveRecord(DrawFaceData drawFaceData, UserInfo userInfo) {
        if (drawFaceData != null) {
            Log.i(TAG, "saveFaceLiveRecord: save bitmap for liveness");
            if (drawFaceData.faceDataList == null || drawFaceData.faceDataList.isEmpty()) {
                return;
            }
            FaceData faceData = drawFaceData.faceDataList.get(0);
            if (faceData == null) {
                return;
            }
            IdentifyInfo identifyInfo = null;
            if (faceData.bHasIdentifyInfo) {
                identifyInfo = faceData.identifyInfoList.get(0);
            } else {
                identifyInfo = new IdentifyInfo();
                identifyInfo.setSimilarity(userInfo.similarity);
                identifyInfo.setUserId(userInfo.userId);
            }
            String curUserPin = "unknown";
            if (!TextUtils.isEmpty(ArmaturaFragment.Companion.getCUR_USER_PIN())) {
                curUserPin = ArmaturaFragment.Companion.getCUR_USER_PIN();
            }
            String dateTimeStr = sdf.format(new Date());
            String liveScore = "0";
            if (faceData.liveness != null) {
                BigDecimal bigDecimal = new BigDecimal(faceData.liveness.livenessScore);
                liveScore = bigDecimal.setScale(8, RoundingMode.HALF_UP).toPlainString();
            }

            String userId = identifyInfo == null ? "" : identifyInfo.getUserId();
            String similarity = "0";
            if (identifyInfo != null) {
                BigDecimal bigDecimal = BigDecimal.valueOf(identifyInfo.getSimilarity());
                similarity = bigDecimal.setScale(8, RoundingMode.HALF_UP).toPlainString();
            }
            String vlFileName = String.format("vl_%s_%s_%s_%s_%s",
                    dateTimeStr, userId,
                    curUserPin, similarity, liveScore);
            if (faceData.bHasFeature) {
                saveFaceImage(faceData.faceFeature.image, vlFileName, "/" + curUserPin);
            }
            CsvHelper.getInstance().appendFaceTestRecord(
                    faceData.liveness.liveness == 2 ? "real" : "fake",
                    dateTimeStr, userId, curUserPin, similarity, liveScore);
        } else {
            Log.e(TAG, "saveFaceLiveRecord: save failed, frameData is null");
        }
    }

    private void getSegmentData(CustomData customData) {

        String bioJsonInfo = new String(customData.data, 0, customData.data.length).trim();
        JsonObject jsonObject = null;
        try {
            jsonObject = JsonParser.parseString(bioJsonInfo).getAsJsonObject();
        } catch (Exception e) {
            Log.e(TAG, "getSegmentData: " + bioJsonInfo);
        }


        if (jsonObject == null) {
            return;
        }

        int label = jsonObject.get("label").getAsInt();

        switch (label) {
            case Label.LABEL_FACE:
                faceBioProcess(jsonObject, customData);
                break;
            case Label.LABEL_PALM:
                palmBioProcess(jsonObject);
                break;
            case Label.LABEL_CARD:
                cardProcess(jsonObject);
                break;
            case Label.LABEL_DISTANCE:
                detectionDistance(bioJsonInfo);
                break;
            default:
                break;
        }
    }


    /**
     * process bio info when label = 1
     *
     * @param jsonObject BioInfo json object
     * @param customData UVC Custom Data
     */
    private void faceBioProcess(JsonObject jsonObject, CustomData customData) {
        List<FaceData> faceDataArrayList = new ArrayList<>();
        DrawFaceData drawFaceData = new DrawFaceData();
        drawFaceData.width = customData.width;
        drawFaceData.height = customData.height;
        drawFaceData.index = customData.frame_index;

        JsonArray faceArray = jsonObject.get("face").getAsJsonArray();

        if (faceArray == null) {
            return;
        }

        for (JsonElement element : faceArray) {
            JsonObject faceJsonObject = element.getAsJsonObject();
            FaceData faceData = new FaceData();
            //tracker
            if (faceJsonObject.has("tracker")) {
                faceData.trackData = gson.fromJson(faceJsonObject.get("tracker"), TrackData.class);
                faceData.bHasTrack = true;
            }
            //attribute
            if (faceJsonObject.has("attribute")) {
                faceData.attribute = gson.fromJson(faceJsonObject.get("attribute"), Attribute.class);
                faceData.bHasAttr = true;
            }

            //feature
            if (faceJsonObject.has("feature")) {
                faceData.faceFeature = gson.fromJson(faceJsonObject.get("feature"), FaceFeature.class);
                faceData.bHasFeature = true;
            }

            //liveness
            if (faceJsonObject.has("liveness")) {
                faceData.liveness = gson.fromJson(faceJsonObject.get("liveness"), LiveData.class);
                faceData.bHasLiveScore = true;
            }

            //identify
            if (faceJsonObject.has("identify")) {
                JsonArray identifyArray = faceJsonObject.getAsJsonArray("identify");
                for (JsonElement jsonElement : identifyArray) {
                    IdentifyInfo identifyInfo = gson.fromJson(jsonElement, IdentifyInfo.class);
                    if (identifyInfo.getUserId() != null && !identifyInfo.getUserId().isEmpty()) {
                        faceData.bHasIdentifyInfo = true;
                    }
                    faceData.identifyInfoList.add(identifyInfo);
                }
            }

            faceDataArrayList.add(faceData);
        }

        if (faceDataArrayList.isEmpty()) {
            Log.w(TAG, "[getSegmentData]: No Face detect");
            return;
        }

        drawFaceData.faceDataList = faceDataArrayList;

        faceDataHandler.obtainMessage(0, drawFaceData).sendToTarget();

        //draw face info
        if (customDataListener != null) {
            customDataListener.onDrawFaceDataComing(drawFaceData);
        }
    }

    /**
     * process bio info when label = 5
     *
     * @param jsonObject BioInfo json object
     */
    private void palmBioProcess(JsonObject jsonObject) {
        JsonArray palmArray = jsonObject.get("palm").getAsJsonArray();
        if (palmArray == null) {
            return;
        }
//        Log.i(TAG, "[getFaceSegmentData palm]: " + jsonObject);

        for (JsonElement element : palmArray) {
            PalmRecognizeData palmRecognizeData = JSONUtil.getPalmRecognizeData(element.toString());

            if (palmRecognizeData != null) {
                palmDataHandler.obtainMessage(0, palmRecognizeData).sendToTarget();
            }
        }
    }

    /**
     * {"data":{"cardNum":"8020049051244E"},"label":9}
     */
    private void cardProcess(JsonObject jsonObject) {
        Log.i(TAG, "cardProcess: " + jsonObject.toString());
        if (jsonObject.has("data")) {
            JsonObject data = jsonObject.getAsJsonObject("data");
            String cardNum = data.get("cardNum").getAsString();
            if (cardNum != null) {
                cardDataHandler.obtainMessage(0, cardNum).sendToTarget();
            }
        }
    }


    /**
     * process bio info when label = 99
     */
    private void detectionDistance(String str) {
        try {
            JSONObject jsonObject = new JSONObject(str);
            if (jsonObject.has("data")) {
                JSONObject distanceObj = jsonObject.getJSONObject("data");
                if (distanceObj.has("distance")) {
                    int palmDistance = distanceObj.getInt("distance");
                    if (customDataListener != null) {
                        customDataListener.onInfraredDistanceChanged(palmDistance);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "palmDistance: json error\n" + str);
        }
    }

    private void saveFaceImage(String data, String fileName, String dirName) {
        if (TextUtils.isEmpty(data)) {
            return;
        }
        byte[] decodeBytes = Base64.decode(data, Base64.NO_WRAP);
        FileUtils.saveBytes(decodeBytes, fileName + ".jpg", FileUtils.livePath + File.separator + dirName + File.separator);
    }

    private void savePalmImage(String data, String fileName, String dirName) {
        if (TextUtils.isEmpty(data)) {
            return;
        }
        byte[] decodeBytes = Base64.decode(data, Base64.NO_WRAP);
        FileUtils.saveBytes(decodeBytes, fileName + ".jpg", FileUtils.testPhotoPath + File.separator + dirName + File.separator);
    }

    private void savePalmNV21Image(byte[] data, String fileName, String dirName) {
        if (data == null) {
            return;
        }
        FileUtils.saveBytes(data, fileName + ".nv21", FileUtils.testPhotoPath + File.separator + dirName + File.separator);
    }
}
