package com.armatura.biomodule.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogCaptureRegisterBinding
import com.armatura.biomodule.util.toastAnywhere


class CaptureRegisterDialog : DialogFragment(), View.OnClickListener {


    interface CaptureRegisterDialogClickListener {
        fun onClickConfirm(dialogFragment: DialogFragment, name: String, userPin: String)
        fun onClickCancel(dialogFragment: DialogFragment, name: String, userPin: String)
        fun onClickRecapture(dialogFragment: DialogFragment, name: String, userPin: String)
    }

    private var listener: CaptureRegisterDialogClickListener? = null
    private var avatar: Bitmap? = null

    private var centerBtnVisible: Boolean = true

    private lateinit var binding: DialogCaptureRegisterBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_capture_register,
            container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.captureAvatar.setImageBitmap(avatar)
        binding.registerDialogCancel.setOnClickListener(this)
        binding.registerDialogConfirm.setOnClickListener(this)
        binding.registerDialogRecapture.setOnClickListener(this)
        if (centerBtnVisible) {
            binding.registerDialogRecapture.visibility = View.VISIBLE
        } else {
            binding.registerDialogRecapture.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.register_dialog_cancel -> {
                listener?.onClickCancel(
                    this,
                    binding.userNameInputView.text.toString(),
                    binding.userPinInputView.text.toString()
                )
            }

            R.id.register_dialog_confirm -> {
                if (binding.userPinInputView.text.toString().isEmpty()) {
                    toastAnywhere(R.string.reg_face_tip_empty_pin)
                } else {
                    listener?.onClickConfirm(
                        this,
                        binding.userNameInputView.text.toString(),
                        binding.userPinInputView.text.toString()
                    )
                }
            }

            R.id.register_dialog_recapture -> {
                listener?.onClickRecapture(
                    this,
                    binding.userNameInputView.text.toString(),
                    binding.userPinInputView.text.toString()
                )

            }
        }
    }


    fun show(manager: FragmentManager?) {
        val transaction = manager?.beginTransaction()
        transaction?.add(this, CaptureRegisterDialog::class.java.simpleName)
        transaction?.commitAllowingStateLoss()
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    companion object {
        fun newInstance(
            centerBtnVisible: Boolean,
            avatar: Bitmap,
            listener: CaptureRegisterDialogClickListener,
        ) =
            CaptureRegisterDialog().apply {
                this.avatar = avatar
                this.listener = listener
                this.centerBtnVisible = centerBtnVisible
            }
    }
}