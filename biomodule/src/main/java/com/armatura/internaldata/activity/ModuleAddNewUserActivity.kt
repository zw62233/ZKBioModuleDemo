package com.armatura.internaldata.activity

import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.common.AddNewUserType
import com.armatura.internaldata.fragment.ModuleAddUserInfoFragment

/**
 * Created by Jeremy on 2022/11/3.
 * This page is used to demonstrate how to manage user data within the module.
 * Not all modules support this function, please consult business.
 */
class ModuleAddNewUserActivity : BaseActivity() {
    companion object {
        private val TAG = ModuleAddNewUserActivity::class.java.simpleName
        const val ADD_TYPE = "addType"
        const val REQ_CODE_USER_PIN = 0x1001
        const val DATA_TAG_USER_PIN = "userPin"
    }

    private var addUserType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_user)
        if (intent == null) {
            finish()
            return
        }
        addUserType = intent.getIntExtra(ADD_TYPE, AddNewUserType.MODULE_INSIDE)

        onBackPressedDispatcher.addCallback(this) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }
        addFragment(ModuleAddUserInfoFragment())
    }

    override fun onStop() {
        super.onStop()
        setResult(RESULT_OK)
    }

    private fun addFragment(fragment: Fragment?) {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.main_fragment_fl, fragment!!)
        }.also {
            it.commitAllowingStateLoss()
        }
    }
}