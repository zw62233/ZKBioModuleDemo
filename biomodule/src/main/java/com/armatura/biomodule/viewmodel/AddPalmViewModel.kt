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
import com.armatura.biomodule.manager.NIRPalmManager
import com.armatura.biomodule.manager.PalmManager
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData
import com.armatura.biomodule.pojo.setting.VLPalmSetting
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.util.SpeakerHelper
import com.armatura.constant.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class PalmEnrollData(
    val userId: String,
    val operate: Int,
    val palmRecognizeData: PalmRecognizeData,
)

class AddPalmViewModel : BaseRegisterViewModel() {
    companion object {
        private const val TAG = "AddPalmViewModel"
        const val MAX_CACHE_SIZE: Int = 5
        const val IMAGE_WIDTH: Int = 720
        const val IMAGE_HEIGHT: Int = 1280
        const val TIMEOUT = 15_000
        const val ENROLL_STATE_TIME_OUT = 1001
        const val ENROLL_STATE_OK = 1002
        const val MAX_STILL_COUNT: Int = 2
    }

    private val mCachePalmRecognizeDataList =
        mutableListOf<PalmRecognizeData>()
    private val enrollArea = Rect()
    protected val _enrollStateFlow = MutableSharedFlow<Int>()
    val enrollStateFlow = _enrollStateFlow.asSharedFlow()

    private val _palmRecognizeDataQueue = LinkedBlockingQueue<PalmEnrollData>(1)
    private val mCacheRect = Rect()
    private var mPalmStillFrameCount = 0

    fun offerPalmRecognizeData(palmRecognizeData: PalmEnrollData) {
        _palmRecognizeDataQueue.offer(palmRecognizeData)
    }

    fun enrollPalmByFlow(): Flow<AMTResult<Int>> {
        _palmRecognizeDataQueue.clear()
        return flow {
            var startTime = SystemClock.elapsedRealtime()
            do {
                val palmEnrollData = _palmRecognizeDataQueue.poll(20, TimeUnit.MILLISECONDS)
                if (SystemClock.elapsedRealtime() - startTime > TIMEOUT) {
                    _enrollStateFlow.emit(ENROLL_STATE_TIME_OUT)
                    break
                }
                if (mCachePalmRecognizeDataList.size >= MAX_CACHE_SIZE) {
                    _enrollStateFlow.emit(ENROLL_STATE_OK)
                    break
                }
                if (palmEnrollData == null) {
                    continue
                }
                val enrollPalmByFeatureResult = enrollPalmByFeature(palmEnrollData)
                if (enrollPalmByFeatureResult.code == ErrorCode.ERROR_NONE) {
                    //reset timeout
                    startTime = SystemClock.elapsedRealtime()
                } else {
                    //let msg show
                    kotlinx.coroutines.delay(500)
                }
                emit(enrollPalmByFeatureResult)
            } while (true)
        }
            .flowOn(Dispatchers.IO)
    }

