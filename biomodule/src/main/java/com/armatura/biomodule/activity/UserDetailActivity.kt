package com.armatura.biomodule.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.common.RegisterOperate
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.common.RegisterWay
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.databinding.ActivityUserDetailBinding
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnSelectListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Magic on 2020/9/17
 */
class UserDetailActivity : BaseActivity() {
    private var userPin: String? = null

    private lateinit var binding: ActivityUserDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this@UserDetailActivity, R.layout.activity_user_detail
        )
        initView()
        val intent = intent
        if (intent == null) {
            finish()
            return
        }
        val bundleExtra = intent.extras
        if (bundleExtra == null) {
            finish()
            return
        }
        userPin = bundleExtra.getString(DATA_NAME)
    }

    private fun initView() {
        binding.myToolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                finish()
            }

            override fun onClickRight() {}
        })
    }

    private fun initData(userInfo: UserInfo) {
        //avatar
        Glide.with(this).load(userInfo.avatarDrawable)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .error(R.drawable.default_avatar)
            .into(binding.userAvatarIv)
        binding.userPinTv.text = userInfo.userId
        binding.userNameTv.text = userInfo.name
        val isFaceRegistered = userInfo.faceFeature != null
        binding.faceRegisterState.text =
            if (isFaceRegistered) resources.getString(R.string.user_registered) else resources.getString(
                R.string.user_none
            )
        val isPalmRegistered = userInfo.palmFeature1 != null || userInfo.palmFeature2 != null
        binding.palmRegisterState.text =
            if (isPalmRegistered) resources.getString(R.string.user_registered) else resources.getString(
                R.string.user_none
            )

        val isRFIDRegistered = BioDataUtil.isUserInfoHasCard(userPin)
        binding.rfidInfoState.text =
            if (isRFIDRegistered) resources.getString(R.string.user_registered) else resources.getString(
                R.string.user_none
            )
        binding.faceDetailRl.setOnClickListener {
            showRegisterFaceWayChooseDialog(
                userInfo.userId,
                if (isFaceRegistered) RegisterOperate.UPDATE else RegisterOperate.ADD
            )
        }
        binding.palmDetailRl.setOnClickListener {
            showRegisterPalmWayChooseDialog(
                userInfo.userId,
                if (isPalmRegistered) RegisterOperate.UPDATE else RegisterOperate.ADD
            )
        }

        binding.rfidInfoRl.setOnClickListener {
            showRegisterRFIDWayChooseDialog(
                userInfo.userId,
                if (isRFIDRegistered) RegisterOperate.UPDATE else RegisterOperate.ADD
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            val list = File(FileUtils.USER_BIO_PHOTO + File.separator + userPin).list()
            withContext(Dispatchers.Main) {
                binding.featurePhotoState.text = if (list == null) {
                    resources.getString(
                        R.string.user_none
                    )
                } else {
                    "${list.size}"
                }
            }
        }

        binding.featureImageRl.setOnClickListener {
            File(FileUtils.USER_BIO_PHOTO + File.separator + userPin).also {
                if (it.exists() && it.isDirectory) {
                    val photoList = ArrayList<String>()
                    it.listFiles()?.forEach { file ->
                        photoList.add(file.absolutePath)
                    }
                    PhotoListActivity.action(this@UserDetailActivity, photoList)
                }
            }
        }

        binding.face1v1DetailRl.setOnClickListener {
            BioTemplateRegisterActivity.actionFace1V1(this@UserDetailActivity)
        }

        with(binding.userAvatarIv) {
            setOnClickListener {
                BioTemplateRegisterActivity.actionTakePhoto(
                    this@UserDetailActivity,
                    userInfo.userId, true
                )
            }
        }


        if (!Config.isSupportFace) {
            binding.face1v1DetailRl.visibility = View.GONE
            binding.faceDetailRl.visibility = View.GONE
        }
        if (!Config.isSupportPalm) {
            binding.palmDetailRl.visibility = View.GONE
        }
        if (!Config.isSupportRFID) {
            binding.rfidInfoRl.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (userPin == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val userInfo = suspendCoroutine {
                BioDataUtil.findUserInfoFromDatabasesByUserPin(userPin).also { userInfo ->
                    it.resume(userInfo)
                }
            }
            userInfo ?: return@launch
            initData(userInfo)
        }
    }

    /**
     * register face by these way
     * 1.local image
     * 2.camera,UVC stream
     * 3.directly get from module by current frame
     */
    private fun showRegisterFaceWayChooseDialog(userId: String, operation: Int) {
//        BioTemplateRegisterActivity.action(
//            this@UserDetailActivity,
//            RegisterType.FACE,
//            userId,
//            operation, RegisterWay.UVC_STREAM
//        )

        //The following sections provide multiple registration methods
        val builder = XPopup.Builder(this)
        builder.asCenterList(null,
            arrayOf(
                getString(R.string.local_image),
                getString(R.string.camera),
                getString(R.string.snap_shot_register)
            ),
            object : OnSelectListener {
                override fun onSelect(position: Int, text: String?) {
                    if (text.equals(getString(R.string.local_image))) {
                        BioTemplateRegisterActivity.action(
                            this@UserDetailActivity,
                            RegisterType.FACE,
                            userId,
                            operation, RegisterWay.LOCAL_IMAGE
                        )
                    } else if (text.equals(getString(R.string.camera))) {
                        BioTemplateRegisterActivity.action(
                            this@UserDetailActivity,
                            RegisterType.FACE,
                            userId,
                            operation, RegisterWay.UVC_STREAM
                        )
                    } else if (text.equals(getString(R.string.snap_shot_register))) {
                        BioTemplateRegisterActivity.action(
                            this@UserDetailActivity,
                            RegisterType.FACE,
                            userId,
                            operation, RegisterWay.SNAP_SHOT
                        )
                    }
                }

            })
            .show()

    }

    /**
     * register palm by these way
     * 1.local image(if you have image)
     * 2.camera,UVC stream
     * 3.directly get from module by current frame
     */
    private fun showRegisterPalmWayChooseDialog(userId: String, operation: Int) {
//        BioTemplateRegisterActivity.action(
//            this@UserDetailActivity,
//            RegisterType.PALM,
//            userId,
//            operation, RegisterWay.UVC_STREAM
//        )

        //The following sections provide multiple registration methods
        val builder = XPopup.Builder(this)
        builder.asCenterList(null,
            arrayOf(
                getString(R.string.local_image),
                getString(R.string.camera),
                getString(R.string.snap_shot_register)
            )
        ) { _, text ->
            if (text.equals(getString(R.string.local_image))) {
                BioTemplateRegisterActivity.action(
                    this@UserDetailActivity,
                    RegisterType.PALM,
                    userId,
                    operation, RegisterWay.LOCAL_IMAGE
                )
            } else if (text.equals(getString(R.string.camera))) {
                BioTemplateRegisterActivity.action(
                    this@UserDetailActivity,
                    RegisterType.PALM,
                    userId,
                    operation, RegisterWay.UVC_STREAM
                )
            } else if (text.equals(getString(R.string.snap_shot_register))) {
                BioTemplateRegisterActivity.action(
                    this@UserDetailActivity,
                    RegisterType.PALM,
                    userId,
                    operation, RegisterWay.SNAP_SHOT
                )
            }
        }
            .show()
    }

    private fun showRegisterRFIDWayChooseDialog(userId: String, operation: Int) {
        BioTemplateRegisterActivity.action(
            this@UserDetailActivity,
            RegisterType.RFID,
            userId,
            operation, RegisterWay.RFID
        )
    }

    companion object {
        private const val TAG = "UserDetailActivity"
        private const val DATA_NAME = "UserInfo"

        @JvmStatic
        fun action(context: Context, userPin: String?) {
            val intent = Intent()
            intent.putExtra(DATA_NAME, userPin)
            intent.setClass(context, UserDetailActivity::class.java)
            context.startActivity(intent)
        }
    }
}