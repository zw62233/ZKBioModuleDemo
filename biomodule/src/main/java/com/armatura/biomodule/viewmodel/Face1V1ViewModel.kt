package com.armatura.biomodule.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.pojo.face.FaceFeature
import com.armatura.biomodule.pojo.face.register.DetectFaceRequest
import com.armatura.biomodule.pojo.face.register.DetectFaceResponse
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.HidHelper
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.getDataJSONObject
import com.armatura.biomodule.util.getDetail
import com.armatura.biomodule.util.getStatus
import com.armatura.constant.ErrorCode
import com.armatura.constant.StatusCode
import com.armatura.translib.AMTHidManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class Face1V1ViewModel : ViewModel() {
    companion object {
        private const val TAG = "Face1V1ViewModel"
    }

    private val faceTemplateCache by lazy { mutableListOf<FaceFeature>() }
    private val gson by lazy { Gson() }


    fun extractFace(): Flow<AMTResult<Int>> {
        return flow {
            HidHelper.exitStandByMode()
            if (faceTemplateCache.isNotEmpty()) {
                faceTemplateCache.clear()
            }
            emit(detectFace())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }


    fun verifyFace(): Flow<AMTResult<Int>> {
        return flow {
            HidHelper.exitStandByMode()
            if (faceTemplateCache.isEmpty()) {
                emit(
                    AMTResult(
                        0,
                        getString(RegisterStatus.STEP1_NEED_FIRST),
                        0
                    )
                )
                return@flow
            }
            if (faceTemplateCache.size == 2) {
                faceTemplateCache.removeAt(1)
            }
            emit(detectFace())
            if (faceTemplateCache.size < 2) {
                emit(
                    AMTResult(
                        0,
                        getString(RegisterStatus.NEEDS_TWO_FEATURE),
                        100
                    )
                )
                return@flow
            }
            emit(verifyFaceInModule())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }


    private fun verifyFaceInModule(): AMTResult<Int> {
        //do face 1:1 make sure it's same face
        val verifyRequestJSONObject = JSONObject()
        verifyRequestJSONObject.put(
            "features", JSONArray().let {
                it.put(JSONObject().apply {
                    put("bioType", "face")
                    put("data", faceTemplateCache[0].data)
                    put("size", faceTemplateCache[0].size)
                })
                it.put(JSONObject().apply {
                    put("bioType", "face")
                    put("data", faceTemplateCache[1].data)
                    put("size", faceTemplateCache[1].size)
                })
            }
        )

        val data: ByteArray = verifyRequestJSONObject.toString().toByteArray()
        val result = ByteArray(255)
        val size = IntArray(result.size)

        val ret = AMTHidManager.instance()
            .verify(data, result, size)
        if (ret != ErrorCode.ERROR_NONE) {
            return AMTResult(
                0,
                getString(RegisterStatus.HID_FAILED),
                0
            )
        }
        val verifyResponseJSONObject = JSONObject(String(result, 0, size[0]))
        val verifyStatus = verifyResponseJSONObject.getStatus()
        if (verifyStatus != StatusCode.SUCCESS) {
            return AMTResult(
                0,
                verifyResponseJSONObject.getDetail(),
                0
            )
        }
        val similarity =
            verifyResponseJSONObject.getDataJSONObject().getDouble("similarity")
        if (similarity >= Config.instance().faceVerifyThreshold) {
            return AMTResult(
                0,
                String.format(Locale.US, "Verify success,similarity = %.2f", similarity),
                300
            )
        } else {
            return AMTResult(
                0,
                String.format(Locale.US, "Verify failed,similarity = %.2f", similarity),
                0
            )
        }
    }

    private fun detectFace(): AMTResult<Int> {
        val detectFaceRequest = DetectFaceRequest()
        detectFaceRequest.setIsNeedFaceInfo(true)
        detectFaceRequest.setIsNeedPicture(true)
        detectFaceRequest.setIsNeedFeature(true)
        val data = JSONUtil.getJsonString(detectFaceRequest)
        val result =
            ByteArray(400 * 1024 /*if you need image of feature,please alloc a lit big*/)
        val size = intArrayOf(result.size)
        val ret = AMTHidManager.instance().registerFace(data.toByteArray(), result, size)
        if (ret == 0) {
            val jsonResult = String(result, 0, size[0])
            try {
                val jsonObject = JSONObject(jsonResult)
                if (jsonObject.has("status")) {
                    val status = jsonObject.getInt("status")
                    if (status == ErrorCode.ERROR_NONE) {
                        val detectFaceResultJsonObj = jsonObject.getJSONObject("data")
                        val detectFaceResponse: DetectFaceResponse =
                            gson.fromJson(
                                detectFaceResultJsonObj.toString(),
                                DetectFaceResponse::class.java
                            )
                        if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
                            return AMTResult(
                                404,
                                getString(RegisterStatus.NO_DETECT_FACE),
                                0
                            )
                        }
                        if (detectFaceResponse.faces.size > 1) {
                            return AMTResult(
                                404,
                                getString(RegisterStatus.DETECT_TOO_MUCH_FACE),
                                0
                            )
                        }
                        val biggestFace = detectFaceResponse.faces[0]
                        val faceFeature = biggestFace.feature
                        faceTemplateCache.add(faceFeature)
                        return (AMTResult(0, "success", faceTemplateCache.size * 100))
                    } else {
                        val detail = jsonObject.getString("detail")
                        Log.w(
                            TAG,
                            "extractFace: failed,status = $status,$detail"
                        )
                        return AMTResult(0, detail, 0)
                    }
                } else {
                    return AMTResult(0, getString(RegisterStatus.JSON_FAILED), 0)
                }
            } catch (e: JSONException) {
                Log.e(TAG, "extractFace: ", e)
                return AMTResult(0, getString(RegisterStatus.JSON_FAILED), 0)
            }
        } else {
            return AMTResult(ret, "Send HID command failed, error = $ret", 0)
        }
    }
}