package com.armatura.biomodule.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.armatura.LoggerHelper;
import com.armatura.biomodule.config.Config;

public class ConfigUtil {
    private static final String PREFS_NAME = "ConfigPrefsFile";

    public static void initConfig(Context context, Config config) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        config.isNeedShowFPS = settings.getBoolean("isNeedShowFPS", config.isNeedShowFPS);
        config.isShowDetailFaceInfo = settings.getBoolean("isShowDetailFaceInfo", config.isShowDetailFaceInfo);
        config.isShowFaceInfo = settings.getBoolean("show_faceinfo", config.isShowFaceInfo);
        config.cameraResolution = settings.getInt("camera_resolution", config.cameraResolution);
        config.cameraFps = settings.getInt("camera_fps", config.cameraFps);
        config.isDisplayLivenessInfo = settings.getBoolean("is_uvc_liveness", config.isDisplayLivenessInfo);
        config.isNeedFeatureWhenCacheReg = settings.getBoolean("isNeedFeatureWhenCacheReg", config.isNeedFeatureWhenCacheReg);
        config.isNeedPictureWhenCacheReg = settings.getBoolean("isNeedPictureWhenCacheReg", config.isNeedPictureWhenCacheReg);
        config.isNeedInfoWhenCacheReg = settings.getBoolean("isNeedInfoWhenCacheReg", config.isNeedInfoWhenCacheReg);
        config.isTestMode = settings.getBoolean("testMode", config.isTestMode);
        config.isShowFacePose = settings.getBoolean("isShowFacePose", config.isShowFacePose);
        config.isShowAgeAttribute = settings.getBoolean("isShowAgeAttribute", config.isShowAgeAttribute);
        config.isShowGenderAttribute = settings.getBoolean("isShowGenderAttribute", config.isShowGenderAttribute);
        config.isShowExpressionAttribute = settings.getBoolean("isShowExpressionAttribute", config.isShowExpressionAttribute);
        config.powerSaveMode = settings.getBoolean("powerSaveMode", config.powerSaveMode);
        config.isShowMustacheAttribute = settings.getBoolean("isShowMustacheAttribute", config.isShowMustacheAttribute);
        config.isShowGlassesAttribute = settings.getBoolean("isShowGlassesAttribute", config.isShowGlassesAttribute);
        config.isShowHatAttribute = settings.getBoolean("isShowHatAttribute", config.isShowHatAttribute);
        config.isShowMaskAttribute = settings.getBoolean("isShowMaskAttribute", config.isShowMaskAttribute);
        config.recognizeMode = settings.getInt("recognizeMode", config.recognizeMode);
        config.sdkLogLevel = settings.getInt("sdkLogLevel", config.sdkLogLevel);
        LoggerHelper.setLogLevel(config.sdkLogLevel);
        config.maxIdentifyFailedCount = settings.getInt("maxIdentifyFailedCount", config.maxIdentifyFailedCount);
        config.drawPalmRectWhenIdentify = settings.getBoolean("drawPalmRectWhenIdentify", config.drawPalmRectWhenIdentify);
        config.identifyInfoStayTime = settings.getInt("identifyInfoStayTime", config.identifyInfoStayTime);
        config.greedLedLightingDuration = settings.getInt("greedLedLightingDuration", config.greedLedLightingDuration);
        config.redLedLightingDuration = settings.getInt("redLedLightingDuration", config.redLedLightingDuration);
        config.isDisplayCPUTempInfo = settings.getBoolean("isDisplayCPUTempInfo", config.isDisplayCPUTempInfo);
    }

    public static void saveConfig(Context context, Config config) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("isNeedShowFPS", config.isNeedShowFPS);
        editor.putBoolean("isShowDetailFaceInfo", config.isShowDetailFaceInfo);
        editor.putBoolean("show_faceinfo", config.isShowFaceInfo);
        editor.putInt("camera_resolution", config.cameraResolution);
        editor.putInt("camera_fps", config.cameraFps);
        editor.putBoolean("is_uvc_liveness", config.isDisplayLivenessInfo);
        editor.putBoolean("isNeedFeatureWhenCacheReg", config.isNeedFeatureWhenCacheReg);
        editor.putBoolean("isNeedPictureWhenCacheReg", config.isNeedPictureWhenCacheReg);
        editor.putBoolean("isNeedInfoWhenCacheReg", config.isNeedInfoWhenCacheReg);
        editor.putBoolean("isShowFacePose", config.isShowFacePose);
        editor.putBoolean("isShowAgeAttribute", config.isShowAgeAttribute);
        editor.putBoolean("isShowGenderAttribute", config.isShowGenderAttribute);
        editor.putBoolean("isShowExpressionAttribute", config.isShowExpressionAttribute);
        editor.putBoolean("isShowMustacheAttribute", config.isShowMustacheAttribute);
        editor.putBoolean("isShowGlassesAttribute", config.isShowGlassesAttribute);
        editor.putBoolean("isShowHatAttribute", config.isShowHatAttribute);
        editor.putBoolean("isShowMaskAttribute", config.isShowMaskAttribute);
        editor.putBoolean("drawPalmRectWhenIdentify", config.drawPalmRectWhenIdentify);
        editor.putBoolean("powerSaveMode", config.powerSaveMode);
        editor.putBoolean("isDisplayCPUTempInfo", config.isDisplayCPUTempInfo);
        editor.putInt("recognizeMode", config.recognizeMode);
        editor.putInt("maxIdentifyFailedCount", config.maxIdentifyFailedCount);
        editor.putInt("identifyInfoStayTime", config.identifyInfoStayTime);
        editor.putInt("redLedLightingDuration", config.redLedLightingDuration);
        editor.putInt("greedLedLightingDuration", config.greedLedLightingDuration);
        editor.putInt("sdkLogLevel", config.sdkLogLevel);
        editor.putBoolean("testMode", config.isTestMode);
        editor.apply();
    }

}
