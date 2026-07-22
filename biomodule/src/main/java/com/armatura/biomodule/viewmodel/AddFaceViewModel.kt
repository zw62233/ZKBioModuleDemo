package com.armatura.biomodule.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.ExApplication.Companion.instance
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.common.RegisterOperate
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.manager.FaceManager
import com.armatura.biomodule.pojo.face.FaceFeature
import com.armatura.biomodule.pojo.face.register.DetectFaceRequest
import com.armatura.biomodule.pojo.face.register.DetectFaceResponse
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.SpeakerHelper
import com.armatura.biomodule.util.getDataJSONObject
import com.armatura.biomodule.util.getDetail
import com.armatura.biomodule.util.getStatus
import com.armatura.constant.ErrorCode
import com.armatura.constant.FaceErrorCode
import com.armatura.constant.StatusCode
import com.armatura.translib.AMTHidManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

class AddFaceViewModel : BaseRegisterViewModel() {
    companion object {
        private const val TAG = "AddFaceViewModel"
        const val MAX_CACHE_SIZE: Int = 5
        const val IMAGE_WIDTH: Int = 720
        const val IMAGE_HEIGHT: Int = 1280
        const val TIMEOUT = 15_000
        const val ENROLL_STATE_TIME_OUT = 1001
        const val ENROLL_STATE_OK = 1002
    }

    private val gson by lazy { Gson() }
    private val mCacheFaceRecognizeDataList =
        mutableListOf<DetectFaceResponse>()
    private val enrollArea = Rect()
    private val _enrollStateFlow = MutableSharedFlow<Int>()
    val enrollStateFlow = _enrollStateFlow.asSharedFlow()

    fun enrollFaceByFlow(userId: String, @RegisterOperate operate: Int): Flow<AMTResult<Int>> {
        return flow {
            var startTime = SystemClock.elapsedRealtime()
            do {
                if (SystemClock.elapsedRealtime() - startTime > TIMEOUT) {
                    _enrollStateFlow.emit(ENROLL_STATE_TIME_OUT)
                    break
                }
                val enrollResult = enrollFace(userId, operate)
                emit(enrollResult)
                if(enrollResult.code == ErrorCode.ERROR_NONE){
                    //reset timeout
                    startTime = SystemClock.elapsedRealtime()
                }
                if (enrollResult.result!! >= MAX_CACHE_SIZE) {
                    _enrollStateFlow.emit(ENROLL_STATE_OK)
                    break
                }
            } while (true)
        }.flowOn(Dispatchers.IO)
    }


    private fun enrollFace(
        userId: String,
        @RegisterOperate operate: Int,
    ): AMTResult</*progress*/Int> {
        val extractFaceResult = extractFace()
        if (extractFaceResult.code != ErrorCode.ERROR_NONE) {
            return AMTResult(
                extractFaceResult.code,
                extractFaceResult.message,
                mCacheFaceRecognizeDataList.size
            )
        }

        if (mCacheFaceRecognizeDataList.size >= MAX_CACHE_SIZE) {
            return AMTResult(ErrorCode.ERROR_NONE, "cache is full", 0)
        }

        val detectFaceResponse = extractFaceResult.result!!
        val filterFaceFeatureResult = filterFaceFeature(detectFaceResponse, userId, operate)
        if (filterFaceFeatureResult.code != ErrorCode.ERROR_NONE) {
            return AMTResult(
                filterFaceFeatureResult.code,
                filterFaceFeatureResult.message,
                mCacheFaceRecognizeDataList.size
            )
        }

        val userInfo = filterFaceFeatureResult.result!!

        //do 1:1 with first palm feature ,check if it's same face
        if (mCacheFaceRecognizeDataList.size > 1) {
            val verifyTemplate = detectFaceResponse.faces[0].feature.feature
            val firstFaceTemplate = mCacheFaceRecognizeDataList[0].faces[0].feature.feature
            val similarity = FaceManager.getInstance().dbVerify(firstFaceTemplate, verifyTemplate)
            val faceVLRecognizeThreshold = Config.instance().faceIdentifyThreshold
            Log.d(
                TAG,
                "[checkVLFeature]: dbVerify score=$similarity, recognizeThreshold=$faceVLRecognizeThreshold"
            )
            if (similarity < faceVLRecognizeThreshold) {
                Log.w(
                    TAG,
                    "[checkVLFeature]: verify failed, score=$similarity, threshold=$faceVLRecognizeThreshold, need the same palm to finish enrolling the user"
                )
                return AMTResult(
                    404,
                    getString(RegisterStatus.ENROLL_NEED_SAME_FACE),
                    mCacheFaceRecognizeDataList.size
                )
            }
        }

        mCacheFaceRecognizeDataList.add(detectFaceResponse)
        SpeakerHelper.playSound(instance(), R.raw.beep)

        Log.i(
            TAG,
            String.format(
                "enrollFaceByFeature: prepare a preTemplate,count =%d",
                mCacheFaceRecognizeDataList.size
            )
        )

        if (mCacheFaceRecognizeDataList.size >= MAX_CACHE_SIZE) {
            doRegisterFace(userInfo)
        }
        return AMTResult(0, "success", mCacheFaceRecognizeDataList.size)
    }


