package com.armatura.biomodule.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.SystemSettingActivity
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.CardInfo
import com.armatura.biomodule.bean.IdentifyFailedData
import com.armatura.biomodule.camera.AMTCameraView
import com.armatura.biomodule.camera.CameraController
import com.armatura.biomodule.camera.UVCPowerManager
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache
import com.armatura.biomodule.common.IdentifyState
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.manager.FaceManager
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.manager.NIRPalmManager
import com.armatura.biomodule.manager.PalmManager
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo
import com.armatura.biomodule.thread.AMTWorkManager
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.util.HidHelper
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.constant.ErrorCode
import com.armatura.constant.ParamIndex
import com.armatura.translib.AMTHidManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Created by Magic on 2022/3/23
 */
class AMTViewModel(private val applicationCtx: Application) : AndroidViewModel(applicationCtx) {

    companion object {
        const val TAG = "AMTViewModel"
        const val ACTION_USB_MULTI_BIO_PERMISSION = "com.multi.bio.usb.permission"
        const val PLATFORM_ZAM218_LATEST_VERSION = 20240726
        const val PLATFORM_ZAM210_LATEST_VERSION = 20230823
        const val ERROR_CODE_VERSION_NOT_MATCH = -1001
        const val ERROR_CODE_USB_PERMISSION_DENIED = -1002
        const val ERROR_CODE_TIME_OUT = -1003
        const val ERROR_CODE_HID_NOT_READY = -1004
        const val BUNDLE_KEY_ERROR_CODE = "ErrorCode"
        const val BUNDLE_KEY_VERSION = "Version"
        const val BUNDLE_KEY_ERROR_MSG = "ErrorMsg"


        @JvmStatic
        fun isAMTDevice(device: UsbDevice): Boolean {
            return when (device.vendorId) {
                0x34c9 -> {
                    when (device.productId shr 8) {
                        0x22, 0x12, 0x32 -> true //multi bio 3.0
                        else -> false
                    }
                }

                0x1b55 -> {
                    device.productId == 0x0504
                }

                else -> false
            }
        }

        @JvmStatic
        fun isUpgradeDevice(device: UsbDevice): Boolean {
            return when (device.vendorId) {
                0x34c9 -> device.productId == 0x3000/*compatible with old device*/ || device.productId == 0x0505
                0x1b55 -> device.productId == 0x0505
                else -> false
            }
        }
    }

    val usbPermissionLiveData by lazy {
        MutableLiveData<Boolean>()
    }

    private val usbReceiver = UsbBroadcastReceiver()
    private lateinit var mCameraView: AMTCameraView
    val drawFaceDataLiveData by lazy {
        MutableLiveData<DrawFaceData?>()
    }
    val palmInfoLiveData by lazy {
        MutableLiveData<PalmInfo?>()
    }

    val cardInfoLiveData by lazy {
        MutableLiveData<CardInfo?>()
    }

    val hidDeviceAttached by lazy {
        MutableLiveData<Boolean>()
    }

    val cpuTempLiveData by lazy {
        MutableLiveData<String>()
    }

    val errorTipsLiveData by lazy {
        MutableLiveData<Bundle>()
    }

    val syncConfigFlag by lazy {
        MutableLiveData<Boolean>()
    }

    val initDbLiveData by lazy {
        MutableLiveData<Boolean>()
    }

    val identifyFailedDataLiveData by lazy {
        MutableLiveData<IdentifyFailedData>()
    }

