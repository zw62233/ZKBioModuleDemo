package com.armatura.biomodule.fragment

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.CardInfo
import com.armatura.biomodule.bean.IdentifyFailedData
import com.armatura.biomodule.camera.CameraController
import com.armatura.biomodule.camera.ICameraView
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.FragmentRegisterByUvcBinding
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData
import com.armatura.biomodule.pojo.setting.CommonSettingData
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.CustomDraw
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.view.CircleDetectView
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.biomodule.viewmodel.AddFaceViewModel
import com.armatura.biomodule.viewmodel.AddPalmViewModel
import com.armatura.biomodule.viewmodel.PalmEnrollData
import com.armatura.constant.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Created by Magic on 2020/9/17
 * Register by UVC Stream , which image format is MJpeg
 */
class RegisterByUvcFragment : BaseFragmentEX(), SurfaceHolder.Callback, ICameraView {

    private lateinit var title: String
    private var type = 0
    private lateinit var userId: String
    private var operate = 0 //add or update


    private var isSurfaceHolderReady = false
    private var isRectInfoSurfaceHolderReady = false

    private var isConfigSurfaceLayout = false
    private var isCalScale = false
    private var mRectInfoScale = 1.0f
    private var mLastNoRectInfoTimestamp = 0L

    private val displayArea = Rect()
    private val sourceArea = Rect()

    private lateinit var binding: FragmentRegisterByUvcBinding
    private val addFaceViewModel by viewModels<AddFaceViewModel>()
    private val addPalmViewModel by viewModels<AddPalmViewModel>()


