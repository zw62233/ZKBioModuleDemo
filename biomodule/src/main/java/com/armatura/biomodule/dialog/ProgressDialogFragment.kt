package com.armatura.biomodule.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.FragmentProgressDialogBinding

class ProgressDialogFragment : DialogFragment() {

    private var dialogTitle = ""
    private var dialogMsg = ""
    private var dialogCancelable = true

    private lateinit var binding: FragmentProgressDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            dialogMsg = it.getString(ARG_MSG) ?: ""
            dialogTitle = it.getString(ARG_TITLE) ?: ""
            dialogCancelable = it.getBoolean(ARG_CANCELABLE)
        }
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        isCancelable = dialogCancelable
        return dialog
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels

            val isLandscape = screenWidth > screenHeight

            val width = if (isLandscape) {
                (screenWidth * 0.4).toInt()
            } else {
                (screenWidth * 0.8).toInt()
            }

            val height = if (isLandscape) {
                (screenHeight * 0.16).toInt()
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }

            setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_progress_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = dialogTitle
        binding.textMessage.text = dialogMsg
        if (dialogTitle.isEmpty()) {
            binding.tvTitle.visibility = View.GONE
        }
    }


    fun setMessage(msg: String?) {
        binding.textMessage.text = msg ?: ""
    }

    fun dismiss(manager: FragmentManager) {
        manager.findFragmentByTag(TAG)?.let {
            (it as DialogFragment).dismissAllowingStateLoss()
            manager.executePendingTransactions()
        }
    }

    companion object {
        private const val TAG = "ProgressDialogFragment"
        private const val ARG_TITLE = "title"
        private const val ARG_MSG = "msg"
        private const val ARG_CANCELABLE = "cancelable"


        fun show(
            fragmentManager: FragmentManager,
            title: String,
            msg: String = "",
            cancelable: Boolean = true,
        ): ProgressDialogFragment {
            fragmentManager.findFragmentByTag(TAG)?.let {
                (it as DialogFragment).dismissAllowingStateLoss()
            }

            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MSG, msg)
                putBoolean(ARG_CANCELABLE, cancelable)
            }
            val dialogFragment = ProgressDialogFragment().apply {
                arguments = args
                show(fragmentManager, TAG)
            }
            return dialogFragment
        }

    }
}