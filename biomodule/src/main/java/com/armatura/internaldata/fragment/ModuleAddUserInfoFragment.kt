package com.armatura.internaldata.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity.RESULT_OK
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.databinding.FragmentModuleAddBaseInfoBinding
import com.armatura.biomodule.fragment.BaseFragment
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.showOKAlertDialog
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.constant.ErrorCode
import com.armatura.internaldata.activity.ModuleAddNewUserActivity.Companion.DATA_TAG_USER_PIN
import com.armatura.internaldata.util.ModuleBioDataUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Created by Jeremy on 2022/11/3.
 * This page is used to demonstrate how to add basic user information to the module.
 * Not all modules support this function, please consult business.
 */
class ModuleAddUserInfoFragment : BaseFragment() {
    companion object {
        private val TAG = ModuleAddUserInfoFragment::class.java.simpleName
    }

    private lateinit var binding: FragmentModuleAddBaseInfoBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentModuleAddBaseInfoBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }


    private fun initView() {
        binding.topToolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {}
        })
        binding.moduleUserPinText.setText("${System.currentTimeMillis()}")
        binding.moduleCancelBtn.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.moduleConfirmBtn.setOnClickListener {
            lifecycleScope.launch {
                val userId = binding.moduleUserPinText.text.toString()
                val userName = binding.moduleUserNameText.text.toString()
                if (TextUtils.isEmpty(userName)) {
                    binding.moduleUserNameText.requestFocus()
                    binding.moduleUserNameText.error = getString(R.string.reg_face_tip_empty_name)
                    return@launch
                }
                if (TextUtils.isEmpty(userId)) {
                    binding.moduleUserPinText.requestFocus()
                    binding.moduleUserPinText.error = getString(R.string.reg_face_tip_empty_pin)
                    return@launch
                }
                flow {
                    val getPersonResult = ModuleBioDataUtil.instance().getPerson(
                        userId, needFaceFeature = false, needPalmFeature = false
                    )
                    if (getPersonResult.code == ErrorCode.ERROR_NONE) {
                        //means already exist in module,not allow to add
                        emit(
                            AMTResult(
                                555/*fake code*/,
                                getString(R.string.tips_user_exist),
                                null
                            )
                        )
                    } else {
                        //means not exist,allow to add
                        UserInfo().apply {
                            this.userId = userId
                            this.name = userName
                            this.personId = userId
                        }.also {
                            //add
                            ModuleBioDataUtil.instance().addPerson(it).also { amtResult ->
                                emit(amtResult)
                            }
                        }
                    }
                }.flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            //add success
                            val intent = Intent()
                            intent.putExtra(DATA_TAG_USER_PIN, it.result)
                            activity?.apply {
                                setResult(RESULT_OK, intent)
                                finish()
                            }
                        } else {
                            activity?.showOKAlertDialog(it.message)
                        }
                    }

            }

        }
    }

    override fun onImagePicked(uri: Uri?) {
    }

    override fun onFilePicked(uri: Uri?) {
    }

}