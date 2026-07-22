package com.armatura.biomodule.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.adapter.AboutInfoAdapter
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.databinding.ActivityAboutBinding
import com.armatura.biomodule.pojo.module.PersonStatistics
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.biomodule.viewmodel.SettingsViewModel
import com.armatura.constant.ErrorCode
import com.armatura.uvclib.util.AMTUtil
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.util.Collections

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    private val infoKey: MutableList<String> = Collections.synchronizedList(ArrayList())
    private val infoValue: MutableList<String> = Collections.synchronizedList(ArrayList())
    private lateinit var aboutInfoAdapter: AboutInfoAdapter

    private val settingsViewModel by viewModels<SettingsViewModel>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_about)
        aboutInfoAdapter = AboutInfoAdapter(this@AboutActivity, infoKey, infoValue)
        initView()
    }

    private fun initView() {
        binding.myToolBar.also {
            it.setTitle(resources.getString(R.string.device_info))
            it.hideSaveIcon()
            it.setToolBarClickListener(object : ToolBarClickListener {
                override fun onClickLeft() {
                    finish()
                }

                override fun onClickRight() {
                    finish()
                }
            })
        }

        binding.rvAbout.apply {
            setLayoutManager(LinearLayoutManager(this@AboutActivity))
            setAdapter(aboutInfoAdapter)
        }

        binding.deviceInfoSrl.apply {
            setColorSchemeColors(Color.GREEN)
            setOnRefreshListener {
                if (!this.isRefreshing) {
                    infoKey.clear()
                    infoValue.clear()
                    refreshDeviceInfo()
                } else {
                    isRefreshing = false
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        refreshDeviceInfo()
    }


    private fun refreshDeviceInfo() {
        settingsViewModel.getDeviceInfoFlow().flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                when (it.code) {
                    ErrorCode.ERROR_NONE -> {
                        initDeviceInfoView(it.result!!)
                        refreshUI()
                    }

                    else -> toastAnywhere(it.message)
                }
            }

        settingsViewModel.getPersonStatisticInfoFlow().flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                when (it.code) {
                    ErrorCode.ERROR_NONE -> {
                        initPersonStatisticInfoView(it.result!!)
                        refreshUI()
                    }

                    else -> toastAnywhere(it.message)
                }
            }


        settingsViewModel.getFuncSettingsFlow().flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                when (it.code) {
                    ErrorCode.ERROR_NONE -> {
                        initFuncSettingsView(it.result!!)
                        refreshUI()
                    }

                    else -> toastAnywhere(it.message)
                }
            }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshUI() {
        aboutInfoAdapter.notifyDataSetChanged()
    }

    private fun initDeviceInfoView(deviceInfoObj: JSONObject) {
        val keyIterator = deviceInfoObj.keys()
        while (keyIterator.hasNext()) {
            val nextKey = keyIterator.next()
            infoKey.add(capitalize(nextKey))
            infoValue.add(deviceInfoObj.getString(nextKey))
        }
        infoKey.add("IP")
        infoValue.add(AMTUtil.getIPAddress(true))
        infoKey.add("Mac")
        infoValue.add(AMTUtil.getMacAddr())
        infoKey.add("AppVersion")
        infoValue.add(AMTUtil.getVersionName(this))
    }

    private fun initPersonStatisticInfoView(jsonObject: JSONObject) {
        val personStatistics = Gson().fromJson(
            jsonObject.toString(),
            PersonStatistics::class.java
        )
        if (personStatistics != null) {
            infoKey.add("PalmCount (Inside the module)")
            infoValue.add(personStatistics.palmCount.toString())

            infoKey.add("PersonCount (Inside the module)")
            infoValue.add(personStatistics.personCount.toString())

            infoKey.add("FaceCount (Inside the module)")
            infoValue.add(personStatistics.faceCount.toString())

            infoKey.add("DatabaseSize (Inside the module)")
            infoValue.add(personStatistics.databaseSize.toString())
        }
    }

    private fun initFuncSettingsView(jsonObject: JSONObject) {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject[key]
            infoKey.add(capitalize(key))
            infoValue.add(value.toString())
        }
    }


    companion object {
        private const val TAG = "AboutActivity"
        private const val GET_DEVICE_INFO = 1001
        private const val GET_DEVICE_INFO_SUCCESS = 1003
        private const val SHOW_TOAST = 1002

        fun action(context: Context) {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
        }

        fun capitalize(inputString: String): String {
            if (inputString.isEmpty()) return inputString
            val firstLetter = inputString[0].uppercaseChar()
            return firstLetter + inputString.substring(1)
        }
    }
}