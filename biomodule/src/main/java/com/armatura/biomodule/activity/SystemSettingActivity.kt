package com.armatura.biomodule.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.armatura.LoggerHelper
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.activity.base.ExApplication
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.ActivitySystemSettingBinding
import com.armatura.biomodule.dialog.FileChooseDialog
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.pojo.setting.MotionDetectSetting
import com.armatura.biomodule.pojo.setting.SyncTime
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.safeShowAlertDialog
import com.armatura.biomodule.util.showCancelableAlertDialog
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.TextFileDialogFragment
import com.armatura.biomodule.view.ToolBar
import com.armatura.constant.ConfigType
import com.armatura.constant.DbgType
import com.armatura.constant.ErrorCode
import com.armatura.constant.FileType
import com.armatura.constant.RebootType
import com.armatura.constant.SendFileProgressListener
import com.armatura.translib.AMTHidManager
import com.daimajia.numberprogressbar.CustomProgressDialog
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rosuh.filepicker.config.FilePickerManager
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemSettingActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivitySystemSettingBinding


    companion object {
        private const val TAG = "SystemSettingActivity"
        private const val SEND_FILE = 1012
        private const val SEND_FIRMWARE_FILE_PROGRESS = 102
        private const val SEND_FILE_SUCCESS = 1013
        private const val UPGRADE_SUCCESS = 1014
        private const val SYNC_TIME = 1015
        private const val UPDATE_MODULE_TIME = 1016
        private const val WAIT_UNTIL_REBOOT_FINISH = 1017
        private const val SHOW_TOAST = 103
        private const val PICK_FIRMWARE = 2001
        private const val PICK_IMAGE = 2002
        const val ACTION_HID_UPGRADE = "com.armatura.upgrade"
        private const val STOP_REFRESH = 104
    }


    private var customProgressDialog: CustomProgressDialog? = null

    private var mMotionDetectSetting: MotionDetectSetting? = null

    private val waitRebootDialog by lazy {
        XPopup.Builder(this@SystemSettingActivity)
            .dismissOnBackPressed(false)
            .dismissOnTouchOutside(false)
            .asLoading("Rebooting")
    }

    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            SEND_FIRMWARE_FILE_PROGRESS -> {
                val progress = msg.arg1
                customProgressDialog?.setProgress(progress)

            }

            SEND_FILE -> {
                val transMode = msg.obj as Int
                val fileChooseDialog = FileChooseDialog()
                val bundle = Bundle()
                bundle.putInt(FileChooseDialog.TRANS_MODE_KEY, transMode)
                fileChooseDialog.arguments = bundle
                fileChooseDialog.show(supportFragmentManager, FileChooseDialog::class.java.name)
            }

            SHOW_TOAST -> {
                val msgStr = msg.obj as String
                toastAnywhere(msgStr)
            }

            SEND_FILE_SUCCESS -> {
                //dismiss dialog
                dismissUpgradeDialog()
                when (msg.obj as Int) {
                    FileType.FILE -> {
                        toastAnywhere("send file success")
                    }
                }
            }

            UPGRADE_SUCCESS -> {
                //dismiss dialog
                dismissUpgradeDialog()
                toastAnywhere("Upgrade Complete")
            }

            STOP_REFRESH -> {
                binding.motionDetectSrl.isRefreshing = false
            }

            SYNC_TIME -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val syncTime = SyncTime()
                    syncTime.syncTime = simpleDateFormat.format(Date())
                    val json = JSONUtil.getJsonString(syncTime)
                    val data = ByteArray(json.length)
                    System.arraycopy(json.toByteArray(), 0, data, 0, json.toByteArray().size)
                    val size = intArrayOf(1024)
                    val i = AMTHidManager.instance().setConfig(ConfigType.DEVICE_TIME, data, size)
                    if (i == 0) {
                        val result = String(data, 0, size[0])
                        Log.i(TAG, "syncTime: $result")
                        getModuleTime()
                    } else {
                        Log.e(TAG, "syncTime: $i")
                    }
                }
            }

            UPDATE_MODULE_TIME -> {
                val time = msg.obj as String
                binding.syncTimeView.setSettingHint(time)
            }

            WAIT_UNTIL_REBOOT_FINISH -> {
                waitRebootDialog.show()
            }
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_system_setting)

        binding.myToolBar.setToolBarClickListener(object : ToolBar.ToolBarClickListener {
            override fun onClickLeft() {
                onBackPressedDispatcher.onBackPressed()
            }

            override fun onClickRight() {
                Config.instance().isTestMode = binding.testMode.isChecked()

                Config.instance().isNeedPictureWhenCacheReg =
                    binding.requestPictureWhenRegisterWithCacheId.isChecked()
                Config.instance().isNeedFeatureWhenCacheReg =
                    binding.requestTemplateWhenRegisterWithCacheId.isChecked()
                Config.instance().isNeedInfoWhenCacheReg =
                    binding.requestInfoWhenRegisterWithCacheId.isChecked()
                Config.instance().isDisplayLivenessInfo =
                    binding.displayLivenessInfoSwitchView.isChecked()

                Config.instance().isShowFaceInfo = binding.showFaceInfoSetView.isChecked()

                //saveMotionConfigSetting()

                val saveRecognizeMode =
                    when (binding.recognizeModelSetView.getSelectionValue() as String) {
                        "Host" -> Config.HOST_MODE
                        "MultiBioModule" -> Config.MULTI_BIO_MODULE_INTERNAL_MODE
                        else -> Config.HOST_MODE
                    }

                if (Config.instance().recognizeMode != saveRecognizeMode) {
                    Config.instance().recognizeMode = saveRecognizeMode
                    RecognizedBioDataCache.instance().clearRecFaces()
                }

                Config.instance().maxIdentifyFailedCount =
                    binding.maxPalmIdentifyFailedCount.getSeekBarValue()

                Config.instance().drawPalmRectWhenIdentify =
                    binding.drawPalmRectWhenIdentify.isChecked()

                Config.instance().identifyInfoStayTime =
                    binding.identifyInfoStayTime.getSeekBarValue()

                Config.instance().greedLedLightingDuration =
                    binding.greedLedLightingDurationSeekBar.getSeekBarValue()

                Config.instance().redLedLightingDuration =
                    binding.redLedLightingDurationSeekBar.getSeekBarValue()

                //attribute display settings
                with(binding) {
                    Config.instance().isShowFacePose = showFacePoseSetView.isChecked()
                    Config.instance().isShowAgeAttribute = showAgeInfoSetView.isChecked()
                    Config.instance().isShowGenderAttribute = showGenderInfoSetView.isChecked()
                    Config.instance().isShowExpressionAttribute =
                        showExpressionInfoSetView.isChecked()
                    Config.instance().isShowMustacheAttribute = showMustacheInfoSetView.isChecked()
                    Config.instance().isShowGlassesAttribute = showGlassesInfoSetView.isChecked()
                    Config.instance().isShowHatAttribute = showHatInfoSetView.isChecked()
                    Config.instance().isShowMaskAttribute = showMaskInfoSetView.isChecked()

                }

                Config.instance().powerSaveMode = binding.powerSaveModeSwitchView.isChecked()

                Config.instance().sdkLogLevel =
                    binding.sdkLogLevelSpinner.getSelectionItemPosition() + 2
                Log.i(TAG, "onClickRight: SDK level=" + Config.instance().sdkLogLevel)
                LoggerHelper.setLogLevel(Config.instance().sdkLogLevel)


                Config.instance().isDisplayCPUTempInfo =
                    binding.displayCPUTempInfoSwitchView.isChecked()

                Config.instance().isShowDetailFaceInfo =
                    binding.showDetailFaceInfoSwitchView.isChecked()

                Config.instance().isNeedShowFPS =
                    binding.displayFPSInfoSwitchView.isChecked()

                Config.instance().save(this@SystemSettingActivity)

                finish()
            }
        })

        initView()
        //initMotionDetectData()
        getModuleTime()
        register()
    }

    private fun dismissUpgradeDialog() {
        customProgressDialog?.dismiss()
        customProgressDialog = null
    }

    private fun getModuleTime() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = ByteArray(256)
            val size = IntArray(1)
            size[0] = result.size
            AMTHidManager.instance().getConfig(ConfigType.DEVICE_TIME, result, size).let {
                when (it) {
                    0 -> {
                        val resultStr = String(result, 0, size[0])
                        if (TextUtils.isEmpty(resultStr)) {
                            return@let
                        }
                        try {
                            val jsonObject = JSONObject(resultStr)
                            if (jsonObject.get("status") == 0 && jsonObject.has("data")) {
                                val data = jsonObject.get("data").toString()
                                val timeObj = JSONObject(data)
                                if (timeObj.has("sysTime")) {
                                    val sysTime = timeObj.get("sysTime").toString()
                                    handler.obtainMessage(UPDATE_MODULE_TIME, sysTime)
                                        .sendToTarget()
                                } else {
                                    Log.e(TAG, "getModuleTime: json invalid,$resultStr")
                                }
                            } else {
                                Log.e(TAG, "getModuleTime: failed")
                            }
                        } catch (_: JSONException) {
                        }
                    }

                    else -> Log.e(TAG, "getModuleTime: failed,$it")
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregister()
    }


    /**
     * get motion detect setting
     * No more use.From 2023.1.4
     */
    private fun initMotionDetectData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = ByteArray(2048)
            val size = IntArray(1)
            val ret =
                AMTHidManager.instance().getConfig(ConfigType.MOTION_DETECT_CONFIG, data, size)
            if (ret == 0) {
                try {
                    String(data, 0, size[0]).also {
                        val motionDetectSetting = JSONUtil.getMotionDetectSetting(it)
                        if (motionDetectSetting != null) {
                            mMotionDetectSetting = motionDetectSetting
                            withContext(Dispatchers.Main) {
                                binding.motionDetectConfig = motionDetectSetting
                            }
                        }
                    }
                } finally {
                    handler.obtainMessage(STOP_REFRESH).sendToTarget()
                }
            } else {
                Log.e(TAG, "initData: get failed,ret = $ret")
                withContext(Dispatchers.Main) {
                    toastAnywhere("Get Motion Config failed,$ret")
                }
            }
        }
    }


    /**
     * set MotionConfigSetting
     *     No more use.From 2023.1.4
     */
    fun saveMotionConfigSetting() {
        mMotionDetectSetting?.let {
            val motionDetectSettingObject = JSONObject().apply {
                put("motionDetectFunOn", binding.motionDetectFunOnSwitch.isChecked())
                put("brightnessThreshold", binding.brightnessThrSetView.getSeekBarValue())
                put("idleTimeOutMS", binding.idleTimeOutThrSetView.getSeekBarValue())
                put("sensitivityThreshold", binding.sensitivityThresholdSetView.getSeekBarValue())
            }

            val jsonObject = JSONObject()
            jsonObject.put("MotionDetectionSetting", motionDetectSettingObject)

            Log.i(TAG, "saveMotionConfigSetting: $jsonObject")
            val json = jsonObject.toString().toByteArray()
            val size = IntArray(1) { json.size }
            AMTHidManager.instance().setConfig(ConfigType.MOTION_DETECT_CONFIG, json, size)
                .let { ret ->
                    when (ret) {
                        0 -> toastAnywhere("save motion setting success")
                        else -> toastAnywhere("save motion setting failed,$ret")
                    }
                }
        }
    }

    fun initView() {
        binding.systemConfig = Config.instance()
        binding.systemUpdateView.setOnClickListener(this)
        binding.firmwareUpdateView.setOnClickListener(this)
        binding.brightnessThrSetView.setOnClickListener(this)
        binding.idleTimeOutThrSetView.setOnClickListener(this)
        binding.sensitivityThresholdSetView.setOnClickListener(this)
        binding.syncTimeView.setOnClickListener(this)

        binding.motionDetectSrl.setColorSchemeColors(Color.BLUE, Color.YELLOW, Color.GREEN)
        binding.motionDetectSrl.setOnRefreshListener {
//            initMotionDetectData()
            handler.obtainMessage(STOP_REFRESH).sendToTarget()
        }

        binding.restoreFactoryView.setOnClickListener {
            safeShowAlertDialog(
                message = getString(R.string.module_reset_factory),
                positiveAction = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val json = ByteArray(1024)
                        val size = IntArray(1) { json.size }
                        AMTHidManager.instance().setConfig(ConfigType.RESTORE_FACTORY, json, size)
                            .let {
                                withContext(Dispatchers.Main) {
                                    when (it) {
                                        0 -> {
                                            toastAnywhere("restore success")
                                            setResult(RESULT_OK)
                                            //initMotionDetectData()
                                        }

                                        else -> toastAnywhere("restore failed $it")
                                    }
                                }
                            }
                    }
                }
            )
        }

        if (!Config.isSupportFace) {
            binding.faceDisplaySettingsLayout.visibility = View.GONE

        }

        if (!Config.isSupportPalm) {
            binding.drawPalmRectWhenIdentify.visibility = View.GONE
            binding.maxPalmIdentifyFailedCount.visibility = View.GONE
        }

        if (!Config.isSupportIndicator) {
            binding.greedLedLightingDurationSeekBar.visibility = View.GONE
            binding.redLedLightingDurationSeekBar.visibility = View.GONE
        }

        if (Config.isSupportStoreInModule) {
            binding.recognizeModelSetView.visibility = View.VISIBLE
        } else {
            binding.recognizeModelSetView.visibility = View.GONE
        }

        binding.btnConnectHistoryLog.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val dialog = TextFileDialogFragment.newInstance()
                withContext(Dispatchers.Main) {
                    dialog.show(
                        supportFragmentManager,
                        TextFileDialogFragment::class.java.name
                    )
                }
                File(ExApplication.instance().externalCacheDir, "record.txt").also {
                    val content = if (it.exists()) {
                        it.readText()
                    } else {
                        "No connection history log"
                    }
                    withContext(Dispatchers.Main) {
                        dialog.setContent(content)
                    }
                }
            }
        }

        binding.btnViewWatchLog.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val dialog = TextFileDialogFragment.newInstance()
                withContext(Dispatchers.Main) {
                    dialog.show(
                        supportFragmentManager,
                        TextFileDialogFragment::class.java.name
                    )
                }
                File(ExApplication.instance().externalCacheDir, "watchdog.log").also {
                    val content = if (it.exists()) {
                        it.readText()
                    } else {
                        "No watch dog log"
                    }
                    withContext(Dispatchers.Main) {
                        dialog.setContent(content)
                    }
                }
            }
        }

        binding.btnRecuseLog.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val dialog = TextFileDialogFragment.newInstance()
                withContext(Dispatchers.Main) {
                    dialog.show(
                        supportFragmentManager,
                        TextFileDialogFragment::class.java.name
                    )
                }
                File(ExApplication.instance().externalCacheDir, "rescue.log").also {
                    val content = if (it.exists()) {
                        it.readText()
                    } else {
                        "No rescue log"
                    }
                    withContext(Dispatchers.Main) {
                        dialog.setContent(content)
                    }
                }
            }
        }

        binding.btnViewModuleKernelLog.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val textDialogFragment = TextFileDialogFragment.newInstance()
                withContext(Dispatchers.Main) {
                    textDialogFragment.show(
                        supportFragmentManager,
                        TextFileDialogFragment::class.java.name
                    )
                }
                val buffer = ByteArray(2 * 1024 * 1024)
                val size = IntArray(1)
                size[0] = buffer.size
                val ret = AMTHidManager.instance().dbg(DbgType.SYSTEM, buffer, size)
                val content = if (ret == ErrorCode.ERROR_NONE) {
                    String(buffer, 0, size[0])
                } else {
                    "Get kernel log failed:$ret"
                }
                withContext(Dispatchers.Main) {
                    textDialogFragment.setContent(content)
                }
            }
        }


        binding.btnViewModuleFirmwareLog.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val textDialogFragment = TextFileDialogFragment.newInstance()
                withContext(Dispatchers.Main) {
                    textDialogFragment.show(
                        supportFragmentManager,
                        TextFileDialogFragment::class.java.name
                    )
                }

                val buffer = ByteArray(2 * 1024 * 1024)
                val size = IntArray(1)
                size[0] = buffer.size
                val ret = AMTHidManager.instance().dbg(DbgType.FIRMWARE, buffer, size)
                val content = if (ret == ErrorCode.ERROR_NONE) {
                    String(buffer, 0, size[0])
                } else {
                    "Get firmware log failed:$ret"
                }
                withContext(Dispatchers.Main) {
                    textDialogFragment.setContent(content)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.systemUpdateView -> {
                safeShowAlertDialog(
                    title = resources.getString(R.string.camcfg_upgrade_reboot_tip),
                    iconRes = android.R.drawable.ic_dialog_info,
                    positiveAction = {
                        AMTHidManager.instance().reboot(RebootType.REBOOT_UPGRADE)
                        handler.obtainMessage(WAIT_UNTIL_REBOOT_FINISH).sendToTarget()
                    }
                )
            }

            R.id.firmwareUpdateView -> {
                //send file or firmware
                FilePickerManager.from(this).forResult(PICK_FIRMWARE)
            }

            R.id.syncTimeView -> {
                handler.obtainMessage(SYNC_TIME).sendToTarget()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_FIRMWARE -> {
                    val filePath = FilePickerManager.obtainData()[0]
                    SendFileThread(FileType.FILE, filePath, object : SendFileProgressListener {
                        override fun onProgressUpdate(progress: Int) {
                            //progress update
                            handler.obtainMessage(SEND_FIRMWARE_FILE_PROGRESS, progress, 0)
                                .sendToTarget()
                        }

                        override fun onSendFileComplete() {
                            //only happen when send file
                            handler.obtainMessage(SEND_FILE_SUCCESS, FileType.FILE).sendToTarget()
                        }

                        override fun onUpgradeComplete() {
                            //only happen when send system image
                            handler.obtainMessage(UPGRADE_SUCCESS).sendToTarget()
                        }

                        override fun onError(errorCode: Int, message: String?) {
                            Log.e(TAG, "onError: code=$errorCode ,msg=$message")
                            lifecycleScope.launch {
                                showCancelableAlertDialog("Upgrade Firmware Failed", message ?: "")
                                dismissUpgradeDialog()
                            }
                        }

                    }).start()
                    showUploadProgressDialog()
                }

                PICK_IMAGE -> {
                    //disable heartbeat
                    ModuleHeartbeatManager.getInstance().heatBeatStop()
                    val filePath = FilePickerManager.obtainData()[0]
                    SendFileThread(
                        FileType.SYSTEM_IMAGE,
                        filePath,
                        object : SendFileProgressListener {
                            override fun onProgressUpdate(progress: Int) {
                                //progress update
                                handler.obtainMessage(SEND_FIRMWARE_FILE_PROGRESS, progress, 0)
                                    .sendToTarget()
                            }

                            override fun onSendFileComplete() {
                                //only happen when send file
                                handler.obtainMessage(SEND_FILE_SUCCESS, FileType.SYSTEM_IMAGE)
                                    .sendToTarget()
                            }

                            override fun onUpgradeComplete() {
                                //only happen when send system image
                                handler.obtainMessage(UPGRADE_SUCCESS).sendToTarget()
                            }

                            override fun onError(errorCode: Int, message: String?) {
                                Log.e(TAG, "onError: code=$errorCode ,msg=$message")
                                lifecycleScope.launch {
                                    showCancelableAlertDialog("Upgrade IMG Failed", message ?: "")
                                    dismissUpgradeDialog()
                                }
                            }

                        }).start()
                    showUploadProgressDialog()
                }
            }
        }
    }

    private fun showUploadProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog = null
        }
        customProgressDialog = CustomProgressDialog(this, R.style.CustomInputDialogStyle)
        customProgressDialog?.apply {
            setCancelable(false)
            show()
        }
    }


    /**
     * it's better to send file in a thread,because it will take a lot of time
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private class SendFileThread(
        private var mode: Int,
        private var filePath: String,
        private var listener: SendFileProgressListener,
    ) : Thread() {

        override fun run() {
            val sendFileRet = AMTHidManager.instance().sendFile(mode, filePath, listener)
            Log.i(TAG, "sendFile ret = $sendFileRet")
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun register() {
        val filter = IntentFilter().apply {
            addAction(ACTION_HID_UPGRADE)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(upgradeReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(upgradeReceiver, filter)
        }
    }

    private fun unregister() {
        unregisterReceiver(upgradeReceiver)
    }


    /**
     *  broadcast from internal
     */
    private val upgradeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_HID_UPGRADE != action) {
                if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                    dismissUpgradeDialog()
                }
            } else {
                waitRebootDialog?.dismiss()
                FilePickerManager.from(this@SystemSettingActivity).forResult(PICK_IMAGE)
            }
        }

    }
}