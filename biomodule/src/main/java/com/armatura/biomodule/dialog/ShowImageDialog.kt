package com.armatura.biomodule.dialog

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RelativeLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogShowImageBinding


/**
 * show snapshot image
 */
class ShowImageDialog : DialogFragment(), View.OnClickListener {


    interface ShowImageDialogClickListener {
        fun onClickConfirm(dialogFragment: DialogFragment)
        fun onClickCancel(dialogFragment: DialogFragment)
    }

    private var listener: ShowImageDialogClickListener? = null
    private var bitmap: Bitmap? = null
    private lateinit var binding: DialogShowImageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_show_image,
            container, false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        val showImageWidth = bitmap?.width?.times(0.5F)?.toInt()
        val showImageHeight = bitmap?.height?.times(0.5F)?.toInt()

        val layoutParams = binding.captureAvatar.layoutParams as RelativeLayout.LayoutParams
        if (showImageWidth != null && showImageHeight != null) {
            layoutParams.width = showImageWidth
            layoutParams.height = showImageHeight
        } else {
            layoutParams.width = 360
            layoutParams.height = 640
        }
        binding.captureAvatar.layoutParams = layoutParams

        binding.captureAvatar.setImageBitmap(bitmap)

        binding.registerDialogCancel.setOnClickListener(this)
        binding.registerDialogConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.register_dialog_cancel -> {
                listener?.onClickCancel(this)
            }
            R.id.register_dialog_confirm -> {
                listener?.onClickConfirm(this)
            }
        }
    }

    fun show(manager: FragmentManager?) {
        val transaction = manager?.beginTransaction()
        transaction?.add(this, tag)
        transaction?.commitAllowingStateLoss()
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    companion object {
        fun newInstance(image: Bitmap, listener: ShowImageDialogClickListener) =
            ShowImageDialog().apply {
                this.bitmap = image
                this.listener = listener
            }
    }
}