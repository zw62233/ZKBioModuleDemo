package com.armatura.biomodule.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.common.RegisterOperate
import com.armatura.biomodule.common.RegisterType
import com.armatura.biomodule.common.RegisterWay
import com.armatura.biomodule.fragment.AvatarFragment.Companion.newInstance
import com.armatura.biomodule.fragment.Face1V1Fragment
import com.armatura.biomodule.fragment.RFIDFragment.Companion.newInstance
import com.armatura.biomodule.fragment.RegisterByImageFragment.Companion.newInstance
import com.armatura.biomodule.fragment.RegisterBySnapShotFragment
import com.armatura.biomodule.fragment.RegisterByUvcFragment
import com.armatura.biomodule.util.toastAnywhere

/**
 * Created by Magic on 2020/9/17
 * Register face or palm decide on your choose
 */
class BioTemplateRegisterActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bio_template_register)
        initData()
    }

    private fun initData() {
        val intent = intent
        if (intent == null) {
            toastAnywhere("Invalid type")
            finish()
            return
        }
        val bundleExtra = intent.extras
        if (bundleExtra == null) {
            toastAnywhere("Invalid type")
            finish()
            return
        }
        val type = bundleExtra.getInt(REGISTER_TYPE)
        val userId = bundleExtra.getString(REGISTER_USER_ID)
        val operate = bundleExtra.getInt(REGISTER_OPERATE)
        val way = bundleExtra.getInt(REGISTER_WAY)
        val isHostAvatar = bundleExtra.getBoolean(AVATAR_TYPE, true)

        val title = when (type) {
            RegisterType.FACE -> getString(R.string.face)
            RegisterType.PALM -> getString(R.string.palm)
            RegisterType.TAKE_AVATAR -> getString(R.string.set_your_avatar)
            RegisterType.RFID -> "RFID"
            FAVE_1V1 -> "Face 1V1"
            else -> throw UnsupportedOperationException("Unknown type,$type")
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (type == RegisterType.TAKE_AVATAR) {
            fragmentTransaction.add(R.id.main_fragment_fl, newInstance(title, userId, isHostAvatar))
        } else {
            when (way) {
                RegisterWay.LOCAL_IMAGE -> fragmentTransaction.add(
                    R.id.main_fragment_fl,
                    newInstance(type, title, userId, operate)
                )

                RegisterWay.UVC_STREAM -> fragmentTransaction.add(
                    R.id.main_fragment_fl,
                    RegisterByUvcFragment.newInstance(type, title, userId, operate)
                )

                RegisterWay.SNAP_SHOT -> fragmentTransaction.add(
                    R.id.main_fragment_fl,
                    RegisterBySnapShotFragment.newInstance(type, title, userId, operate)
                )

                RegisterWay.RFID -> fragmentTransaction.add(
                    R.id.main_fragment_fl,
                    newInstance(title, userId, operate)
                )

                FAVE_1V1 -> fragmentTransaction.add(
                    R.id.main_fragment_fl,
                    Face1V1Fragment.newInstance(title)
                )

                else -> throw UnsupportedOperationException("Unknown way,$way")


            }
        }
        fragmentTransaction.commitAllowingStateLoss()
    }


    companion object {
        private const val TAG = "BioTemplateRegisterActi"
        private const val REGISTER_TYPE = "type"
        private const val REGISTER_USER_ID = "userId"
        private const val REGISTER_OPERATE = "register_operate" //update or add
        private const val REGISTER_WAY = "register_way"

        private const val AVATAR_TYPE = "avatar"

        private const val FAVE_1V1 = 11

        fun action(
            context: Context, @RegisterType type: Int, userId: String?,
            @RegisterOperate operate: Int, @RegisterWay way: Int
        ) {
            val intent = Intent()
            intent.putExtra(REGISTER_TYPE, type)
            intent.putExtra(REGISTER_USER_ID, userId)
            intent.putExtra(REGISTER_OPERATE, operate)
            intent.putExtra(REGISTER_WAY, way)
            intent.setClass(context, BioTemplateRegisterActivity::class.java)
            context.startActivity(intent)
        }

        fun actionFace1V1(context: Context) {
            val intent = Intent()
            intent.putExtra(REGISTER_TYPE, FAVE_1V1)
            intent.putExtra(REGISTER_WAY, FAVE_1V1)
            intent.setClass(context, BioTemplateRegisterActivity::class.java)
            context.startActivity(intent)
        }

        fun actionTakePhoto(context: Context, userId: String?, isHostAvatar: Boolean) {
            val intent = Intent()
            intent.putExtra(REGISTER_TYPE, RegisterType.TAKE_AVATAR)
            intent.putExtra(REGISTER_USER_ID, userId)
            intent.putExtra(AVATAR_TYPE, isHostAvatar)
            intent.setClass(context, BioTemplateRegisterActivity::class.java)
            context.startActivity(intent)
        }
    }
}