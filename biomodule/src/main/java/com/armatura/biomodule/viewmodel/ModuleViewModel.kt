package com.armatura.biomodule.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.pojo.face.register.DetectFaceRequest
import com.armatura.biomodule.pojo.info.SnapshotData
import com.armatura.biomodule.pojo.setting.VLPalmSetting
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.BitmapUtil
import com.armatura.biomodule.util.HidHelper
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.getDataJSONObject
import com.armatura.biomodule.util.getDetail
import com.armatura.biomodule.util.getStatus
import com.armatura.biomodule.util.stringFormatD
import com.armatura.constant.ErrorCode
import com.armatura.constant.SnapType
import com.armatura.constant.StatusCode
import com.armatura.translib.AMTHidManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
    }

    private val gson by lazy { Gson() }

    var cacheVLBitmap: Bitmap? = null

    override fun onCleared() {
        super.onCleared()
        cacheVLBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }


    fun snapShotByFlow(@SnapType snapType: Int): Flow<AMTResult<Bitmap?>> {
        return flow {
            HidHelper.exitStandByMode()
            emit(snapShot(snapType))
        }.catch { exception ->
            emit(AMTResult(404, "${exception}", null))
        }
    }

    fun registerFaceByFlow(bitmap: Bitmap): Flow<AMTResult<ByteArray?>> {
        return flow {
            ModuleHeartbeatManager.getInstance().heatBeatStop()
            emit(registerFaceByBitmap(bitmap))
        }.catch { exception ->
            emit(AMTResult(404, "${exception}", null))
        }
    }

    fun registerPalmByFlow(bitmap: Bitmap): Flow<AMTResult<List<TempPalmFeature>?>> {
        return flow {
            ModuleHeartbeatManager.getInstance().heatBeatStop()
            emit(registerPalmByBitmap(bitmap))
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun registerPalmByFlow(
        vlBitmap: Bitmap,
        irBitmap: Bitmap,
    ): Flow<AMTResult<List<TempPalmFeature>?>> {
        return flow {
            ModuleHeartbeatManager.getInstance().heatBeatStop()
            emit(registerPalmByBitmap(vlBitmap, irBitmap))
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    private fun snapShot(@SnapType snapType: Int): AMTResult<Bitmap?> {
        val snapData = ByteArray(2 * 1024 * 1024)
        val length = IntArray(1)
        val ret = AMTHidManager.instance().snapShot(snapType, snapData, length)
        if (ret != ErrorCode.ERROR_NONE) {
            return AMTResult(ret, "Send Hid Command failed,$ret", null)
        }
        val response = String(snapData, 0, length[0])
        val status = response.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(status, response.getDetail(), null)
        }
        val snapShotJSONObject = JSONObject(response).getDataJSONObject(SnapshotData.KEY)
        val snapShotData = gson.fromJson(snapShotJSONObject.toString(), SnapshotData::class.java)

        val imageData = Base64.decode(snapShotData.data, Base64.NO_WRAP)
        val bitmap = if (snapType == SnapType.SNAP_GRAY) {
            BitmapUtil.createGrayBitmap(
                imageData,
                snapShotData.width,
                snapShotData.height
            )
        } else {
            BitmapUtil.getBitmapFromByte(imageData)
        }
        return AMTResult(ErrorCode.SUCCESS, "success", bitmap)
    }

    private fun registerFaceByBitmap(
        bitmap: Bitmap,
    ): AMTResult<ByteArray?> {
        val detectFaceRequest = DetectFaceRequest()
        val filter = DetectFaceRequest.Filter()
        filter.widthMinValue = 10
        filter.heightMinValue = 10
        detectFaceRequest.setFilter(filter)

        val image = Image()
        if (!bitmap.isRecycled) {
            val scaleBitmap = BitmapUtil.scaleBitmapByWidth(bitmap, 720)
            image.bioType = BioType.FACE
            image.format = Image.Format.JPEG
            image.data = BitmapUtil.bitmapToBase64(scaleBitmap)
            detectFaceRequest.setImage(image)
        }
        detectFaceRequest.setIsNeedFaceInfo(true)
        detectFaceRequest.setIsNeedPicture(true)
        detectFaceRequest.setIsNeedFeature(true)
        val data = JSONUtil.getJsonString(detectFaceRequest)
        val result = ByteArray(400 * 1024)
        val size = IntArray(1)
        val ret = AMTHidManager.instance().registerFace(data.toByteArray(), result, size)
        if (ret != ErrorCode.ERROR_NONE) {
            return AMTResult(ret, stringFormatD(RegisterStatus.HID_FAILED, ret), null)
        }

        val responseStr = String(result, 0, size[0])
        val responseJSONObject = JSONObject(responseStr)
        val status = responseJSONObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(status, responseJSONObject.getDetail(), null)
        }

        val detectFaceResponse =
            JSONUtil.getFacesFromResponse(responseJSONObject.getDataJSONObject().toString())
                ?: return AMTResult(404, getString(RegisterStatus.JSON_FAILED), null)

        if (detectFaceResponse.faces.isNullOrEmpty()) {
            return AMTResult(404, getString(RegisterStatus.NO_DETECT_FACE), null)
        }

        //get index = 0
        val face = detectFaceResponse.faces[0]
        val faceFeature = Base64.decode(face.feature.data, Base64.DEFAULT)
        return AMTResult(ErrorCode.ERROR_NONE, getString(R.string.success), faceFeature)
    }


    private fun registerPalmByBitmap(bitmap: Bitmap): AMTResult<List<TempPalmFeature>?> {
        val registerPalmJson = JSONObject()
        val imagesJsonArray = JSONArray()
        val imagesJson = JSONObject()
        try {
            registerPalmJson.put("palmInfo", false)
            registerPalmJson.put("picture", false)
            registerPalmJson.put("feature", true)
            registerPalmJson.put("featureVein", true)

            imagesJson.put(
                "bioType", when (Config.palmTemplateMode) {
                    VLPalmSetting.PALM_TEMPLATE_MODE_IR -> BioType.PALM_VEIN
                    VLPalmSetting.PALM_TEMPLATE_MODE_VL -> BioType.PALM
                    else -> {
                        return AMTResult(
                            404, "Image registration in dual " +
                                    "palmprint mode is not currently supported.", null
                        )
                    }
                }
            )
            val scaleBitmap = BitmapUtil.scaleBitmapByWidth(bitmap, 720)
            imagesJson.put("data", BitmapUtil.bitmapToBase64(scaleBitmap))
            imagesJson.put("format", "jpeg")
            imagesJson.put("width", bitmap.width)
            imagesJson.put("height", bitmap.height)

            imagesJsonArray.put(imagesJson)

            registerPalmJson.put("images", imagesJsonArray)
        } catch (e: JSONException) {
            return AMTResult(404, getString(RegisterStatus.JSON_FAILED), null)
        }
        val result = ByteArray(200 * 1024)
        val size = intArrayOf(result.size)
        val hidRet = AMTHidManager.instance().registerPalm(
            registerPalmJson.toString().toByteArray(), result, size
        )
        if (hidRet != ErrorCode.ERROR_NONE) {
            return AMTResult(hidRet, stringFormatD(RegisterStatus.HID_FAILED, hidRet), null)
        }

        val resultJsonObject = JSONObject(String(result, 0, size[0]))
        val status = resultJsonObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(status, resultJsonObject.getDetail(), null);
        }

        val tempPalmFeatureList = mutableListOf<TempPalmFeature>()
        val data = resultJsonObject.getJSONObject("data")
        val palms = data.getJSONArray("palms")
        val firstPalm = palms.getJSONObject(0)
        if (firstPalm.has("feature")) {
            val feature = firstPalm.getJSONObject("feature")
            val verTemplate = feature.getString("verTemplate")
            val palmFeature = Base64.decode(verTemplate, Base64.DEFAULT)
            tempPalmFeatureList.add(TempPalmFeature(palmFeature, BioType.PALM))
            return AMTResult(
                ErrorCode.ERROR_NONE,
                getString(R.string.success),
                tempPalmFeatureList
            );
        } else if (firstPalm.has("featureVein")) {
            val featureVein = firstPalm.getJSONObject("featureVein")
            val verTemplate = featureVein.getString("verTemplate")
            val palmFeature = Base64.decode(verTemplate, Base64.DEFAULT)
            tempPalmFeatureList.add(TempPalmFeature(palmFeature, BioType.PALM_VEIN))
            return AMTResult(
                ErrorCode.ERROR_NONE,
                getString(R.string.success),
                tempPalmFeatureList
            );
        } else {
            return AMTResult(404, "No feature or feature vein", null)
        }

    }


    private fun registerPalmByBitmap(
        vlBitmap: Bitmap,
        irBitmap: Bitmap,
    ): AMTResult<List<TempPalmFeature>?> {
        val registerPalmJson = JSONObject()
        val imagesJsonArray = JSONArray()
        val vlImagesJson = JSONObject()
        val irImagesJson = JSONObject()
        try {
            registerPalmJson.put("palmInfo", false)
            registerPalmJson.put("picture", false)
            registerPalmJson.put("feature", true)
            registerPalmJson.put("featureVein", true)

            vlImagesJson.put("bioType", BioType.PALM)
            val scaleVLBitmap = BitmapUtil.scaleBitmapByWidth(vlBitmap, 720)
            vlImagesJson.put("data", BitmapUtil.bitmapToBase64(scaleVLBitmap))
            vlImagesJson.put("format", "jpeg")
            vlImagesJson.put("width", scaleVLBitmap.width)
            vlImagesJson.put("height", scaleVLBitmap.height)
            imagesJsonArray.put(vlImagesJson)

            irImagesJson.put("bioType", BioType.PALM_VEIN)
            val scaleIRBitmap = BitmapUtil.scaleBitmapByWidth(irBitmap, 720)
            irImagesJson.put("data", BitmapUtil.bitmapToBase64(scaleIRBitmap))
            irImagesJson.put("format", "jpeg")
            irImagesJson.put("width", scaleIRBitmap.width)
            irImagesJson.put("height", scaleIRBitmap.height)
            imagesJsonArray.put(irImagesJson)

            registerPalmJson.put("images", imagesJsonArray)
        } catch (e: JSONException) {
            return AMTResult(404, getString(RegisterStatus.JSON_FAILED), null)
        }
        val result = ByteArray(200 * 1024)
        val size = intArrayOf(result.size)
        val hidRet = AMTHidManager.instance().registerPalm(
            registerPalmJson.toString().toByteArray(), result, size
        )
        if (hidRet != ErrorCode.ERROR_NONE) {
            return AMTResult(hidRet, stringFormatD(RegisterStatus.HID_FAILED, hidRet), null)
        }

        val resultJsonObject = JSONObject(String(result, 0, size[0]))
        val status = resultJsonObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(status, resultJsonObject.getDetail(), null);
        }

        val tempPalmFeatures = mutableListOf<TempPalmFeature>()
        val data = resultJsonObject.getJSONObject("data")
        val palms = data.getJSONArray("palms")
        val firstPalm = palms.getJSONObject(0)
        if (firstPalm.has("feature")) {
            val feature = firstPalm.getJSONObject("feature")
            val verTemplate = feature.getString("verTemplate")
            val palmFeature = Base64.decode(verTemplate, Base64.DEFAULT)
            tempPalmFeatures.add(TempPalmFeature(palmFeature, BioType.PALM))
        } else {
            return AMTResult(404, "No feature", null)
        }

        if (firstPalm.has("featureVein")) {
            val featureVein = firstPalm.getJSONObject("featureVein")
            val verTemplate = featureVein.getString("verTemplate")
            val palmFeature = Base64.decode(verTemplate, Base64.DEFAULT)
            tempPalmFeatures.add(TempPalmFeature(palmFeature, BioType.PALM_VEIN))
        } else {
            return AMTResult(404, "No feature vein", null)
        }

        return AMTResult(ErrorCode.ERROR_NONE, "Success", tempPalmFeatures)
    }
}