    private fun extractFace(): AMTResult<DetectFaceResponse?> {
        val detectFaceRequest = DetectFaceRequest()
        detectFaceRequest.setIsNeedFaceInfo(true)
        detectFaceRequest.setIsNeedPicture(true)
        detectFaceRequest.setIsNeedFeature(true)
        val data = JSONUtil.getJsonString(detectFaceRequest)
        val result = ByteArray(400 * 1024 /*if you need image of feature,please alloc a lit big*/)
        val size = intArrayOf(result.size)
        val ret = AMTHidManager.instance().registerFace(data.toByteArray(), result, size)
        if (ret != ErrorCode.ERROR_NONE) {
            return AMTResult(ret, "Send HID command failed,$ret", null)
        }
        val jsonResult = String(result, 0, size[0])
        val response = JSONObject(jsonResult)
        val status = response.getStatus()
        if (status != StatusCode.SUCCESS) {
            return AMTResult(status, response.getDetail(), null)
        }
        val detectFaceResultJsonObj = response.getDataJSONObject()
        val detectFaceResponse: DetectFaceResponse =
            gson.fromJson(
                detectFaceResultJsonObj.toString(),
                DetectFaceResponse::class.java
            )
        return AMTResult(ErrorCode.ERROR_NONE, "", detectFaceResponse)
    }


    private fun filterFaceFeature(
        detectFaceResponse: DetectFaceResponse,
        userId: String,
        @RegisterOperate operate: Int,
    ): AMTResult<UserInfo?> {
        if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
            return AMTResult(404, getString(RegisterStatus.NO_DETECT_FACE), null)
        }
        if (detectFaceResponse.faces.size > 1) {
            return AMTResult(404, getString(RegisterStatus.DETECT_TOO_MUCH_FACE), null)
        }

        //set first face as max face default
        var biggestFace = detectFaceResponse.faces[0]
        //find max face
        for (face in detectFaceResponse.faces) {
            val w1 =
                abs((face.faceInfo.rect.right - face.faceInfo.rect.left).toDouble()).toInt()
            val w2 =
                abs((biggestFace.faceInfo.rect.right - biggestFace.faceInfo.rect.left).toDouble())
                    .toInt()
            if (w1 > w2) {
                biggestFace = face
            }
        }

        val faceInfo = biggestFace.faceInfo

        val faceRect = faceInfo.rect
        calEnrollArea(enrollArea, IMAGE_WIDTH, IMAGE_HEIGHT)
        val isBigFace =
            faceRect.width() > enrollArea.width() && faceRect.width() < IMAGE_WIDTH
                    && faceRect.top >= enrollArea.top && faceRect.bottom <= IMAGE_HEIGHT && faceRect.right > 0 && faceRect.right < IMAGE_WIDTH
        if (!isBigFace && !enrollArea.contains(faceRect)) {
            //not big face and not in enroll area
            return AMTResult(404, getString(RegisterStatus.FACE_NOT_IN_ENROLL_AREA), null)
        }

