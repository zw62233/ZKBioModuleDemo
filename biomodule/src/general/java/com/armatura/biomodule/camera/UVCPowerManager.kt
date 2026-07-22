package com.armatura.biomodule.camera

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.util.ShellUtils
import com.armatura.biomodule.viewmodel.AMTViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Created by Magic on 2023/3/24
 * Description:It is recommended to support resetting the UVC power supply when there are
 * some abnormal and rare problems that cannot be solved by soft restarting.
 */
object UVCPowerManager {
    private const val TAG = "UVCPowerManager"
    private const val WATCH_DOG_FILE_NAME = "watchdog.log"
    private const val TIME_FORMAT = "yyyy-MM-dd-HH:mm:ss"
    private val gpioScope = CoroutineScope(Dispatchers.IO)
    private val executors: ScheduledExecutorService =
        Executors.newScheduledThreadPool(
            1
        ) { r -> Thread(r, "MonitorUSB") }

    @Synchronized
    fun resetUVCPower(context: Context) {
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.US)
        FileUtils.saveRecord(
            context,
            WATCH_DOG_FILE_NAME,
            "${timeFormat.format(Date())} \t needs reset power,but device not support"
        )
    }


    /**
     * When the application starts running, it periodically detects whether the USB device exists,
     * and resets the USB power supply if it does not exist.
     */
    fun startMonitorDevice(context: Context) {
        //nothing in general.If your device support reset USB Device power,please implement self
        Log.i(
            TAG, "startMonitorDevice: If your device support reset USB Device power," +
                    "please implement self"
        )
    }

    fun stopMonitorDevice() {
        //nothing in general.If your device support reset USB Device power,please implement self
        Log.i(
            TAG, "startMonitorDevice: If your device support reset USB Device power," +
                    "please implement self"
        )
    }
}