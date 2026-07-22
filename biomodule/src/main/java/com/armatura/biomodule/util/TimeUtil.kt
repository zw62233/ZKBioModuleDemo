package com.armatura.biomodule.util

import com.armatura.biomodule.pojo.setting.SyncTime
import com.armatura.constant.ConfigType
import com.armatura.translib.AMTHidManager
import java.text.SimpleDateFormat
import java.util.*

class TimeUtil {
    companion object {
        val INSTANCE = TimeUtil()
    }


    fun syncTime(): Int {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val syncTime = SyncTime()
        syncTime.syncTime = simpleDateFormat.format(Date())
        val json = JSONUtil.getJsonString(syncTime)
        val data = ByteArray(json.length)
        System.arraycopy(json.toByteArray(), 0, data, 0, json.toByteArray().size)
        val size = intArrayOf(1024)
        return AMTHidManager.instance().setConfig(ConfigType.DEVICE_TIME, data, size)
    }
}