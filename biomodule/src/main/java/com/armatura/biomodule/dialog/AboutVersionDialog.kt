package com.armatura.biomodule.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.armatura.BuildConfig
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogAboutVersionBinding
import com.armatura.uvclib.util.AMTUtil


class AboutVersionDialog : DialogFragment() {

    private lateinit var binding: DialogAboutVersionBinding



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)


        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_about_version,
            container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()

    }

    override fun onResume() {
        super.onResume()
        dialog?.let {
            val window = it.window
            window!!.setDimAmount(0.2F)
            window.setGravity(Gravity.CENTER)
            val layoutParams = window.attributes
            if (context?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                layoutParams.width = (0.6F * deviceWidth(requireContext())).toInt()
                layoutParams.height = (0.6F * deviceHeight(requireContext())).toInt()
            } else {
                layoutParams.width = (0.6F * deviceWidth(requireContext())).toInt()
                layoutParams.height = (0.5F * deviceHeight(requireContext())).toInt()
            }

            window.setLayout(layoutParams.width, layoutParams.height)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun deviceWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    private fun deviceHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        binding.btnOk.setOnClickListener {
            dismiss()
        }
        binding.tvDemoVersion.text = "Demo Version:${AMTUtil.getVersionName(requireContext())}"
        binding.tvSdkVersion.text = "MultiBio3.0 SDK Version:${BuildConfig.VERSION}"
    }


    fun show(manager: FragmentManager?) {
        val transaction = manager?.beginTransaction()
        transaction?.add(this, AboutVersionDialog::class.java.simpleName)
        transaction?.commitAllowingStateLoss()
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    companion object {
        fun newInstance() =
            AboutVersionDialog()
    }
}