package com.armatura.biomodule.util

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.armatura.biomodule.activity.base.ExApplication
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.pojo.module.register.AddPersonRequest
import com.armatura.biomodule.pojo.module.register.Features
import com.armatura.biomodule.pojo.setting.CommonSettingData
import com.armatura.biomodule.pojo.setting.DeviceSettings
import com.armatura.constant.ConfigType
import com.armatura.constant.ErrorCode
import com.armatura.constant.ParamIndex
import com.armatura.constant.StatusCode
import com.armatura.translib.AMTHidManager
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by Magic on 2020/9/12
 *
 *
 * these method only use to fill user info when you want to use match inside function
 */
object HidHelper {
    private val TAG: String = HidHelper::class.java.simpleName

    /**
     * if you upload user with face template or palm template to module,you can use this method
     *
     * @param userInfo the userInfo you want to append extra info
     */
    @JvmStatic
    fun appendUploadTemplateInfo(userInfo: UserInfo) {
        userInfo.updateTime = SimpleDateFormat(
            "yyyyMMddHHmmss",
            Locale.US
        ).format(Date())
        val featuresList: MutableList<Features> = ArrayList()
        //add face feature
        if (userInfo.faceFeature != null) {
            val features = Features()
            features.data = String.format(
                "data:face/txt;base64,%s",
                Base64.encodeToString(
                    userInfo.faceFeature,
                    Base64.NO_WRAP
                )
            )
            featuresList.add(features)
        }
        //add palm feature1
        if (userInfo.palmFeature1 != null) {
            val features = Features()
            features.data = String.format(
                "data:palm/txt;base64,%s",
                Base64.encodeToString(
                    userInfo.palmFeature1,
                    Base64.NO_WRAP
                )
            )
            featuresList.add(features)
        }
        //add palm feature2
        if (userInfo.palmFeature2 != null) {
            val features = Features()
            features.data = String.format(
                "data:palm/txt;base64,%s",
                Base64.encodeToString(
                    userInfo.palmFeature2,
                    Base64.NO_WRAP
                )
            )
            featuresList.add(features)
        }

        userInfo.personId = userInfo.userId

        userInfo.features = featuresList
    }


    /**
     * if you upload user with face template or palm template to module,you can use this method
     *
     * @param userInfo    the userInfo you want to append extra info
     * @param faceBitmaps the picture of face
     */
    @JvmStatic
    fun createAddPersonRequest(userInfo: UserInfo, faceBitmaps: Array<Bitmap>?): AddPersonRequest {
        val addPersonRequest = AddPersonRequest()
        addPersonRequest.personId = userInfo.userId /*userId or personId ,pick one of two*/
        addPersonRequest.groupId = null
        addPersonRequest.name = userInfo.name /*required*/
        addPersonRequest.age = userInfo.age //if you need ,fill it
        addPersonRequest.gender = userInfo.gender //if you need ,fill it
        addPersonRequest.phone = null //if you need ,fill it
        addPersonRequest.email = null //if you need ,fill it
        addPersonRequest.certificateType = 0
        addPersonRequest.certificateNumber = null
        addPersonRequest.updateTime = SimpleDateFormat(
            "yyyyMMddHHmmss",
            Locale.US
        ).format(Date())

        //add images
        if (faceBitmaps != null && faceBitmaps.size > 0) {
            val images: MutableList<Image> = ArrayList()
            for (faceBitmap in faceBitmaps) {
                val temp = Image()
                temp.data = BitmapUtil.bitmapToBase64(faceBitmap)
                temp.format = Image.Format.JPEG
                temp.bioType = BioType.FACE
                temp.width = faceBitmap.width
                temp.height = faceBitmap.height
                images.add(temp)
            }
            addPersonRequest.images = images
        }


        val featuresList: MutableList<Features> = ArrayList()
        //add face feature
        if (userInfo.faceFeature != null) {
            val features = Features()
            features.setBioType(BioType.FACE)
            features.data =
                Base64.encodeToString(
                    userInfo.faceFeature,
                    Base64.NO_WRAP
                )
            features.setSize(userInfo.faceFeature.size)
            featuresList.add(features)
        }
        //add palm feature1
        if (userInfo.palmFeature1 != null) {
            val features = Features()
            features.setBioType("palm")
            features.data =
                Base64.encodeToString(
                    userInfo.palmFeature1,
                    Base64.NO_WRAP
                )
            features.setSize(userInfo.palmFeature1.size)
            featuresList.add(features)
        }
        //add palm feature2
        if (userInfo.palmFeature2 != null) {
            val features = Features()
            features.setBioType(BioType.PALM_VEIN)
            features.data =
                Base64.encodeToString(
                    userInfo.palmFeature2,
                    Base64.NO_WRAP
                )
            features.setSize(userInfo.palmFeature2.size)
            featuresList.add(features)
        }
        addPersonRequest.features = featuresList

        //ignore access info
        return addPersonRequest
    }


    @JvmStatic
    @Synchronized
    fun getAndSaveFile(filePath: String?): ByteArray? {
        if(filePath == null){
            Log.i(TAG, "getAndSaveFile: path is null")
            return null
        }
        val buffer = ByteArray(1024 * 1024 * 2)
        val size = IntArray(1)
        val ret = AMTHidManager.instance().getFile(
            filePath,
            buffer, size, 10_000
        )
        Log.i(TAG, "getAndSaveFile: get $filePath ret=$ret")
        if (ret == ErrorCode.ERROR_NONE) {
            return buffer.copyOfRange(0, size[0])
        }
        return null
    }

