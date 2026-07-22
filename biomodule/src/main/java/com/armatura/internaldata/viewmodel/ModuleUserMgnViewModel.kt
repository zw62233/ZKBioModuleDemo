package com.armatura.internaldata.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.ExApplication
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.manager.PalmManager
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData
import com.armatura.biomodule.pojo.palm.register.DetectPalmRequest
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.BitmapUtil
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.util.HidHelper
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.SpeakerHelper
import com.armatura.biomodule.util.getDetail
import com.armatura.constant.ErrorCode
import com.armatura.constant.ManageType
import com.armatura.constant.SnapType
import com.armatura.constant.StatusCode
import com.armatura.internaldata.util.ModuleBioDataUtil
import com.armatura.internaldata.util.RegisterResult
import com.armatura.internaldata.util.SingleLiveEvent
import com.armatura.translib.AMTHidManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Arrays
import java.util.Locale
import kotlin.math.abs

/**
 * Created by Jeremy on 2022/11/3.
 */
class ModuleUserMgnViewModel(private val applicationCtx: Application) :
    AndroidViewModel(applicationCtx) {

    companion object {
        private const val TAG = "ModuleUserMgViewModel"
        private const val PREPARE_START_ENROLL_TIME = 4000L
        private const val MAX_STILL_COUNT = 3
        private const val MAX_CACHE_SIZE = 5
    }

    private val taskRunningFlag = BooleanArray(1) { true }

    private val mCacheRect = Rect()
    private var mPalmStillFrameCount = 0

    private val mCachePalmRecognizeDataList =
        java.util.ArrayList<PalmRecognizeData>(MAX_CACHE_SIZE)

    private var regType = 0
    private var regUserId: String? = null

    //add or update
    private var operate = 0

    var bNeedPicture: Boolean = false
    var bNeedFeature: Boolean = false
    var bNeedInfo: Boolean = false

    private var bStartEnrollPalm = false
    private var regFeatureBytes: ByteArray? = null
    private val enrollArea = Rect()

    val snapshotStartLiveData by lazy {
        SingleLiveEvent<Boolean>()
    }
    val snapshotFinishLiveData by lazy {
        SingleLiveEvent<Bitmap?>()
    }
    val progressLiveData by lazy {
        SingleLiveEvent<Int>()
    }
    val statusLiveData by lazy {
        SingleLiveEvent<String>()
    }
    val registerFinishLiveData by lazy {
        SingleLiveEvent<Boolean>()
    }
    val countdownLiveData by lazy {
        SingleLiveEvent<Long>()
    }
    val toastMsgLiveData by lazy {
        SingleLiveEvent<String?>()
    }
    val resultLiveData by lazy {
        SingleLiveEvent<String>()
    }


    var cacheBitmap: Bitmap? = null


    private val prepareEnrollCountDownTimer: CountDownTimer =
        object : CountDownTimer(PREPARE_START_ENROLL_TIME, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                countdownLiveData.postValue(millisUntilFinished / 1000)
            }

            override fun onFinish() {
                countdownLiveData.postValue(0L)
                startRegister()
            }
        }

    private fun saveUser(feature: ByteArray?) {
        if (feature == null) {
            return
        }

        val userInfo = UserInfo().apply {
            personId = regUserId
        }
        if (regType == RegisterType.FACE) {
            var bAddFailed = true
            // First, check if the face is duplicated.
            // If not duplicated, clear the current user's face data.
            // Second, add the new face data.
            for (i in 0..1) {
                //add new feature
                ModuleBioDataUtil.instance().addFaceByFeature(
                    userInfo.personId,
                    feature
                ).let { result ->
                    when (result.code) {
                        ErrorCode.ERROR_NONE -> {
                            toastMessage(result.message)
                            if (i == 0) {
                                //remove before feature
                                ModuleBioDataUtil.instance().removeFaceByPersonId(userInfo.personId)
                            } else {
                                postRegisterFinished()
                            }
                            bAddFailed = false
                        }

                        else -> {
                            updateStatus(result.message)
                            if (result.result == StatusCode.FACE_REPEATED) {
                                updateProgress(-200)
                                doRetry(800)
                            }
                        }
                    }
                }

                if (bAddFailed) {
                    break
                }
            }
        } else {
            updateStatus(RegisterStatus.REGISTING)
            if (Config.instance().is16Platform) {
                //remove before feature
                ModuleBioDataUtil.instance().removePalmByPersonId(userInfo.personId)
            }
            //add new feature
            ModuleBioDataUtil.instance().addPalmByFeature(
                userInfo.personId,
                feature
            ).let { result ->
                when (result.code) {
                    ErrorCode.ERROR_NONE -> {
                        toastMessage(result.message)
                        postRegisterFinished()
                    }

                    else -> {
                        updateStatus(result.message)
                    }
                }
            }
        }
    }

    fun startRegister() {
        when (regType) {
            RegisterType.FACE -> {
                viewModelScope.launch(Dispatchers.IO) {
                    enrollFaceByUvcStream()
                    regFeatureBytes = null
                }
            }

            RegisterType.PALM -> {
                bStartEnrollPalm = true
            }

            else -> {}
        }
    }

    fun setRegisterOperate(userId: String?, type: Int, operate: Int) {
        this.regUserId = userId
        this.regType = type
        this.operate = operate
    }

    fun doRetry(delay: Long = 0) {
        when (regType) {
            RegisterType.FACE -> {
                viewModelScope.launch(Dispatchers.IO) {
                    delay(delay)
                    enrollFaceByUvcStream()
                }
            }

            else -> {
                Log.v(TAG, "doRetry: unsupported")
            }
        }
    }

    fun prepareEnrollCountDown() {
        prepareEnrollCountDownTimer.cancel()
        prepareEnrollCountDownTimer.start()
    }

    private fun resetPalmStillFrameCount() {
        mPalmStillFrameCount = 0
    }

    fun stopAllTask() {
        taskRunningFlag[0] = false
    }

    fun resetConfigs() {
        bNeedPicture = false
        bNeedFeature = false
        bNeedInfo = false
        mCachePalmRecognizeDataList.clear()
    }

    override fun onCleared() {
        prepareEnrollCountDownTimer.cancel()
        cacheBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        super.onCleared()
    }

    private fun updateStatus(status: String) {
        statusLiveData.postValue(status)
    }

    private fun updateStatus(status: RegisterStatus) {
        statusLiveData.postValue(getString(status.resId))
    }

    private fun toastMessage(message: String) {
        if (message.isNotEmpty()) {
            toastMsgLiveData.postValue(message)
        }
    }

    private fun updateResult(result: String) {
        resultLiveData.postValue(result)
    }

    private fun updateProgress(progress: Int) {
        progressLiveData.postValue(progress)
    }

    private fun postRegisterFinished() {
        registerFinishLiveData.postValue(true)
    }

    private fun getSnapshot(@SnapType snapType: Int): Bitmap? {
        var bitmap: Bitmap? = null
        val snapData = ByteArray(2 * 1024 * 1024)
        val length = IntArray(1)
        val ret = AMTHidManager.instance().snapShot(snapType, snapData, length)
        if (ret == 0) {
            val snapshotData = JSONUtil.getSnapShotData(String(snapData, 0, length[0]))
            if (snapshotData != null) {
                val imageDataBytes = Base64.decode(snapshotData.data, Base64.NO_WRAP)
                Log.i(TAG, "getSnapshot: imageSize=" + imageDataBytes.size)
                bitmap = BitmapUtil.getBitmapFromByte(imageDataBytes)
            }
        }
        return bitmap
    }

    suspend fun enrollFaceByBitmap(bitmap: Bitmap?) {
        withContext(Dispatchers.IO) {
            if (bitmap == null || bitmap.isRecycled) {
                updateStatus("invalid bitmap")
                return@withContext
            }
            ModuleHeartbeatManager.getInstance().heatBeatStop()
            var image: Image? = null
            ModuleBioDataUtil.instance().generateFaceImage(bitmap, false).let { result ->
                when (result.code) {
                    ErrorCode.ERROR_NONE -> {
                        image = result.result
                    }

                    else -> {
                        updateStatus(result.message)
                    }
                }
            }
            if (image != null) {
                var bAddFailed = true
                // First, check if the face is duplicated.
                // If not duplicated, clear the current user's face data.
                // Second, add the new face data.
                for (i in 0..1) {
                    ModuleBioDataUtil.instance().addRegInfoByImage(
                        userPin = regUserId.toString(),
                        image = image,
                        ManageType.ADD_FACE
                    ).let { result ->
                        when (result.code) {
                            ErrorCode.ERROR_NONE -> {
                                toastMessage(result.message)
                                if (i == 0) {
                                    ModuleBioDataUtil.instance()
                                        .removeFaceByPersonId(regUserId.toString())
                                } else {
                                    postRegisterFinished()
                                }
                                bAddFailed = false
                            }

                            else -> {
                                updateStatus(result.message)
                            }
                        }
                    }

                    if (bAddFailed) {
                        break
                    }
                }
            }
        }
    }

    suspend fun enrollFaceByUvcStream() {
        withContext(Dispatchers.IO) {
            taskRunningFlag[0] = true
            ModuleBioDataUtil.instance().registerFaceByModuleInternal(
                true, taskRunningFlag
            ).flowOn(Dispatchers.IO)
                .collect { result ->
                    when (result) {
                        is RegisterResult.Status -> {
                            if (result.result != 0) {
                                updateProgress(result.result ?: 0)
                            }
                            updateStatus(result.message)
                            if (result.code != 400) {
                                updateStatus("")
                                doRetry()
                            }
                        }

                        is RegisterResult.ExtractInfo -> {
                            saveUser(Base64.decode(result.feature, Base64.NO_WRAP))
                        }

                        is RegisterResult.Progress -> {
                            if (result.result != 0) {
                                updateProgress(result.result ?: 0)
                            }
                        }
                    }
                }
        }
    }

    fun enrollPalmByFeature(palmRecognizeData: PalmRecognizeData) {
        if (!bStartEnrollPalm || mCachePalmRecognizeDataList.size >= MAX_CACHE_SIZE) {
            return
        }

        val palmRect = palmRecognizeData.trackInfo.rect
        if (palmRect != null) {
            val newRect = palmRect.rect
            calEnrollArea(enrollArea, 720, 1280)
            if (!enrollArea.contains(newRect)) {
                updateStatus(RegisterStatus.PALM_NOT_IN_ENROLL_AREA)
                resetPalmStillFrameCount()
                return
            }
            if (mCacheRect.centerX() == 0 /*first*/) {
                mCacheRect.set(newRect)
                return
            }
            if (!checkPalmImageIsStable(newRect, mCacheRect)) {
                updateStatus(RegisterStatus.DO_NOT_MOVE_PALM)
                mCacheRect.setEmpty()
                resetPalmStillFrameCount()
                Log.w(TAG, "enrollPalmByFeature: not stable")
                return
            }
            mPalmStillFrameCount++
            if (mPalmStillFrameCount < MAX_STILL_COUNT) {
                Log.i(TAG, "stable palm frame count: $mPalmStillFrameCount")
                return
            }
            resetPalmStillFrameCount()
        } else {
            updateStatus(RegisterStatus.PREPARE_TO_ENROLL_PALM)
            return
        }


        val palmFeature = palmRecognizeData.feature
        if (palmFeature != null) {
            val palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold
            if (mCachePalmRecognizeDataList.size > 1) {
                //do 1:1 with first palm feature ,check if it's same palm
                val firstPalmTemplate: ByteArray =
                    mCachePalmRecognizeDataList[0].feature.byteVerTemplate
                val scoreArr = FloatArray(1)
                val verifyRlt =
                    PalmManager.getInstance()
                        .dbVerify(firstPalmTemplate, palmFeature.byteVerTemplate, scoreArr)
                if (verifyRlt) {
                    val verifyScore = scoreArr[0]
                    Log.d(
                        TAG,
                        "[enrollPalmByFeature]: dbVerify score=$verifyScore, recognizeThreshold=$palmVLRecognizeThreshold"
                    )
                    if (verifyScore < palmVLRecognizeThreshold) {
                        Log.w(
                            TAG,
                            "[enrollPalmByFeature]: verify failed, score=$verifyScore, threshold=$palmVLRecognizeThreshold, need the same palm to finish enrolling the user"
                        )
                        updateStatus(RegisterStatus.ENROLL_NEED_SAME_PALM)
                        return
                    }
                }
            }


            //play sound
            playSoundBeep()
            mCachePalmRecognizeDataList.add(palmRecognizeData)
            progressLiveData.postValue(100)
            Log.i(
                TAG,
                String.format(
                    "enrollPalm: prepare a preTemplate,count =%d",
                    mCachePalmRecognizeDataList.size
                )
            )
            if (mCachePalmRecognizeDataList.size >= MAX_CACHE_SIZE) {
                //find best
                mCachePalmRecognizeDataList.sortWith { o1, o2 ->
                    o2.trackInfo.imageQuality - o1.trackInfo.imageQuality
                }
                val bestQualityPalm: PalmRecognizeData = mCachePalmRecognizeDataList[0]
                //best to enroll
                val bestFeature = bestQualityPalm.feature.byteVerTemplate
                progressLiveData.postValue(100)
                bStartEnrollPalm = false
                saveUser(Arrays.copyOf(bestFeature, bestFeature.size))
            }
        }
    }

    suspend fun enrollPalmByBitmap(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        ModuleHeartbeatManager.getInstance().heatBeatStop()
        val image = Image().apply {
            val scaleBitmap = BitmapUtil.scaleBitmapByWidth(bitmap, 720)
            format = Image.Format.JPEG
            width = bitmap.width
            height = bitmap.height
            bioType = BioType.PALM
            data = BitmapUtil.bitmapToBase64(scaleBitmap)
        }
        val detectPalmRequest = DetectPalmRequest().apply {
            setImages(arrayListOf(image))
            setIsNeedPalmInfo(true)
            setIsNeedPicture(false)
            setIsNeedFeature(true)
        }
        val jsonString = JSONUtil.getJsonString(detectPalmRequest)
        val resultByteArray = ByteArray(1024 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret =
            AMTHidManager.instance()
                .registerPalm(jsonString.toByteArray(), resultByteArray, resultSize)
        if (ret == 0) {
            try {
                val resultJson = String(
                    resultByteArray, 0,
                    resultSize[0]
                )
                val resultJsonObject = JSONObject(resultJson)
                if (!resultJsonObject.has("status")) {
                    updateStatus(RegisterStatus.JSON_FAILED)
                    return@withContext
                }
                if (resultJsonObject.getInt("status") != 0) {
                    val detail = resultJsonObject.getDetail()
                    updateStatus(detail)
                    return@withContext
                }
                val dataContainer = resultJsonObject.getJSONObject("data")
                if (!dataContainer.has("palms")) {
                    updateStatus("Extract failed")
                    return@withContext
                }
                val palms = dataContainer.getJSONArray("palms")
                if (palms.isNull(0)) {
                    updateStatus("")
                    return@withContext
                }
                val firstPalm = palms.getJSONObject(0)
                val feature = firstPalm.getJSONObject("feature")
                val verTemplate = feature.getString("verTemplate")
                val palmFeature = Base64.decode(verTemplate, Base64.DEFAULT)
                if (palmFeature.isNotEmpty()) {
                    if (palmFeature != null) {
                        playSoundBeep()
                        ModuleBioDataUtil.instance().addRegInfoByImage(
                            regUserId.toString(),
                            image,
                            ManageType.ADD_PALM
                        ).let { result ->
                            when (result.code) {
                                ErrorCode.ERROR_NONE -> {
                                    toastMessage(result.message)
                                    postRegisterFinished()
                                }

                                else -> {
                                    updateStatus(result.message)
                                }
                            }
                        }
                    }
                } else {
                    toastMessage("No valid palm feature")
                }

            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        } else {
            toastMessage(String.format(Locale.getDefault(), "Send failed %d", ret))
        }
    }

    suspend fun enrollPalmByImage(image: Image?) = withContext(Dispatchers.IO) {
        if (image == null) {
            Log.w(TAG, "enrollPalmByImage: image is null")
            return@withContext
        }
        if (regUserId.isNullOrEmpty()) {
            Log.w(TAG, "enrollPalmByImage: userId is null")
            return@withContext
        }
        val detectPalmRequest = DetectPalmRequest().apply {
            setImages(arrayListOf(image))
            setIsNeedPalmInfo(bNeedInfo)
            setIsNeedPicture(bNeedPicture)
            setIsNeedFeature(bNeedFeature)
        }
        val jsonString = JSONUtil.getJsonString(detectPalmRequest)
        val resultByteArray = ByteArray(1024 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        val ret = AMTHidManager.instance()
            .registerPalm(jsonString.toByteArray(), resultByteArray, resultSize)
        if (ret == 0) {
            try {
                val resultJson = String(
                    resultByteArray, 0,
                    resultSize[0]
                )
                val resultJsonObject = JSONObject(resultJson)
                if (!resultJsonObject.has("status")) {
                    updateStatus(RegisterStatus.JSON_FAILED)
                    return@withContext
                }
                if (resultJsonObject.getInt("status") != 0) {
                    val detail = resultJsonObject.getString("detail")
                    updateStatus(detail)
                    return@withContext
                }
                val dataContainer = resultJsonObject.getJSONObject("data")
                if (!dataContainer.has("palms")) {
                    updateStatus("Extract failed")
                    return@withContext
                }
                val palms = dataContainer.getJSONArray("palms")
                if (palms.isNull(0)) {
                    updateStatus("Extract failed")
                    return@withContext
                }
                val firstPalm = palms.getJSONObject(0)
                val feature = firstPalm.getJSONObject("feature")
                val verTemplate = feature.getString("verTemplate")
                val palmFeature = Base64.decode(verTemplate, Base64.DEFAULT)
                if (palmFeature.isNotEmpty()) {
                    if (palmFeature != null) {
                        playSoundBeep()
                        ModuleBioDataUtil.instance().addRegInfoByImage(
                            regUserId.toString(),
                            image,
                            ManageType.ADD_PALM
                        ).also { result ->
                            when (result.code) {
                                ErrorCode.ERROR_NONE -> {
                                    toastMessage(result.message)
                                    postRegisterFinished()
                                }

                                else -> {
                                    updateStatus(result.message)
                                }
                            }
                        }
                    }
                } else {
                    toastMessage("No valid palm feature")
                }

            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        } else {
            toastMessage(String.format(Locale.US, "Send failed %d", ret))
        }
    }

    fun snapshot(snapType: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            snapshotStartLiveData.postValue(true)
            HidHelper.exitStandByMode()
            snapshotFinishLiveData.postValue(getSnapshot(snapType))
        }
    }

    private fun calEnrollArea(enrollArea: Rect, width: Int, height: Int) {
        val fixRectLeft = width / 6
        val fixRectRight = fixRectLeft * 5
        val fixRectTop = height / 5
        val fixRectBottom = fixRectTop * 4
        enrollArea.set(fixRectLeft, fixRectTop, fixRectRight, fixRectBottom)
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

    private fun saveBimap(bitmap: Bitmap?, fileName: String, dirName: String, pin: String) {
        if (bitmap == null) {
            Log.e(
                TAG,
                "[saveBimap]: save enroll palm pic fail,bitmap is null, pin=$pin"
            )
            return
        }
        FileUtils.saveBitmap(
            bitmap,
            fileName,
            FileUtils.testPhotoPath + File.separator + dirName + File.separator
        )
    }


    private fun playSoundBeep() {
        SpeakerHelper.playSound(ExApplication.instance(), R.raw.beep)
    }

    fun exitStandByMode() {
        viewModelScope.launch(Dispatchers.IO) {
            HidHelper.exitStandByMode()
        }
    }
}

