package com.armatura.biomodule.util;

import android.util.Log;

import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.pojo.common.BioType;
import com.armatura.biomodule.pojo.common.CommonConfigData;
import com.armatura.biomodule.pojo.face.register.DetectFaceResponse;
import com.armatura.biomodule.pojo.info.DeviceInfo;
import com.armatura.biomodule.pojo.info.SnapshotData;
import com.armatura.biomodule.pojo.module.PersonStatistics;
import com.armatura.biomodule.pojo.module.register.Features;
import com.armatura.biomodule.pojo.module.register.PersonId;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.pojo.palm.register.DetectPalmResponse;
import com.armatura.biomodule.pojo.palm.register.MergePalmResponse;
import com.armatura.biomodule.pojo.setting.CaptureFilterConfig;
import com.armatura.biomodule.pojo.setting.CommonSettingData;
import com.armatura.biomodule.pojo.setting.DeviceSettings;
import com.armatura.biomodule.pojo.setting.FaceSettings;
import com.armatura.biomodule.pojo.setting.MotionDetectSetting;
import com.armatura.biomodule.pojo.setting.PalmSetting;
import com.armatura.biomodule.pojo.setting.VLPalmSetting;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JSONUtil {
    public final static int ERROR_COMMON_DATA_EXCEPTION = -1;
    public final static int ERROR_PERSON_ID_EXCEPTION = -2;
    private static final String TAG = "JSONUtil";

    private static final Gson gson = new Gson();

    public static String getJsonString(Object object) {
        return gson.toJson(object);
    }

    public static <T> T getObject(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static CommonSettingData getCommonSetting(String jsonStr) {
        CommonSettingData commonSettingData = null;
        try {
            commonSettingData = gson.fromJson(jsonStr, CommonSettingData.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "[getCommonSetting]: " + jsonStr, e);
        }
        return commonSettingData;
    }

    public static FaceSettings getFaceSetting(String jsonStr) {
        FaceSettings faceSettings = null;
        try {
            faceSettings = gson.fromJson(jsonStr, FaceSettings.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "[getFaceSetting]: " + jsonStr, e);
        }
        return faceSettings;
    }


    public static DeviceSettings getDeviceSetting(String jsonStr) {
        DeviceSettings deviceSettings = null;
        try {
            deviceSettings = gson.fromJson(jsonStr, DeviceSettings.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "[getFaceSetting]: " + jsonStr, e);
        }
        return deviceSettings;
    }

    public static SnapshotData getSnapShotData(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        SnapshotData snapshotData = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                JSONObject jsonObject = new JSONObject(gson.toJson(commonConfigData.data));
                snapshotData = gson.fromJson(jsonObject.getString("snapshot"), SnapshotData.class);
            } catch (JsonSyntaxException | JSONException e) {
                e.printStackTrace();
            }

        }
        return snapshotData;
    }

    public static CommonConfigData getCommonConfigData(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return commonConfigData;
    }


    public static DetectFaceResponse getDetectFaceResponse(CommonConfigData commonConfigData) {
        DetectFaceResponse detectFaceResponse = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                detectFaceResponse = gson.fromJson(gson.toJson(commonConfigData.data), DetectFaceResponse.class);
                Log.w(TAG, "[getFaceRegisterResult]: " + detectFaceResponse);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return detectFaceResponse;
    }

    public static DetectFaceResponse getDetectFaceResponse(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        DetectFaceResponse detectFaceResponse = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                detectFaceResponse = gson.fromJson(gson.toJson(commonConfigData.data), DetectFaceResponse.class);
                Log.w(TAG, "[getFaceRegisterResult]: " + detectFaceResponse);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return detectFaceResponse;
    }

    public static DetectFaceResponse getFacesFromResponse(String facesJsonStr) {

        DetectFaceResponse detectFaceResponse = null;
        try {
            detectFaceResponse = gson.fromJson(facesJsonStr, DetectFaceResponse.class);
            Log.w(TAG, "[getFacesFromResponse]: " + detectFaceResponse);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return detectFaceResponse;
    }


    public static int getStatus(String json) {
        return KotlinExtentKt.getStatus(json);
    }

    public static DetectPalmResponse getDetectPalmResponse(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        DetectPalmResponse detectPalmResponse = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                detectPalmResponse = gson.fromJson(gson.toJson(commonConfigData.data), DetectPalmResponse.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return detectPalmResponse;
    }

    public static MergePalmResponse getMergePalmResponse(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        MergePalmResponse mergePalmResponse = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                mergePalmResponse = gson.fromJson(gson.toJson(commonConfigData.data), MergePalmResponse.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return mergePalmResponse;
    }

    public static PalmRecognizeData getPalmRecognizeData(String jsonStr) {

        PalmRecognizeData palmRecognizeData = null;
        try {
            palmRecognizeData = gson.fromJson(jsonStr, PalmRecognizeData.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "[getPalmRegisterData]: " + palmRecognizeData);
            e.printStackTrace();
        }
        return palmRecognizeData;

    }

    public static CaptureFilterConfig getCaptureFilterConfig(String jsonStr) {
        CaptureFilterConfig captureFilterConfig = null;
        try {
            captureFilterConfig = gson.fromJson(jsonStr, CaptureFilterConfig.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "getCaptureFilterConfig: failed,str=" + jsonStr, e);
        }
        return captureFilterConfig;
    }

    public static MotionDetectSetting getMotionDetectSetting(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        MotionDetectSetting motionDetectSetting = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                JSONObject jsonObject = new JSONObject(gson.toJson(commonConfigData.data));
                motionDetectSetting = gson.fromJson(jsonObject.getString("MotionDetectionSetting")
                        , MotionDetectSetting.class);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return motionDetectSetting;
    }

    public static PalmSetting getPalmSetting(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        PalmSetting palmSetting = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                JSONObject jsonObject = new JSONObject(gson.toJson(commonConfigData.data));
                palmSetting = gson.fromJson(jsonObject.getString("PALMSetting"), PalmSetting.class);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return palmSetting;
    }

    public static VLPalmSetting getVLPalmSettings(String jsonStr) {
        VLPalmSetting palmSetting = null;
        try {
            palmSetting = gson.fromJson(jsonStr, VLPalmSetting.class);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "getVLPalmSettings: ", e);
        }
        return palmSetting;
    }


    public static DeviceInfo getDeviceInfo(String jsonStr) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(jsonStr, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        DeviceInfo deviceInfo = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                JSONObject jsonObject = new JSONObject(gson.toJson(commonConfigData.data));
                deviceInfo = gson.fromJson(jsonObject.getString("deviceInfo"), DeviceInfo.class);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return deviceInfo;
    }

    public static PersonStatistics getPersonStatistics(String json) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(json, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        PersonStatistics personStatistics = null;
        if (commonConfigData != null && commonConfigData.status == 0) {
            try {
                personStatistics = gson.fromJson(gson.toJson(commonConfigData.data), PersonStatistics.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
                Log.e(TAG, "getPersonStatistics: failed" + json);
            }
        }

        return personStatistics;
    }


    public static int getAddPersonResult(String json, PersonId personId) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(json, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        if (commonConfigData == null) {
            return ERROR_COMMON_DATA_EXCEPTION;
        }

        if (commonConfigData.status == 0) {
            try {
                JSONObject jsonObject = new JSONObject(gson.toJson(commonConfigData.data));
                personId.setPersonId(jsonObject.getString("personId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return commonConfigData.status;
    }

    public static List<UserInfo> getRemoteUserInfos(String json) {

        List<UserInfo> userInfos = new ArrayList<>();

        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(json, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }


        if (commonConfigData != null && commonConfigData.status == 0 && commonConfigData.data != null) {
            JsonElement jsonElement = JsonParser.parseString(gson.toJson(commonConfigData.data));
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                UserInfo userInfo = gson.fromJson(element, UserInfo.class);
                if (userInfo.faceFeature != null) {
                    userInfo.face++;
                }
                if (userInfo.palmFeature1 != null) {
                    userInfo.palm++;
                }
                if (userInfo.palmFeature2 != null) {
                    userInfo.palm++;
                }
                userInfos.add(userInfo);
            }

        }

        return userInfos;
    }

    public static CommonConfigData getDataResult(String json) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(json, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return commonConfigData;
    }

    public static UserInfo getRemoteUserInfo(String json) {
        CommonConfigData commonConfigData = null;
        try {
            commonConfigData = gson.fromJson(json, CommonConfigData.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        if (commonConfigData != null && commonConfigData.status == 0 && commonConfigData.data != null) {
            JsonElement jsonElement = JsonParser.parseString(gson.toJson(commonConfigData.data));
            UserInfo userInfo = gson.fromJson(jsonElement, UserInfo.class);

            List<Features> features = userInfo.getFeatures();
            if (features != null && !features.isEmpty()) {
                for (Features feature : features) {
                    if (feature != null) {
                        String bioType = feature.getBioType();
                        switch (bioType) {
                            case BioType.FACE:
                                userInfo.face++;
                                break;
                            case "palm":
                            case BioType.PALM_VEIN:
                            case BioType.PALM:
                                userInfo.palm++;
                                break;
                        }
                    }
                }
            }
            return userInfo;
        }
        return null;
    }
}
