package com.armatura.biomodule.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.AboutActivity
import com.armatura.biomodule.activity.BioRecognizeSettingActivity
import com.armatura.biomodule.activity.SystemSettingActivity
import com.armatura.biomodule.activity.UserManageActivity
import com.armatura.biomodule.activity.adapter.IdentifyInfoAdapter
import com.armatura.biomodule.camera.AMTCameraView
import com.armatura.biomodule.camera.CameraController
import com.armatura.biomodule.common.Common
import com.armatura.biomodule.common.IdentifyState
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.ActivityAmaturaBinding
import com.armatura.biomodule.dialog.AboutVersionDialog
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.pojo.setting.CommonSettingData
import com.armatura.biomodule.pojo.setting.FuncSettings.SensorType
import com.armatura.biomodule.thread.AMTWorkManager
import com.armatura.biomodule.util.safeShowAlertDialog
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.viewmodel.AMTViewModel
import com.armatura.constant.RebootType
import com.armatura.internaldata.activity.ModuleUserManageActivity
import com.armatura.translib.AMTHidManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

class ArmaturaFragment : Fragment() {

    companion object {
        private const val TAG = "ArmaturaFragment"
        private const val REQUEST_PERMISSION_CODE = 1001
        private val PERMISSIONS = mutableListOf<String>().apply {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        var CUR_USER_PIN: String? = null
    }

    private var _binding: ActivityAmaturaBinding? = null
    private val binding get() = _binding!!
    private lateinit var amtViewModel: AMTViewModel
    private lateinit var cameraView: AMTCameraView

    private val identifyInfoAdapter by lazy { IdentifyInfoAdapter() }
    private var syncConfigDialog: ProgressDialogFragment? = null
    private lateinit var settingResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater, R.layout.activity_amatura, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Using requireActivity() to get the Activity context for ViewModel
        amtViewModel = ViewModelProvider(requireActivity())[AMTViewModel::class.java]

        settingResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { intent ->
                if (intent.resultCode == android.app.Activity.RESULT_OK) {
                    safeShowAlertDialog(
                        getString(R.string.tip_restart_app_2),
                        iconRes = android.R.drawable.ic_dialog_info,
                        positiveAction = {
                            AMTHidManager.instance().reboot(RebootType.REBOOT)
                        },
                        cancelable = false
                    )
                }
            }

        cameraView = AMTCameraView(
            binding.centerView.frameLayoutPreview,
            amtViewModel.drawFaceDataLiveData,
            amtViewModel.currentIdentifyState,
            amtViewModel.cardInfoLiveData,
            amtViewModel.palmInfoLiveData,
            amtViewModel.identifyFailedDataLiveData
        )

        binding.state = Common.state
        // Using viewLifecycleOwner to observe LiveData
        amtViewModel.currentIdentifyState.observe(viewLifecycleOwner) { identifyState ->
            Common.state = identifyState
            binding.state = identifyState
            when (identifyState) {
                IdentifyState.IDENTIFY_ONCE, IdentifyState.IDENTIFY_CONST -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        CameraController.instance().resumeCam(cameraView)
                    }
                    amtViewModel.exitStandByMode()
                    cameraView.enterSavePowerModeWhenIdle()
                }

                else -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        CameraController.instance().pauseCam(cameraView)
                        cameraView.clearVideoDataView()
                        cameraView.clearCustomInfoView()
                    }
                    identifyInfoAdapter.clearData()
                    amtViewModel.enterStandByMode()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            }
        }

        if (isAllPermissionGranter()) {
            onPermissionGranted()
        } else {
            requestPermission()
        }

        with(binding.rightView) {
            tvInformation.movementMethod = ScrollingMovementMethod.getInstance()
            tvNoteContent.movementMethod = ScrollingMovementMethod.getInstance()
            btnIdentifyOnce.setOnClickListener {
                amtViewModel.currentIdentifyState.value = IdentifyState.IDENTIFY_ONCE.apply {
                    isIdentified = false
                }
            }

            btnIdentifyConst.setOnClickListener {
                amtViewModel.currentIdentifyState.value = IdentifyState.IDENTIFY_CONST
            }

            btnStop.setOnClickListener {
                amtViewModel.currentIdentifyState.value = IdentifyState.STOP
                editUserPin.setText("")
                CUR_USER_PIN = null
            }
            identifyInfoRecyclerView.adapter = identifyInfoAdapter
            btnConfirmUserPin.setOnClickListener {
                val userPin = editUserPin.text.toString()
                if (userPin.isEmpty()) {
                    return@setOnClickListener
                } else {
                    CUR_USER_PIN = userPin
                    toastAnywhere("pin:$userPin")
                }
            }
            elUserPin.setEndIconOnClickListener {
                editUserPin.setText("")
                CUR_USER_PIN = null
            }
        }
        with(binding.leftView) {
            tvWelcomeTips.movementMethod = ScrollingMovementMethod.getInstance()
            ivLogo.setOnClickListener {
                AboutVersionDialog.newInstance().show(parentFragmentManager)
            }
            btnUserManage.setOnClickListener {
                UserManageActivity.actionTransition(requireActivity())
            }
            btnModuleUserManage.setOnClickListener {
                ModuleUserManageActivity.action(requireActivity())
            }

            btnSettingMenu.setOnClickListener { view ->
                val popupMenu = PopupMenu(requireContext(), view)
                popupMenu.menuInflater.inflate(R.menu.popu_main_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.nav_show_register_list -> {
                            UserManageActivity.actionTransition(requireActivity())
                        }

                        R.id.nav_about_device -> {
                            AboutActivity.action(requireContext())
                        }

                        R.id.nav_system_setting -> {
                            settingResultLauncher.launch(
                                Intent(requireContext(), SystemSettingActivity::class.java)
                            )
                        }

                        R.id.nav_bio_recognize_config -> {
                            settingResultLauncher.launch(
                                Intent(
                                    requireContext(),
                                    BioRecognizeSettingActivity::class.java
                                )
                            )
                        }

                        R.id.nav_exit -> {
                            amtViewModel.release()
                            exitProcess(0)
                        }
                    }
                    return@setOnMenuItemClickListener false
                }
                popupMenu.show()
            }
            btnAbout.setOnClickListener {
                AboutVersionDialog.newInstance().show(parentFragmentManager)
            }
        }
        binding.ivSwitchCamera.setOnClickListener {
            if (Config.videoStreamMode != CommonSettingData.VIDEO_STREAM_MODE_ALL) {
                safeShowAlertDialog(
                    message = getString(R.string.switch_video_stream_mode_warning),
                    title = getString(R.string.title_warning),
                    positiveAction = {
                        lifecycleScope.launch() {
                            withContext(Dispatchers.IO) {
                                CameraController.instance().switchCamera()
                            }
                            cameraView.clearVideoDataView()
                        }
                    },
                    cancelable = false
                )
            } else {
                CameraController.instance().switchCamera()
            }
        }

        amtViewModel.errorTipsLiveData.observe(viewLifecycleOwner) { value ->
            val errorCode = value.getInt(AMTViewModel.BUNDLE_KEY_ERROR_CODE)
            when (errorCode) {
                AMTViewModel.ERROR_CODE_VERSION_NOT_MATCH -> {
                    safeShowAlertDialog(
                        message = getString(
                            R.string.version_not_match_tips,
                            value.getString(AMTViewModel.BUNDLE_KEY_VERSION)
                        ),
                        title = getString(R.string.title_warning)
                    )
                }

                AMTViewModel.ERROR_CODE_USB_PERMISSION_DENIED -> {
                    safeShowAlertDialog(
                        message = value.getString(AMTViewModel.BUNDLE_KEY_ERROR_MSG) ?: "",
                        title = getString(R.string.title_warning),
                        cancelable = false,
                        positiveAction = {
                            amtViewModel.requestPermissionWhenDenied(
                                requireContext(),
                                cameraView
                            )
                        }
                    )
                }

                AMTViewModel.ERROR_CODE_HID_NOT_READY, AMTViewModel.ERROR_CODE_TIME_OUT -> {
//                    safeShowAlertDialog(
//                        message = resources.getString(
//                            R.string.hid_comm_exception
//                        ),
//                        title = getString(R.string.title_warning),
//                        cancelable = false
//                    )
                }
            }
        }
    }

    private fun onPermissionGranted() {
        amtViewModel.init(cameraView)
        // Using viewLifecycleOwner to observe LiveData
        amtViewModel.drawFaceDataLiveData.observe(viewLifecycleOwner) { value ->
            identifyInfoAdapter.addFaceIdentifyInfo(value)
        }

        amtViewModel.palmInfoLiveData.observe(viewLifecycleOwner) { value ->
            identifyInfoAdapter.addPalmIdentifyInfo(value)
        }

        amtViewModel.cardInfoLiveData.observe(viewLifecycleOwner) {
            identifyInfoAdapter.addCardInfo(it)
        }

        amtViewModel.initDbLiveData.observe(viewLifecycleOwner) {
            if (it) {
                dismissProgressDialog()
                syncConfigDialog = ProgressDialogFragment.show(
                    parentFragmentManager,
                    title = "",
                    msg = "Database Initializing"
                )
            } else {
                dismissProgressDialog()
            }
        }
        amtViewModel.hidDeviceAttached.observe(viewLifecycleOwner) {
            if (it) {
                AMTWorkManager.startWorkManager(
                    this,
                    requireContext(),
                    amtViewModel.cpuTempLiveData,
                    amtViewModel.errorTipsLiveData,
                    amtViewModel.syncConfigFlag
                )
                amtViewModel.syncConfigFlag.value = false
            }
        }
        amtViewModel.usbPermissionLiveData.observe(viewLifecycleOwner) {
            if (it) {
                amtViewModel.openDeviceWhenPermissionGranted(requireContext(), cameraView)
            }
        }
        amtViewModel.cpuTempLiveData.observe(viewLifecycleOwner) {
            cameraView.updateCPUTemp("Temp: $it")
        }
        amtViewModel.syncConfigFlag.observe(viewLifecycleOwner) {
            if (it) {
                Log.i(TAG, "onPermissionGranted: dismiss")
                syncConfigDialog?.dismiss(parentFragmentManager)
                syncConfigDialog = null
                amtViewModel.currentIdentifyState.value = IdentifyState.IDENTIFY_CONST
                ModuleHeartbeatManager.getInstance().hearBeat()
                amtViewModel.openUVCDeviceIfConnect(requireContext(), cameraView)
                if (Config.shouldUseCircleIndicatorView()) {
                    binding.centerView.circleReflectView.visibility = View.VISIBLE
                } else {
                    binding.centerView.circleReflectView.visibility = View.GONE
                }
                binding.ivSwitchCamera.isVisible =
                    Config.instance().sensorType == SensorType.SENSOR_TYPE_RGB_AND_NIR
                amtViewModel.initHostMatchAlgorithm()
            } else {
                Log.i(TAG, "onPermissionGranted: dismiss and show")
                syncConfigDialog?.dismiss(parentFragmentManager)
                syncConfigDialog = null
                syncConfigDialog = ProgressDialogFragment.show(
                    parentFragmentManager,
                    "", "synchronizing", false
                )
            }
        }
    }

    private fun requestPermission() {
        requestPermissions(PERMISSIONS, REQUEST_PERMISSION_CODE)
    }

    private fun isAllPermissionGranter(): Boolean {
        var isPermissionGranted = true
        for (permission in PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionGranted = false
                break
            }
        }
        return isPermissionGranted
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (isAllPermissionGranter()) {
                onPermissionGranted()
            } else {
                requestPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: ")
        amtViewModel.resetTrackId()
        if (Common.state != IdentifyState.STOP) {
            amtViewModel.currentIdentifyState.value = IdentifyState.IDENTIFY_CONST
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause: ")
        if (Common.state != IdentifyState.STOP) {
            amtViewModel.currentIdentifyState.value = IdentifyState.STOP
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG, "onDestroyView: ")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.unInit()
        Log.i(TAG, "onDestroy: ")
    }

    private fun dismissProgressDialog() {
        syncConfigDialog?.dismissAllowingStateLoss()
        syncConfigDialog = null
    }
}