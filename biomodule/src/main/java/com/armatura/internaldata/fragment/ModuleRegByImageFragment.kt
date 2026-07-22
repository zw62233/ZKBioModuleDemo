package com.armatura.internaldata.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.FragmentModuleRegisterByImageBinding
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.util.BitmapUtil
import com.armatura.biomodule.util.flipAnim
import com.armatura.biomodule.util.rotate90Anim
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.internaldata.viewmodel.ModuleUserMgnViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Created by Jeremy on 2022/11/4.
 * This page is used to demonstrate how to add faces/palms to existing users in the module through pictures.
 * Not all modules support this function, please consult business.
 */
class ModuleRegByImageFragment : RegisterBaseFragment() {

    private var title: String? = null
    private var type: Int = 0
    private var userId: String = ""
    private var operate: Int = 0
    private var cachePalmImage: Image? = null
    private lateinit var binding: FragmentModuleRegisterByImageBinding
    private lateinit var viewModel: ModuleUserMgnViewModel
    private var loadingDialog: ProgressDialogFragment? = null

    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getAndroidViewModel(ModuleUserMgnViewModel::class.java)
        val fragArg = requireArguments()
        title = fragArg.getString(REGISTER_TITLE)
        type = fragArg.getInt(REGISTER_TYPE)
        userId = fragArg.getString(REGISTER_USER_ID).toString()
        operate = fragArg.getInt(REGISTER_OPERATE)
        viewModel.setRegisterOperate(userId, type, operate)
        viewModel.bNeedFeature = true
        viewModel.bNeedInfo = Config.instance().isNeedInfoWhenCacheReg
        viewModel.bNeedPicture = Config.instance().isNeedPictureWhenCacheReg

        pickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    onImagePicked(uri)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentModuleRegisterByImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.myToolBar.setTitle(title)
        binding.myToolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {}
        })
        binding.registerStatusTv.text = ""

        binding.snapShotBtn.setOnClickListener {
            if (type == RegisterType.FACE) {
                pickImage()
            } else {
                pickImage()
            }
        }
        binding.confirmBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(it: View) {
                if (type == RegisterType.FACE) {
                    if (viewModel.cacheBitmap == null) {
                        showToastMsg(getString(R.string.pls_choose_photo_first))
                        return
                    }
                    showLoadingDialog()
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.enrollFaceByBitmap(viewModel.cacheBitmap)
                    }
                } else {
                    if (viewModel.cacheBitmap == null) {
                        showToastMsg(getString(R.string.pls_choose_photo_first))
                        return
                    }
                    showLoadingDialog()
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.enrollPalmByBitmap(viewModel.cacheBitmap!!)
                    }
                }
            }
        })

        binding.btnRotateImage.setOnClickListener {
            viewModel.cacheBitmap ?: return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.cacheBitmap =
                    BitmapUtil.rotateImageView(
                        90,
                        viewModel.cacheBitmap
                    )

                withContext(Dispatchers.Main) {
                    binding.btnRotateImage.rotate90Anim()
                    binding.pickImageIv.rotate90Anim {
                        binding.pickImageIv.setImageBitmap(viewModel.cacheBitmap)
                    }
                }
            }
        }

        binding.btnFlipImage.setOnClickListener {
            viewModel.cacheBitmap ?: return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.cacheBitmap =
                    BitmapUtil.mirrorBitmapHorizontally(viewModel.cacheBitmap)
                withContext(Dispatchers.Main) {
                    binding.btnFlipImage.rotate90Anim()
                    binding.pickImageIv.flipAnim {
                        binding.pickImageIv.setImageBitmap(viewModel.cacheBitmap)
                    }
                }
            }
        }

        viewModel.registerFinishLiveData.observe(viewLifecycleOwner) {
            if (it) {
                lifecycleScope.launch {
                    dismissDialog()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }

        viewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.registerProgressBar.incrementProgressBy(it)
        }

        viewModel.statusLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.registerStatusTv.setText(it)
            }
            dismissDialog()
        }

        viewModel.toastMsgLiveData.observe(viewLifecycleOwner) {
            it?.let {
                showToastMsg(it)
            }
        }
    }

    override fun onDestroy() {
        viewModel.resetConfigs()
        dismissDialog()
        super.onDestroy()
    }

    private fun showLoadingDialog(message: String = getString(R.string.registering)) {
        ProgressDialogFragment.show(parentFragmentManager, "", message, true).also {
            loadingDialog = it
        }
    }

    private fun dismissDialog() {
        loadingDialog?.dismiss(parentFragmentManager)
    }

    private fun onImagePicked(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                binding.pickImageIv.setImageBitmap(null)
            }
            val bitmap =
                com.armatura.biomodule.util.BitmapUtil.uriToBitmapWithRotation(context, uri)
            if (bitmap == null) {
                showToastMsg("Bitmap decode error")
                return@launch
            }
            withContext(Dispatchers.Main) {
                binding.pickImageIv.setImageBitmap(bitmap)
                binding.btnRotateImage.visibility = View.VISIBLE
                binding.btnFlipImage.visibility = View.VISIBLE
            }
            viewModel.cacheBitmap = bitmap.copy(bitmap.config, false)
        }
    }


    /**
     * open image picker
     */
    private fun pickImage() {
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }


    companion object {
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
        ): ModuleRegByImageFragment {
            val fragment = ModuleRegByImageFragment()
            fragment.arguments = Bundle().apply {
                putInt(REGISTER_TYPE, type)
                putString(REGISTER_TITLE, title)
                putString(REGISTER_USER_ID, userId)
                putInt(REGISTER_OPERATE, operate)
            }
            return fragment
        }
    }
}