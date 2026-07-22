package com.armatura.internaldata.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.ExApplication
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.camera.biodata.FaceData
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.manager.FaceManager
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.pojo.face.register.DetectFaceRequest
import com.armatura.biomodule.pojo.face.register.Face
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.BitmapUtil
import com.armatura.biomodule.util.HidHelper
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.getDataJSONObject
import com.armatura.biomodule.util.getDetail
import com.armatura.biomodule.util.getStatus
import com.armatura.biomodule.util.stringFormatD
import com.armatura.constant.ErrorCode
import com.armatura.constant.ManageType
import com.armatura.constant.StatusCode
import com.armatura.internaldata.bean.ModuleUserInfo
import com.armatura.translib.AMTHidManager
import com.armatura.uvclib.util.AMTUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs

/**
 * Created by Jeremy at 2022/11/3.
 * Modified by Magic at 2024/12/3
 */
class ModuleBioDataUtil private constructor() {
    companion object {
        private val TAG = ModuleBioDataUtil::class.java.simpleName

        @JvmStatic
        fun instance(): ModuleBioDataUtil {
            return InstanceHolder.INSTANCE
        }

        private object InstanceHolder {
            val INSTANCE: ModuleBioDataUtil = ModuleBioDataUtil()
        }
    }

    private val enrollArea = Rect()


    /**
     * Manage Module Data
     * -----------
     * ADD_PERSON
     * -----------
     */
    fun addPerson(userInfo: UserInfo): AMTResult<String?> {
        val addPersonRequest = HidHelper.createAddPersonRequest(userInfo, null)
        val jsonString = JSONUtil.getJsonString(addPersonRequest)
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val hidRet = AMTHidManager.instance().manageModuleData(
            ManageType.ADD_PERSON, jsonString.toByteArray(), resultByteArray, resultSize
        )
        if (hidRet == ErrorCode.ERROR_NONE) {
            try {
                val result = String(resultByteArray, 0, resultSize[0])
                val jsonObject = JSONObject(result)
                val status = jsonObject.getStatus()
                if (status == StatusCode.SUCCESS) {
                    val personId = jsonObject.getDataJSONObject().getString("personId")
                    return AMTResult(ErrorCode.ERROR_NONE, "success", personId)
                } else {
                    val detail = jsonObject.getDetail()
                    return AMTResult(status, detail, null)
                }
            } catch (e: JSONException) {
                return AMTResult(404, "${e.message}", null)
            }

        }
        return AMTResult(hidRet, "Send HID failed,ret = $hidRet", null)
    }

