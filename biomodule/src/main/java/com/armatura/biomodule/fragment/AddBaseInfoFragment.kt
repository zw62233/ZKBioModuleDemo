package com.armatura.biomodule.fragment

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.armatura.biomodule.R
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.databinding.FragmentAddBaseInfoBinding
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener

/**
 * Created by Magic on 2020/9/17
 */
class AddBaseInfoFragment : BaseFragment() {

    override fun onImagePicked(uri: Uri) {
    }

    override fun onFilePicked(uri: Uri) {
    }

    interface OnBaseInfoListener {
        fun onCancel()

        fun onNext(userId: String?, userName: String?)
    }


    private lateinit var binding: FragmentAddBaseInfoBinding


    private var onBaseInfoListener: OnBaseInfoListener? = null

    fun setOnBaseInfoListener(onBaseInfoListener: OnBaseInfoListener?) {
        this.onBaseInfoListener = onBaseInfoListener
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_add_base_info,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onDetach() {
        super.onDetach()
        onBaseInfoListener = null
    }

    private fun initView() {
        binding.myToolBar.setToolBarClickListener(object : ToolBarClickListener {
            override fun onClickLeft() {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            override fun onClickRight() {
            }
        })

        if (Config.instance().isTestMode) {
            binding.userPinInputView.setText("")
        } else {
            binding.userPinInputView.setText("${System.currentTimeMillis()}")
        }
        binding.cancelBtn.setOnClickListener {
            if (onBaseInfoListener != null) {
                onBaseInfoListener!!.onCancel()
            }
        }
        binding.btnLl.setOnClickListener(View.OnClickListener {
            if (onBaseInfoListener != null) {
                val userId = binding.userPinInputView.getText().toString()
                val userName = binding.userNameInputView.getText().toString()
                if (TextUtils.isEmpty(userId)) {
                    binding.userPinInputView.requestFocus()
                    binding.userPinInputView.setError(getString(R.string.reg_face_tip_empty_pin))
                    return@OnClickListener
                }

                if (!Config.instance().isTestMode) {
                    if (TextUtils.isEmpty(userName)) {
                        binding.userNameInputView.requestFocus()
                        binding.userNameInputView.error =
                            getString(R.string.reg_face_tip_empty_name)
                        return@OnClickListener
                    }
                }else{
                    binding.userNameInputView.setText(userId)
                }


                if (BioDataUtil.findUserInfoFromDatabasesByUserPin(userId) != null) {
                    binding.userPinInputView.requestFocus()
                    binding.userPinInputView.setError(
                        String.format(
                            getString(R.string.reg_user_pin_already_regist),
                            userId
                        )
                    )
                    return@OnClickListener
                }
                onBaseInfoListener!!.onNext(
                    binding.userPinInputView.getText().toString(),
                    binding.userNameInputView.getText().toString()
                )
            }
        })
    }

    companion object {
        private const val TAG = "AddBaseInfoFragment"

        @JvmStatic
        fun newInstance(onBaseInfoListener: OnBaseInfoListener?): AddBaseInfoFragment {
            val fragment = AddBaseInfoFragment()
            fragment.setOnBaseInfoListener(onBaseInfoListener)
            return fragment
        }
    }
}