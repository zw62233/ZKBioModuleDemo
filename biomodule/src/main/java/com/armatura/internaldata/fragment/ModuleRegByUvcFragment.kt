package com.armatura.internaldata.fragment

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.camera.CameraController
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.FragmentModuleRegisterByUvcBinding
import com.armatura.biomodule.fragment.BaseFragment
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData
import com.armatura.biomodule.pojo.setting.CommonSettingData
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.CustomDraw
import com.armatura.biomodule.view.CircleDetectView
import com.armatura.biomodule.view.ToolBar
import com.armatura.internaldata.viewmodel.ModuleUserMgnViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Created by Jeremy on 2022/11/4.
 * This page is for demonstration, preview and register at the same time.
 */
class ModuleRegByUvcFragment : RegisterBaseFragment() {
    private var title: String? = null
    private var type: Int = 0
    private var userId: String = ""
    private var operate: Int = 0
    private var displayArea: Rect = Rect()
    private var sourceArea: Rect = Rect()
    private lateinit var binding: FragmentModuleRegisterByUvcBinding
    private lateinit var viewModel: ModuleUserMgnViewModel
    private lateinit var surfaceHolder: SurfaceHolder
    private var isCalScale = false
    private var mRectInfoScale = 1.0f

    private var isSurfaceHolderReady = false
    private var isRectInfoSurfaceHolderReady = false

