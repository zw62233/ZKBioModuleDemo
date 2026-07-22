package com.armatura.biomodule.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.databinding.FragmentRegisterByImageBinding
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.util.BitmapUtil
import com.armatura.biomodule.util.flipAnim
import com.armatura.biomodule.util.rotate90Anim
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.biomodule.viewmodel.DBViewModel
import com.armatura.biomodule.viewmodel.ModuleViewModel
import com.armatura.constant.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Magic on 2020/9/17
 */
class RegisterByImageFragment : BaseFragmentEX() {
    private lateinit var title: String
    private var type: Int = 0
    private lateinit var userId: String
    private var operate: Int = 0


    private lateinit var binding: FragmentRegisterByImageBinding
    private val moduleViewModel by viewModels<ModuleViewModel>()
    private val dbViewModel by viewModels<DBViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.also {
            title = it.getString(REGISTER_TITLE)!!
            type = it.getInt(REGISTER_TYPE)
            userId = it.getString(REGISTER_USER_ID)!!
            operate = it.getInt(REGISTER_OPERATE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_register_by_image,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initView() {
        binding.myToolBar.setTitle(title)
        binding.myToolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {
            }
        })

        binding.registerProgressBar.max = 100
        binding.snapShotBtn.setOnClickListener {
            pickImage()
        }
        binding.confirmBtn.setOnClickListener {
            moduleViewModel.cacheVLBitmap ?: return@setOnClickListener
            showLoadingDialog()
            when (type) {
                RegisterType.FACE -> {
                    moduleViewModel.registerFaceByFlow(moduleViewModel.cacheVLBitmap!!)
                        .flowOn(Dispatchers.IO)
                        .flatMapConcat {
                            if (it.code == ErrorCode.ERROR_NONE) {
                                dbViewModel.addFaceToUserByFlow(userId, it.result!!, operate)
                            } else {
                                flowOf(it)
                            }
                        }.flowOn(Dispatchers.IO)
                        .safeCollect(lifecycle, lifecycleScope) {
                            dismissLoadingDialog()
                            binding.registerStatusTv.text = it.message
                            if (it.code != ErrorCode.SUCCESS) {
                                return@safeCollect
                            }
                            binding.registerProgressBar.progress = 100
                            delay(1000)
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                }

                RegisterType.PALM -> {
                    moduleViewModel.registerPalmByFlow(moduleViewModel.cacheVLBitmap!!)
                        .flowOn(Dispatchers.IO)
                        .flatMapConcat {
                            if (it.code == ErrorCode.ERROR_NONE) {
                                dbViewModel.addPalmToUserByFlow(userId, it.result!!, operate)
                            } else {
                                flowOf(it)
                            }
                        }.flowOn(Dispatchers.IO)
                        .safeCollect(lifecycle, lifecycleScope) {
                            dismissLoadingDialog()
                            binding.registerStatusTv.text = it.message
                            if (it.code != ErrorCode.SUCCESS) {
                                return@safeCollect
                            }
                            binding.registerProgressBar.progress = 100
                            delay(1000)
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                }
            }
        }

        binding.btnRotateImage.setOnClickListener {
            moduleViewModel.cacheVLBitmap ?: return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                moduleViewModel.cacheVLBitmap =
                    BitmapUtil.rotateImageView(90, moduleViewModel.cacheVLBitmap)

                withContext(Dispatchers.Main) {
                    binding.btnRotateImage.rotate90Anim()
                    binding.pickImageIv.rotate90Anim {
                        binding.pickImageIv.setImageBitmap(moduleViewModel.cacheVLBitmap)
                    }
                }
            }
        }

        binding.btnFlipImage.setOnClickListener {
            moduleViewModel.cacheVLBitmap ?: return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                moduleViewModel.cacheVLBitmap =
                    BitmapUtil.mirrorBitmapHorizontally(moduleViewModel.cacheVLBitmap)
                withContext(Dispatchers.Main) {
                    binding.btnFlipImage.rotate90Anim()
                    binding.pickImageIv.flipAnim {
                        binding.pickImageIv.setImageBitmap(moduleViewModel.cacheVLBitmap)
                    }
                }
            }
        }
    }


    override fun onFilePicked(uri: Uri?) {
        if (uri == null) {
            return
        }
        val context = requireContext()
        try {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                if (inputStream != null) {
                    val rawGray = ByteArray(inputStream.available())
                    inputStream.read(rawGray)
                    //palm image must be width = 720 and height = 1280
                    val image = Image()
                    image.bioType = BioType.PALM
                    image.data = Base64.encodeToString(rawGray, Base64.NO_WRAP)
                    image.format = Image.Format.JPEG
                    image.width = 720
                    image.height = 1280
                    val bitmap = image.bitmap
                    lifecycleScope.launch {
                        binding.pickImageIv.setImageBitmap(bitmap)
                    }
                    //cache
                    moduleViewModel.cacheVLBitmap = bitmap.copy(bitmap.config, false)
                }
            }
        } catch (ignore: Exception) {
            toastAnywhere("Pick File data error")
        }
    }

    override fun onImagePicked(uri: Uri?) {
        if (uri == null) {
            Log.e(TAG, "onImagePicked: invalid uri")
            return
        }
        val context = requireContext()
        try {
            val bitmap = BitmapUtil.uriToBitmapWithRotation(context, uri)
            if (bitmap == null) {
                toastAnywhere("Bitmap decode error")
                return
            }
            lifecycleScope.launch {
                binding.pickImageIv.setImageBitmap(bitmap)
                binding.btnRotateImage.visibility = View.VISIBLE
                binding.btnFlipImage.visibility = View.VISIBLE
            }
            moduleViewModel.cacheVLBitmap = bitmap.copy(bitmap.config, false)
        } catch (e: Exception) {
            Log.e(TAG, "onImagePicked: ", e)
            toastAnywhere("Pick picture data error")
        }
    }

    companion object {
        private const val TAG = "RegisterByImageFragment"
        private const val REGISTER_TYPE = "type"
        private const val REGISTER_TITLE = "title"
        private const val REGISTER_USER_ID = "userId"
        private const val REGISTER_OPERATE = "operate"


        @JvmStatic
        fun newInstance(
            type: Int,
            title: String?,
            userId: String?,
            operate: Int,
        ): RegisterByImageFragment {
            val fragment = RegisterByImageFragment()
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