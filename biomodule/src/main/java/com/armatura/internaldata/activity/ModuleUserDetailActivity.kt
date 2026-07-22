package com.armatura.internaldata.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.common.RegisterOperate
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.common.RegisterWay
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.ActivityModuleUserDetailBinding
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.showOKAlertDialog
import com.armatura.biomodule.view.ToolBar
import com.armatura.constant.ErrorCode
import com.armatura.internaldata.activity.ModuleAddNewUserActivity.Companion.DATA_TAG_USER_PIN
import com.armatura.internaldata.util.ModuleBioDataUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Created by Jeremy on 2022/11/3.
 * This page is used to demonstrate how to manage user data within the module.
 * Not all modules support this function, please consult business.
 */
class ModuleUserDetailActivity : BaseActivity() {
    companion object {
        private val TAG = ModuleUserDetailActivity::class.java.simpleName
        const val DATA_NAME = "UserInfo"
    }

    private lateinit var binding: ActivityModuleUserDetailBinding
    private var userPin: String = ""

    private var popupView: BasePopupView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this@ModuleUserDetailActivity, R.layout.activity_module_user_detail
        )
        val intent = intent
        if (intent == null) {
            toastMsg("Get User Detail Failed")
            finish()
            return
        }
        val bundleExtra = intent.extras
        if (bundleExtra == null) {
            toastMsg("Get User Detail Failed")
            finish()
            return
        }
        userPin = bundleExtra.getString(DATA_NAME).toString()

        binding.userNameTv.text = ""
        binding.userPinTv.text = ""

        binding.topToolBar.setToolBarClickListener(object : ToolBar.ToolBarClickListener {
            override fun onClickLeft() {
                finish()
            }

            override fun onClickRight() {
            }
        })
    }

    private fun initData(userInfo: UserInfo) {
        Glide.with(this).load(R.drawable.default_avatar)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .error(R.drawable.default_avatar)
            .into(binding.userAvatarIv)

        var isFaceRegistered = false
        var isPalmRegistered = false
        with(userInfo.features) {
            if (this != null && this.size > 0) {
                this.forEach { features ->
                    if (features.bioType.equals(BioType.FACE)) {
                        isFaceRegistered = true
                    }
                    if (features.bioType.equals(BioType.PALM)
                        || features.bioType.equals("palm")/*compatible with old version*/
                        || features.bioType.equals(BioType.PALM_VEIN)
                    ) {
                        isPalmRegistered = true
                    }
                    if (isFaceRegistered && isPalmRegistered) return@forEach
                }
            }
        }

        with(binding) {
            userPinTv.text = userInfo.userId
            userNameTv.text = userInfo.name
            faceRegisterState.text =
                if (isFaceRegistered) resources.getString(R.string.user_registered) else resources.getString(
                    R.string.user_none
                )
            palmRegisterState.text =
                if (isPalmRegistered) resources.getString(R.string.user_registered) else resources.getString(
                    R.string.user_none
                )

            faceDetailRl.setOnClickListener {
                showRegisterFaceWayChooseDialog(
                    userInfo.userId,
                    if (isFaceRegistered) RegisterOperate.UPDATE else RegisterOperate.ADD
                )
            }

            palmDetailRl.setOnClickListener {
                showRegisterPalmWayChooseDialog(
                    userInfo.userId,
                    if (isPalmRegistered) RegisterOperate.UPDATE else RegisterOperate.ADD
                )
            }

            face1v1DetailRl.setOnClickListener {
                ModuleRegisterOperateActivity.actionFace1V1(this@ModuleUserDetailActivity)
            }

            if (Config.isSupportPalm) {
                palmDetailRl.visibility = View.VISIBLE
            }
            if (Config.isSupportFace) {
                faceDetailRl.visibility = View.VISIBLE
                face1v1DetailRl.visibility = View.VISIBLE
            }

            val intent = Intent()
            intent.putExtra(DATA_TAG_USER_PIN, userInfo.userId)
            setResult(RESULT_OK, intent)
        }
    }

    override fun onResume() {
        super.onResume()
        showProgressDialog(
            this@ModuleUserDetailActivity,
            resources.getString(R.string.loading_user)
        )
        flow {
            ModuleBioDataUtil.instance().getPerson(
                userPin, needFaceFeature = true, needPalmFeature = true
            ).also {
                emit(it)
            }
        }.flowOn(Dispatchers.IO)
            .catch { exception ->
                emit(AMTResult(404, "${exception.message}", null))
            }
            .safeCollect(lifecycle, lifecycleScope) {
                if (it.code == ErrorCode.ERROR_NONE) {
                    initData(it.result!!)
                    dismissProgressDialog()
                } else {
                    dismissProgressDialog()
                    showOKAlertDialog(it.message, onConfirm = {
                        finish()
                    })

                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
        popupView = null
    }

    private fun showRegisterFaceWayChooseDialog(userId: String, operation: Int) {
//        ModuleRegisterOperateActivity.action(
//            this@ModuleUserDetailActivity,
//            RegisterType.FACE,
//            userId,
//            operation,
//            RegisterWay.UVC_STREAM
//        )
        val builder = XPopup.Builder(this)
        popupView = builder.asCenterList(
            null, arrayOf(
                getString(R.string.local_image),
                getString(R.string.camera),
                getString(R.string.snap_shot_register)
            )
        ) { _, text ->
            var registerWay = 0
            when (text) {
                getString(R.string.local_image) -> {
                    registerWay = RegisterWay.LOCAL_IMAGE
                }

                getString(R.string.camera) -> {
                    registerWay = RegisterWay.UVC_STREAM
                }

                getString(R.string.snap_shot_register) -> {
                    registerWay = RegisterWay.SNAP_SHOT
                }
            }
            ModuleRegisterOperateActivity.action(
                this@ModuleUserDetailActivity,
                RegisterType.FACE,
                userId,
                operation, registerWay
            )
        }.show()
    }

    private fun showRegisterPalmWayChooseDialog(userId: String, operation: Int) {
//        ModuleRegisterOperateActivity.action(
//            this@ModuleUserDetailActivity,
//            RegisterType.PALM,
//            userId,
//            operation,
//            RegisterWay.UVC_STREAM
//        )

        val builder = XPopup.Builder(this)
        popupView = builder.asCenterList(
            null, arrayOf(
                getString(R.string.local_image),
                getString(R.string.camera),
                getString(R.string.snap_shot_register)
            )
        ) { _, text ->
            when (text) {
                getString(R.string.camera) -> {
                    ModuleRegisterOperateActivity.action(
                        this@ModuleUserDetailActivity,
                        RegisterType.PALM,
                        userId,
                        operation, RegisterWay.UVC_STREAM
                    )
                }

                getString(R.string.snap_shot_register) -> {
                    ModuleRegisterOperateActivity.action(
                        this@ModuleUserDetailActivity,
                        RegisterType.PALM,
                        userId,
                        operation, RegisterWay.SNAP_SHOT
                    )
                }

                getString(R.string.local_image) -> {
                    ModuleRegisterOperateActivity.action(
                        this@ModuleUserDetailActivity,
                        RegisterType.PALM,
                        userId,
                        operation, RegisterWay.LOCAL_IMAGE
                    )
                }
            }
        }.show()
    }
}