package com.armatura.internaldata.fragment

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.FragmentModuleRegisterBySnapshotBinding
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.constant.SnapType
import com.armatura.internaldata.viewmodel.ModuleUserMgnViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Jeremy on 2022/11/4.
 * This page is used to demonstrate images collected through Snapshot and adding faces/palms to
 * existing users in the module.
 * Not all modules support this function, please consult business.
 */
class ModuleRegBySnapShotFragment : RegisterBaseFragment() {

    private var title: String? = null
    private var type: Int = 0
    private var userId: String = ""
    private var operate: Int = 0
    private var cacheBitmap: Bitmap? = null
    private lateinit var binding: FragmentModuleRegisterBySnapshotBinding
    private lateinit var viewModel: ModuleUserMgnViewModel
    private var loadingDialog: ProgressDialogFragment? = null

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
        viewModel.bNeedFeature = true
        viewModel.bNeedInfo = Config.instance().isNeedInfoWhenCacheReg
        viewModel.bNeedPicture = Config.instance().isNeedPictureWhenCacheReg

        viewModel.registerFinishLiveData.observe(this) {
            if (it) {
                lifecycleScope.launch {
                    dismissDialog()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }

        viewModel.progressLiveData.observe(this) {
            binding.registerProgressBar.incrementProgressBy(it)
        }

        viewModel.statusLiveData.observe(this) {
            if (it.isNotEmpty()) {
                binding.registerStatusTv.setText(it)
            }
            dismissDialog()
        }

        viewModel.snapshotStartLiveData.observe(this) {
            if (it) {
                showLoadingDialog()
            }
        }

        viewModel.snapshotFinishLiveData.observe(this) { bitmap ->
            dismissDialog()
            if (bitmap == null) {
                showToastMsg(getString(R.string.snap_shot_failed))
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    cacheBitmap = bitmap.copy(bitmap.config, false)
                    withContext(Dispatchers.Main) {
                        binding.pickImageIv.setImageBitmap(bitmap)
                    }
                    Log.i(
                        TAG,
                        String.format(
                            "onSnapFinish: bitmap width = %d height =%d",
                            cacheBitmap!!.width,
                            cacheBitmap!!.height
                        )
                    )
                }
            }
        }

        viewModel.toastMsgLiveData.observe(this) {
            showToastMsg(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentModuleRegisterBySnapshotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topToolBar.apply {
            setTitle(title)
            setToolBarClickListener(object : ToolBarClickListener {
                override fun onClickLeft() {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }

                override fun onClickRight() {}
            })
        }
        binding.registerStatusTv.text = ""
        binding.registerProgressBar.max = if (type == RegisterType.FACE) 100 else 600
        binding.snapShotBtn.setOnClickListener {
            if (cacheBitmap != null && !cacheBitmap!!.isRecycled) {
                cacheBitmap!!.recycle()
            }
            viewModel.snapshot(SnapType.SNAP_RGB)
        }
        binding.confirmBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(it: View) {
                if (cacheBitmap == null || cacheBitmap!!.isRecycled) {
                    showToastMsg(getString(R.string.pls_choose_photo_first))
                    return
                }
                showLoadingDialog(getString(R.string.registering))
                when (type) {
                    RegisterType.FACE -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.enrollFaceByBitmap(cacheBitmap)
                        }
                    }

                    RegisterType.PALM ->
                        cacheBitmap?.let {
                            lifecycleScope.launch(Dispatchers.IO) {
                                viewModel.enrollPalmByBitmap(it)
                            }
                        }

                    else -> {}
                }
            }
        })

    }

    override fun onDestroy() {
        viewModel.resetConfigs()
        dismissDialog()
        super.onDestroy()
    }

    private fun showLoadingDialog(message: String = getString(R.string.snapshot)) {
        ProgressDialogFragment.show(
            parentFragmentManager,
            "", message, true
        ).also {
            loadingDialog = it
        }
    }

    private fun dismissDialog() {
        loadingDialog?.dismiss(parentFragmentManager)
    }

    companion object {
        private const val REGISTER_TYPE = "type"
        private const val REGISTER_TITLE = "title"
        private const val REGISTER_USER_ID = "userId"
        private const val REGISTER_OPERATE = "operate"
        private const val TAG = "MRegBySnapShotFragment"

        @JvmStatic
        fun newInstance(
            type: Int,
            title: String?,
            userId: String?,
            operate: Int,
        ): ModuleRegBySnapShotFragment {
            val fragment = ModuleRegBySnapShotFragment()
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