    private val prepareEnrollCountDownTimer: CountDownTimer = object : CountDownTimer(
        PREPARE_START_ENROLL_TIME, 1000L
    ) {
        override fun onTick(millisUntilFinished: Long) {
            var prompt = getString(R.string.prepare_enroll_face)
            if (type == RegisterType.PALM) {
                prompt = getString(R.string.prepare_enroll_palm)
            }
            binding.registerStatusTv.text = String.format(
                Locale.US,
                prompt, millisUntilFinished / 1000
            )
        }

        override fun onFinish() {
            binding.registerStatusTv.text = ""
            startEnroll()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.also {
            title = it.getString(REGISTER_TITLE)!!
            type = it.getInt(REGISTER_TYPE)
            userId = it.getString(REGISTER_USER_ID)!!
            operate = it.getInt(REGISTER_OPERATE)
        }
    }

    override fun onImagePicked(uri: Uri?) {
    }

    override fun onFilePicked(uri: Uri?) {
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_register_by_uvc,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        if (type == RegisterType.FACE) {
            binding.registerStatusTv.text = getString(RegisterStatus.PREPARE_TO_ENROLL_FACE)
        } else if (type == RegisterType.PALM) {
            binding.registerStatusTv.text = getString(RegisterStatus.PREPARE_TO_ENROLL_PALM)
        }
        addFaceViewModel.enrollStateFlow.safeCollect(lifecycle, lifecycleScope) {
            delay(1000)
            withContext(Dispatchers.Main) {
                if (it == AddFaceViewModel.ENROLL_STATE_TIME_OUT) {
                    binding.registerStatusTv.text = getString(R.string.register_timeout)
                }
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
        addPalmViewModel.enrollStateFlow.safeCollect(lifecycle, lifecycleScope) {
            delay(1000)
            withContext(Dispatchers.Main) {
                if (it == AddPalmViewModel.ENROLL_STATE_TIME_OUT) {
                    binding.registerStatusTv.text = getString(R.string.register_timeout)
                }
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CameraController.instance().resumeCam(this)
    }

    override fun onPause() {
        super.onPause()
        CameraController.instance().pauseCam(this)
    }


    private fun initView() {
        binding.cameraPreviewSv.holder.addCallback(this)
        binding.rectInfoSv.setZOrderOnTop(true)
        binding.rectInfoSv.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
                    isRectInfoSurfaceHolderReady = true
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
                    isRectInfoSurfaceHolderReady = false
                }
            }
        })
        binding.myToolBar.setTitle(title)
        binding.myToolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                //interrupt current register
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {
            }
        })
        binding.registerProgressBar.max = 500
        binding.circleDetectView.visibility = if (Config.shouldUseCircleIndicatorView()) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated: ")
        addFaceViewModel.exitStandByMode()
        isSurfaceHolderReady = true
        prepareEnrollCountDownTimer.cancel()
        prepareEnrollCountDownTimer.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed: ")
        isSurfaceHolderReady = false
        prepareEnrollCountDownTimer.cancel()
    }


    private fun initCameraPreviewView(capw: Int, caph: Int) {
        binding.enrollAreaRect.post {
            val displayMetrics = DisplayMetrics()
            sourceArea[0, 0, capw] = caph
            activity?.windowManager?.defaultDisplay?.getRealMetrics(displayMetrics)
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels

            val ratio = screenHeight * 1.0f / caph
            Log.d(TAG, "drawView: ratio=$ratio")
            val newWidth = (capw * ratio).toInt()
            val newHeight = (caph * ratio).toInt()
            val xDistance = ((screenWidth - newWidth) * 1.0f / 2).toInt()
            val yDistance = ((screenHeight - newHeight) * 1.0f / 2).toInt()
            val right = xDistance + newWidth
            val bottom = yDistance + newHeight
            displayArea[xDistance, yDistance, right] = bottom
            val layoutParams = binding.enrollAreaRect.layoutParams
            layoutParams.width = newWidth
            layoutParams.height = newHeight
            binding.enrollAreaRect.layoutParams = layoutParams
            binding.enrollAreaRect.visibility =
                if (Config.shouldUseCircleIndicatorView()) View.INVISIBLE else View.VISIBLE
        }
    }


    override fun drawVideoData(bitmap: Bitmap?) {
        if (bitmap != null) {
            if (!isConfigSurfaceLayout) {
                initCameraPreviewView(bitmap.width, bitmap.height)
                isConfigSurfaceLayout = true
            }
            //            if (type == RegisterType.FACE && startSendFacePhoto) {
//                handler.obtainMessage(AddFaceHandler.MSG_ADD_FACE_FORM_VIDEO,
//                        bitmap.copy(bitmap.getConfig(), true)).sendToTarget();
//                //after send a face photo to BioModule, should wait until receive result
//                // on onStatusCallback if register face failure
//                startSendFacePhoto = false;
//            }
            synchronized(CAMERA_SURFACE_HOLDER_LOCK) {
                if (!isSurfaceHolderReady) {
                    Log.w(TAG, "drawView: camera surface holder destroyed")
                    return
                }
                CustomDraw.drawBitmapOnly(binding.cameraPreviewSv, displayArea, bitmap)
            }
        }
    }

    private fun startEnroll() {
        when (type) {
            RegisterType.PALM -> {
                addPalmViewModel.enrollPalmByFlow()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .onEach {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            binding.registerProgressBar.incrementProgressBy(100)
                            val progress = binding.circleDetectView.progress() + 100
                            binding.circleDetectView.updateProgress(
                                progress,
                                binding.registerProgressBar.max
                            )
                        }
                        binding.registerStatusTv.text = it.message
                    }.launchIn(lifecycleScope)
            }

            RegisterType.FACE -> {
                addFaceViewModel.enrollFaceByFlow(userId, operate)
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .onEach {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            binding.registerProgressBar.incrementProgressBy(100)
                        }
                        binding.registerStatusTv.text = it.message
                    }.launchIn(lifecycleScope)
            }

            else -> {
                throw UnsupportedOperationException("Unknown register type!")
            }
        }
    }


    override fun drawFaceInfo(drawFaceData: DrawFaceData?) {
        //only biometric info
        if (drawFaceData != null) {
            synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
                if (!isRectInfoSurfaceHolderReady) {
                    Log.w(TAG, "drawView failed,surface holder destroyed")
                    return
                }
                if (!isCalScale) {
                    mRectInfoScale = binding.rectInfoSv.measuredWidth / 720f
                    isCalScale = true
                }
                CustomDraw.drawFaceAndPalmInfo(
                    binding.rectInfoSv, mRectInfoScale, drawFaceData,
                    null, null,
                    Config.shouldUseCircleIndicatorView(),
                    false
                )
            }
        } else {
            if (mLastNoRectInfoTimestamp == 0L) {
                clearRectInfoSurface()
                mLastNoRectInfoTimestamp = SystemClock.elapsedRealtime()
            } else if (SystemClock.elapsedRealtime() - mLastNoRectInfoTimestamp > 500) {
                clearRectInfoSurface()
                mLastNoRectInfoTimestamp = SystemClock.elapsedRealtime()
            }
        }
    }

    override fun drawPalmInfo(palmRecognizeData: PalmRecognizeData?) {
        if (type == RegisterType.PALM) {
            palmRecognizeData?.let {
                if (it.feature != null || it.featureVein != null) {
                    addPalmViewModel.offerPalmRecognizeData(
                        PalmEnrollData(
                            userId, operate, it
                        )
                    )
                }
            }
        }
        var palmInfo: PalmInfo? = null
        if (palmRecognizeData != null) {
            palmInfo = palmRecognizeData.trackInfo
            changeCircleDetectViewIndicator(palmInfo)
        }
        //only biometric info
        if (palmInfo != null) {
            synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
                if (!isRectInfoSurfaceHolderReady) {
                    Log.w(TAG, "drawView failed,surface holder destroyed")
                    return
                }
                if (!isCalScale) {
                    mRectInfoScale = binding.rectInfoSv.measuredWidth / 720f
                    isCalScale = true
                }
                CustomDraw.drawFaceAndPalmInfo(
                    binding.rectInfoSv, mRectInfoScale, null,
                    null, palmInfo,
                    Config.shouldUseCircleIndicatorView(),
                    false
                )
            }
        } else {
            if (mLastNoRectInfoTimestamp == 0L) {
                clearRectInfoSurface()
                mLastNoRectInfoTimestamp = SystemClock.elapsedRealtime()
            } else if (SystemClock.elapsedRealtime() - mLastNoRectInfoTimestamp > 500) {
                clearRectInfoSurface()
                mLastNoRectInfoTimestamp = SystemClock.elapsedRealtime()
            }
        }
    }

    override fun onIdentifyFailed(identifyFailedData: IdentifyFailedData?) {
    }

    override fun clearCustomInfoView() {
        clearRectInfoSurface()
    }

    override fun clearVideoDataView() {
    }

    override fun onFPSUpdate(fps: IntArray?) {
    }

    override fun onInfraredDistance(distance: Int) {
    }

    override fun onCardInfo(cardInfo: CardInfo?) {
    }

    private fun changeCircleDetectViewIndicator(palmInfo: PalmInfo?) {
        if (palmInfo != null) {
            val imageQuality = palmInfo.imageQuality
            if (imageQuality != 0
                && imageQuality < Config.instance().palmImageQualityThreshold
            ) {
                binding.circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.ORANGE)
                return
            }

            val liveScore = palmInfo.liveScore
            if ((liveScore == -3f) || liveScore >= 0 && liveScore < Config.instance().palmVLLivenessThreshold) {
                binding.circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.RED)
                return
            }
            binding.circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.PROGRESS)
        } else {
            binding.circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.PROGRESS)
        }
    }

    private fun clearRectInfoSurface() {
        synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
            if (!isRectInfoSurfaceHolderReady) {
                Log.w(TAG, "drawView failed,surface holder destroyed")
                return
            }
            CustomDraw.clearSurface(binding.rectInfoSv)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prepareEnrollCountDownTimer.cancel()
    }

    companion object {
        private const val TAG = "RegisterFragment"
        private const val REGISTER_TYPE = "type"
        private const val REGISTER_TITLE = "title"
        private const val REGISTER_USER_ID = "userId"
        private const val REGISTER_OPERATE = "operate"
        private const val PREPARE_START_ENROLL_TIME = 4000L
        fun newInstance(
            type: Int,
            title: String?,
            userId: String?,
            operate: Int,
        ): RegisterByUvcFragment {
            val fragment = RegisterByUvcFragment()
            val args = Bundle()
            args.putInt(REGISTER_TYPE, type)
            args.putString(REGISTER_TITLE, title)
            args.putString(REGISTER_USER_ID, userId)
            args.putInt(REGISTER_OPERATE, operate)
            fragment.arguments = args
            return fragment
        }
    }
}