    fun importUser(userInfo: UserInfo): AMTResult<Int> {
        val addPersonRequest = HidHelper.createAddPersonRequest(userInfo, null)
        val jsonString = JSONUtil.getJsonString(addPersonRequest)
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.IMPORT_PERSON, jsonString.toByteArray(), resultByteArray, resultSize
        )
        if (ret == ErrorCode.ERROR_NONE) {
            try {
                val result = String(resultByteArray, 0, resultSize[0])
                val jsonObject = JSONObject(result)
                val status = jsonObject.getStatus()
                if (status == StatusCode.SUCCESS) {
                    return AMTResult(ErrorCode.ERROR_NONE, "success", ErrorCode.ERROR_NONE)
                } else {
                    val detail = jsonObject.getDetail()
                    return AMTResult(status, detail, null)
                }
            } catch (e: JSONException) {
                return AMTResult(404, "${e.message}", 404)
            }

        }
        return AMTResult(ret, "Send HID failed,ret = $ret", ret)
    }

    /**
     * Manage Module Data
     * -----------
     * QUERY_ALL_PERSON
     * -----------
     */
    fun queryAllPerson(pageIndex: Int, pageSize: Int): AMTResult<List<UserInfo>?> {
        val queryAllPersonJson = JSONObject().apply {
            put("pageIndex", pageIndex)
            put("pageSize", pageSize)
        }
        val resultByteArray = ByteArray(20 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val hidRet = AMTHidManager.instance().manageModuleData(
            ManageType.QUERY_ALL_PERSON,
            queryAllPersonJson.toString().toByteArray(),
            resultByteArray,
            resultSize
        )
        return if (hidRet == ErrorCode.ERROR_NONE) {
            val result = String(resultByteArray, 0, resultSize[0])
            try {
                val jsonObject = JSONObject(result)
                val status = jsonObject.getStatus()
                if (status == StatusCode.SUCCESS) {
                    AMTResult(ErrorCode.ERROR_NONE, "success", JSONUtil.getRemoteUserInfos(result))
                } else {
                    AMTResult(status, jsonObject.getDetail(), null)
                }
            } catch (e: java.lang.Exception) {
                AMTResult(404, "${e.message}", null)
            }
        } else {
            return AMTResult(hidRet, "Send HID failed,ret = $hidRet", null)
        }
    }

    /**
     * Manage Module Data
     * -----------
     * DEL_PERSON
     * -----------
     */
    fun deletePerson(userPin: String): AMTResult<String> {
        val deletePersonJson = JSONObject().apply {
            put("personId", userPin)
        }
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.DEL_PERSON,
            deletePersonJson.toString().toByteArray(),
            resultByteArray,
            resultSize
        )
        if (ret == ErrorCode.ERROR_NONE) {
            val result = String(resultByteArray, 0, resultSize[0])
            return JSONObject(result).let {
                val status = it.getStatus()
                if (status == StatusCode.SUCCESS) {
                    AMTResult(ErrorCode.ERROR_NONE, "success", null)
                } else {
                    AMTResult(status, it.getDetail(), null)
                }
            }
        }
        return AMTResult(ret, "Send HID failed,ret = $ret", null)
    }

    /**
     * Manage Module Data
     * -----------
     * CLEAR
     * -----------
     */
    fun clearPersons(): AMTResult<String> {
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.CLEAR, null, resultByteArray, resultSize
        )
        if (ret == ErrorCode.ERROR_NONE) {
            val result = String(resultByteArray, 0, resultSize[0])
            Log.i(TAG, "clearPersons: \n$result")
            return JSONObject(result).let {
                val status = it.getStatus()
                if (status == StatusCode.SUCCESS) {
                    AMTResult(ErrorCode.ERROR_NONE, "success", null)
                } else {
                    AMTResult(status, it.getDetail(), null)
                }
            }
        }
        return AMTResult(ret, "Send HID failed,ret = $ret", null)
    }


    /**
     * Manage Module Data
     * -----------
     * GET_PERSON
     * -----------
     */
    fun getPerson(
        userPin: String, needFaceFeature: Boolean = true, needPalmFeature: Boolean = true,
    ): AMTResult<UserInfo?> {
        val getPersonJson = JSONObject().apply {
            put("personId", userPin)
            put("data", JSONArray().let { data ->
                data.put(
                    JSONObject().apply {
                        put("bioType", "face")
                        put("feature", needFaceFeature)
                    }
                )
                data.put(
                    JSONObject().apply {
                        put("bioType", "palm")
                        put("feature", needPalmFeature)
                    }
                )
            })
        }
        val resultByteArray = ByteArray(1024 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.GET_PERSON,
            getPersonJson.toString().toByteArray(),
            resultByteArray,
            resultSize
        )
        return if (ret == ErrorCode.ERROR_NONE) {
            val result = String(resultByteArray, 0, resultSize[0])
            try {
                val status = result.getStatus()
                if (status == StatusCode.SUCCESS) {
                    AMTResult(ErrorCode.ERROR_NONE, "success", JSONUtil.getRemoteUserInfo(result))
                } else {
                    AMTResult(404, result.getDetail(), null)
                }
            } catch (e: Exception) {
                AMTResult(404, "${e.message}", null)
            }
        } else {
            AMTResult(ret, "Send Hid failed,ret = $ret", null)
        }
    }


    /**
     * Manage Module Data
     * -----------
     * ADD_FACE
     * -----------
     */
    fun addFaceByFeature(
        userPin: String,
        faceFeature: ByteArray,
    ): AMTResult<Int> {
        val addFaceJson = JSONObject().apply {
            put("personId", userPin)
            put("features", JSONArray().let { features ->
                features.put(
                    JSONObject().apply {
                        put("bioType", "face")
                        put("data", convertToFeatureData(faceFeature))
                        put("size", faceFeature.size)
                    }
                )
            })
        }
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.ADD_FACE, addFaceJson.toString().toByteArray(), resultByteArray, resultSize
        )
        if (ret != 0) {
            Log.e(TAG, "Add face by template failed, ret: $ret.")
            return AMTResult(
                -1, message = stringFormatD(RegisterStatus.HID_FAILED, ret), null
            )
        }

        val result = String(resultByteArray, 0, resultSize[0])
        val commonConfigData = JSONUtil.getDataResult(result)
        if (commonConfigData == null) {
            Log.e(TAG, "Get result failed")
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), 0
            )
        }

        return when (commonConfigData.status) {
            StatusCode.SUCCESS -> AMTResult(
                ErrorCode.ERROR_NONE,
                getString(R.string.register_success),
                commonConfigData.status
            )

            StatusCode.FACE_REPEATED -> AMTResult(
                400,
                getString(RegisterStatus.FACE_ALREADY_IN_MODULE),
                commonConfigData.status
            )

            else -> AMTResult(404, commonConfigData.detail, commonConfigData.status)
        }
    }

    /**
     * Manage Module Data
     * -----------
     * REMOVE_FACE
     * -----------
     */
    fun removeFaceByPersonId(
        personId: String,
    ): Boolean {
        val requestJson = JSONObject().apply {
            put("personId", personId)
        }
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.REMOVE_FACE,
            requestJson.toString().toByteArray(),
            resultByteArray,
            resultSize
        )
        if (ret != ErrorCode.SUCCESS) {
            Log.e(TAG, "removeFaceByPersonId: send hid failed,ret = $ret")
            return false
        }

        val responseJSONObject = JSONObject(String(resultByteArray, 0, resultSize[0]))
        val status = responseJSONObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            Log.e(TAG, "removeFaceByPersonId: " + responseJSONObject.getDetail())
        } else {
            Log.i(TAG, "removeFaceByPersonId: success")
        }
        return status == StatusCode.SUCCESS
    }

    /**
     * Manage Module Data
     * -----------
     * ADD_PALM
     * -----------
     */
    fun addPalmByFeature(
        userPin: String,
        palmFeature: ByteArray,
    ): AMTResult<Int> {
        val addFaceJson = JSONObject().apply {
            put("personId", userPin)
            put("features", JSONArray().let { features ->
                features.put(
                    JSONObject().apply {
                        put("bioType", "palm")
                        put("data", convertToFeatureData(palmFeature))
                        put("size", palmFeature.size)
                    }
                )
            })
        }
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.ADD_PALM,
            addFaceJson.toString().toByteArray(),
            resultByteArray,
            resultSize
        )
        if (ret != 0) {
            Log.e(TAG, "add palm by template failed, ret: $ret.")
            return AMTResult(
                ret, stringFormatD(RegisterStatus.HID_FAILED, ret), null
            )
        }

        val result = String(resultByteArray, 0, resultSize[0])
        val commonConfigData = JSONUtil.getDataResult(result)
        if (commonConfigData == null) {
            Log.e(TAG, "get result failed")
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), 0
            )
        }

        return when (commonConfigData.status) {
            StatusCode.SUCCESS -> AMTResult(
                ErrorCode.ERROR_NONE,
                getString(R.string.register_success),
                commonConfigData.status
            )

            StatusCode.PALM_REPEATED -> AMTResult(
                400,
                getString(RegisterStatus.PALM_ALREADY_IN_MODULE),
                commonConfigData.status
            )

            else -> AMTResult(404, commonConfigData.detail, commonConfigData.status)
        }
    }

    /**
     * Manage Module Data
     * -----------
     * REMOVE_PALM
     * -----------
     */
    fun removePalmByPersonId(
        personId: String,
    ): Boolean {
        val requestJson = JSONObject().apply {
            put("personId", personId)
        }
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.REMOVE_PALM,
            requestJson.toString().toByteArray(),
            resultByteArray,
            resultSize
        )
        if (ret != ErrorCode.SUCCESS) {
            Log.e(TAG, "removePalmByPersonId: send hid failed,ret = $ret")
            return false
        }

        val responseJSONObject = JSONObject(String(resultByteArray, 0, resultSize[0]))
        val status = responseJSONObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            Log.e(TAG, "removePalmByPersonId: " + responseJSONObject.getDetail())
        } else {
            Log.i(TAG, "removePalmByPersonId: success")
        }
        return status == StatusCode.SUCCESS
    }

    /**
     * when identify source is MultiBioModule
     * The CustomData will include identify info.
     * This method show how to get identify info from CustomData.
     */
    fun getUserInfoFromFaceData(faceData: FaceData): UserInfo? {
        val identifyInfo = faceData.identifyInfoList[0] ?: return null

        val score = identifyInfo.similarity
        //compare the score with threshold
        if (score <= Config.instance().faceIdentifyThreshold) {
            Log.w(TAG, "[getUserInfoFromFaceData]: $score")
            return null
        }
        return UserInfo().apply {
            similarity = score
            groupId = identifyInfo.groupId
            personId = identifyInfo.personId
            name = identifyInfo.name
            userId = identifyInfo.userId
            avatarIndex = -1
        }
    }


    /**
     * Experimental features may have been deprecated.
     */
    fun addRegInfoByImage(
        userPin: String,
        image: Image,
        type: Int,
    ): AMTResult<Int> {
        if (TextUtils.isEmpty(userPin)) {
            return AMTResult(-1, "pin is emtpy", 0)
        }
        val userInfo = ModuleUserInfo()
        userInfo.personId = userPin
        userInfo.images = arrayListOf(image)
        val jsonString = JSONUtil.getJsonString(userInfo)
        val resultByteArray = ByteArray(1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            type, jsonString.toByteArray(), resultByteArray, resultSize
        )
        val addType = if (type == ManageType.ADD_FACE) "face" else "palm"
        if (ret != ErrorCode.ERROR_NONE) {
            Log.e(TAG, "add $addType by image: failed, code: $ret.")
            return AMTResult(
                ret, stringFormatD(RegisterStatus.HID_FAILED, ret), null
            )
        }

        val result = String(resultByteArray, 0, resultSize[0])
        Log.i(TAG, "add $addType by image: \n$result.")
        val commonConfigData = JSONUtil.getDataResult(result)
        if (commonConfigData == null) {
            Log.e(TAG, "get result failed")
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), 0
            )
        }

        return when (commonConfigData.status) {
            StatusCode.SUCCESS -> AMTResult(
                ErrorCode.ERROR_NONE,
                getString(R.string.register_success),
                commonConfigData.status
            )

            StatusCode.FACE_REPEATED -> AMTResult(
                400,
                getString(RegisterStatus.FACE_ALREADY_IN_MODULE),
                commonConfigData.status
            )

            StatusCode.PALM_REPEATED -> AMTResult(
                400,
                getString(RegisterStatus.PALM_ALREADY_IN_MODULE),
                commonConfigData.status
            )

            else -> AMTResult(404, commonConfigData.detail, commonConfigData.status)
        }
    }

    /**
     * Experimental features may have been deprecated.
     */
    @Synchronized
    fun registerFaceByBitmap(
        bitmap: Bitmap,
        userId: String?,
        isNeedFilter: Boolean,
    ): AMTResult<Int> {
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
            Log.i(TAG, "registerFaceByBitmap: registerFace ret=$ret")
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                ret, stringFormatD(RegisterStatus.HID_FAILED, ret), null
            )
        }

        val jsonResult = String(result, 0, size[0])
        val jsonObject = JSONObject(jsonResult)
        val status = jsonObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(
                -1, message = jsonObject.getDetail(), 0
            )
        }
        val commonConfigData = JSONUtil.getCommonConfigData(jsonResult)
        if (commonConfigData == null) {
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), 0
            )
        }

        val detectFaceResponse = JSONUtil.getDetectFaceResponse(commonConfigData)
        if (detectFaceResponse == null) {
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), 0
            )
        }

        if (detectFaceResponse.faces.isNullOrEmpty()) {
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                -1, message = getString(RegisterStatus.NO_DETECT_FACE), 0
            )
        }

        //find max face
        val biggestFace = detectFaceResponse.faces.maxBy {
            abs(it.faceInfo.rect.right - it.faceInfo.rect.left)
        }
        if (isNeedFilter) {
            val faceInfo = biggestFace.faceInfo

            val faceRect = faceInfo.rect
            bitmap.also {
                calEnrollArea(enrollArea, bitmap.width, bitmap.height)
                if (!enrollArea.contains(faceRect)) {
                    AMTUtil.safeReleaseBitmap(bitmap)
                    return AMTResult(
                        -1,
                        message = getString(RegisterStatus.FACE_NOT_IN_ENROLL_AREA),
                        0
                    )
                }
            }

            //face size filter
            val faceWidth = faceRect.right - faceRect.left
            val faceHeight = faceRect.bottom - faceRect.top
            if (faceWidth < 100 || faceHeight < 100) {
                AMTUtil.safeReleaseBitmap(bitmap)
                return AMTResult(
                    -1,
                    message = ExApplication.instance().getString(
                        RegisterStatus.FACE_TOO_SMALL.resId,
                        100.toFloat(),
                        faceWidth.toFloat()
                    ), 0
                )
            }
        }
        return addRegInfoByImage(
            userId.toString(), image, ManageType.ADD_FACE
        )
    }

    @Synchronized
    fun generateFaceImage(
        bitmap: Bitmap,
        isNeedFilter: Boolean,
    ): AMTResult<Image?> {
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
            Log.i(TAG, "registerFaceByBitmap: registerFace ret=$ret")
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                ret, stringFormatD(RegisterStatus.HID_FAILED, ret), null
            )
        }

        val jsonResult = String(result, 0, size[0])
        val jsonObject = JSONObject(jsonResult)
        val status = jsonObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(
                -1, message = jsonObject.getDetail(), null
            )
        }
        val commonConfigData = JSONUtil.getCommonConfigData(jsonResult)
        if (commonConfigData == null) {
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), null
            )
        }

        val detectFaceResponse = JSONUtil.getDetectFaceResponse(commonConfigData)
        if (detectFaceResponse == null) {
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                -1, message = getString(RegisterStatus.JSON_FAILED), null
            )
        }

        if (detectFaceResponse.faces.isNullOrEmpty()) {
            AMTUtil.safeReleaseBitmap(bitmap)
            return AMTResult(
                -1, message = getString(RegisterStatus.NO_DETECT_FACE), null
            )
        }

        //find max face
        val biggestFace = detectFaceResponse.faces.maxBy {
            abs(it.faceInfo.rect.right - it.faceInfo.rect.left)
        }
        if (isNeedFilter) {
            val faceInfo = biggestFace.faceInfo

            val faceRect = faceInfo.rect
            bitmap.also {
                calEnrollArea(enrollArea, bitmap.width, bitmap.height)
                if (!enrollArea.contains(faceRect)) {
                    AMTUtil.safeReleaseBitmap(bitmap)
                    return AMTResult(
                        -1,
                        message = getString(RegisterStatus.FACE_NOT_IN_ENROLL_AREA), null
                    )
                }
            }

            //face size filter
            val faceWidth = faceRect.right - faceRect.left
            val faceHeight = faceRect.bottom - faceRect.top
            if (faceWidth < 100 || faceHeight < 100) {
                AMTUtil.safeReleaseBitmap(bitmap)
                return AMTResult(
                    -1,
                    message = ExApplication.instance().getString(
                        RegisterStatus.FACE_TOO_SMALL.resId,
                        100.toFloat(),
                        faceWidth.toFloat()
                    ), null
                )
            }
        }
        return AMTResult(result = image, code = ErrorCode.ERROR_NONE, message = "")
    }


    @Synchronized
    fun registerFaceByModuleInternal(
        isNeedFilter: Boolean,
        taskRunningFlag: BooleanArray,
    ): Flow<RegisterResult<Int>> {
        return flow {

            val requestJSONObject = JSONObject().apply {
                put("feature", true)
                put("faceInfo", true)
                put("picture", true)
            }
            val result = ByteArray(400 * 1024)
            val size = IntArray(1)

            val cacheFaceList = mutableListOf<Face>()
            val readyToEnrollFaceList = mutableListOf<Face>()

            while (taskRunningFlag[0]) {
                size[0] = result.size
                var ret = AMTHidManager.instance()
                    .registerFace(requestJSONObject.toString().toByteArray(), result, size)
                if (ret != ErrorCode.ERROR_NONE) {
                    emit(
                        RegisterResult.Status(
                            400, stringFormatD(RegisterStatus.HID_FAILED, ret), 0
                        )
                    )
                    continue
                }
                val responseString = String(result, 0, size[0])
                val responseJSONObject = JSONObject(responseString)
                val status = responseJSONObject.getStatus()
                if (status != StatusCode.SUCCESS) {
                    emit(
                        RegisterResult.Status(
                            400,
                            message = responseJSONObject.getDetail() + "($status)",
                            0
                        )
                    )
                    continue
                }
                val dataJSONObject = responseJSONObject.getDataJSONObject()

                val detectFaceResponse = JSONUtil.getFacesFromResponse(dataJSONObject.toString())
                if (detectFaceResponse == null) {
                    emit(
                        RegisterResult.Status(
                            400, getString(RegisterStatus.JSON_FAILED), 0
                        )
                    )
                    continue
                }
                if (detectFaceResponse.faces.isNullOrEmpty()) {
                    emit(
                        RegisterResult.Status(
                            400,
                            message = getString(RegisterStatus.NO_DETECT_FACE),
                            0
                        )
                    )
                    continue
                }

                //find max face
                val biggestFace = detectFaceResponse.faces.maxBy {
                    abs(it.faceInfo.rect.right - it.faceInfo.rect.left)
                }
                if (isNeedFilter) {
                    val faceInfo = biggestFace.faceInfo

                    val faceRect = faceInfo.rect
                    //face size filter
                    val faceWidth = faceRect.right - faceRect.left
                    val faceHeight = faceRect.bottom - faceRect.top
                    if (faceWidth < 100 || faceHeight < 100) {
                        emit(
                            RegisterResult.Status(
                                400,
                                message = ExApplication.instance().getString(
                                    RegisterStatus.FACE_TOO_SMALL.resId,
                                    100.toFloat(),
                                    faceWidth.toFloat()
                                ),
                                0
                            )
                        )
                        continue
                    }

                    //make sure face in center of image
                    calEnrollArea(enrollArea, 720, 1280)
                    if (!enrollArea.contains(faceRect)) {
                        emit(
                            RegisterResult.Status(
                                400,
                                message = getString(RegisterStatus.FACE_NOT_IN_ENROLL_AREA),
                                0
                            )
                        )
                        continue
                    }
                }

                cacheFaceList.add(biggestFace)
                emit(
                    RegisterResult.Progress(
                        0, "", 100
                    )
                )

                if (cacheFaceList.size == 2) {
                    if (Config.instance().is16Platform) {
                        //do face 1:1 make sure it's same face
                        val verifyRequestJSONObject = JSONObject()
                        verifyRequestJSONObject.put(
                            "features", JSONArray().let {
                                it.put(JSONObject().apply {
                                    put("bioType", "face")
                                    put("data", cacheFaceList[0].feature.data)
                                    put("size", cacheFaceList[0].feature.size)
                                })
                                it.put(JSONObject().apply {
                                    put("bioType", "face")
                                    put("data", cacheFaceList[1].feature.data)
                                    put("size", cacheFaceList[1].feature.size)
                                })
                            }
                        )

                        if (!taskRunningFlag[0]) {
                            break
                        }

                        size[0] = result.size
                        ret = AMTHidManager.instance()
                            .verify(verifyRequestJSONObject.toString().toByteArray(), result, size)
                        if (ret != ErrorCode.ERROR_NONE) {
                            emit(
                                RegisterResult.Status(
                                    400,
                                    "hid verify failed, ret=$ret",
                                    ret
                                )
                            )
                            continue
                        }
                        val verifyResponseJSONObject = JSONObject(String(result, 0, size[0]))
                        val verifyStatus = verifyResponseJSONObject.getStatus()
                        if (verifyStatus != StatusCode.SUCCESS) {
                            emit(
                                RegisterResult.Status(
                                    400, verifyResponseJSONObject.getDetail() + "($verifyStatus)", 0
                                )
                            )
                            continue
                        }
                        val similarity =
                            verifyResponseJSONObject.getDataJSONObject().getDouble("similarity")
                        if (similarity >= Config.instance().faceVerifyThreshold) {
                            readyToEnrollFaceList.addAll(cacheFaceList)
                            break
                        } else {
                            cacheFaceList.clear()
                            emit(
                                RegisterResult.Status(
                                    400, getString(RegisterStatus.ENROLL_NEED_SAME_FACE), -200
                                )
                            )
                        }
                        Log.i(TAG, "registerFaceByModuleInternal: $similarity")
                    } else {
                        val similarity = FaceManager.getInstance().dbVerify(
                            cacheFaceList[0].feature.feature,
                            cacheFaceList[1].feature.feature
                        )
                        if (similarity >= Config.instance().faceVerifyThreshold) {
                            readyToEnrollFaceList.addAll(cacheFaceList)
                            break
                        } else {
                            cacheFaceList.clear()
                            emit(
                                RegisterResult.Status(
                                    400, getString(RegisterStatus.ENROLL_NEED_SAME_FACE), -200
                                )
                            )
                        }
                    }

                }
            }

            if (readyToEnrollFaceList.size < 2) {
                return@flow
            }
            //get the best quality
            readyToEnrollFaceList.sortBy { face ->
                face.faceInfo.score
            }

            val feature = readyToEnrollFaceList[readyToEnrollFaceList.size - 1].feature.data
            if (feature.isNullOrBlank()) {
                emit(
                    RegisterResult.Status(
                        -1, getString(RegisterStatus.NO_DETECT_FACE), 0
                    )
                )
                return@flow
            }
            emit(
                RegisterResult.ExtractInfo(
                    0, feature, 0
                )
            )
        }.catch { exception ->
            emit(RegisterResult.Status(404, "${exception.message}", null))
        }
    }


    private fun convertToFeatureData(byteArr: ByteArray): String {
        return Base64.encodeToString(byteArr, Base64.NO_WRAP)
    }


    /**
     * Manage Module Data
     * -----------
     * QUERY_STATISTICS
     * -----------
     */
    fun queryStatistics(): AMTResult<Int> {
        val resultByteArray = ByteArray(20 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance().manageModuleData(
            ManageType.QUERY_STATISTICS, null, resultByteArray, resultSize
        )
        if (ret != ErrorCode.ERROR_NONE) {
            return AMTResult(ret, "Send Hid Command Failed($ret)", 0)
        }
        val result = String(resultByteArray, 0, resultSize[0])
        val resultJSONObject = JSONObject(result)
        val status = resultJSONObject.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(status, resultJSONObject.getDetail(), 0)
        }
        try {
            val personCount = resultJSONObject.getDataJSONObject().getInt("personCount")
            return AMTResult(ErrorCode.ERROR_NONE, resultJSONObject.getDetail(), personCount)
        } catch (e: Exception) {
            return AMTResult(404, "${e.message} \n$result", 0)
        }
    }

    private fun calEnrollArea(enrollArea: Rect, width: Int, height: Int) {
        val fixRectLeft = width / 6
        val fixRectRight = fixRectLeft * 5
        val fixRectTop = height / 5
        val fixRectBottom = fixRectTop * 4
        enrollArea.set(fixRectLeft, fixRectTop, fixRectRight, fixRectBottom)
    }
}