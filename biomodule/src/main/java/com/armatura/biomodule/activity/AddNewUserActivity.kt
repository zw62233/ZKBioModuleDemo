package com.armatura.biomodule.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.fragment.AddBaseInfoFragment.Companion.newInstance
import com.armatura.biomodule.fragment.AddBaseInfoFragment.OnBaseInfoListener
import com.armatura.biomodule.util.safeCollect
import com.armatura.constant.ErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Created by Magic on 2020/9/17
 */
class AddNewUserActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_user)

        val intent = intent
        if (intent == null) {
            finish()
            return
        }

        val addBaseInfoFragment = newInstance(object : OnBaseInfoListener {
            override fun onCancel() {
                onBackPressedDispatcher.onBackPressed()
            }

            override fun onNext(userId: String?, userName: String?) {
                flow {
                    emit(saveUser(userId, userName))
                }.catch { emit(AMTResult(404, it.message.toString(), null)) }
                    .flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) { result ->
                        if (result.code == ErrorCode.SUCCESS) {
                            toastMsg(result.message)
                            UserDetailActivity.action(this@AddNewUserActivity, userId)
                            finish()
                        } else {
                            toastMsg(getString(R.string.tips_add_user_failed))
                        }
                    }
            }
        })
        addFragment(addBaseInfoFragment)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val backStackEntryCount = supportFragmentManager.backStackEntryCount
                if (backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })
    }

    fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_fragment_fl, fragment)
        fragmentTransaction.commitAllowingStateLoss()
    }

    fun addFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.main_fragment_fl, fragment)
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun saveUser(pin: String?, userName: String?): AMTResult<String> {
        if (pin.isNullOrEmpty() || userName.isNullOrEmpty()) {
            return AMTResult(404, "invalid user info", null)
        }
        if (BioDataUtil.instance().isUserExist(pin)) {
            return AMTResult(404, "user already exist", null)
        } else {
            val userInfo = UserInfo().apply {
                userId = pin
                name = userName
                personId = pin
            }
            BioDataUtil.instance().insertUserInfo(userInfo)
            return AMTResult(
                ErrorCode.SUCCESS,
                getString(R.string.tips_add_user_success).format(pin),
                null
            )
        }
    }

    companion object {
        private const val ADD_TYPE = "addType"
        fun action(context: Context, addType: Int) {
            val intent = Intent()
            intent.putExtra(ADD_TYPE, addType)
            intent.setClass(context, AddNewUserActivity::class.java)
            context.startActivity(intent)
        }
    }
}