    private fun enrollPalmByFeature(palmEnrollData: PalmEnrollData): AMTResult<Int> {
        val palmRect = palmEnrollData.palmRecognizeData.trackInfo.rect
            ?: return AMTResult(
                404,
                getString(RegisterStatus.PREPARE_TO_ENROLL_PALM),
                mCachePalmRecognizeDataList.size
            )
        val newRect = palmRect.rect
        calEnrollArea(enrollArea, IMAGE_WIDTH, IMAGE_HEIGHT)
        if (!enrollArea.contains(newRect)) {
            resetPalmStillFrameCount()
            return AMTResult(
                404,
                getString(RegisterStatus.PALM_NOT_IN_ENROLL_AREA),
                mCachePalmRecognizeDataList.size
            )
        }
        if (mCacheRect.centerX() == 0 /*first*/) {
            mCacheRect.set(newRect)
            return AMTResult(404, "", mCachePalmRecognizeDataList.size)
        }
        if (!checkPalmImageIsStable(newRect, mCacheRect)) {
            mCacheRect.setEmpty()
            resetPalmStillFrameCount()
            return AMTResult(
                404,
                getString(RegisterStatus.DO_NOT_MOVE_PALM),
                mCachePalmRecognizeDataList.size
            )
        }
        mPalmStillFrameCount++
        if (mPalmStillFrameCount < MAX_STILL_COUNT) {
            return AMTResult(404, "", mCachePalmRecognizeDataList.size)
        }
        resetPalmStillFrameCount()

        val result = when (Config.palmTemplateMode) {
            VLPalmSetting.PALM_TEMPLATE_MODE_IR -> {
                val checkIRFeatureResult = checkIRFeature(
                    palmEnrollData.palmRecognizeData,
                    palmEnrollData.userId,
                    palmEnrollData.operate
                )
                if (checkIRFeatureResult.result!!) {
                    mCachePalmRecognizeDataList.add(palmEnrollData.palmRecognizeData)
                    SpeakerHelper.playSound(instance(), R.raw.beep)
                    AMTResult(
                        ErrorCode.ERROR_NONE,
                        "success",
                        mCachePalmRecognizeDataList.size
                    )
                } else {
                    AMTResult(
                        checkIRFeatureResult.code,
                        checkIRFeatureResult.message,
                        mCachePalmRecognizeDataList.size
                    )
                }
            }

            VLPalmSetting.PALM_TEMPLATE_MODE_VL -> {
                val checkVLFeature = checkVLFeature(
                    palmEnrollData.palmRecognizeData,
                    palmEnrollData.userId,
                    palmEnrollData.operate
                )
                if (checkVLFeature.result!!) {
                    mCachePalmRecognizeDataList.add(palmEnrollData.palmRecognizeData)
                    SpeakerHelper.playSound(instance(), R.raw.beep)
                    AMTResult(
                        ErrorCode.ERROR_NONE,
                        "success",
                        mCachePalmRecognizeDataList.size
                    )
                } else {
                    AMTResult(
                        checkVLFeature.code,
                        checkVLFeature.message,
                        mCachePalmRecognizeDataList.size
                    )
                }
            }

            VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR -> {
                val vlCheck = checkVLFeature(
                    palmEnrollData.palmRecognizeData,
                    palmEnrollData.userId,
                    palmEnrollData.operate
                )
                if (!vlCheck.result!!) {
                    AMTResult(
                        vlCheck.code,
                        vlCheck.message,
                        mCachePalmRecognizeDataList.size
                    )
                }
                val irCheck = checkIRFeature(
                    palmEnrollData.palmRecognizeData,
                    palmEnrollData.userId,
                    palmEnrollData.operate
                )
                if (!irCheck.result!!) {
                    AMTResult(
                        irCheck.code,
                        irCheck.message,
                        mCachePalmRecognizeDataList.size
                    )
                } else {
                    mCachePalmRecognizeDataList.add(palmEnrollData.palmRecognizeData)
                    SpeakerHelper.playSound(instance(), R.raw.beep)
                    AMTResult(
                        ErrorCode.ERROR_NONE,
                        "success",
                        mCachePalmRecognizeDataList.size
                    )
                }
            }

            else -> throw UnsupportedOperationException("Unsupported palmTemplateMode " + Config.palmTemplateMode)
        }

        if (mCachePalmRecognizeDataList.size >= MAX_CACHE_SIZE) {
            val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(palmEnrollData.userId)
            doRegisterPalm(userInfo)
        }
        return result
    }

    private fun resetPalmStillFrameCount() {
        mPalmStillFrameCount = 0
    }

