package com.armatura.biomodule.fragment

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.databinding.FragmentRegisterBySnapshotBinding
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.ToolBar
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.biomodule.viewmodel.DBViewModel
import com.armatura.biomodule.viewmodel.ModuleViewModel
import com.armatura.constant.ErrorCode
import com.armatura.constant.SnapType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Created by Magic on 2020/9/17
 * * Register by snap shot , which image format is jpeg ,but more
 */
class RegisterBySnapShotFragment : BaseFragmentEX() {
    private lateinit var title: String
    private var type: Int = 0
    private lateinit var userId: String
    private var operate: Int = 0

    private lateinit var binding: FragmentRegisterBySnapshotBinding


    private var cacheBitmap: Bitmap? = null

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
            R.layout.fragment_register_by_snapshot,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initView(view: View) {
        val toolBar = view.findViewById<ToolBar>(R.id.my_tool_bar)
        toolBar.setTitle(title)
        toolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {
            }
        })
        binding.registerProgressBar.max = 100

        binding.snapShotBtn.setOnClickListener {
            if (cacheBitmap != null && !cacheBitmap!!.isRecycled) {
                cacheBitmap!!.recycle()
            }

            lifecycleScope.launch {
                showLoadingDialog()
                moduleViewModel.snapShotByFlow(SnapType.SNAP_RGB)
                    .flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) {
                        dismissLoadingDialog()
                        binding.registerStatusTv.text = it.message
                        if (it.code != ErrorCode.ERROR_NONE) {
                            return@safeCollect
                        }
                        //cache this bitmap ,if next snapshot ,recycle it
                        cacheBitmap = it.result!!.copy(it.result.config, false)
                        binding.pickImageIv.setImageBitmap(it.result)
                        cacheBitmap?.let { bitmap ->
                            Log.i(
                                TAG,
                                String.format(
                                    "onSnapFinish: bitmap width = %d height =%d",
                                    bitmap.getWidth(),
                                    bitmap.getHeight()
                                )
                            )
                        }
                    }
            }
        }

        binding.confirmBtn.setOnClickListener {
            if (cacheBitmap == null) {
                toastAnywhere(getString(R.string.pls_choose_photo_first))
                return@setOnClickListener
            }
            lifecycleScope.launch {
                showLoadingDialog()
                when (type) {
                    RegisterType.FACE -> {
                        moduleViewModel.registerFaceByFlow(cacheBitmap!!)
                            .flowOn(Dispatchers.IO)
                            .flatMapConcat {
                                if (it.code == ErrorCode.ERROR_NONE) {
                                    dbViewModel.addFaceToUserByFlow(userId, it.result!!, operate)
                                } else {
                                    flowOf(it)
                                }
                            }.flowOn(Dispatchers.IO)
                            .flowWithLifecycle(this@RegisterBySnapShotFragment.lifecycle)
                            .onEach {
                                dismissLoadingDialog()
                                binding.registerStatusTv.text = it.message
                                if (it.code != ErrorCode.SUCCESS) {
                                    return@onEach
                                }
                                binding.registerProgressBar.progress = 100
                                delay(1000)
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            }.launchIn(lifecycleScope)
                    }

                    RegisterType.PALM -> {
                        moduleViewModel.registerPalmByFlow(cacheBitmap!!)
                            .flowOn(Dispatchers.IO)
                            .flatMapConcat {
                                if (it.code == ErrorCode.ERROR_NONE) {
                                    dbViewModel.addPalmToUserByFlow(userId, it.result!!, operate)
                                } else {
                                    flowOf(it)
                                }
                            }.flowOn(Dispatchers.IO)
                            .flowWithLifecycle(this@RegisterBySnapShotFragment.lifecycle)
                            .onEach {
                                dismissLoadingDialog()
                                binding.registerStatusTv.text = it.message
                                if (it.code != ErrorCode.SUCCESS) {
                                    return@onEach
                                }
                                binding.registerProgressBar.progress = 100
                                delay(1000)
                                activity?.onBackPressedDispatcher?.onBackPressed()
                            }.launchIn(lifecycleScope)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SnapShotFragment"
        private const val REGISTER_TYPE = "type"
        private const val REGISTER_TITLE = "title"
        private const val REGISTER_USER_ID = "userId"
        private const val REGISTER_OPERATE = "operate"


        fun newInstance(
            type: Int,
            title: String?,
            userId: String?,
            operate: Int,
        ): RegisterBySnapShotFragment {
            val fragment = RegisterBySnapShotFragment()
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