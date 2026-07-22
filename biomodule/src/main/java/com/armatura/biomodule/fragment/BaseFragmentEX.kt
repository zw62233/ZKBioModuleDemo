package com.armatura.biomodule.fragment

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.util.HidHelper
import kotlinx.coroutines.launch

/**
 * Created by Magic on 2020/9/18
 */
abstract class BaseFragmentEX : Fragment() {
    private lateinit var pickVisualMediaRequestActivityResultLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Array<String>>
    private var progressDialogFragment: ProgressDialogFragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickVisualMediaRequestActivityResultLauncher =
            registerForActivityResult<PickVisualMediaRequest, Uri>(
                ActivityResultContracts.PickVisualMedia()
            ) { o -> onImagePicked(o) }

        activityResultLauncher = registerForActivityResult<Array<String>, Uri>(
            ActivityResultContracts.OpenDocument()
        ) { o -> onFilePicked(o) }
    }

    /**
     * open image picker
     */
    protected fun pickImage() {
        val pickVisualMediaRequest: PickVisualMediaRequest = PickVisualMediaRequest.Builder()
            .setMediaType(ImageOnly)
            .build()
        pickVisualMediaRequestActivityResultLauncher.launch(pickVisualMediaRequest)
    }

    protected fun showLoadingDialog() {
        lifecycleScope.launch {
            ProgressDialogFragment.show(
                parentFragmentManager,
                "", getString(R.string.registering),
                true
            ).also {
                progressDialogFragment = it
            }
        }
    }

    protected fun dismissLoadingDialog() {
        lifecycleScope.launch {
            progressDialogFragment?.dismiss(parentFragmentManager)
        }
    }

    protected abstract fun onImagePicked(uri: Uri?)

    protected abstract fun onFilePicked(uri: Uri?)

    /**
     * open file picker
     */
    protected fun pickFile() {
        activityResultLauncher!!.launch(
            arrayOf(
                "*/*"
            )
        )
    }

    protected fun exitStandByMode() {
        HidHelper.exitStandByMode()
    }

    protected fun enterStandByMode() {
        HidHelper.enterStandByMode()
    }

    companion object {
        val CAMERA_SURFACE_HOLDER_LOCK: Any = Any()
        val RECT_INFO_SURFACE_HOLDER_LOCK: Any = Any()
    }
}
