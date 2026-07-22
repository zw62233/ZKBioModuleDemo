package com.armatura.biomodule.dialog

import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogCustomInputBinding
import com.armatura.biomodule.util.toastAnywhere
import java.util.regex.Pattern

class CustomInputDialog : Dialog {

    interface OnBtnClickListener {
        fun onPositiveButtonClick(dialog: CustomInputDialog, content: String)
        fun onNegativeButtonClick(dialog: CustomInputDialog, content: String)
    }

    private lateinit var onBtnClickListener: OnBtnClickListener

    private lateinit var binding: DialogCustomInputBinding

    var min = -1
    var max = -1

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
        val view: View = inflater.inflate(R.layout.dialog_custom_input, null)
        binding = DataBindingUtil.bind(view)!!
        setContentView(binding.root)


        window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        )
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        binding.dialogContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (s.isNullOrEmpty()) {
//                    return
//                } else {
//                    if (min != -1 && max != -1) {
//                        val num = s.toString().toFloat()
//                        if (num > max) {
//                            dialog_content.removeTextChangedListener(this)
//                            dialog_content.setText(max.toString())
//                            dialog_content.setSelection(dialog_content.text.length)
//                            dialog_content.addTextChangedListener(this)
//                            return
//                        }
//                        if (num < min) {
//                            dialog_content.removeTextChangedListener(this)
//                            dialog_content.setText(min.toString())
//                            dialog_content.setSelection(dialog_content.text.length)
//                            dialog_content.addTextChangedListener(this)
//                            return
//                        }
//
//                    }
//                }

            }
        })

        binding.dialogConfirm.setOnClickListener {
            if (binding.dialogContent.text.isEmpty()) {
                toastAnywhere(R.string.invalid_data)
                return@setOnClickListener
            }

            if (binding.dialogContent.text.toString()
                    .startsWith("0") && !isDoubleOrFloat(binding.dialogContent.text)
            ) {
                binding.dialogContent.setText(
                    binding.dialogContent.text.toString().toInt().toString()
                )
            }
            onBtnClickListener.onPositiveButtonClick(this, binding.dialogContent.text.toString())
        }
        binding.dialogCancel.setOnClickListener {
            if (binding.dialogContent.text.toString()
                    .startsWith("0") && !isDoubleOrFloat(binding.dialogContent.text)
            ) {
                binding.dialogContent.setText(
                    binding.dialogContent.text.toString().toInt().toString()
                )
            }
            onBtnClickListener.onNegativeButtonClick(this, binding.dialogContent.text.toString())
        }
    }

    fun setTitle(title: String): CustomInputDialog {
        binding.dialogTitle.text = title
        return this
    }

    fun setHintContent(hintContent: String): CustomInputDialog {
        binding.dialogContent.hint = hintContent
        return this
    }

    fun setInputType(inputType: Int) {
        binding.dialogContent.inputType = inputType
    }

    fun setOnBtnClickListener(onBtnClickListener: OnBtnClickListener): CustomInputDialog {
        this.onBtnClickListener = onBtnClickListener
        return this
    }

    fun getInputContent(): String {
        return binding.dialogContent.text.toString()
    }


    fun isDoubleOrFloat(s: CharSequence): Boolean {
        val pattern = Pattern.compile("^[-\\+]?[.\\d]*$")
        return pattern.matcher(s).matches()
    }


    private fun showSoftInput() {
        val inputManager =
            context.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED)
    }
}