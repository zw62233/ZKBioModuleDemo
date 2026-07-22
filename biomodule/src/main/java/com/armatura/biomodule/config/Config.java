package com.armatura.biomodule.config;

import android.content.Context;

import androidx.annotation.NonNull;

import com.armatura.LoggerHelper;
import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.manager.FaceManager;
import com.armatura.biomodule.pojo.setting.DeviceSettings;
import com.armatura.biomodule.pojo.setting.FuncSettings;
import com.armatura.biomodule.util.ConfigUtil;

import java.io.File;
import java.io.Serializable;

/**
 * System Param Config
 */
public class Config implements Serializable {
    public static final String RootPath = ExApplication.instance().getCacheDir().getAbsolutePath()
            + "/face_data/";
    public static final String CapturePath = RootPath + "face_capture/";
    private static final String ModuleAvatarPath = RootPath + "face_pic/";
    private static final String DatabasePath = RootPath + "face_db/";
    private static final String HostAvatarPath = RootPath + "face_pic2/";
    private static final String DatabasePath2 = RootPath + "face_db2/";
    private static Config config = null;

    /**
     * GPlatform contain "0.16.x"
     */
    public boolean is16Platform = false;
    public boolean is210Platform = false;
    public boolean is218Platform = false;

    public int sensorType = FuncSettings.SensorType.SENSOR_TYPE_RGB_AND_NIR;

    public static String sfaceVer = FaceManager.FACE_VERSION_8_1;

    public float faceIdentifyThreshold = 0.90f;//sync from module after connect module
    public float faceVerifyThreshold = 0.90f;//sync from module after connect module
    public int faceWidthMinSize = 100;//sync from module after connect module
    public int faceHeightMinSize = 100;//sync from module after connect module

    public float faceYawMinThreshold = 20F;//sync from module after connect module
    public float faceYawMaxThreshold = 20F;//sync from module after connect module

    public float facePitchMinThreshold = 20F;//sync from module after connect module
    public float facePitchMaxThreshold = 20F;//sync from module after connect module

    public float faceRollMinThreshold = 20F;//sync from module after connect module
    public float faceRollMaxThreshold = 20F;//sync from module after connect module

    public float palmVLIdentifyThreshold = 67F;//sync from module after connect module
    public boolean bPalmLivenessEnable = false;//sync from module after connect module

    public float palmVLLivenessThreshold = 70F;//sync from module after connect module
    public float faceRegistrationQuality = 40f;//sync from module after connect module

    public float faceBlurThreshold = 30f;//sync from module after connect module

    public int maxIdentifyFailedCount = 3;

    public boolean drawPalmRectWhenIdentify = false;

    public int palmImageQualityThreshold = 60;//sync from module after connect module

    public int identifyInfoStayTime = 1000;

    public int greedLedLightingDuration = 3000;
    public int redLedLightingDuration = 3000;
    public boolean powerSaveMode = false;


    public boolean isNeedShowFPS = false;//show fps
    public boolean isShowDetailFaceInfo = false;//show more detail info on face rect
    public boolean isShowFaceInfo = true;//show face info on face rect
    public boolean isShowFacePose = true;//show face pose info on right info board
    public boolean isShowAgeAttribute = true;//show age info on right info board
    public boolean isShowGenderAttribute = true;//show gender info on right info board
    public boolean isShowExpressionAttribute = true;//show expression info on right info board
    public boolean isShowMustacheAttribute = true;//show mustache info on right info board
    public boolean isShowGlassesAttribute = true;//show glasses info on right info board
    public boolean isShowHatAttribute = true;//show hat info on right info board
    public boolean isShowMaskAttribute = true;//show mask info on right info board
    public boolean isRestartApp = false;//if need restart app
    public boolean isRestartUVC = false;//if need reboot uvc device
    public int cameraResolution = 1; //0:480*640 1:720*1280
    public int cameraFps = 0; //0:25 1:30//TODO remove this
    public boolean isDisplayLivenessInfo = false;//display liveness result
    public boolean isDisplayCPUTempInfo = false;