    private fun doRegisterPalm(userInfo: UserInfo) {
        //find best visible palm template
        mCachePalmRecognizeDataList.sortWith { o1, o2 -> o2.trackInfo.imageQuality - o1.trackInfo.imageQuality }
        val bestQualityPalm = mCachePalmRecognizeDataList[0]

        //best to save in database
        when (Config.palmTemplateMode) {
            VLPalmSetting.PALM_TEMPLATE_MODE_IR -> {
                userInfo.palmFeature2 = bestQualityPalm.featureVein.byteVerTemplate
                saveIREnrollImage(bestQualityPalm, userInfo)
            }

            VLPalmSetting.PALM_TEMPLATE_MODE_VL -> {
                userInfo.palmFeature1 = bestQualityPalm.feature.byteVerTemplate
                saveVLEnrollImage(bestQualityPalm, userInfo)
            }

            VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR -> {
                userInfo.palmFeature1 = bestQualityPalm.feature.byteVerTemplate
                saveIREnrollImage(bestQualityPalm, userInfo)
                userInfo.palmFeature2 = bestQualityPalm.featureVein.byteVerTemplate
                saveVLEnrollImage(bestQualityPalm, userInfo)
            }

            else -> throw UnsupportedOperationException("Unsupported palmTemplateMode " + Config.palmTemplateMode)
        }

        BioDataUtil.instance().updateUserInfo(userInfo)
    }


    private fun saveVLEnrollImage(palmRecognizeData: PalmRecognizeData, userInfo: UserInfo) {
        val userId = userInfo.userId
        //save vlPalmImage
        val vlPalmImage = palmRecognizeData.feature.image
        if (vlPalmImage != null) {
            val jpegData = Base64.decode(vlPalmImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            saveBimap(bitmap, userId + "_palm_vl", userId)
        }
    }

    private fun saveIREnrollImage(palmRecognizeData: PalmRecognizeData, userInfo: UserInfo) {
        val userId = userInfo.userId
        val nirPalmImage = palmRecognizeData.featureVein.image
        if (nirPalmImage != null) {
            val jpegData = Base64.decode(nirPalmImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            saveBimap(bitmap, userId + "_palm_ir", userId)
        }
    }

    private fun checkVLFeature(
        palmRecognizeData: PalmRecognizeData,
        userId: String,
        operate: Int,
    ): AMTResult<Boolean> {
        val palmFeature =
            palmRecognizeData.feature ?: return AMTResult(404, " NO VL Feature", false)
        //if register ,should verify for is registered person
        //do 1:N ,check if repeat
        val verifyTemplate = palmFeature.byteVerTemplate
        val id = ByteArray(40)
        val score = FloatArray(1)
        PalmManager.getInstance().dbIdentify(verifyTemplate, id, score)
        val palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold
        Log.d(
            TAG,
            "[checkVLFeature]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold
        )
        if (score[0] > Config.instance().palmVLIdentifyThreshold) {
            val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))
            if (operate == RegisterOperate.ADD) {
                return AMTResult(
                    404, String.format(
                        getString(RegisterStatus.PALM_REGISTERED), identifyUserId,
                        Config.instance().palmVLIdentifyThreshold, score[0] * 1.0F
                    ), false
                )
            }
            if (operate == RegisterOperate.UPDATE) {
                if (identifyUserId != userId) {
                    return AMTResult(
                        404, String.format(
                            getString(RegisterStatus.PALM_REGISTERED), identifyUserId,
                            Config.instance().palmVLIdentifyThreshold, score[0] * 1.0F
                        ), false
                    )
                }
            }
        }

        if (mCachePalmRecognizeDataList.size > 1) {
            //do 1:1 with first palm feature ,check if it's same palm
            val firstPalmTemplate = mCachePalmRecognizeDataList[0].feature.byteVerTemplate
            val scoreArr = FloatArray(1)
            val verifyRlt =
                PalmManager.getInstance().dbVerify(firstPalmTemplate, verifyTemplate, scoreArr)
            if (verifyRlt) {
                val verifyScore = scoreArr[0]
                Log.d(
                    TAG,
                    "[checkVLFeature]: dbVerify score=$verifyScore, recognizeThreshold=$palmVLRecognizeThreshold"
                )
                if (verifyScore < palmVLRecognizeThreshold) {
                    Log.w(
                        TAG,
                        "[checkVLFeature]: verify failed, score=$verifyScore, threshold=$palmVLRecognizeThreshold, need the same palm to finish enrolling the user"
                    )
                    return AMTResult(404, getString(RegisterStatus.ENROLL_NEED_SAME_PALM), false)
                }
            }
        }
        return AMTResult(ErrorCode.ERROR_NONE, "", true)
    }

