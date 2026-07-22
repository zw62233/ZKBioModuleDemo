package com.armatura.biomodule.thread

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.manager.FaceManager
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.pojo.setting.DeviceSettings
import com.armatura.biomodule.pojo.setting.FaceSettings
import com.armatura.biomodule.pojo.setting.FuncSettings
import com.armatura.biomodule.pojo.setting.FuncSettings.SensorType
import com.armatura.biomodule.util.CsvHelper
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.TimeUtil
import com.armatura.biomodule.util.getDataJSONObject
import com.armatura.biomodule.util.getDetail
import com.armatura.biomodule.util.getStatus
import com.armatura.biomodule.viewmodel.AMTViewModel
import com.armatura.constant.ConfigType
import com.armatura.constant.ErrorCode
import com.armatura.constant.StatusCode
import com.armatura.translib.AMTHidManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Created by Magic on 2023/2/13
 * Description:
 */
object AMTWorkManager {
    private val ZAM218_PLATFORM = arrayOf("ar9311", "ZAM218")
    private val ZAM210_PLATFORM = arrayOf("ar9341", "ZAM210")
    private const val TAG = "AMTWorkManager"
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var executor: ScheduledExecutorService? = null
    private var latestModuleTemp = ""


    fun startWorkManager(
        lifecycle: LifecycleOwner,
        context: Context, cpuTempLiveData: MutableLiveData<String>,
        errorTipsLiveData: MutableLiveData<Bundle>,
        syncFlagConfigLiveData: MutableLiveData<Boolean>,
    ) {
        ioScope.launch {
            FileUtils.checkDir()
            CsvHelper.getInstance().initAll(context)

            try {
                val resultArray = ByteArray(1024)
                val size = IntArray(1) { 1024 }
                var isHidOk = false
                val startTime = SystemClock.elapsedRealtime()
                var sendHidCommandFailedCount = 0
                do {
                    size[0] = 1024
                    val ret =
                        AMTHidManager.instance()
                            .getConfig(ConfigType.DEVICE_TIME, resultArray, size)
                    if (ret == ErrorCode.ERROR_NONE) {
                        try {
                            val jsonObject = JSONObject(String(resultArray, 0, size[0]))
                            Log.i(TAG, "wait module->$jsonObject")
                            if (jsonObject.has("status")) {
                                val status = jsonObject.getInt("status")
                                isHidOk = status == 0
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        sendHidCommandFailedCount++
                        Log.i(
                            TAG,
                            "WaitModule ready....$ret,failed count =$sendHidCommandFailedCount"
                        )
                        if (sendHidCommandFailedCount > 6
                            || ret == ErrorCode.ERROR_NOT_OPEN
                            || ret == ErrorCode.ERROR_DEVICE_NOT_FOUND
                        ) {
                            break
                        }
                    }
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                } while (!isHidOk && SystemClock.elapsedRealtime() - startTime < 120 * 1000)

                if (!isHidOk) {
                    errorTipsLiveData.postValue(Bundle().apply {
                        putInt(
                            AMTViewModel.BUNDLE_KEY_ERROR_CODE,
                            AMTViewModel.ERROR_CODE_HID_NOT_READY
                        )
                    })
                    return@launch
                }

                //sync time
                for (i in 0..5) {
                    if (TimeUtil.INSTANCE.syncTime() == ErrorCode.ERROR_NONE) {
                        break
                    }
                }
                //check module version
                checkModuleFirmVersion(errorTipsLiveData)
                //sync config
                syncConfig()
                //start heart beat
                ModuleHeartbeatManager.getInstance().hearBeat()
                //start get cpu temp
                readModuleCPUTemp(cpuTempLiveData)
            } finally {
                Log.i(TAG, "startWorkManager: finish--------------")
                syncFlagConfigLiveData.postValue(true)
            }
        }
    }

    fun stopWorkManager() {
        executor?.shutdownNow()
    }


    private fun checkModuleFirmVersion(errorTipsLiveData: MutableLiveData<Bundle>) {
        val configData = ByteArray(30 * 1024)
        val size = IntArray(1) { configData.size }
        AMTHidManager.instance()
            .getConfig(ConfigType.DEVICE_INFORMATION, configData, size)
            .also { ret ->
                if (ret == 0) {
                    val deviceInfo = String(configData, 0, size[0])
                    JSONUtil.getDeviceInfo(deviceInfo)?.let {
                        if ("NONE" != it.faceVer && !it.faceVer.isNullOrEmpty()) {
                            Config.sfaceVer = it.faceVer
                        } else {
                            Config.sfaceVer = FaceManager.FACE_VERSION_60_1
                        }
                        Log.i(TAG, "checkFaceVersion: ${Config.sfaceVer}")

                        val platformVersion = findPlatformVersion(it.firmVer)
                        Log.i(TAG, "Check Module Version:$platformVersion")
                        if (is218Platform(it.gPlatform_VERSION)) {
                            Config.instance().is218Platform = true
                            Log.i(TAG, "218 Module detected")
                            if (java.lang.Long.parseLong(platformVersion) < AMTViewModel.PLATFORM_ZAM218_LATEST_VERSION) {
                                errorTipsLiveData.postValue(Bundle().apply {
                                    putInt(
                                        AMTViewModel.BUNDLE_KEY_ERROR_CODE,
                                        AMTViewModel.ERROR_CODE_VERSION_NOT_MATCH
                                    )
                                    putString(
                                        AMTViewModel.BUNDLE_KEY_VERSION,
                                        platformVersion
                                    )
                                })
                                return
                            }
                        } else if (is210Platform(it.gPlatform_VERSION)) {
                            Config.instance().is210Platform = true
                            Log.i(TAG, "210 Module detected")
                            if (java.lang.Long.parseLong(platformVersion) < AMTViewModel.PLATFORM_ZAM210_LATEST_VERSION) {
                                errorTipsLiveData.postValue(Bundle().apply {
                                    putInt(
                                        AMTViewModel.BUNDLE_KEY_ERROR_CODE,
                                        AMTViewModel.ERROR_CODE_VERSION_NOT_MATCH
                                    )
                                    putString(
                                        AMTViewModel.BUNDLE_KEY_VERSION,
                                        platformVersion
                                    )
                                })
                                return
                            }
                        } else {
                            errorTipsLiveData.postValue(Bundle().apply {
                                putInt(
                                    AMTViewModel.BUNDLE_KEY_ERROR_CODE,
                                    AMTViewModel.ERROR_CODE_VERSION_NOT_MATCH
                                )
                                putString(
                                    AMTViewModel.BUNDLE_KEY_VERSION,
                                    "Unknown Platform!"
                                )
                            })
                            return
                        }

                        val moduleSystemVersion = it.gPlatform_VERSION.split("-")[2]
                        Log.i(
                            TAG,
                            "checkModuleFirmVersion: Module System Version = $moduleSystemVersion"
                        )
                        Config.instance().is16Platform = if (moduleSystemVersion.contains("16")) {
                            true
                        } else {
                            false
                        }
                    }

                }
            }
    }

    private fun syncConfig() {
        val configData = ByteArray(30 * 1024)
        val size = IntArray(1) { configData.size }
        val ret = AMTHidManager.instance()
            .getConfig(ConfigType.COMMON_CONFIG, configData, size)
        if (ret == ErrorCode.ERROR_NONE) {
            val response = String(configData, 0, size[0])
            val responseJSONObject = JSONObject(response)
            val commonSettingJSONObject =
                responseJSONObject.getDataJSONObject("commonSettings")
            val status = responseJSONObject.getStatus()
            if (status == StatusCode.SUCCESS) {
                JSONUtil.getCommonSetting(commonSettingJSONObject.toString())
                    ?.let { commonSetting ->
                        Config.instance().faceIdentifyThreshold =
                            commonSetting.recogThreshold
                        Config.controlLEDByHost =
                            (commonSetting.ledControlMode == 1)
                        Config.videoStreamMode = commonSetting.videoStreamMode
                        Config.isFeaturePhotoFunOn = commonSetting.pushPhotoEnable
                        Config.pushDetectionDistance = commonSetting.pushDetectionDistance
                        Log.i(TAG, "syncConfig: sync common config success")
                    }
            } else {
                Log.w(
                    TAG,
                    "get common config failed,$status,trt get [DEVSettings] And [FaceSettings]"
                )
                size[0] = configData.size
                AMTHidManager.instance()
                    .getConfig(ConfigType.DEVICE_CONFIG, configData, size).let {
                        if (it == ErrorCode.ERROR_NONE) {
                            val response = String(configData, 0, size[0])
                            val responseJSONObject = JSONObject(response)
                            val deviceSettingJSONObject =
                                responseJSONObject.getDataJSONObject(DeviceSettings.KEY)
                            JSONUtil.getDeviceSetting(deviceSettingJSONObject.toString())
                                ?.let { deviceSettings ->
                                    Config.controlLEDByHost =
                                        (deviceSettings.ledControlMode == 1)
                                    Config.videoStreamMode = deviceSettings.videoStreamMode
                                    Config.isFeaturePhotoFunOn = deviceSettings.pushPhotoEnable
                                    Config.pushDetectionDistance = deviceSettings.pushDetectionDistance
                                    Log.i(TAG, "syncConfig: sync device config success")
                                }
                        } else {
                            Log.e(TAG, "syncConfig: get device config failed,ret = $it")
                        }
                    }
                size[0] = configData.size
                AMTHidManager.instance()
                    .getConfig(ConfigType.FACE_CONFIG, configData, size).let {
                        if (it == ErrorCode.ERROR_NONE) {
                            val response = String(configData, 0, size[0])
                            val responseJSONObject = JSONObject(response)
                            val faceSettingJSONObject =
                                responseJSONObject.getDataJSONObject(FaceSettings.KEY)
                            JSONUtil.getFaceSetting(faceSettingJSONObject.toString())
                                ?.let { faceSettings ->
                                    Config.instance().faceIdentifyThreshold =
                                        faceSettings.recogThreshold
                                    Config.instance().faceVerifyThreshold =
                                        faceSettings.verifyThreshold
                                    Log.i(TAG, "syncConfig: sync face config success")
                                }
                        } else {
                            Log.e(TAG, "syncConfig: get face config failed,ret = $it")
                        }
                    }
            }
        }

        size[0] = configData.size
        //sync func settings
        AMTHidManager.instance()
            .getConfig(ConfigType.FUNC_SETTINGS, configData, size).let {
                if (it != ErrorCode.ERROR_NONE) {
                    applyDefaultConfig()
                    Log.w(TAG, "Sync Func Settings failed,Hid ret = $it.Use default config")
                    return@let
                }
                val response = String(configData, 0, size[0])
                val responseJSONObject = JSONObject(response)
                val status = responseJSONObject.getStatus()
                if (status != StatusCode.SUCCESS) {
                    Log.w(TAG, "Sync Func Settings failed(${responseJSONObject.getDetail()}).")
                    applyDefaultConfig()
                    return@let
                }

                val funcSettingsJsonObj = responseJSONObject.getDataJSONObject("FuncSettings")
                val funcSettings =
                    gson.fromJson(
                        funcSettingsJsonObj.toString(),
                        FuncSettings::class.java
                    )
                Config.isSupportFace = funcSettings.isSupportFace
                Config.isSupportPalm = funcSettings.isSupportPalm
                Config.isSupportRFID = funcSettings.isSupportRFID
                Config.isSupportIndicator = funcSettings.isSupportLed
                Config.isSupportStoreInModule =
                    funcSettings.isSupportStoreInModule
                if (Config.instance().is218Platform) {
                    Config.instance().recognizeMode = Config.HOST_MODE
                }
                Config.instance().sensorType = funcSettings.sensorType
                Log.i(
                    TAG, "sync func success,\n" +
                            "isSupportFace = ${Config.isSupportFace}\n" +
                            "isSupportPalm = ${Config.isSupportPalm}\n" +
                            "isSupportRFID = ${Config.isSupportRFID}\n" +
                            "isSupportIndicator = ${Config.isSupportIndicator}\n" +
                            "isSupportStoreInModule = ${Config.isSupportStoreInModule}"
                )
            }
        size[0] = configData.size
        //sync face quality threshold
        AMTHidManager.instance()
            .getConfig(ConfigType.CAPTURE_FILTER_CONFIG, configData, size).let {
                if (it != ErrorCode.ERROR_NONE) {
                    Log.w(TAG, "syncConfig: get capture filter config failed! Hid ret =$it")
                    return@let
                }
                val response = String(configData, 0, size[0])
                val responseJSONObject = JSONObject(response)
                val status = responseJSONObject.getStatus()
                if (status != StatusCode.SUCCESS) {
                    Log.w(TAG, "get capture config failed!(${responseJSONObject.getDetail()})")
                    return@let
                }
                val captureFilterConfigJSONObject =
                    responseJSONObject.getDataJSONObject("captureFilter")
                JSONUtil.getCaptureFilterConfig(captureFilterConfigJSONObject.toString())
                    ?.let { captureFilterConfig ->
                        with(Config.instance()) {
                            faceRegistrationQuality =
                                captureFilterConfig.scoreThreshold.toFloat()
                            faceHeightMinSize = captureFilterConfig.heightMinValue
                            faceWidthMinSize = captureFilterConfig.widthMinValue
                            facePitchMaxThreshold =
                                captureFilterConfig.pitchMaxValue.toFloat()
                            facePitchMinThreshold =
                                captureFilterConfig.pitchMinValue.toFloat()
                            faceYawMaxThreshold =
                                captureFilterConfig.yawMaxValue.toFloat()
                            faceYawMinThreshold =
                                captureFilterConfig.yawMinValue.toFloat()
                            faceRollMaxThreshold =
                                captureFilterConfig.rollMaxValue.toFloat()
                            faceRollMinThreshold =
                                captureFilterConfig.rollMinValue.toFloat()
                            faceBlurThreshold =
                                captureFilterConfig.blurThreshold.toFloat()
                        }
                        Log.i(TAG, "sync capture filter success")
                    }
            }
        size[0] = configData.size
        //sync palm identify threshold
        AMTHidManager.instance()
            .getConfig(ConfigType.PALM_CONFIG, configData, size).let {
                if (it != ErrorCode.ERROR_NONE) {
                    Log.w(TAG, "syncConfig: get palm config failed! Hid ret =$it")
                    return@let
                }
                val response = String(configData, 0, size[0])
                val responseJSONObject = JSONObject(response)
                val status = responseJSONObject.getStatus()
                if (status != StatusCode.SUCCESS) {
                    Log.w(TAG, "get palm failed!(${responseJSONObject.getDetail()})")
                    return@let
                }

                val palmSettingJSONObject = responseJSONObject.getDataJSONObject("PALMSetting")


                JSONUtil.getVLPalmSettings(palmSettingJSONObject.toString())?.let { palmSetting ->
                    Config.instance().palmVLIdentifyThreshold =
                        palmSetting.palmIdentifyThreshold * 1.0F
                    Config.instance().palmVLLivenessThreshold =
                        palmSetting.palmLivenessThreshold * 1.0F
                    Config.instance().bPalmLivenessEnable =
                        palmSetting.palmLiveness
                    Config.instance().palmImageQualityThreshold =
                        palmSetting.imageQualityThreshold
                    Config.palmTemplateMode = palmSetting.palmTemplateMode
                    Log.i(TAG, "PalmLiveThr=${Config.instance().palmVLLivenessThreshold}")
                    Log.i(TAG, "PalmIdentifyThr=${Config.instance().palmVLIdentifyThreshold}")
                    Log.i(TAG, "palmTemplateMode=${Config.palmTemplateMode}")
                    Log.i(TAG, "sync palm config success")
                }
            }
    }

    private fun applyDefaultConfig() {
        if (Config.instance().is210Platform) {
            Log.i(TAG, "syncConfig: apply 210 platform default config")
            Config.isSupportFace = true
            Config.isSupportPalm = true
            Config.isSupportRFID = false
            Config.isSupportIndicator = false
            Config.isSupportStoreInModule = true
            Config.instance().sensorType = SensorType.SENSOR_TYPE_RGB_AND_NIR
        } else if (Config.instance().is218Platform) {
            Log.i(TAG, "syncConfig: apply 218 platform default config")
            Config.isSupportFace = false
            Config.isSupportPalm = true
            Config.isSupportRFID = false
            Config.isSupportIndicator = true
            Config.isSupportStoreInModule = false
            Config.instance().recognizeMode = Config.HOST_MODE
            Config.instance().sensorType = SensorType.SENSOR_TYPE_RGB_AND_NIR
        } else {
            Log.i(TAG, "syncConfig: unknown platform,apply default config")
            //old old old version
            Config.isSupportFace = true
            Config.isSupportPalm = true
            Config.isSupportRFID = false
            Config.isSupportIndicator = false
            Config.isSupportStoreInModule = true
            Config.instance().recognizeMode = Config.HOST_MODE
            Config.instance().sensorType = SensorType.SENSOR_TYPE_RGB_AND_NIR
        }
    }


    private fun findPlatformVersion(platformVersion: String): String {
        val matcher = Pattern.compile("\\d{8}").matcher(platformVersion)
        while (matcher.find()) {
            return matcher.group()
        }
        return "not found"
    }

    private fun is218Platform(platform: String): Boolean {
        for (platform50 in ZAM218_PLATFORM) {
            if (platform.contains(platform50)) {
                return true
            }
        }
        return false
    }

    private fun is210Platform(platform: String): Boolean {
        for (platform30 in ZAM210_PLATFORM) {
            if (platform.contains(platform30)) {
                return true
            }
        }
        return false
    }

    private fun readModuleCPUTemp(cpuTempLiveData: MutableLiveData<String>) {
        executor = Executors.newScheduledThreadPool(1)
        executor?.scheduleWithFixedDelay({
            val data = ByteArray(128)
            val size = intArrayOf(data.size)
            var msg: String = ""
            AMTHidManager.instance().getConfig(ConfigType.CPU_TEMP, data, size).let {
                if (it != ErrorCode.ERROR_NONE) {
                    Log.w(TAG, "readModuleCPUTemp failed! Hid ret =$it")
                    msg = "$it"
                    return@let
                }
                val response = String(data, 0, size[0])
                val responseJSONObject = JSONObject(response)
                val status = responseJSONObject.getStatus()
                if (status != StatusCode.SUCCESS) {
                    msg = "$status"
                    Log.w(TAG, "get cpu temp failed!(${responseJSONObject.getDetail()})")
                    return@let
                }
                val dataJSONObject = responseJSONObject.getDataJSONObject()
                if (dataJSONObject.has("temp")) {
                    //Get CPU Temp
                    val temp = dataJSONObject.getString("temp")
                    val tempNum = temp.toLong()
                    msg = String.format(
                        Locale.US,
                        "%.1f",
                        tempNum * 1.0F / 10000F
                    )
                    latestModuleTemp = msg
                    Log.i(TAG, "readModuleCPUTemp: $msg")
                }
            }
            cpuTempLiveData.postValue(latestModuleTemp)
        }, 20L, 20L, TimeUnit.SECONDS)
    }
}