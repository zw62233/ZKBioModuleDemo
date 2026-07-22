package com.armatura.biomodule.dialog

import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogCustomInputNameAndPinBinding
import com.armatura.biomodule.util.toastAnywhere

class CustomUserPinInputDialog : Dialog {

    interface OnBtnClickListener {
        fun onPositiveButtonClick(
            dialog: CustomUserPinInputDialog,
            userPin: String?,
            userName: String?
        )

        fun onNegativeButtonClick(
            dialog: CustomUserPinInputDialog,
            userPin: String?,
            userName: String?
        )
    }

    private lateinit var onBtnClickListener: OnBtnClickListener
    private lateinit var binding: DialogCustomInputNameAndPinBinding

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, themeResId: Int) : super(context, themeResId) {
        init()
    }


    constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener) {
        init()
    }


    private fun init() {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.dialog_custom_input_name_and_pin, null)
        binding = DataBindingUtil.bind(view)!!
        setContentView(binding.root)

        window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)


        binding.dialogConfirm.setOnClickListener {
            when {
                binding.userPinEditText.text.toString().isEmpty() -> {
                    toastAnywhere(R.string.reg_face_tip_empty_pin)
                }
                else -> {
                    onBtnClickListener.onPositiveButtonClick(
                        this,
                        binding.userPinEditText.text.toString(),
                        binding.userNameEditText.text.toString()
                    )
                }
            }

        }
        binding.dialogCancel.setOnClickListener {
            onBtnClickListener.onNegativeButtonClick(
                this,
                binding.userPinEditText.text.toString(),
                binding.userNameEditText.text.toString()
            )
        }
    }

    fun setTitle(title: String): CustomUserPinInputDialog {
        binding.dialogTitle.text = title
        return this
    }

    fun setOnBtnClickListener(onBtnClickListener: OnBtnClickListener): CustomUserPinInputDialog {
        this.onBtnClickListener = onBtnClickListener
        return this
    }


    private fun showSoftInput() {
        val inputManager =
            context.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED)
    }
}