package com.armatura.internaldata.fragment

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.camera.CameraController
import com.armatura.biomodule.camera.ICameraView
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.FragmentModuleFace1v1ByUvcBinding
import com.armatura.biomodule.fragment.BaseFragment
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData
import com.armatura.biomodule.pojo.setting.DeviceSettings
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.register.RegisterStatusCallback
import com.armatura.biomodule.util.CustomDraw
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.view.CircleDetectView
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.internaldata.viewmodel.ModuleFace1V1ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ModuleFace1V1Fragment : BaseFragment(), SurfaceHolder.Callback, ICameraView,
    RegisterStatusCallback {
    private val displayArea = Rect()
    private val sourceArea = Rect()
    private var title: String? = null

    private var isSurfaceHolderReady = false
    private var isRectInfoSurfaceHolderReady = false

    private var isConfigSurfaceLayout = false
    private var isCalScale = false
    private var mRectInfoScale = 1.0f
    private var mLastNoRectInfoTimestamp = 0L

    private lateinit var binding: FragmentModuleFace1v1ByUvcBinding
    private val viewModel: ModuleFace1V1ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            title = arguments?.getString(REGISTER_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_module_face_1v1_by_uvc,
                container,
                false
            )
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun onResume() {
        super.onResume()
        CameraController.instance().resumeCam(this)
    }

    override fun onPause() {
        super.onPause()
        CameraController.instance().pauseCam(this)
    }


    private fun initView(view: View) {
        val surfaceHolder = binding.cameraPreviewSv.holder
        surfaceHolder.addCallback(this)
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
                height: Int
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
        binding.registerProgressBar.max = 300
        binding.btnExtractFace1.setOnClickListener {
            binding.progressExtract.visibility = View.VISIBLE
            binding.registerProgressBar.progress = 0
            lifecycleScope.launch {
                viewModel.extractFace()
                    .flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) { amtResult ->
                        with(binding) {
                            registerStatusTv.text = amtResult.message
                            registerProgressBar.progress = amtResult.result!!
                            progressExtract.visibility = View.GONE
                        }
                    }
            }

        }
        binding.btnVerifyFace.setOnClickListener {
            binding.progressExtract.visibility = View.VISIBLE
            lifecycleScope.launch {
                viewModel.verifyFace()
                    .flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) { amtResult ->
                        with(binding) {
                            registerStatusTv.text = amtResult.message
                            registerProgressBar.progress = amtResult.result!!
                            progressExtract.visibility = View.GONE
                        }
                    }
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated: ")
        isSurfaceHolderReady = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed: ")
        isSurfaceHolderReady = false
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
            binding.enrollAreaRect.visibility = View.VISIBLE
        }
    }


    override fun drawVideoData(bitmap: Bitmap?) {
        if (bitmap != null) {
            if (!isConfigSurfaceLayout) {
                initCameraPreviewView(bitmap.width, bitmap.height)
                isConfigSurfaceLayout = true
            }
            synchronized(CAMERA_SURFACE_HOLDER_LOCK) {
                if (!isSurfaceHolderReady) {
                    Log.w(TAG, "drawView: camera surface holder destroyed")
                    return
                }
                CustomDraw.drawBitmapOnly(binding.cameraPreviewSv, displayArea, bitmap)
            }
        }
    }

    override fun onImagePicked(uri: Uri) {
    }

    override fun onFilePicked(uri: Uri) {
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
                    Config.videoStreamMode == DeviceSettings.VIDEO_STREAM_MODE_DISABLE
                            || Config.videoStreamMode == DeviceSettings.VIDEO_STREAM_MODE_IR,
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


    override fun clearCustomInfoView() {
        clearRectInfoSurface()
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

    override fun onStatusCallback(status: RegisterStatus) {
        lifecycleScope.launch {
            binding.progressExtract.visibility = View.INVISIBLE
            binding.registerStatusTv.setText(RegisterStatus.getStatusString(status))
            binding.circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.ORANGE)
        }
    }

    override fun onResult(detail: String) {
        lifecycleScope.launch {
            binding.progressExtract.visibility = View.INVISIBLE
            binding.registerStatusTv.text = detail
        }
    }

    override fun onHidFailed(ret: Int) {
        lifecycleScope.launch {
            binding.progressExtract.visibility = View.INVISIBLE
            binding.registerStatusTv.text =
                String.format(getString(R.string.hid_failed), ret)
        }
    }

    override fun onSuccess(feature: ByteArray?) {
        lifecycleScope.launch {
            binding.progressExtract.visibility = View.INVISIBLE
            binding.registerProgressBar.incrementProgressBy(100)
        }
    }


    override fun onRegisterFinish() {
        lifecycleScope.launch {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "Face1V1Fragment"
        private const val REGISTER_TITLE = "title"
        private const val PREPARE_START_ENROLL_TIME = 2000L
        fun newInstance(title: String?): ModuleFace1V1Fragment {
            val fragment = ModuleFace1V1Fragment()
            val bundle = Bundle()
            bundle.putString(REGISTER_TITLE, title)
            fragment.arguments = bundle
            return fragment
        }
    }
}