    private var isConfigSurfaceLayout = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getAndroidViewModel(ModuleUserMgnViewModel::class.java)
        arguments?.also {
            title = it.getString(REGISTER_TITLE)
            type = it.getInt(REGISTER_TYPE)
            userId = it.getString(REGISTER_USER_ID).toString()
            operate = it.getInt(REGISTER_OPERATE)
        }
        viewModel.setRegisterOperate(userId, type, operate)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_module_register_by_uvc,
            container,
            false
        )
        if (type == RegisterType.FACE) {
            binding.registerStatusTv.setText(RegisterStatus.getStatusString(RegisterStatus.PREPARE_TO_ENROLL_FACE))
        } else if (type == RegisterType.PALM) {
            binding.registerStatusTv.setText(RegisterStatus.getStatusString(RegisterStatus.PREPARE_TO_ENROLL_PALM))
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        surfaceHolder = binding.cameraPreviewSv.holder
        surfaceHolder.addCallback(this)

        if (Config.shouldUseCircleIndicatorView()) {
            binding.circleDetectView.setVisibility(View.VISIBLE)
        } else {
            binding.circleDetectView.setVisibility(View.GONE)
        }
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

        binding.topToolBar.setToolBarClickListener(object : ToolBar.ToolBarClickListener {
            override fun onClickLeft() {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {}
        })
        binding.registerProgressBar.max = if (type == RegisterType.FACE) 200 else 600

        viewModel.registerFinishLiveData.observe(viewLifecycleOwner) { t ->
            if (t) {
                lifecycleScope.launch(Dispatchers.IO) {
                    delay(500)
                    withContext(Dispatchers.Main) {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }
                }
            }
        }

        viewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.registerProgressBar.incrementProgressBy(it)
        }

        viewModel.statusLiveData.observe(viewLifecycleOwner) { status ->
            if (status.isNotEmpty()) {
                binding.registerStatusTv.text = status
            }
        }

        viewModel.countdownLiveData.observe(viewLifecycleOwner) { t ->
            var prompt: String = getString(R.string.prepare_enroll_face)
            if (type == RegisterType.PALM) {
                prompt = getString(R.string.prepare_enroll_palm)
            }
            if (t == 0L) {
                prompt = ""
            }
            binding.registerStatusTv.text = prompt.format(Locale.US, t)
        }

        viewModel.resultLiveData.observe(viewLifecycleOwner) { t ->
            binding.registerStatusTv.text = t
        }

        viewModel.toastMsgLiveData.observe(viewLifecycleOwner, object : Observer<String?> {
            override fun onChanged(value: String?) {
                value ?: return
                showToastMsg(value)
            }
        })
    }

    override fun onDestroy() {
        viewModel.resetConfigs()
        viewModel.stopAllTask()
        super.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceHolderReady = true
        super.surfaceCreated(holder)
        CameraController.instance().resumeCam(this)
        viewModel.prepareEnrollCountDown()
        viewModel.exitStandByMode()
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceHolderReady = false
        super.surfaceDestroyed(holder)
        CameraController.instance().pauseCam(this)
    }

    override fun drawVideoData(bitmap: Bitmap?) {
        bitmap?.let {
            if (!isConfigSurfaceLayout) {
                initCameraPreviewView(bitmap.width, bitmap.height)
                isConfigSurfaceLayout = true
            }
            synchronized(SURFACE_HOLDER_LOCK) {
                if (!isSurfaceHolderReady) {
                    Log.w(TAG, "drawView: surface holder destroyed")
                    return@synchronized
                }
                CustomDraw.drawBitmapOnly(binding.cameraPreviewSv, displayArea, bitmap)
            }
        }
    }


    private fun initCameraPreviewView(previewWidth: Int, previewHeight: Int) {
        binding.enrollAreaRect.post {
            sourceArea.set(0, 0, previewWidth, previewHeight)
            val displayMetrics = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getRealMetrics(displayMetrics)
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels
            val ratio = screenHeight * 1.0f / previewHeight
            val newWidth = (previewWidth * ratio).toInt()
            val newHeight = (previewHeight * ratio).toInt()
            val xDistance = ((screenWidth - newWidth) * 1.0f / 2).toInt()
            val yDistance = ((screenHeight - newHeight) * 1.0f / 2).toInt()
            val right = xDistance + newWidth
            val bottom = yDistance + newHeight
            displayArea.set(xDistance, yDistance, right, bottom)
            val layoutParams = binding.enrollAreaRect.layoutParams.apply {
                width = newWidth
                height = newHeight
            }
            binding.enrollAreaRect.layoutParams = layoutParams
            binding.enrollAreaRect.visibility =
                if (Config.shouldUseCircleIndicatorView()) View.GONE else View.VISIBLE
        }
    }

    override fun drawFaceInfo(drawFaceData: DrawFaceData?) {
        synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
            if (!isRectInfoSurfaceHolderReady) {
                Log.w(
                    TAG,
                    "drawView failed,rect info surface holder destroyed"
                )
                return
            }
            if (!isCalScale) {
                mRectInfoScale = binding.rectInfoSv.measuredWidth / 720f
                isCalScale = true
            }
            if (drawFaceData != null) {
                synchronized(BaseFragment.RECT_INFO_SURFACE_HOLDER_LOCK) {
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
                        Config.videoStreamMode == CommonSettingData.VIDEO_STREAM_MODE_DISABLE
                                || Config.videoStreamMode == CommonSettingData.VIDEO_STREAM_MODE_IR,
                        false
                    )
                }
            } else {
                clearRectInfoSurface()
            }
        }
    }

    override fun drawPalmInfo(palmRecognizeData: PalmRecognizeData?) {

        if (palmRecognizeData != null) {
            viewModel.enrollPalmByFeature(palmRecognizeData)
        }

        synchronized(RECT_INFO_SURFACE_HOLDER_LOCK) {
            if (!isRectInfoSurfaceHolderReady) {
                Log.w(
                    TAG,
                    "drawView failed,rect info surface holder destroyed"
                )
                return
            }
            if (!isCalScale) {
                mRectInfoScale = binding.rectInfoSv.measuredWidth / 720f
                isCalScale = true
            }
            if (palmRecognizeData != null) {
                synchronized(BaseFragment.RECT_INFO_SURFACE_HOLDER_LOCK) {
                    if (!isRectInfoSurfaceHolderReady) {
                        Log.w(TAG, "drawView failed,surface holder destroyed")
                        return
                    }
                    if (!isCalScale) {
                        mRectInfoScale = binding.rectInfoSv.measuredWidth / 720f
                        isCalScale = true
                    }
                    val palmInfo = palmRecognizeData.trackInfo
                    CustomDraw.drawFaceAndPalmInfo(
                        binding.rectInfoSv, mRectInfoScale, null,
                        null, palmInfo,
                        Config.videoStreamMode == CommonSettingData.VIDEO_STREAM_MODE_DISABLE
                                || Config.videoStreamMode == CommonSettingData.VIDEO_STREAM_MODE_IR,
                        false
                    )
                }
            } else {
                clearRectInfoSurface()
            }
        }
    }


    override fun clearCustomInfoView() {
        clearRectInfoSurface()
    }

    private fun clearRectInfoSurface() {
        synchronized(BaseFragment.RECT_INFO_SURFACE_HOLDER_LOCK) {
            if (!isRectInfoSurfaceHolderReady) {
                Log.w(TAG, "drawView failed,surface holder destroyed")
                return
            }
            CustomDraw.clearSurface(binding.rectInfoSv)
        }
    }

    companion object {
        private const val REGISTER_TYPE = "type"
        private const val REGISTER_TITLE = "title"
        private const val REGISTER_USER_ID = "userId"
        private const val REGISTER_OPERATE = "operate"
        private const val TAG = "ModuleRegByUvcFragment"

        @JvmStatic
        fun newInstance(
            type: Int,
            title: String?,
            userId: String?,
            operate: Int
        ): ModuleRegByUvcFragment {
            val args = Bundle().apply {
                putInt(REGISTER_TYPE, type)
                putString(REGISTER_TITLE, title)
                putString(REGISTER_USER_ID, userId)
                putInt(REGISTER_OPERATE, operate)
            }
            val fragment = ModuleRegByUvcFragment().also {
                it.arguments = args
            }
            return fragment
        }
    }
}