    /**
     * if you upload user with face photo,you can use this method
     *
     * @param userInfo the userInfo you want to append extra info
     */
    @JvmStatic
    fun appendUploadImageInfo(userInfo: UserInfo, bitmap: Bitmap?) {
        userInfo.personId = userInfo.userId
        userInfo.updateTime = SimpleDateFormat(
            "yyyyMMddHHmmss",
            Locale.getDefault()
        ).format(Date())
        val imageList = ArrayList<Image>()
        val image = Image()
        image.data = "data:face/jpeg;base64," + BitmapUtil.bitmapToBase64(bitmap)
        imageList.add(image)
        userInfo.images = imageList
    }

    @JvmStatic
    fun controlIndicatorGreenLED() {
        controlIndicatorGreenLED(Config.instance().greedLedLightingDuration, 0, 1)
    }

    @JvmStatic
    fun controlIndicatorGreenLED(
        onTime: Int = Config.instance().greedLedLightingDuration,
        offTime: Int = 0,
        count: Int = 1,
    ) {
        controlIndicatorLED(1, onTime, offTime, count)
    }

    @JvmStatic
    fun controlIndicatorRedLED(
    ) {
        controlIndicatorRedLED(Config.instance().redLedLightingDuration, 0, 1)
    }


    @JvmStatic
    fun controlIndicatorRedLED(
        onTime: Int = Config.instance().redLedLightingDuration,
        offTime: Int = 0,
        count: Int = 1,
    ) {
        controlIndicatorLED(2, onTime, offTime, count)
    }

    @JvmStatic
    fun controlIndicatorLEDOff(onTime: Int, offTime: Int, count: Int) {
        controlIndicatorLED(0, onTime, offTime, count)
    }

    @JvmStatic
    fun controlIndicatorLED(ledType: Int, onTime: Int, offTime: Int, count: Int) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("type", ledType)
            jsonObject.put("onTime", onTime)
            jsonObject.put("offTime", offTime)
            jsonObject.put("count", count)
            val data = jsonObject.toString().toByteArray()
            val result = ByteArray(255)
            val size = intArrayOf(result.size)
            val ret = AMTHidManager.instance()
                .setParam(ParamIndex.CONTROL_INDICATOR_LED, data, result, size)
            if (ret == 0) {
                val resultStr = String(result, 0, size[0])
                val resultJsonObject = JSONObject(resultStr)
                if (resultJsonObject.has("status") && resultJsonObject.has("detail")) {
                    val status = resultJsonObject.getInt("status")
                    val detail = resultJsonObject.getString("detail")
                    Log.i(
                        TAG,
                        "controlIndicatorLED: status = $status,detail = $detail"
                    )
                } else {
                    Log.w(TAG, "controlIndicatorLED: $resultStr")
                }
            } else {
                Log.e(
                    TAG,
                    "controlIndicatorLED: hid set param failed,ret = $ret"
                )
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun exitStandByMode() {
        try {
            if (!enterStandByModeByDeviceSettings(false)) {
                enterStandByModeByCommonSettings(false)
            }
        } catch (ignore: Exception) {
        }

    }

    @JvmStatic
    fun enterStandByMode() {
        try {
            if (!enterStandByModeByDeviceSettings(true)) {
                enterStandByModeByCommonSettings(true)
            }
        } catch (ignore: Exception) {
        }

    }

    private fun enterStandByModeByDeviceSettings(enter: Boolean): Boolean {
        val jsonObject = JSONObject()
        val deviceSettings = JSONObject()
        deviceSettings.put(DeviceSettings.STAND_BY_MODE_KEY, enter)
        jsonObject.put(DeviceSettings.KEY, deviceSettings)
        val json = jsonObject.toString().toByteArray()
        val data = ByteArray(255)
        val size = intArrayOf(data.size)
        System.arraycopy(json, 0, data, 0, json.size)
        val ret = AMTHidManager.instance()
            .setConfig(ConfigType.DEVICE_CONFIG, data, size)

        if (ret == ErrorCode.ERROR_NONE) {
            val response = String(data, 0, size[0])
            val status = response.getStatus()
            if (status == StatusCode.SUCCESS) {
                Log.i(TAG, "enterStandByModeByDeviceSettings: success")
                return true
            } else {
                Log.e(TAG, "enterStandByModeByDeviceSettings: failed,${response.getDetail()}")
                return false
            }
        } else {
            Log.e(TAG, "enterStandByModeByDeviceSettings: failed,ret = $ret")
            return false
        }
    }

    private fun enterStandByModeByCommonSettings(enter: Boolean): Boolean {
        val jsonObject = JSONObject()
        val deviceSettings = JSONObject()
        deviceSettings.put(CommonSettingData.STAND_BY_MODE_KEY, enter)
        jsonObject.put(CommonSettingData.KEY, deviceSettings)
        val json = jsonObject.toString().toByteArray()
        val data = ByteArray(255)
        val size = intArrayOf(data.size)
        System.arraycopy(json, 0, data, 0, json.size)
        val ret = AMTHidManager.instance()
            .setConfig(ConfigType.COMMON_CONFIG, data, size)

        if (ret == ErrorCode.ERROR_NONE) {
            val response = String(data, 0, size[0])
            val status = response.getStatus()
            if (status == StatusCode.SUCCESS) {
                Log.i(TAG, "enterStandByModeByCommonSettings: success")
                return true
            } else {
                Log.e(TAG, "enterStandByModeByCommonSettings: failed,${response.getDetail()}")
                return false
            }
        } else {
            Log.e(TAG, "enterStandByModeByCommonSettings: failed,ret = $ret")
            return false
        }
    }
}