    val currentIdentifyState by lazy { MutableLiveData<IdentifyState>() }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun init(cameraView: AMTCameraView) {
        viewModelScope.launch(Dispatchers.IO) {
            Config.instance().updateFolder()
            Config.instance().initConfig(applicationCtx)

            withContext(Dispatchers.Main) {
                val filter = IntentFilter()
                filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                filter.addAction(ACTION_USB_MULTI_BIO_PERMISSION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getApplication<Application>().applicationContext.registerReceiver(
                        usbReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    getApplication<Application>().applicationContext.registerReceiver(
                        usbReceiver,
                        filter
                    )
                }

                mCameraView = cameraView
                checkDeviceWhenAppStart()
                //kiosk device needs monitor always until application exit
                UVCPowerManager.startMonitorDevice(applicationCtx)
            }
        }
    }

    private fun checkDeviceWhenAppStart() {
        openDeviceIfConnect(applicationCtx, mCameraView)
    }


    fun requestPermissionWhenDenied(context: Context, cameraView: AMTCameraView) {
        openDeviceIfConnect(context, cameraView)
    }

    fun openDeviceWhenPermissionGranted(context: Context, cameraView: AMTCameraView) {
        openDeviceIfConnect(context, cameraView)
    }

    fun initHostMatchAlgorithm() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                initDbLiveData.value = true
            }
            Log.i(TAG, "initHostMatchAlgorithm: start")
            //init algorithm
            PalmManager.getInstance().init()
            NIRPalmManager.getInstance().init()
            FaceManager.getInstance().init(Config.sfaceVer)
            //add user to memory
            BioDataUtil.instance().updateUsers()
            Log.i(TAG, "initHostMatchAlgorithm: finish")
            withContext(Dispatchers.Main) {
                initDbLiveData.value = false
            }
        }
    }

    private fun openDeviceIfConnect(context: Context, cameraView: AMTCameraView) {
        var amtModuleUsbDevice: UsbDevice? = null
        val usbManager =
            getApplication<Application>().applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        for ((_, value) in deviceList) {
            if (isAMTDevice(value)) {
                amtModuleUsbDevice = value
                Log.i(
                    TAG, "openDeviceIfConnect: find device ，%4X：%4X".format(
                        value.vendorId, value.productId
                    )
                )
                break
            }
        }

        if (amtModuleUsbDevice == null) {
            Log.w(TAG, "openDeviceIfConnect: Not found Device")
            toastAnywhere(getApplication<Application>().applicationContext.getString(R.string.device_not_found))
            return
        }

        if (usbManager.hasPermission(amtModuleUsbDevice)) {
            Log.i(TAG, "openDeviceIfConnect: device already has permission,try connect")
            connectHIDDevice(context, amtModuleUsbDevice)
        } else {
            var flag = PendingIntent.FLAG_MUTABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flag = PendingIntent.FLAG_MUTABLE/*use IMMUTABLE wil cause
                                UsbManager.EXTRA_PERMISSION_GRANTED alwayse false*/
            }
            PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_USB_MULTI_BIO_PERMISSION).apply {
                    `package` = context.packageName
                }, flag
            ).also {
                Log.i(TAG, "openDeviceIfConnect: device no permission,try get it")
                usbManager.requestPermission(amtModuleUsbDevice, it)
            }
        }
    }

    fun openUVCDeviceIfConnect(context: Context, cameraView: AMTCameraView) {
        var amtModuleUsbDevice: UsbDevice? = null
        val usbManager =
            getApplication<Application>().applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        for ((_, value) in deviceList) {
            if (isAMTDevice(value)) {
                amtModuleUsbDevice = value
                Log.i(
                    TAG, "openUVCDeviceIfConnect: find device ，%4X：%4X".format(
                        value.vendorId, value.productId
                    )
                )
                break
            }
        }

        if (amtModuleUsbDevice == null) {
            Log.w(TAG, "openUVCDeviceIfConnect: Not found Device")
            toastAnywhere(R.string.device_not_found)
            return
        }

        if (usbManager.hasPermission(amtModuleUsbDevice)) {
            Log.i(TAG, "openUVCDeviceIfConnect: device already has permission,try connect")
            viewModelScope.launch(Dispatchers.IO) {
                //open uvc camera
                cameraView.autoOpen(
                    getApplication<Application>().applicationContext,
                    amtModuleUsbDevice
                )
            }
        } else {
            var flag = PendingIntent.FLAG_MUTABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flag = PendingIntent.FLAG_MUTABLE/*use IMMUTABLE wil cause
                                UsbManager.EXTRA_PERMISSION_GRANTED alwayse false*/
            }
            PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_USB_MULTI_BIO_PERMISSION).apply {
                    `package` = context.packageName
                }, flag
            ).also {
                Log.i(TAG, "openUVCDeviceIfConnect: device no permission,try get it")
                usbManager.requestPermission(amtModuleUsbDevice, it)
            }
        }
    }


    fun enterStandByMode() {
        viewModelScope.launch(Dispatchers.IO) {
            HidHelper.enterStandByMode()
        }
    }

    fun exitStandByMode() {
        viewModelScope.launch(Dispatchers.IO) {
            HidHelper.exitStandByMode()
        }
    }

    private fun disconnectHidAndUVCDevice() {
        AMTHidManager.instance().close()
        AMTWorkManager.stopWorkManager()
        CameraController.instance().closeCam()
    }

    private fun connectHIDDevice(context: Context, usbDevice: UsbDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            //connect hid device
            AMTHidManager.instance().open(context.applicationContext, usbDevice).also { ret ->
                if (ret == ErrorCode.ERROR_NONE) {
                    //when connected,reset track id
                    resetTrackId()
                    if (Looper.getMainLooper() == Looper.myLooper()) {
                        hidDeviceAttached.value = true
                    } else {
                        hidDeviceAttached.postValue(true)
                    }
                } else {
                    Log.e(TAG, "openHid: hid connect failed,$ret")
                    withContext(Dispatchers.Main) {
                        toastAnywhere("Connect HID Failed ，$ret")
                    }
                }
            }
        }
    }

    fun release() {
        //stop heartbeat
        ModuleHeartbeatManager.getInstance().heatBeatStop()
        getApplication<Application>().applicationContext.unregisterReceiver(usbReceiver)
        AMTHidManager.instance().close()
        FaceManager.getInstance().destroy()
        PalmManager.getInstance().destroy()
        NIRPalmManager.getInstance().destroy()
        CameraController.instance().onDestroy()
        Log.i(TAG, "release")
    }

    override fun onCleared() {
        super.onCleared()
        release()
        //kiosk device stop monitor
        UVCPowerManager.stopMonitorDevice()
        exitProcess(0)
    }


    fun toastMsg(content: String?) {
        content?.also {
            toastAnywhere(it)
        }
    }

    fun resetTrackId() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ByteArray(255)
            val resultSize = IntArray(1)
            val ret =
                AMTHidManager.instance()
                    .setParam(ParamIndex.RESET_TRACK_ID, null, result, resultSize)
            if (ret == 0) {
                Log.i(TAG, "reset track Id:${String(result, 0, resultSize[0])}")
                RecognizedBioDataCache.instance().clearRecFaces()
            } else {
                Log.i(TAG, "reset track Id:ret= $ret")
            }
        }
    }

    inner class UsbBroadcastReceiver : BroadcastReceiver() {

        private fun toastMsg(content: String) {
            this@AMTViewModel.toastMsg(content)
        }

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE,
                                UsbDevice::class.java
                            ) as UsbDevice
                        } else {
                            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                        }
                    Log.i(
                        TAG, "Receiver %4X：%4X Attached".format(
                            device.vendorId, device.productId
                        )
                    )
                    if (isAMTDevice(device)) {
                        Log.i(TAG, "onReceive: multi bio  device attached")
                        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                        if (usbManager.hasPermission(device)) {
                            Log.i(
                                TAG, "Receiver %4X：%4X Attached ,has permission,try connect".format(
                                    device.vendorId, device.productId
                                )
                            )
                            usbPermissionLiveData.postValue(true)
                            toastMsg("Multi Bio Module ATTACHED")
                        } else {
                            Log.i(
                                TAG, "Receiver %4X：%4X Attached ,request permission".format(
                                    device.vendorId, device.productId
                                )
                            )
                            var flag = 0

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                flag = PendingIntent.FLAG_MUTABLE/*use IMMUTABLE wil cause
                                UsbManager.EXTRA_PERMISSION_GRANTED alwayse false*/
                            }
                            PendingIntent.getBroadcast(
                                context, 0,
                                Intent(ACTION_USB_MULTI_BIO_PERMISSION).apply {
                                    `package` = context.packageName
                                }, flag
                            ).also {
                                usbManager.requestPermission(device, it)
                            }
                        }
                    }

                    if (isUpgradeDevice(device)) {
                        Log.i(TAG, "onReceive: upgrade device attached")
                        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                        if (usbManager.hasPermission(device)) {
                            toastMsg("UPGRADE USB DEVICE ATTACHED")
                            AMTHidManager.instance().open(context.applicationContext, device)
                                .also {
                                    Log.i(TAG, "onReceive: open $it")
                                    if (it == 0) {
                                        toastMsg("UPGRADE USB DEVICE ATTACHED")
                                        this@AMTViewModel.getApplication<Application>().applicationContext
                                            .sendBroadcast(Intent(SystemSettingActivity.ACTION_HID_UPGRADE))
                                    }
                                }
                        } else {
                            Log.i(
                                TAG, "Receiver %4X：%4X Attached ,request permission".format(
                                    device.vendorId, device.productId
                                )
                            )
                            var flag = 0
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                flag = PendingIntent.FLAG_MUTABLE/*use IMMUTABLE wil cause
                                UsbManager.EXTRA_PERMISSION_GRANTED alwayse false*/
                            }
                            PendingIntent.getBroadcast(
                                context, 0,
                                Intent(ACTION_USB_MULTI_BIO_PERMISSION).apply {
                                    `package` = context.packageName
                                }, flag
                            ).also {
                                usbManager.requestPermission(device, it)
                            }
                        }
                    }
                    FileUtils.saveAllRecord(
                        context, String.format(
                            "%s usb device PID[%4X] VID[%4X] attached", SimpleDateFormat(
                                "yyyy-MM-dd-HH:mm:ss", Locale.getDefault()
                            ).format(Date()), device.productId, device.vendorId
                        )
                    )
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE,
                                UsbDevice::class.java
                            ) as UsbDevice
                        } else {
                            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                        }
                    Log.w(
                        TAG, "%4X：%4X detached".format(
                            device.vendorId, device.productId
                        )
                    )
                    if (isAMTDevice(device)) {
                        toastMsg("Multi Bio Device DETACHED")
                        //close what you can close
                        this@AMTViewModel.disconnectHidAndUVCDevice()
                        FileUtils.saveAllRecord(
                            context, String.format(
                                "%s usb device PID[%4X] VID[%4X] detached", SimpleDateFormat(
                                    "yyyy-MM-dd-HH:mm:ss", Locale.getDefault()
                                ).format(Date()), device.productId, device.vendorId
                            )
                        )
                    }
                }

                ACTION_USB_MULTI_BIO_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        val usbDevice =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    UsbManager.EXTRA_DEVICE,
                                    UsbDevice::class.java
                                ) as UsbDevice
                            } else {
                                intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                            }
                        usbDevice.let { device ->
                            Log.i(
                                TAG, "onReceive:%4X：%4X permission granted".format(
                                    device.vendorId, device.productId
                                )
                            )
                            if (isAMTDevice(device)) {
                                usbPermissionLiveData.postValue(true)
                            } else if (isUpgradeDevice(device)) {
                                Log.i(TAG, "onReceive: upgrade mode")
                                AMTHidManager.instance()
                                    .open(context.applicationContext, device).also {
                                        Log.i(TAG, "onReceive: open $it")
                                        if (it == 0) {
                                            toastMsg("UPGRADE USB DEVICE ATTACHED")
                                            this@AMTViewModel.getApplication<Application>().applicationContext
                                                .sendBroadcast(Intent(SystemSettingActivity.ACTION_HID_UPGRADE))
                                        }
                                    }

                            }

                        }
                    } else {
                        toastMsg("Permission Denied!")
                        this@AMTViewModel.errorTipsLiveData.value = (Bundle().apply {
                            putInt(BUNDLE_KEY_ERROR_CODE, ERROR_CODE_USB_PERMISSION_DENIED)
                            putString(
                                BUNDLE_KEY_ERROR_MSG,
                                "Device permission denied, unable to connect to device.Click OK to request again!"
                            )
                        })
                    }
                }
            }
        }
    }
}