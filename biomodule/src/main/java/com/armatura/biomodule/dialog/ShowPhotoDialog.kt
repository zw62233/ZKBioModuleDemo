package com.armatura.biomodule.dialog

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogShowPhotoBinding
import com.bumptech.glide.Glide

/**
 * Created by Magic on 2022/9/14
 * Description:
 */
class ShowPhotoDialog : DialogFragment {
    companion object {
        const val TAG = "ShowImageDialog"
        const val FLAG_PHOTO_PATH = "photo_path"
    }

    private var imagePath: String? = null
    private var bitmap: Bitmap? = null
    private var isBitmap = false

    constructor()

    constructor(imagePath: String) {
        this.imagePath = imagePath
    }

    constructor(bitmap: Bitmap, isBitmap: Boolean) {
        this.bitmap = bitmap
        this.isBitmap = isBitmap
    }

    private lateinit var binding: DialogShowPhotoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_show_photo, container, false)
        savedInstanceState?.let {
            val photoPath = it.getString(FLAG_PHOTO_PATH)
            imagePath = if (!photoPath.isNullOrEmpty()) {
                photoPath
            } else imagePath
        }
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        dialog?.let {
            val window = it.window
            window!!.setDimAmount(0.2F)
            window.setGravity(Gravity.CENTER)
            val layoutParams = window.attributes
            layoutParams.width =
                ((if (!isBitmap) 0.7F else 1.0F) * deviceWidth(requireContext())).toInt()
            layoutParams.height =
                ((if (!isBitmap) 0.7F else 1.0F) * deviceHeight(requireContext())).toInt()
            window.setLayout(
                layoutParams.width,
                (if (!isBitmap) layoutParams.height else layoutParams.width)
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

    }


    private fun deviceWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    private fun deviceHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "[onViewCreated]: image path?=$imagePath")
        Glide.with(view.context).load(
            if (imagePath == null) {
                bitmap
            } else {
                imagePath
            }
        ).into(binding.ivPhoto)
        binding.tvPath.text = imagePath
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(FLAG_PHOTO_PATH, imagePath)
        super.onSaveInstanceState(outState)
    }
}