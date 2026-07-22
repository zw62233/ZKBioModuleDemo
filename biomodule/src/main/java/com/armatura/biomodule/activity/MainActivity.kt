package com.armatura.biomodule.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.fragment.ArmaturaFragment
import com.armatura.biomodule.fragment.ArmaturaPortFragment
import com.armatura.biomodule.manager.OrientationManager
import com.armatura.biomodule.util.HidHelper
import com.armatura.constant.ConfigType
import com.armatura.translib.AMTHidManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class MainActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val orientation = resources.configuration.orientation
        showFragment(orientation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        showFragment(newConfig.orientation)
    }

    private fun showFragment(orientation: Int) {
        val fragment = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ArmaturaFragment()
        } else {
            ArmaturaPortFragment()
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss()
    }


    @SuppressLint("SourceLockedOrientationActivity")
    fun rotateScreen() {
        // 获取当前请求的屏幕方向
        val currentOrientation = requestedOrientation

        val newOrientation = when (currentOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    rotateUVC(180)
                    HidHelper.exitStandByMode()
                }
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    rotateUVC(0)
                    HidHelper.exitStandByMode()
                }
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
        }
        OrientationManager.orientation = newOrientation
        requestedOrientation = newOrientation
    }

    private fun rotateUVC(degree: Int) {
        val deviceSettingObj = JSONObject().apply {
            put("DEVSetting", JSONObject().apply {
                put("uvcRotateAngle", degree)
            })
        }
        val jsonByteArray = deviceSettingObj.toString().toByteArray()
        val size = IntArray(jsonByteArray.size)
        AMTHidManager.instance().setConfig(
            ConfigType.DEVICE_CONFIG,
            jsonByteArray, size
        )
    }
}