    private fun checkIRFeature(
        palmRecognizeData: PalmRecognizeData,
        userId: String,
        operate: Int,
    ): AMTResult<Boolean> {
        val palmVein =
            palmRecognizeData.featureVein ?: return AMTResult(404, "NO IR Feature", false)
        //if register ,should verify for is registered person
        //do 1:N ,check if repeat
        val verifyTemplate = palmVein.byteVerTemplate
        val id = ByteArray(40)
        val score = FloatArray(1)
        NIRPalmManager.getInstance().dbIdentify(verifyTemplate, id, score)
        val palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold
        Log.d(
            TAG,
            "[checkIRFeature]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold
        )
        if (score[0] > Config.instance().palmVLIdentifyThreshold) {
            val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))
            if (operate == RegisterOperate.ADD) {
                return AMTResult(
                    404, String.format(
                        getString(RegisterStatus.PALM_REGISTERED),
                        identifyUserId,
                        Config.instance().palmVLIdentifyThreshold, score[0]
                    ), false
                )
            }
            if (operate == RegisterOperate.UPDATE) {
                if (identifyUserId != userId) {
                    return AMTResult(
                        404, String.format(
                            getString(RegisterStatus.PALM_REGISTERED),
                            identifyUserId,
                            Config.instance().palmVLIdentifyThreshold, score[0]
                        ), false
                    )
                }
            }
        }

        if (mCachePalmRecognizeDataList.size > 1) {
            //do 1:1 with first palm feature ,check if it's same palm
            val firstPalmTemplate = mCachePalmRecognizeDataList[0].featureVein.byteVerTemplate
            val scoreArr = FloatArray(1)
            val verifyRlt = NIRPalmManager.getInstance()
                .dbVerify(firstPalmTemplate, verifyTemplate, scoreArr)
            if (verifyRlt) {
                val verifyScore = scoreArr[0]
                Log.d(
                    TAG,
                    "[checkIRFeature]: dbVerify score=$verifyScore, recognizeThreshold=$palmVLRecognizeThreshold"
                )
                if (verifyScore < palmVLRecognizeThreshold) {
                    Log.w(
                        TAG,
                        "[checkIRFeature]: verify failed, score=$verifyScore, threshold=$palmVLRecognizeThreshold, need the same palm to finish enrolling the user"
                    )
                    return AMTResult(
                        404, String.format(
                            getString(RegisterStatus.ENROLL_NEED_SAME_PALM)
                        ), false
                    )
                }
            }
        }
        return AMTResult(ErrorCode.ERROR_NONE, "", true)
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
        val fixRectLeft = width / 6
        val fixRectRight = fixRectLeft * 5
        val fixRectTop = height / 5
        val fixRectBottom = fixRectTop * 4
        enrollArea[fixRectLeft, fixRectTop, fixRectRight] = fixRectBottom
    }

    private fun checkPalmImageIsStable(newRect: Rect, oldRect: Rect): Boolean {
        val xMoveDistance = abs((newRect.centerX() - oldRect.centerX()).toDouble()).toInt()
        val yMoveDistance = abs((newRect.centerY() - oldRect.centerY()).toDouble()).toInt()
        if (xMoveDistance > Config.palmMoveDistanceThreshold) {
            return false
        }
        if (yMoveDistance > Config.palmMoveDistanceThreshold) {
            return false
        }
        Log.i(
            TAG,
            "checkPalmImageIsStable: xMoveDistance=$xMoveDistance,yMoveDistance=$yMoveDistance"
        )
        return true
    }
}