    public boolean isNeedFeatureWhenCacheReg = false;//only exist in UserManage which belongs to BioModule
    public boolean isNeedPictureWhenCacheReg = false;//only exist in UserManage which belongs to BioModule
    public boolean isNeedInfoWhenCacheReg = false;//only exist in UserManage which belongs to BioModule

    public boolean isTestMode = false;//if enable ,will save csv file in /sdcard/Result/record

    public final static int HOST_MODE = 0x3333;
    public final static int MULTI_BIO_MODULE_INTERNAL_MODE = 0x4444;
    public int recognizeMode = HOST_MODE;

    public int sdkLogLevel = LoggerHelper.VERBOSE;

    public String appVersionName = "";


    /**
     * control indicator led by host
     */
    public static boolean controlLEDByHost = false;

    public static boolean isSupportPalm = false;

    public static boolean isSupportFace = false;

    public static boolean isSupportRFID = false;

    public static boolean isSupportIndicator = false;

    public static int palmMoveDistanceThreshold = 50;

    public static int videoStreamMode = 1;

    public static boolean isSupportStoreInModule = false;

    public static int palmTemplateMode = 0;

    public static boolean isFeaturePhotoFunOn = false;

    public static boolean pushDetectionDistance = false;

    private Config() {
    }

    public static Config instance() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    public static String getDatabasePath() {
        return DatabasePath2;
    }

    public static String getHostAvatarPath() {
        return HostAvatarPath;
    }

    public static String getModuleAvatarPath() {
        return ModuleAvatarPath;
    }

    public void initConfig(Context context) {
        ConfigUtil.initConfig(context, config);
    }

    public void save(Context context) {
        ConfigUtil.saveConfig(context, config);
    }

    public void updateFolder() {
        File file = new File(Config.RootPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File file2 = new File(Config.ModuleAvatarPath);
        if (!file2.exists()) {
            file2.mkdirs();
        }
        File file3 = new File(Config.DatabasePath);
        if (!file3.exists()) {
            file3.mkdirs();
        }
        File file4 = new File(Config.CapturePath);
        if (!file4.exists()) {
            file4.mkdirs();
        }
        File file5 = new File(Config.HostAvatarPath);
        if (!file5.exists()) {
            file5.mkdirs();
        }
        File file6 = new File(Config.DatabasePath2);
        if (!file6.exists()) {
            file6.mkdirs();
        }
        File file7 = new File(Config.HostAvatarPath);
        if (!file7.exists()) {
            file7.mkdirs();
        }
    }


    public static boolean shouldUseCircleIndicatorView() {
        return Config.videoStreamMode == DeviceSettings.VIDEO_STREAM_MODE_DISABLE;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                " \nfaceIdentifyThreshold=" + faceIdentifyThreshold +
                ", \nfaceWidthMinSize=" + faceWidthMinSize +
                ", \nfaceHeightMinSize=" + faceHeightMinSize +
                ", \nfaceYawMinThreshold=" + faceYawMinThreshold +
                ", \nfaceYawMaxThreshold=" + faceYawMaxThreshold +
                ", \nfacePitchMinThreshold=" + facePitchMinThreshold +
                ", \nfacePitchMaxThreshold=" + facePitchMaxThreshold +
                ", \nfaceRollMinThreshold=" + faceRollMinThreshold +
                ", \nfaceRollMaxThreshold=" + faceRollMaxThreshold +
                ", \npalmVLIdentifyThreshold=" + palmVLIdentifyThreshold +
                ", \nfaceRegistrationQuality=" + faceRegistrationQuality +
                ", \nisSupportFace=" + isSupportFace +
                ", \nisSupportPalm=" + isSupportPalm +
                ", \nisSupportIndicator=" + isSupportIndicator +
                "\n}" +
                "LED Control By Host = " + controlLEDByHost;
    }
}