        //face size filter
        val face_w = faceRect.right - faceRect.left
        if (face_w < Config.instance().faceWidthMinSize) {
            return AMTResult(
                404,
                String.format(
                    getString(RegisterStatus.FACE_TOO_SMALL),
                    Config.instance().faceWidthMinSize.toFloat(),
                    face_w.toFloat()
                ), null
            )
        }

        val face_h = faceRect.bottom - faceRect.top
        if (face_h < Config.instance().faceHeightMinSize) {
            return AMTResult(
                404,
                String.format(
                    getString(RegisterStatus.FACE_TOO_SMALL),
                    Config.instance().faceHeightMinSize.toFloat(),
                    face_h.toFloat()
                ), null
            )
        }

        if (faceInfo.score < Config.instance().faceRegistrationQuality) {
            return AMTResult(
                404,
                String.format(
                    getString(RegisterStatus.FACE_QUALITY_TOO_BAD),
                    Config.instance().faceRegistrationQuality,
                    faceInfo.score
                ), null
            )
        }

        if (biggestFace.blur > Config.instance().faceBlurThreshold) {
            return AMTResult(
                404,
                String.format(
                    getString(RegisterStatus.FACE_BLUR_TOO_HIGH),
                    Config.instance().faceBlurThreshold,
                    biggestFace.blur
                ), null
            )
        }

        if (faceInfo.pose.yaw > Config.instance().faceYawMaxThreshold
            || faceInfo.pose.yaw < Config.instance().faceYawMinThreshold
        ) {
            return AMTResult(
                404,
                String.format(
                    getString(RegisterStatus.FACE_YAW_TOO_BIG),
                    Config.instance().faceYawMaxThreshold,
                    faceInfo.pose.yaw
                ), null
            )
        }

        if (faceInfo.pose.roll > Config.instance().faceRollMaxThreshold
            || faceInfo.pose.roll < Config.instance().faceRollMinThreshold
        ) {
            return AMTResult(
                404,
                String.format(
                    getString(RegisterStatus.FACE_ROLL_TOO_BIG),
                    Config.instance().faceRollMaxThreshold,
                    faceInfo.pose.roll
                ), null
            )
        }

