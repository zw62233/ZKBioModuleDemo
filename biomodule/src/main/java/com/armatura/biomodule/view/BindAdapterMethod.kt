package com.armatura.biomodule.view

import android.view.View
import androidx.databinding.BindingAdapter
import com.armatura.LoggerHelper
import com.armatura.biomodule.R
import com.armatura.biomodule.config.Config
import kotlin.math.abs
import com.armatura.biomodule.pojo.setting.FaceSettings

object BindAdapterMethod {
    @JvmStatic
    @BindingAdapter("fromValue", "toValue")
    fun setDefaultRange(
        rangeSeekBarLayout: CustomRangeSeekBarLayout,
        fromValue: Float,
        toValue: Float,
    ) {
        rangeSeekBarLayout.rangeSlider.values = listOf(fromValue, toValue)
    }

    @JvmStatic
    @BindingAdapter("custom_visibility")
    fun customSetValue(view: View, visibility: Boolean) {
        view.visibility = if (visibility) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    @JvmStatic
    @BindingAdapter("default_progress")
    fun customSetValue(layout: CustomSeekBarLayout, value: Int) {
        val progress = if (layout.negative) {
            layout.seekBar.max - abs(value)
        } else {
            value
        }
        layout.setDefaultProgress(progress)
    }

    @JvmStatic
    @BindingAdapter("setting_value")
    fun customSetValue(layout: CustomSettingLayout, value: Any) {
        layout.settingValueView.text = value.toString()
    }

    @JvmStatic
    @BindingAdapter("set_sensor_frame")
    fun selectSpinnerValue(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, fps: Int,
    ) {
        val sensorFrameArray =
            customSettingSpinnerLayout.context?.resources?.getStringArray(R.array.sensor_frame_rate_array) as Array<String>
        when (sensorFrameArray.size) {
            1 -> customSettingSpinnerLayout.setSelection(0)//FAPVS-50  only support 25FPS
            3 -> {
                when (fps) {
                    15 -> customSettingSpinnerLayout.setSelection(0)
                    25 -> customSettingSpinnerLayout.setSelection(1)
                    30 -> customSettingSpinnerLayout.setSelection(2)
                    else -> customSettingSpinnerLayout.setSelection(0)
                }
            }

            else -> customSettingSpinnerLayout.setSelection(0)//default
        }

    }

    @JvmStatic
    @BindingAdapter("set_liveness_mode")
    fun setLivenessMode(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, livenessMode: Int,
    ) {
        when (livenessMode) {
            FaceSettings.LIVENESS_MODE_DISABLE -> customSettingSpinnerLayout.setSelection(0)
            FaceSettings.LIVENESS_MODE_SINGLE_LENS -> customSettingSpinnerLayout.setSelection(1)
            FaceSettings.LIVENESS_MODE_DUAL_LENS -> customSettingSpinnerLayout.setSelection(2)
            else -> customSettingSpinnerLayout.setSelection(3)
        }

    }

    @JvmStatic
    @BindingAdapter("set_led_control_mode")
    fun selectLEDControlMode(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, ledControlMode: Int,
    ) {
        when (ledControlMode) {
            0 -> customSettingSpinnerLayout.setSelection(0)
            1 -> customSettingSpinnerLayout.setSelection(1)
            2 -> customSettingSpinnerLayout.setSelection(1/*please set as 1*/)
            else -> throw IndexOutOfBoundsException("Led Control Mode value should be in 0-2,current is $ledControlMode")
        }
    }

    @JvmStatic
    @BindingAdapter("set_led_breath_type")
    fun selectLEDBreathType(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, ledBreathType: Int,
    ) {
        when (ledBreathType) {
            0, 1, 2, 3 -> customSettingSpinnerLayout.setSelection(ledBreathType)
            else -> throw IndexOutOfBoundsException("Led Breath Type value should be in 0-3,current is $ledBreathType")
        }
    }

    @JvmStatic
    @BindingAdapter("set_illumination_trigger_mode")
    fun setIlluminationTriggerMode(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, illuminationTriggerMode: Int,
    ) {
        when (illuminationTriggerMode) {
            0, 1 -> customSettingSpinnerLayout.setSelection(illuminationTriggerMode)
            else -> throw IndexOutOfBoundsException("Illumination Trigger Mode value should be in 0-1,current is $illuminationTriggerMode")
        }
    }


    @JvmStatic
    @BindingAdapter("set_recognize_mode")
    fun setRecognizeMode(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, recognizeMode: Int,
    ) {
        when (recognizeMode) {
            Config.HOST_MODE -> customSettingSpinnerLayout.setSelection(0)
            Config.MULTI_BIO_MODULE_INTERNAL_MODE -> customSettingSpinnerLayout.setSelection(1)
            else -> customSettingSpinnerLayout.setSelection(0)
        }
    }

    @JvmStatic
    @BindingAdapter("set_palm_template_mode")
    fun setPalmTemplateMode(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, palmTemplateMode: Int,
    ) {
        if (palmTemplateMode !in 0..2) {
            throw IndexOutOfBoundsException("Palm Template Mode only support 0,1,2")
        }
        customSettingSpinnerLayout.setSelection(palmTemplateMode)
    }

    @JvmStatic
    @BindingAdapter("set_sdk_log_level")
    fun setSDKLogLevel(
        customSettingSpinnerLayout: CustomSettingSpinnerLayout, sdkLogLevel: Int,
    ) {
        when (sdkLogLevel) {
            LoggerHelper.VERBOSE,
            LoggerHelper.DEBUG,
            LoggerHelper.INFO,
            LoggerHelper.WARN,
            LoggerHelper.ERROR,
            LoggerHelper.ASSERT,
                -> customSettingSpinnerLayout.setSelection(sdkLogLevel - 2)

            else -> customSettingSpinnerLayout.setSelection(0)
        }
    }


    @JvmStatic
    @BindingAdapter("customChecked")
    fun customChecked(layout: CustomSettingSwitchLayout, isChecked: Boolean) {
        layout.setCheck(isChecked)
    }
}