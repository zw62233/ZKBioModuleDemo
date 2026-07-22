package com.armatura.biomodule.camera

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.armatura.biomodule.util.FileUtils
import com.armatura.biomodule.viewmodel.AMTViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class AbstractUVCPowerManager {
    companion object {
        const val TAG = "UVCPowerManager"
        const val TIME_FORMAT = "yyyy-MM-dd-HH:mm:ss"
        const val RESET_POWER_INTERVAL = 6000L
        const val MAX_DEVICE_LOST_COUNT = 2
        const val RESET_POWER_LOG_FILE_NAME = "uvc_power_reset_log.txt"
        const val MONITOR_USB_FILE_NAME = "monitor_usb.txt"
        const val MONITOR_PERIOD = 40L/*need about 30 sec to boot*/
        const val MONITOR_START_DELAY = MONITOR_PERIOD
        const val MIN_RESET_INTERVAL =
            MONITOR_PERIOD * 1000/*between two reset can't be short than boot time*/
    }

    protected var lastRestPowerTimeStamp = 0L
    protected var lastRestHubTimeStamp = 0L
    private var mDeviceLostCount = 0

    private val executors = Executors.newScheduledThreadPool(
        1
    ) { r -> Thread(r, "MonitorUSB") }

    abstract fun resetUVCPower(context: Context)

    abstract fun resetPowerAndUSBHub(context: Context)

    protected fun writeDisablePowerLog(context: Context) {
        writeWatchDogLog("turn OFF the UVC power", context)
    }

    protected fun writeDisableLog(gpioAddress: String, context: Context) {
        writeWatchDogLog("disable $gpioAddress", context)
    }

    protected fun writeEnableLog(gpioAddress: String, context: Context) {
        writeWatchDogLog("enable $gpioAddress", context)
    }

    protected fun writeEnablePowerLog(context: Context) {
        writeWatchDogLog("turn ON the UVC power", context)
    }

    protected fun writeEnableHubLog(context: Context) {
        writeWatchDogLog("turn ON the USB HUB", context)
    }

    protected fun writeDisableHubLog(context: Context) {
        writeWatchDogLog("turn Off the USB HUB", context)
    }

    protected fun writeRestPowerFailedLog(brand: String, context: Context) {
        writeWatchDogLog("unknown brand($brand)! Can't reset uvc power! ", context)
    }

    private fun writeWatchDogLog(log: String, context: Context) {
        val timeFormat = SimpleDateFormat(
            TIME_FORMAT, Locale.US
        )
        FileUtils.saveRecord(
            context,
            RESET_POWER_LOG_FILE_NAME,
            "${timeFormat.format(Date())} \t $log "
        )
    }

    private fun writeMonitorUSBLog(log: String, context: Context) {
        val timeFormat = SimpleDateFormat(
            TIME_FORMAT, Locale.US
        )
        FileUtils.saveRecord(
            context,
            MONITOR_USB_FILE_NAME,
            "${timeFormat.format(Date())} \t $log "
        )
    }

    /**
     * When the application starts running, it periodically detects whether the USB device exists,
     * and resets the USB power supply if it does not exist.
     */
    open fun startMonitorDevice(context: Context) {
        Log.i(TAG, "startMonitorDevice")
        mDeviceLostCount = 0
        val applicationContext = context.applicationContext
        executors.scheduleWithFixedDelay({
            (applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager).also { usbManager ->
                var isDeviceLost = true
                for (usbDevice in usbManager.deviceList.values) {
                    if (AMTViewModel.isAMTDevice(usbDevice)
                        || AMTViewModel.isUpgradeDevice(usbDevice)
                    ) {
                        isDeviceLost = false
                        break
                    }
                }
                if (isDeviceLost) {
                    mDeviceLostCount++
                    Log.w(TAG, "monitor:device lost connect,count=$mDeviceLostCount")
                    if (mDeviceLostCount >= MAX_DEVICE_LOST_COUNT) {
                        writeMonitorUSBLog(
                            "device lost ,count=$mDeviceLostCount",
                            context
                        )
                        if (mDeviceLostCount >= (MAX_DEVICE_LOST_COUNT + 1)) {
                            resetPowerAndUSBHub(context)
                        } else {
                            resetUVCPower(context)
                        }
                    }
                } else {
                    mDeviceLostCount = 0
                    Log.i(TAG, "monitor:device connected")
                }
            }
        }, MONITOR_START_DELAY, MONITOR_PERIOD, TimeUnit.SECONDS)
    }

    fun stopMonitorDevice() {
        Log.i(TAG, "stopMonitorDevice: ")
        executors.shutdownNow()
    }

}