        val faceFeature = biggestFace.feature
        val feature = faceFeature.feature
        val id = ByteArray(40)
        val score = FloatArray(1)
        if (FaceManager.getInstance().dbIdentify(feature, id, score) == FaceErrorCode.SUCCESS) {
            val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))
            if (score[0] > Config.instance().faceIdentifyThreshold) {
                if (operate == RegisterOperate.ADD) {
                    return AMTResult(
                        404,
                        String.format(
                            getString(RegisterStatus.FACE_REGISTERED),
                            identifyUserId,
                            Config.instance().faceIdentifyThreshold,
                            score[0]
                        ), null
                    )
                }
                if (operate == RegisterOperate.UPDATE) {
                    if (identifyUserId != userId) {
                        return AMTResult(
                            404,
                            String.format(
                                getString(RegisterStatus.FACE_REGISTERED),
                                identifyUserId,
                                Config.instance().faceIdentifyThreshold,
                                score[0]
                            ), null
                        )
                    }
                }
            }
        }

        val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId)
        val checkVLFeatureResult = checkVLFeature(faceFeature, userInfo, operate)
        if (checkVLFeatureResult.code != ErrorCode.ERROR_NONE) {
            return AMTResult(checkVLFeatureResult.code, checkVLFeatureResult.message, null)
        }
        return AMTResult(ErrorCode.ERROR_NONE, "success", userInfo)
    }


    private fun checkVLFeature(
        faceFeature: FaceFeature?,
        userInfo: UserInfo,
        @RegisterOperate operate: Int,
    ): AMTResult<ByteArray?> {
        if (faceFeature == null) {
            return AMTResult(
                404,
                "Feature is null",
                null
            )
        }
        //if register ,should verify for is registered person
        //do 1:N ,check if repeat
        val verifyTemplate = faceFeature.feature
        val id = ByteArray(40)
        val score = FloatArray(1)
        FaceManager.getInstance().dbIdentify(verifyTemplate, id, score)
        val faceVLRecognizeThreshold = Config.instance().faceIdentifyThreshold
        Log.d(
            TAG,
            "[checkVLFeature]: dbIdentify score=" + score[0] + ", threshold=" + faceVLRecognizeThreshold
        )
        if (score[0] > faceVLRecognizeThreshold) {
            val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))


            if (operate == RegisterOperate.ADD) {
                return AMTResult(
                    404,
                    String.format(
                        getString(RegisterStatus.FACE_REGISTERED),
                        identifyUserId,
                        faceVLRecognizeThreshold,
                        score[0]
                    ), null
                )
            }

            if (operate == RegisterOperate.UPDATE) {
                if (identifyUserId != userInfo.userId) {
                    return AMTResult(
                        404,
                        String.format(
                            getString(RegisterStatus.FACE_REGISTERED),
                            identifyUserId,
                            faceVLRecognizeThreshold, score[0]
                        ), null
                    )
                }
            }
        }

        if (mCacheFaceRecognizeDataList.size > 1) {
            //do 1:1 with first palm feature ,check if it's same palm
            val firstFaceTemplate = mCacheFaceRecognizeDataList[0].faces[0].feature.feature
            val similarity =
                FaceManager.getInstance().dbVerify(firstFaceTemplate, verifyTemplate)
            Log.d(
                TAG,
                "[checkVLFeature]: dbVerify score=$similarity, recognizeThreshold=$faceVLRecognizeThreshold"
            )
            if (similarity < faceVLRecognizeThreshold) {
                Log.w(
                    TAG,
                    "[checkVLFeature]: verify failed, score=$similarity, threshold=$faceVLRecognizeThreshold, need the same palm to finish enrolling the user"
                )
                return AMTResult(
                    404,
                    getString(RegisterStatus.ENROLL_NEED_SAME_FACE), null
                )
            }
        }
        return AMTResult(ErrorCode.ERROR_NONE, "", null)
    }

    private fun doRegisterFace(userInfo: UserInfo) {
        //find best visible palm template
        mCacheFaceRecognizeDataList.sortWith { o1, o2 -> ((o2.faces[0].faceInfo.score - o1.faces[0].faceInfo.score) * 100).toInt() }
        val detectFaceResponse = mCacheFaceRecognizeDataList[0]

        val faceFeatureBase64Str = detectFaceResponse.faces[0].feature.data
        userInfo.faceFeature = Base64.decode(faceFeatureBase64Str, Base64.DEFAULT)
        //best to save in database
        BioDataUtil.instance().updateUserInfo(userInfo)

        saveVLEnrollImage(detectFaceResponse, userInfo)
    }

    private fun saveVLEnrollImage(detectFaceResponse: DetectFaceResponse, userInfo: UserInfo) {
        val userId = userInfo.userId
        //save vlFaceImage
        val vlFaceImage = detectFaceResponse.faces[0].picture.data
        if (vlFaceImage != null) {
            val jpegData = Base64.decode(vlFaceImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            saveBimap(bitmap, userId + "_face_vl", userId)
        }
    }


    private fun saveBimap(bitmap: Bitmap?, fileName: String, pin: String) {
        if (bitmap == null) {
            Log.e(
                TAG,
                "[saveBimap]: save enroll palm pic fail,bitmap is null, pin=$pin"
            )
            return
        }
        FileUtils.saveBitmap(
            bitmap, fileName,
            FileUtils.USER_BIO_PHOTO + File.separator + pin + File.separator
        )
    }


    private fun calEnrollArea(enrollArea: Rect, width: Int, height: Int) {
        val centerX = width / 2
        val centerY = height / 2
        val fixRectLeft = width / 6
        val fixRectRight = fixRectLeft * 5
        val fixRectTop = centerY - centerX
        val fixRectBottom = centerY + centerX
        enrollArea[fixRectLeft, fixRectTop, fixRectRight] = fixRectBottom
    }
}