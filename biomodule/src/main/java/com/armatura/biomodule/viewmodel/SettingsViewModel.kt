package com.armatura.biomodule.viewmodel

import androidx.lifecycle.ViewModel
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.pojo.info.DeviceInfo
import com.armatura.biomodule.pojo.setting.CommonSettingData
import com.armatura.biomodule.pojo.setting.DeviceSettings
import com.armatura.biomodule.pojo.setting.FaceSettings
import com.armatura.biomodule.pojo.setting.FuncSettings
import com.armatura.biomodule.pojo.setting.PalmSetting
import com.armatura.biomodule.util.getDataJSONObject
import com.armatura.biomodule.util.getDetail
import com.armatura.biomodule.util.getStatus
import com.armatura.constant.ConfigType
import com.armatura.constant.ErrorCode
import com.armatura.constant.ManageType
import com.armatura.translib.AMTHidManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class SettingsViewModel : ViewModel() {

    @Deprecated("use [getFaceSettings] and [getDeviceSettings]")
    fun getCommonSettingsFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getCommonSettings())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getCaptureFilterConfigFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getCaptureFilterConfig())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getPalmSettingFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getPalmSetting())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getDeviceSettingsFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getDeviceSettings())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getFaceSettingsFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getFaceSettings())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getDeviceInfoFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getDeviceInfo())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getPersonStatisticInfoFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getPersonStatisticInfo())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun getFuncSettingsFlow(): Flow<AMTResult<JSONObject?>> {
        return flow {
            emit(getFuncSettings())
        }.catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }


    fun getCommonSettings(): AMTResult<JSONObject?> {
        val configData = ByteArray(30 * 1024)
        val size = IntArray(1)
        AMTHidManager.instance().getConfig(ConfigType.COMMON_CONFIG, configData, size).let {
            if (it != ErrorCode.SUCCESS) {
                return AMTResult(it, "Get CommonSettings failed\nHID ret = $it", null)
            }
            val response = String(configData, 0, size[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.ERROR_NONE) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get CommonSettings Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject(CommonSettingData.KEY)
                )
            }
        }
    }


    private fun getCaptureFilterConfig(): AMTResult<JSONObject?> {
        val data = ByteArray(20 * 1024)
        val size = IntArray(1)
        val ret = AMTHidManager.instance()
            .getConfig(ConfigType.CAPTURE_FILTER_CONFIG, data, size)
        if (ret != ErrorCode.SUCCESS) {
            return AMTResult(ret, "Get CaptureFilter Failed\nHID ret = $ret", null)
        }
        val response = String(data, 0, size[0])
        val responseJSONObject = JSONObject(response)
        val status = responseJSONObject.getStatus()

        if (status != ErrorCode.SUCCESS) {
            val detail = responseJSONObject.getDetail()
            return AMTResult(status, "Get CaptureFilter Failed\n${detail}", null)
        } else {
            return AMTResult(
                ErrorCode.SUCCESS,
                "success",
                responseJSONObject.getDataJSONObject("captureFilter")
            )
        }
    }


    private fun getPalmSetting(): AMTResult<JSONObject?> {
        val data = ByteArray(20 * 1024)
        val size = IntArray(1)
        AMTHidManager.instance().getConfig(ConfigType.PALM_CONFIG, data, size).let { ret ->
            if (ret != ErrorCode.SUCCESS) {
                return AMTResult(ret, "Get PalmSettings failed\nHID ret = $ret", null)
            }
            val response = String(data, 0, size[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.SUCCESS) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get PalmSettings Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject(PalmSetting.KEY)
                )
            }
        }
    }

    private fun getDeviceSettings(): AMTResult<JSONObject?> {
        val configData = ByteArray(30 * 1024)
        val size = IntArray(1)
        AMTHidManager.instance().getConfig(ConfigType.DEVICE_CONFIG, configData, size).let {
            if (it != ErrorCode.SUCCESS) {
                return AMTResult(it, "Get DeviceSettings failed\nHID ret = $it", null)
            }
            val response = String(configData, 0, size[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.ERROR_NONE) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get DeviceSettings Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject(DeviceSettings.KEY)
                )
            }
        }
    }


    private fun getFaceSettings(): AMTResult<JSONObject?> {
        val configData = ByteArray(30 * 1024)
        val size = IntArray(1)
        AMTHidManager.instance().getConfig(ConfigType.FACE_CONFIG, configData, size).let {
            if (it != ErrorCode.SUCCESS) {
                return AMTResult(it, "Get FaceSettings failed\nHID ret = $it", null)
            }
            val response = String(configData, 0, size[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.ERROR_NONE) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get FaceSettings Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject(FaceSettings.KEY)
                )
            }
        }
    }

    private fun getDeviceInfo(): AMTResult<JSONObject?> {
        val configData = ByteArray(30 * 1024)
        val size = IntArray(1)
        AMTHidManager.instance().getConfig(ConfigType.DEVICE_INFORMATION, configData, size).let {
            if (it != ErrorCode.SUCCESS) {
                return AMTResult(it, "Get DeviceInfo failed\nHID ret = $it", null)
            }
            val response = String(configData, 0, size[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.ERROR_NONE) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get DeviceInfo Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject(DeviceInfo.KEY)
                )
            }
        }
    }

    private fun getPersonStatisticInfo(): AMTResult<JSONObject?> {
        val resultByteArray = ByteArray(10 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        AMTHidManager.instance().manageModuleData(
            ManageType.QUERY_STATISTICS, null,
            resultByteArray, resultSize
        ).let {
            if (it != ErrorCode.SUCCESS) {
                return AMTResult(it, "Get personStatisticInfo failed\nHID ret = $it", null)
            }
            val response = String(resultByteArray, 0, resultSize[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.ERROR_NONE) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get personStatisticInfo Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject()
                )
            }
        }
    }


    private fun getFuncSettings(): AMTResult<JSONObject?> {
        val resultByteArray = ByteArray(10 * 1024)
        val resultSize = intArrayOf(resultByteArray.size)
        AMTHidManager.instance().getConfig(
            ConfigType.FUNC_SETTINGS,
            resultByteArray, resultSize
        ).let {
            if (it != ErrorCode.SUCCESS) {
                return AMTResult(it, "Get FuncSettings failed\nHID ret = $it", null)
            }
            val response = String(resultByteArray, 0, resultSize[0])
            val responseJSONObject = JSONObject(response)
            val status = responseJSONObject.getStatus()
            if (status != ErrorCode.ERROR_NONE) {
                val detail = responseJSONObject.getDetail()
                return AMTResult(status, "Get FuncSettings Failed\n${detail}", null)
            } else {
                return AMTResult(
                    ErrorCode.SUCCESS,
                    "success",
                    responseJSONObject.getDataJSONObject(FuncSettings.KEY)
                )
            }
        }
    }
}