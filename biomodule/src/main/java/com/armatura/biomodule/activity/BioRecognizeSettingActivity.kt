package com.armatura.biomodule.activity

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.ActivityFaceRecognizeSettingBinding
import com.armatura.biomodule.databinding.LayoutCaptureFilterSettingsBinding
import com.armatura.biomodule.databinding.LayoutCommonSettingsBinding
import com.armatura.biomodule.databinding.LayoutDeviceSettingsBinding
import com.armatura.biomodule.databinding.LayoutFaceSettingsBinding
import com.armatura.biomodule.databinding.LayoutPalmSettingsBinding
import com.armatura.biomodule.dialog.CustomInputDialog
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.pojo.setting.CommonSettingData
import com.armatura.biomodule.pojo.setting.DeviceSettings
import com.armatura.biomodule.pojo.setting.FaceSettings
import com.armatura.biomodule.pojo.setting.FuncSettings.SensorType
import com.armatura.biomodule.pojo.setting.VLPalmSetting
import com.armatura.biomodule.util.JSONUtil
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.safeShowAlertDialog
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.CustomSettingLayout
import com.armatura.biomodule.view.ToolBar
import com.armatura.biomodule.viewmodel.SettingsViewModel
import com.armatura.constant.ConfigType
import com.armatura.constant.ErrorCode
import com.armatura.translib.AMTHidManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class BioRecognizeSettingActivity : BaseActivity(), ToolBar.ToolBarClickListener,
    View.OnClickListener {
    companion object {
        private const val TAG = "BioRecognizeSetting"
    }

    @Deprecated("use defaultLivenessMode property instead")
    private var defaultNIRLiveness = false

    @Deprecated("use defaultLivenessMode property instead")
    private var defaultVLLiveness = false

    private var defaultLivenessMode = FaceSettings.LIVENESS_MODE_DISABLE

    private var defaultPalmFunOn = false
    private var defaultPalmLiveness = false
    private var defaultSensorFrame = 15
    private var defaultStreamMode = 0
    private var defaultPalmTemplateMode = 0

    private lateinit var binding: ActivityFaceRecognizeSettingBinding

    private var commonSettingViewBinding: LayoutCommonSettingsBinding? = null
    private var captureFilterSettingViewBinding: LayoutCaptureFilterSettingsBinding? = null
    private var palmSettingViewBinding: LayoutPalmSettingsBinding? = null
    private var deviceSettingViewBinding: LayoutDeviceSettingsBinding? = null
    private var faceSettingViewBinding: LayoutFaceSettingsBinding? = null

    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_face_recognize_setting
        )

        binding.faceRecognizeToolBar.setToolBarClickListener(this)
        ViewCompat.setTransitionName(binding.faceRecognizeToolBar, TAG)

        initView()
    }

    override fun onResume() {
        super.onResume()
        //send command to get config
        refreshConfig()
    }

    private fun refreshConfig() {
        lifecycleScope.launch {
            val loadingDialog = ProgressDialogFragment.show(
                supportFragmentManager,
                "",
                "Loading", false
            )
            withContext(Dispatchers.IO) {
                if (Config.isSupportFace) {
                    getCaptureFilterConfig()
                }
                if (Config.isSupportPalm) {
                    getPalmSetting()
                }
                if (!getCommonSetting()) {
                    getDeviceSetting()
                    if (Config.isSupportFace) {
                        getFaceSetting()
                    }
                }
            }
            loadingDialog.dismiss()
            binding.bioParameterSrl.isRefreshing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.unbind()
    }

    private fun initView() {
        //pull down to get config again
        binding.bioParameterSrl.setColorSchemeColors(Color.BLUE, Color.YELLOW, Color.GREEN)
        binding.bioParameterSrl.setOnRefreshListener {
            refreshConfig()
        }
    }


    private suspend fun initCommonSettingView(commonSettingJSONObject: JSONObject) {
        val commonSetting = withContext(Dispatchers.IO) {
            return@withContext JSONUtil.getCommonSetting(commonSettingJSONObject.toString())
        }
        if (commonSetting == null) {
            Log.w(TAG, "initCommonSettingView: get common setting failed!")
            return
        }

        //when this value changed,should tell user to reboot module
        with(commonSettingJSONObject) {
            if (has("NIRLiveness")) {
                defaultNIRLiveness = commonSetting.NIRLiveness
            }
            if (has("VLLiveness")) {
                defaultVLLiveness = commonSetting.VLLiveness
            }
            if (has("sensorFrameRate")) {
                defaultSensorFrame = commonSetting.sensorFrameRate
            }
            if (has("videoStreamMode")) {
                defaultStreamMode = commonSetting.videoStreamMode
            }
        }

        withContext(Dispatchers.Main) {
            if (!binding.commonSettingsLayoutViwStub.isInflated) {
                val commonSettingView = binding.commonSettingsLayoutViwStub.viewStub!!.inflate()
                commonSettingViewBinding = DataBindingUtil.getBinding(commonSettingView)
            }

            with(commonSettingViewBinding!!) {
                this.commonSettings = commonSetting
                //common setting
                attrIntervalSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                recogIntervalSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                recognizeThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                attendIntervalSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                hacknessThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)


                changeVisibility(VLLivenessSwitch, commonSettingJSONObject.has("VLLiveness"))
                changeVisibility(NIRLivenessSwitch, commonSettingJSONObject.has("NIRLiveness"))
                changeVisibility(
                    attendIntervalSetView,
                    commonSettingJSONObject.has("attendInterval")
                )
                changeVisibility(
                    attrIntervalSetView,
                    commonSettingJSONObject.has("attrInterval")
                )
                changeVisibility(
                    attributeRecogSwitchView,
                    commonSettingJSONObject.has("attributeRecog")
                )
                changeVisibility(
                    saveAttendanceSwitchView,
                    commonSettingJSONObject.has("enableStoreAttendLog")
                )
                changeVisibility(
                    saveStrangerAttendanceSwitchView,
                    commonSettingJSONObject.has("enableStoreStrangerAttLog")
                )
                changeVisibility(faceAESwitch, commonSettingJSONObject.has("faceAEEnabled"))
                changeVisibility(
                    hacknessThresholdSetView,
                    commonSettingJSONObject.has("hacknessThreshold")
                )
                changeVisibility(
                    trackMatchSwitchView,
                    commonSettingJSONObject.has("isTrackingMatchMode")
                )
                changeVisibility(
                    maxFaceSwitchView,
                    commonSettingJSONObject.has("maxFaceEnable")
                )
                changeVisibility(
                    pushPhotoEnableSwitchView,
                    commonSettingJSONObject.has("pushPhotoEnable")
                )
                changeVisibility(
                    recogIntervalSetView,
                    commonSettingJSONObject.has("recogInterval")
                )
                changeVisibility(
                    recognizeThresholdSetView,
                    commonSettingJSONObject.has("recogThreshold")
                )
                changeVisibility(
                    sensorFrameSpinner,
                    commonSettingJSONObject.has("sensorFrameRate")
                )
                changeVisibility(
                    standByModeSwitchView,
                    commonSettingJSONObject.has("standbyMode")
                )
                changeVisibility(
                    ledControlModeSpinner,
                    commonSettingJSONObject.has("ledControlMode")
                )
                changeVisibility(
                    ledBreathTypeSpinner,
                    commonSettingJSONObject.has("ledBreathType")
                )
                changeVisibility(
                    pushPhotoEnableSwitchView,
                    commonSettingJSONObject.has("pushPhotoEnable")
                )
                changeVisibility(
                    videoStreamModeSpinner,
                    commonSettingJSONObject.has("videoStreamMode")
                )
                changeVisibility(
                    pushDetectionDistanceSwitchView,
                    commonSettingJSONObject.has("pushDetectionDistance")
                )
                changeVisibility(
                    illuminationTriggerModeSpinner,
                    commonSettingJSONObject.has("illuminationTriggerMode")
                )
                changeVisibility(
                    detectionDistanceSeekBar,
                    commonSettingJSONObject.has("minDetectionDistance")
                            && commonSettingJSONObject.has("maxDetectionDistance")
                )
            }
        }
    }


    private suspend fun initFaceSettingView(faceSettingJSONObject: JSONObject) {
        val faceSetting = withContext(Dispatchers.IO) {
            return@withContext JSONUtil.getFaceSetting(faceSettingJSONObject.toString())
        }
        if (faceSetting == null) {
            Log.w(TAG, "initFaceSettingView: get face setting failed!")
            return
        }

        //when this value changed,should tell user to reboot module
        with(faceSettingJSONObject) {
            if (has("livenessMode")) {
                defaultLivenessMode = faceSetting.livenessMode
            }
        }

        withContext(Dispatchers.Main) {
            if (!binding.faceSettingsLayoutViwStub.isInflated) {
                val faceSettingView = binding.faceSettingsLayoutViwStub.viewStub!!.inflate()
                faceSettingViewBinding = DataBindingUtil.getBinding(faceSettingView)
            }

            with(faceSettingViewBinding!!) {
                this.faceSettings = faceSetting
                //face setting
                attrIntervalSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                recogIntervalSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                recognizeThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                faceVerifyThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                singleLensLivenessThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                dualLensLivenessThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)

                changeVisibility(
                    attrIntervalSetView,
                    faceSettingJSONObject.has("attrInterval")
                )
                changeVisibility(
                    attributeRecogSwitchView,
                    faceSettingJSONObject.has("attributeRecog")
                )
                changeVisibility(faceAESwitch, faceSettingJSONObject.has("faceAEEnabled"))
                changeVisibility(
                    trackMatchSwitchView,
                    faceSettingJSONObject.has("isTrackingMatchMode")
                )
                changeVisibility(
                    maxFaceSwitchView,
                    faceSettingJSONObject.has("maxFaceEnable")
                )
                changeVisibility(
                    recogIntervalSetView,
                    faceSettingJSONObject.has("recogInterval")
                )
                changeVisibility(
                    recognizeThresholdSetView,
                    faceSettingJSONObject.has("recogThreshold")
                )
                changeVisibility(
                    faceVerifyThresholdSetView,
                    faceSettingJSONObject.has("verifyThreshold")
                )
            }
        }
    }


    private suspend fun initDeviceSettingView(deviceSettingJSONObject: JSONObject) {
        val deviceSettings = withContext(Dispatchers.IO) {
            return@withContext JSONUtil.getDeviceSetting(deviceSettingJSONObject.toString())
        }
        if (deviceSettings == null) {
            Log.w(TAG, "initDeviceSettingView: get device setting failed!")
            return
        }

        //when this value changed,should tell user to reboot module
        with(deviceSettingJSONObject) {
            if (has("sensorFrameRate")) {
                defaultSensorFrame = deviceSettings.sensorFrameRate
            }
            if (has("videoStreamMode")) {
                defaultStreamMode = deviceSettings.videoStreamMode
            }
        }

        withContext(Dispatchers.Main) {
            if (!binding.deviceSettingsLayoutViwStub.isInflated) {
                val commonSettingView = binding.deviceSettingsLayoutViwStub.viewStub!!.inflate()
                deviceSettingViewBinding = DataBindingUtil.getBinding(commonSettingView)
            }

            with(deviceSettingViewBinding!!) {
                this.deviceSettings = deviceSettings

                attendIntervalSetView.setOnClickListener(this@BioRecognizeSettingActivity)

                changeVisibility(
                    attendIntervalSetView,
                    deviceSettingJSONObject.has("attendInterval")
                )
                changeVisibility(
                    saveAttendanceSwitchView,
                    deviceSettingJSONObject.has("enableStoreAttendLog")
                )
                changeVisibility(
                    saveStrangerAttendanceSwitchView,
                    deviceSettingJSONObject.has("enableStoreStrangerAttLog")
                )
                changeVisibility(
                    sensorFrameSpinner,
                    deviceSettingJSONObject.has("sensorFrameRate")
                )
                changeVisibility(
                    standByModeSwitchView,
                    deviceSettingJSONObject.has("standbyMode")
                )
                changeVisibility(
                    pushPhotoEnableSwitchView,
                    deviceSettingJSONObject.has("pushPhotoEnable")
                )
                changeVisibility(
                    videoStreamModeSpinner,
                    deviceSettingJSONObject.has("videoStreamMode")
                )
                changeVisibility(
                    dvPushDetectionDistanceSwitchView,
                    deviceSettingJSONObject.has("pushDetectionDistance")
                )
                changeVisibility(
                    ledBreathTypeSpinner,
                    deviceSettingJSONObject.has("ledBreathType")
                )
                changeVisibility(
                    ledControlModeSpinner,
                    deviceSettingJSONObject.has("ledControlMode")
                )
                changeVisibility(
                    illuminationTriggerModeSpinner,
                    deviceSettingJSONObject.has("illuminationTriggerMode")
                )
                changeVisibility(
                    detectionDistanceSeekBar,
                    deviceSettingJSONObject.has("minDetectionDistance")
                            && deviceSettingJSONObject.has("maxDetectionDistance")
                )
            }
        }
    }


    private suspend fun initCaptureFilterView(captureFilterJSONObject: JSONObject) {
        val captureFilterConfig = withContext(Dispatchers.IO) {
            return@withContext JSONUtil.getCaptureFilterConfig(captureFilterJSONObject.toString())
        }
        if (captureFilterConfig == null) {
            Log.w(TAG, "initCaptureFilterView: get capture filter config failed")
            return
        }
        withContext(Dispatchers.Main) {
            if (!binding.faceCaptureFilterLayoutViwStub.isInflated) {
                val captureFilterSettingsView =
                    binding.faceCaptureFilterLayoutViwStub.viewStub!!.inflate()
                captureFilterSettingViewBinding =
                    DataBindingUtil.bind(captureFilterSettingsView)
            }
            with(captureFilterSettingViewBinding!!) {
                this.captureFilterConfig = captureFilterConfig
                changeVisibility(
                    blurThrSetView,
                    captureFilterJSONObject.has("blurThreshold")
                )
                changeVisibility(
                    frontFaceThrSetView,
                    captureFilterJSONObject.has("frontThreshold")
                )
                changeVisibility(
                    faceDetectHeightRange,
                    captureFilterJSONObject.has("heightMaxValue")
                )
                changeVisibility(
                    maxPitchSetView,
                    captureFilterJSONObject.has("pitchMaxValue")
                )
                changeVisibility(
                    minPitchSetView,
                    captureFilterJSONObject.has("pitchMinValue")
                )
                changeVisibility(
                    maxRollSetView,
                    captureFilterJSONObject.has("rollMaxValue")
                )
                changeVisibility(
                    minRollSetView,
                    captureFilterJSONObject.has("rollMinValue")
                )
                changeVisibility(
                    qualityThrSetView,
                    captureFilterJSONObject.has("scoreThreshold")
                )
                changeVisibility(
                    faceDetectWidthRange,
                    captureFilterJSONObject.has("widthMaxValue")
                )
                changeVisibility(
                    maxYawSetView,
                    captureFilterJSONObject.has("yawMaxValue")
                )
                changeVisibility(
                    minYawSetView,
                    captureFilterJSONObject.has("yawMinValue")
                )

                //capture filter
                blurThrSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                frontFaceThrSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                qualityThrSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                maxPitchSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                minPitchSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                maxRollSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                minRollSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                maxYawSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                minYawSetView.setOnClickListener(this@BioRecognizeSettingActivity)
            }
        }

    }


    private fun changeVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showToast(msg: String) {
        lifecycleScope.launch {
            toastAnywhere(msg)
        }
    }


    /**
     * get common setting
     */
    private suspend fun getCommonSetting(): Boolean {
        settingsViewModel.getCommonSettings().also {
            if (it.code == ErrorCode.ERROR_NONE) {
                initCommonSettingView(it.result!!)
                return true
            } else {
                return false
            }
        }
    }

    private fun getFaceSetting() {
        settingsViewModel.getFaceSettingsFlow().flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                when (it.code) {
                    ErrorCode.ERROR_NONE -> initFaceSettingView(it.result!!)
                    else -> showToast(it.message)
                }
            }
    }


    private fun getDeviceSetting() {
        settingsViewModel.getDeviceSettingsFlow().flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                when (it.code) {
                    ErrorCode.ERROR_NONE -> initDeviceSettingView(it.result!!)
                    else -> showToast(it.message)
                }
            }
    }

    private suspend fun getViewVisibility(view: View): Boolean {
        return withContext(Dispatchers.Main) {
            return@withContext view.visibility == View.VISIBLE
        }
    }

    /**
     * set common setting
     */
    @Deprecated("use [setFaceSettings] and [setDeviceSettings]")
    private suspend fun setCommonSetting() {
        commonSettingViewBinding ?: return
        val mainJSONObject = JSONObject()
        val commonSettings = JSONObject()
        try {
            commonSettings.apply {
                with(commonSettingViewBinding!!) {
                    if (getViewVisibility(VLLivenessSwitch)) {
                        put("VLLiveness", VLLivenessSwitch.isChecked())
                    }
                    if (getViewVisibility(NIRLivenessSwitch)) {
                        put("NIRLiveness", NIRLivenessSwitch.isChecked())
                    }
                    if (getViewVisibility(attendIntervalSetView)) {
                        put("attendInterval", attendIntervalSetView.getSettingValue().toInt())
                    }
                    if (getViewVisibility(attrIntervalSetView)) {
                        put("attrInterval", attrIntervalSetView.getSettingValue().toInt())
                    }
                    if (getViewVisibility(attributeRecogSwitchView)) {
                        put("attributeRecog", attributeRecogSwitchView.isChecked())
                    }
                    if (getViewVisibility(saveAttendanceSwitchView)) {
                        put("enableStoreAttendLog", saveAttendanceSwitchView.isChecked())
                    }
                    if (getViewVisibility(saveStrangerAttendanceSwitchView)) {
                        put(
                            "enableStoreStrangerAttLog",
                            saveStrangerAttendanceSwitchView.isChecked()
                        )
                    }
                    if (getViewVisibility(faceAESwitch)) {
                        put("faceAEEnabled", faceAESwitch.isChecked())
                    }
                    if (getViewVisibility(hacknessThresholdSetView)) {
                        put(
                            "hacknessThreshold",
                            hacknessThresholdSetView.getSettingValue().toDouble()
                        )
                    }
                    if (getViewVisibility(trackMatchSwitchView)) {
                        put("isTrackingMatchMode", trackMatchSwitchView.isChecked())
                    }

                    if (getViewVisibility(sensorFrameSpinner)) {
                        put(
                            "sensorFrameRate",
                            (sensorFrameSpinner.getSelectionValue() as String).toInt()
                        )
                    }
                    if (getViewVisibility(maxFaceSwitchView)) {
                        put("maxFaceEnable", maxFaceSwitchView.isChecked())
                    }
                    if (getViewVisibility(recogIntervalSetView)) {
                        put("recogInterval", recogIntervalSetView.getSettingValue().toInt())
                    }
                    if (getViewVisibility(ledBreathTypeSpinner)) {
                        put("ledBreathType", ledBreathTypeSpinner.getSelectionItemPosition())
                    }
                    if (getViewVisibility(videoStreamModeSpinner)) {
                        put("videoStreamMode", videoStreamModeSpinner.getSelectionItemPosition())
                    }
                    if (getViewVisibility(recognizeThresholdSetView)) {
                        put(
                            "recogThreshold",
                            recognizeThresholdSetView.getSettingValue().toDouble()
                        )
                    }
                    if (getViewVisibility(ledControlModeSpinner)) {
                        put("ledControlMode", ledControlModeSpinner.getSelectionItemPosition())
                    }
                    if (getViewVisibility(pushPhotoEnableSwitchView)) {
                        put("pushPhotoEnable", pushPhotoEnableSwitchView.isChecked())
                    }
                    if (getViewVisibility(standByModeSwitchView)) {
                        put("standbyMode", standByModeSwitchView.isChecked())
                    }
                    if (getViewVisibility(pushDetectionDistanceSwitchView)) {
                        put("pushDetectionDistance", pushDetectionDistanceSwitchView.isChecked())
                    }
                    if (getViewVisibility(illuminationTriggerModeSpinner)) {
                        put(
                            "illuminationTriggerMode",
                            illuminationTriggerModeSpinner.getSelectionItemPosition()
                        )
                    }
                    if (getViewVisibility(detectionDistanceSeekBar)) {
                        put(
                            "minDetectionDistance",
                            detectionDistanceSeekBar.getFromValue().toInt()
                        )
                        put(
                            "maxDetectionDistance",
                            detectionDistanceSeekBar.getToValue().toInt()
                        )
                    }
                }
            }
            mainJSONObject.put(CommonSettingData.KEY, commonSettings)
            val json = mainJSONObject.toString().toByteArray()
            Log.i(TAG, "saveCommonSetting: $mainJSONObject")
            val size = IntArray(1) { json.size }
            withContext(Dispatchers.IO) {
                AMTHidManager.instance().setConfig(ConfigType.COMMON_CONFIG, json, size).let {
                    Log.i(TAG, "setCommonSetting: setConfig ret = $it")
                    if (it == 0) {
                        with(Dispatchers.Main) {
                            //update local threshold
                            with(commonSettingViewBinding!!) {
                                Config.instance().faceIdentifyThreshold =
                                    recognizeThresholdSetView.getSettingValue().toFloat()
                                Config.videoStreamMode =
                                    videoStreamModeSpinner.getSelectionItemPosition()
                                Config.isFeaturePhotoFunOn = pushPhotoEnableSwitchView.isChecked()
                                Config.controlLEDByHost =
                                    (ledControlModeSpinner.getSelectionItemPosition() == 1)
                            }

                        }
                        Log.i(TAG, "save common success")
                    } else {
                        Log.e(TAG, "setCommonSetting: failed,$it")
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }


    private suspend fun setFaceSettings() {
        faceSettingViewBinding ?: return
        val mainJSONObject = JSONObject()
        val faceSettingsJSONObject = JSONObject()
        try {
            faceSettingsJSONObject.apply {
                with(faceSettingViewBinding!!) {
                    if (getViewVisibility(faceAESwitch)) {
                        put("faceAEEnabled", faceAESwitch.isChecked())
                    }

                    if (getViewVisibility(trackMatchSwitchView)) {
                        put("isTrackingMatchMode", trackMatchSwitchView.isChecked())
                    }

                    if (getViewVisibility(maxFaceSwitchView)) {
                        put("maxFaceEnable", maxFaceSwitchView.isChecked())
                    }

                    if (getViewVisibility(livenessModeSpinner)) {
                        put("livenessMode", livenessModeSpinner.getSelectionItemPosition())
                    }

                    if (getViewVisibility(singleLensLivenessThresholdSetView)) {
                        put(
                            "singleLensLivenessThreshold",
                            singleLensLivenessThresholdSetView.getSettingValue().toFloat()
                        )
                    }

                    if (getViewVisibility(dualLensLivenessThresholdSetView)) {
                        put(
                            "dualLensLivenessThreshold",
                            dualLensLivenessThresholdSetView.getSettingValue().toFloat()
                        )
                    }


                    if (getViewVisibility(attrIntervalSetView)) {
                        put("attrInterval", attrIntervalSetView.getSettingValue().toInt())
                    }
                    if (getViewVisibility(attributeRecogSwitchView)) {
                        put("attributeRecog", attributeRecogSwitchView.isChecked())
                    }



                    if (getViewVisibility(recogIntervalSetView)) {
                        put("recogInterval", recogIntervalSetView.getSettingValue().toInt())
                    }
                    if (getViewVisibility(recognizeThresholdSetView)) {
                        put(
                            "recogThreshold",
                            recognizeThresholdSetView.getSettingValue().toDouble()
                        )
                    }
                    if (getViewVisibility(faceVerifyThresholdSetView)) {
                        put(
                            "verifyThreshold",
                            faceVerifyThresholdSetView.getSettingValue().toDouble()
                        )
                    }
                }
            }
            mainJSONObject.put(FaceSettings.KEY, faceSettingsJSONObject)
            val json = mainJSONObject.toString().toByteArray()
            Log.i(TAG, "setFaceSettings: $mainJSONObject")
            val size = IntArray(1) { json.size }
            withContext(Dispatchers.IO) {
                AMTHidManager.instance().setConfig(ConfigType.FACE_CONFIG, json, size).let {
                    Log.i(TAG, "setFaceSettings: setConfig ret = $it")
                    if (it == 0) {
                        with(Dispatchers.Main) {
                            //update local threshold
                            with(faceSettingViewBinding!!) {
                                Config.instance().faceIdentifyThreshold =
                                    recognizeThresholdSetView.getSettingValue().toFloat()
                                Config.instance().faceVerifyThreshold =
                                    faceVerifyThresholdSetView.getSettingValue().toFloat()
                            }
                        }
                        Log.i(TAG, "save face settings success")
                    } else {
                        Log.e(TAG, "set face settings: failed,$it")
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }


    private suspend fun setDeviceSettings() {
        deviceSettingViewBinding ?: return
        val mainJSONObject = JSONObject()
        val deviceSettingsJSONObject = JSONObject()
        try {
            deviceSettingsJSONObject.apply {
                with(deviceSettingViewBinding!!) {
                    if (getViewVisibility(attendIntervalSetView)) {
                        put("attendInterval", attendIntervalSetView.getSettingValue().toInt())
                    }
                    if (getViewVisibility(saveAttendanceSwitchView)) {
                        put("enableStoreAttendLog", saveAttendanceSwitchView.isChecked())
                    }
                    if (getViewVisibility(saveStrangerAttendanceSwitchView)) {
                        put(
                            "enableStoreStrangerAttLog",
                            saveStrangerAttendanceSwitchView.isChecked()
                        )
                    }

                    if (getViewVisibility(sensorFrameSpinner)) {
                        put(
                            "sensorFrameRate",
                            (sensorFrameSpinner.getSelectionValue() as String).toInt()
                        )
                    }
                    if (getViewVisibility(videoStreamModeSpinner)) {
                        put("videoStreamMode", videoStreamModeSpinner.getSelectionItemPosition())
                    }
                    if (getViewVisibility(pushPhotoEnableSwitchView)) {
                        put("pushPhotoEnable", pushPhotoEnableSwitchView.isChecked())
                    }
                    if (getViewVisibility(standByModeSwitchView)) {
                        put("standbyMode", standByModeSwitchView.isChecked())
                    }
                    if (getViewVisibility(dvPushDetectionDistanceSwitchView)) {
                        put("pushDetectionDistance", dvPushDetectionDistanceSwitchView.isChecked())
                    }
                    if (getViewVisibility(ledBreathTypeSpinner)) {
                        put("ledBreathType", ledBreathTypeSpinner.getSelectionItemPosition())
                    }
                    if (getViewVisibility(ledControlModeSpinner)) {
                        put("ledControlMode", ledControlModeSpinner.getSelectionItemPosition())
                    }
                    if (getViewVisibility(illuminationTriggerModeSpinner)) {
                        put(
                            "illuminationTriggerMode",
                            illuminationTriggerModeSpinner.getSelectionItemPosition()
                        )
                    }
                    if (getViewVisibility(detectionDistanceSeekBar)) {
                        put(
                            "minDetectionDistance",
                            detectionDistanceSeekBar.getFromValue().toInt()
                        )
                        put(
                            "maxDetectionDistance",
                            detectionDistanceSeekBar.getToValue().toInt()
                        )
                    }
                }
            }
            mainJSONObject.put(DeviceSettings.KEY, deviceSettingsJSONObject)
            val json = mainJSONObject.toString().toByteArray()
            Log.i(TAG, "setDeviceSettings: $mainJSONObject")
            val size = IntArray(1) { json.size }
            withContext(Dispatchers.IO) {
                AMTHidManager.instance().setConfig(ConfigType.DEVICE_CONFIG, json, size).let {
                    Log.i(TAG, "setDeviceSettings: ret = $it,$mainJSONObject")
                    if (it == 0) {
                        with(Dispatchers.Main) {
                            //update local threshold
                            with(deviceSettingViewBinding!!) {
                                Config.videoStreamMode =
                                    videoStreamModeSpinner.getSelectionItemPosition()
                                Config.isFeaturePhotoFunOn = pushPhotoEnableSwitchView.isChecked()
                                Config.controlLEDByHost =
                                    (ledControlModeSpinner.getSelectionItemPosition() == 1)
                            }
                        }
                        Log.i(TAG, "save device settings success")
                    } else {
                        Log.e(TAG, "set device settings: failed,$it")
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    /**
     * get capture filter setting
     */
    private fun getCaptureFilterConfig() {
        settingsViewModel.getCaptureFilterConfigFlow()
            .flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                if (it.code != ErrorCode.ERROR_NONE) {
                    showToast(it.message)
                } else {
                    initCaptureFilterView(it.result!!)
                }
            }
    }

    /**
     * set capture filter setting
     */
    private suspend fun setCaptureFilterSetting() {
        captureFilterSettingViewBinding ?: return
        withContext(Dispatchers.IO) {
            val captureFilterJsonObject = JSONObject().apply {
                with(captureFilterSettingViewBinding!!) {
                    if (getViewVisibility(blurThrSetView)) {
                        put("blurThreshold", blurThrSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(frontFaceThrSetView)) {
                        put("frontThreshold", frontFaceThrSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(faceDetectHeightRange)) {
                        put("heightMaxValue", faceDetectHeightRange.getToValue())
                        put("heightMinValue", faceDetectHeightRange.getFromValue())
                    }
                    if (getViewVisibility(faceDetectWidthRange)) {
                        put("widthMaxValue", faceDetectWidthRange.getToValue())
                        put("widthMinValue", faceDetectWidthRange.getFromValue())
                    }
                    if (getViewVisibility(qualityThrSetView)) {
                        put("scoreThreshold", qualityThrSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(maxPitchSetView)) {
                        put("pitchMaxValue", maxPitchSetView.getSeekBarValue())
                        put("pitchMinValue", minPitchSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(minYawSetView)) {
                        put("yawMaxValue", maxYawSetView.getSeekBarValue())
                        put("yawMinValue", minYawSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(maxRollSetView)) {
                        put("rollMaxValue", maxRollSetView.getSeekBarValue())
                        put("rollMinValue", minRollSetView.getSeekBarValue())
                    }
                }
            }

            val mainJSONObject = JSONObject()
            mainJSONObject.put("captureFilter", captureFilterJsonObject)
            val json = mainJSONObject.toString().toByteArray()
            val size = IntArray(1) { json.size }
            AMTHidManager.instance().setConfig(ConfigType.CAPTURE_FILTER_CONFIG, json, size).let {
                Log.i(TAG, "getCaptureFilterConfig: setConfig ret = $it")
                if (it == 0) {
                    //update local threshold
                    withContext(Dispatchers.Main) {
                        with(captureFilterSettingViewBinding!!) {
                            Config.instance().faceRegistrationQuality =
                                qualityThrSetView.getSeekBarValue().toFloat()
                            Config.instance().faceHeightMinSize =
                                faceDetectHeightRange.getFromValue().toInt()
                            Config.instance().faceWidthMinSize =
                                faceDetectWidthRange.getFromValue().toInt()
                            Config.instance().facePitchMaxThreshold =
                                maxPitchSetView.getSeekBarValue().toFloat()
                            Config.instance().facePitchMinThreshold =
                                minPitchSetView.getSeekBarValue().toFloat()
                            Config.instance().faceYawMaxThreshold =
                                maxYawSetView.getSeekBarValue().toFloat()
                            Config.instance().faceYawMinThreshold =
                                minYawSetView.getSeekBarValue().toFloat()
                            Config.instance().faceRollMaxThreshold =
                                maxRollSetView.getSeekBarValue().toFloat()
                            Config.instance().faceRollMinThreshold =
                                minRollSetView.getSeekBarValue().toFloat()
                        }
                    }
                    Log.i(TAG, "save capture filter success")
                } else {
                    Log.e(TAG, "getCaptureFilterConfig: failed,$it")
                }
            }
        }
    }

    /**
     * get palm setting
     */
    private fun getPalmSetting() {
        settingsViewModel.getPalmSettingFlow()
            .flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                if (it.code != ErrorCode.ERROR_NONE) {
                    showToast(it.message)
                } else {
                    initPalmSettingsView(it.result!!)
                }
            }
    }


    private suspend fun initPalmSettingsView(vlPalmSettingJSONObject: JSONObject) {
        val vlPalmSetting = withContext(Dispatchers.IO) {
            return@withContext JSONUtil.getVLPalmSettings(vlPalmSettingJSONObject.toString())
        }
        if (vlPalmSetting == null) {
            return
        }

        //remember default value
        defaultPalmTemplateMode = vlPalmSetting.palmTemplateMode
        defaultPalmFunOn = vlPalmSetting.palmFunOn
        defaultPalmLiveness = vlPalmSetting.palmLiveness

        withContext(Dispatchers.Main) {
            if (!binding.palmSettingsLayoutViwStub.isInflated) {
                val palmSettingView = binding.palmSettingsLayoutViwStub.viewStub!!.inflate()
                palmSettingViewBinding = DataBindingUtil.getBinding(palmSettingView)
            }
            with(palmSettingViewBinding!!) {

                this.vlPalmSetting = vlPalmSetting

                palmIdentifyThresholdSetView.setMax(100)

                changeVisibility(
                    palmImageQualityThrSetView,
                    vlPalmSettingJSONObject.has("imageQualityThreshold")
                )
                changeVisibility(palmAESetView, vlPalmSettingJSONObject.has("palmAE"))
                changeVisibility(palmFunOnSetView, vlPalmSettingJSONObject.has("palmFunOn"))
                changeVisibility(
                    palmIdentifyIntervalSetView,
                    vlPalmSettingJSONObject.has("palmIdentifyInterval")
                )
                changeVisibility(
                    palmIdentifyThresholdSetView,
                    vlPalmSettingJSONObject.has("palmIdentifyThreshold")
                )
                changeVisibility(
                    palmLivenessSwitchView,
                    vlPalmSettingJSONObject.has("palmLiveness")
                )
                changeVisibility(
                    palmLivenessThresholdSetView,
                    vlPalmSettingJSONObject.has("palmLivenessThreshold")
                )
                changeVisibility(palmMinSizeSetView, vlPalmSettingJSONObject.has("palmMinSize"))
                changeVisibility(
                    palmTemplateModeSpinner,
                    vlPalmSettingJSONObject.has("palmTemplateMode")
                )

                //palm setting
                palmImageQualityThrSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                palmFunOnSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                palmLivenessThresholdSetView.setOnClickListener(this@BioRecognizeSettingActivity)
                palmMinSizeSetView.setOnClickListener(this@BioRecognizeSettingActivity)
            }
        }
    }

    /**
     * set palm setting
     */
    private suspend fun setPalmSetting() {
        palmSettingViewBinding ?: return
        withContext(Dispatchers.IO) {
            val palmSettingJsonObject = JSONObject().apply {
                with(palmSettingViewBinding!!) {
                    if (getViewVisibility(palmImageQualityThrSetView)) {
                        put("imageQualityThreshold", palmImageQualityThrSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(palmAESetView)) {
                        put("palmAE", palmAESetView.isChecked())
                    }
                    if (getViewVisibility(palmFunOnSetView)) {
                        put("palmFunOn", palmFunOnSetView.isChecked())
                    }
                    if (getViewVisibility(palmIdentifyIntervalSetView)) {
                        put("palmIdentifyInterval", palmIdentifyIntervalSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(palmIdentifyThresholdSetView)) {
                        put("palmIdentifyThreshold", palmIdentifyThresholdSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(palmLivenessSwitchView)) {
                        put("palmLiveness", palmLivenessSwitchView.isChecked())
                    }
                    if (getViewVisibility(palmLivenessThresholdSetView)) {
                        put("palmLivenessThreshold", palmLivenessThresholdSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(palmMinSizeSetView)) {
                        put("palmMinSize", palmMinSizeSetView.getSeekBarValue())
                    }
                    if (getViewVisibility(palmTemplateModeSpinner)) {
                        put("palmTemplateMode", palmTemplateModeSpinner.getSelectionItemPosition())
                    }
                }
            }

            val jsonObject = JSONObject()
            jsonObject.putOpt("PALMSetting", palmSettingJsonObject)

            Log.i(TAG, "setPalmSetting: $jsonObject")
            val json = jsonObject.toString().toByteArray()
            val size = IntArray(1) { json.size }
            AMTHidManager.instance().setConfig(ConfigType.PALM_CONFIG, json, size).let {
                if (it == 0) {
                    withContext(Dispatchers.Main) {
                        with(palmSettingViewBinding!!) {
                            Config.instance().palmVLIdentifyThreshold =
                                palmIdentifyThresholdSetView.getSeekBarValue() * 1.0F
                            Config.instance().palmVLLivenessThreshold =
                                palmLivenessThresholdSetView.getSeekBarValue() * 1.0F
                            Config.instance().bPalmLivenessEnable =
                                palmLivenessSwitchView.isChecked()
                            Config.palmTemplateMode =
                                palmTemplateModeSpinner.getSelectionItemPosition()
                        }
                        Log.i(TAG, "set palm config success")
                    }
                } else {
                    Log.e(TAG, "sendGetPalmSettingCmd: $it")
                }
            }
        }
    }

    private suspend fun checkSettingValue(): Boolean {
        var isOptionValid = true
        var detail = ""

        if (Config.instance().sensorType != SensorType.SENSOR_TYPE_RGB_AND_NIR) {

            palmSettingViewBinding?.let {
                val palmTemplateMode =
                    it.palmTemplateModeSpinner.getSelectionItemPosition()

                if (palmTemplateMode == VLPalmSetting.PALM_TEMPLATE_MODE_VL
                    || palmTemplateMode == VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR
                ) {
                    detail += "\n(${getString(R.string.palm_template_mode)}:" +
                            "${
                                resources.getStringArray(
                                    R.array.palm_template_mode
                                )[palmTemplateMode]
                            })"
                    isOptionValid = false
                }
            }

            deviceSettingViewBinding?.let {
                val videoStreamMode =
                    it.videoStreamModeSpinner.getSelectionItemPosition()
                if (videoStreamMode == DeviceSettings.VIDEO_STREAM_MODE_VL
                    || videoStreamMode == DeviceSettings.VIDEO_STREAM_MODE_ALL
                ) {

                    detail += "\n(${getString(R.string.video_stream_mode_title)}:" +
                            "${
                                resources.getStringArray(
                                    R.array.video_stream_mode_type_array
                                )[videoStreamMode]
                            })"
                    isOptionValid = false
                }
            }

            if (!isOptionValid) {
                withContext(Dispatchers.Main) {

                    safeShowAlertDialog(
                        message = getString(
                            R.string.unsupported_option,
                            detail
                        ),
                        title = getString(R.string.title_warning)
                    )
                }
            }

        }

        return isOptionValid
    }

    override fun onClickLeft() {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onClickRight() {
        lifecycleScope.launch {
            val isValidOption = withContext(Dispatchers.IO) {
                return@withContext checkSettingValue()
            }
            if (!isValidOption) {
                return@launch
            }

            val loadingDialog = ProgressDialogFragment.show(
                supportFragmentManager,
                "",
                "Saving", false
            )
            withContext(Dispatchers.IO) {
                setCommonSetting()
                setCaptureFilterSetting()
                setPalmSetting()
                setFaceSettings()
                setDeviceSettings()
                Config.instance().save(this@BioRecognizeSettingActivity.applicationContext)
            }
            loadingDialog.dismiss()

            commonSettingViewBinding?.let {
                //if liveness function changed,should reboot module
                if (defaultVLLiveness != it.VLLivenessSwitch.isChecked()) {
                    Log.i(TAG, "onClickRight: vl liveness changed")
                    setResult(RESULT_OK)
                }
                if (defaultNIRLiveness != it.NIRLivenessSwitch.isChecked()) {
                    Log.i(TAG, "onClickRight: nir liveness changed")
                    setResult(RESULT_OK)
                }
                if (defaultStreamMode != it.videoStreamModeSpinner.getSelectionItemPosition()) {
                    Log.i(TAG, "onClickRight: video stream mode changed")
                    setResult(RESULT_OK)
                }
                if (it.sensorFrameSpinner.visibility == View.VISIBLE &&
                    defaultSensorFrame != (it.sensorFrameSpinner.getSelectionValue() as String).toInt()
                ) {
                    Log.i(TAG, "onClickRight:sensor frame changed")
                    setResult(RESULT_OK)
                }
            }

            faceSettingViewBinding?.let {
                if (defaultLivenessMode != it.livenessModeSpinner.getSelectionItemPosition()) {
                    Log.i(TAG, "onClickRight: liveness mode changed")
                    setResult(RESULT_OK)
                }
            }

            deviceSettingViewBinding?.let {
                if (defaultStreamMode != it.videoStreamModeSpinner.getSelectionItemPosition()) {
                    Log.i(TAG, "onClickRight: video stream mode changed")
                    setResult(RESULT_OK)
                }
                if (it.sensorFrameSpinner.visibility == View.VISIBLE &&
                    defaultSensorFrame != (it.sensorFrameSpinner.getSelectionValue() as String).toInt()
                ) {
                    Log.i(TAG, "onClickRight:sensor frame changed")
                    setResult(RESULT_OK)
                }
            }

            palmSettingViewBinding?.let {
                if (defaultPalmFunOn != it.palmFunOnSetView.isChecked()) {
                    Log.i(TAG, "onClickRight: palm function changed")
                    setResult(RESULT_OK)
                }
                if (defaultPalmLiveness != it.palmLivenessSwitchView.isChecked()) {
                    Log.i(TAG, "onClickRight: palm liveness function changed")
                    setResult(RESULT_OK)
                }

                if (it.palmTemplateModeSpinner.visibility == View.VISIBLE &&
                    defaultPalmTemplateMode != it.palmTemplateModeSpinner.getSelectionItemPosition()
                ) {
                    Log.i(TAG, "onClickRight:palm template mode  changed")
                    setResult(RESULT_OK)
                }
            }

            finish()
        }
    }

    override fun onClick(v: View?) {
        commonSettingViewBinding?.let {
            when (v?.id) {
                R.id.attrIntervalSetView -> showCustomNumberInputDialog(
                    it.attrIntervalSetView,
                    0,
                    10 * 1000
                )

                R.id.recogIntervalSetView -> showCustomInputDialog(
                    it.recogIntervalSetView,
                    0,
                    10 * 1000
                )

                R.id.recognizeThresholdSetView -> showCustomInputDialog(
                    it.recognizeThresholdSetView,
                    0,
                    1
                )

                R.id.hacknessThresholdSetView -> showCustomInputDialog(
                    it.hacknessThresholdSetView,
                    0,
                    1
                )

                R.id.attendIntervalSetView -> showCustomInputDialog(
                    it.attendIntervalSetView,
                    0,
                    100 * 1000
                )
            }
        }
        faceSettingViewBinding?.let {
            when (v?.id) {
                R.id.attrIntervalSetView -> showCustomNumberInputDialog(
                    it.attrIntervalSetView,
                    0,
                    10 * 1000
                )

                R.id.recogIntervalSetView -> showCustomInputDialog(
                    it.recogIntervalSetView,
                    0,
                    10 * 1000
                )

                R.id.recognizeThresholdSetView -> showCustomInputDialog(
                    it.recognizeThresholdSetView,
                    0,
                    1
                )

                R.id.faceVerifyThresholdSetView -> showCustomInputDialog(
                    it.faceVerifyThresholdSetView,
                    0,
                    1
                )

                R.id.singleLensLivenessThresholdSetView -> showCustomInputDialog(
                    it.singleLensLivenessThresholdSetView,
                    0,
                    1
                )

                R.id.dualLensLivenessThresholdSetView -> showCustomInputDialog(
                    it.dualLensLivenessThresholdSetView,
                    0,
                    1
                )
            }
        }

        deviceSettingViewBinding?.let {
            when (v?.id) {
                R.id.attendIntervalSetView -> showCustomInputDialog(
                    it.attendIntervalSetView,
                    0,
                    100 * 1000
                )
            }
        }
    }


    private fun showCustomInputDialog(
        customSettingLayout: CustomSettingLayout,
        min: Int,
        max: Int,
    ) {
        customSettingLayout.let {
            showInputNumberDialog(
                it.getSettingTitle(),
                it.getSettingValue(),
                object : CustomInputDialog.OnBtnClickListener {
                    override fun onPositiveButtonClick(
                        dialog: CustomInputDialog,
                        content: String,
                    ) {
                        val value = content.toFloatOrNull()
                        if (value == null || value < min || value > max) {
                            showToast("Please input a valid value(%d-%d)".format(min, max))
                            return
                        }
                        it.setSettingValue(content)
                        dialog.dismiss()
                    }

                    override fun onNegativeButtonClick(
                        dialog: CustomInputDialog,
                        content: String,
                    ) {
                        dialog.dismiss()
                    }

                },
                InputType.TYPE_CLASS_TEXT
            )
        }
    }

    private fun showCustomNumberInputDialog(
        customSettingLayout: CustomSettingLayout,
        min: Int,
        max: Int,
    ) {
        customSettingLayout.let {
            showInputNumberDialog(
                it.getSettingTitle(),
                it.getSettingValue(),
                object : CustomInputDialog.OnBtnClickListener {
                    override fun onPositiveButtonClick(
                        dialog: CustomInputDialog,
                        content: String,
                    ) {
                        val value = content.toFloatOrNull()
                        if (value == null || value < min || value > max) {
                            showToast("Please input a valid value(%d-%d)".format(min, max))
                            return
                        }
                        it.setSettingValue(content)
                        dialog.dismiss()
                    }

                    override fun onNegativeButtonClick(
                        dialog: CustomInputDialog,
                        content: String,
                    ) {
                        dialog.dismiss()
                    }

                },
                InputType.TYPE_CLASS_NUMBER
            )
        }
    }

}