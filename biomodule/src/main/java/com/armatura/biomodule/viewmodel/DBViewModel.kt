package com.armatura.biomodule.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.common.RegisterOperate
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.manager.FaceManager
import com.armatura.biomodule.manager.NIRPalmManager
import com.armatura.biomodule.manager.PalmManager
import com.armatura.biomodule.pojo.common.BioType
import com.armatura.biomodule.register.RegisterStatus
import com.armatura.biomodule.util.stringFormatSTT
import com.armatura.constant.ErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf

data class TempPalmFeature(
    val feature: ByteArray,
    val bioType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TempPalmFeature

        if (!feature.contentEquals(other.feature)) return false
        if (bioType != other.bioType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = feature.contentHashCode()
        result = 31 * result + bioType.hashCode()
        return result
    }
}

class DBViewModel : ViewModel() {
    companion object {
        private const val TAG = "DBViewModel"
    }

    fun addFaceToUserByFlow(
        userId: String,
        faceFeature: ByteArray,
        operate: Int,
    ): Flow<AMTResult<Unit>> {
        return flowOf(addFaceToUser(userId, faceFeature, operate)).catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }

    fun addPalmToUserByFlow(
        userId: String,
        tempPalmFeatureList: List<TempPalmFeature>,
        operate: Int,
    ): Flow<AMTResult<Unit>> {
        return flowOf(addPalmToUser(userId, tempPalmFeatureList, operate)).catch { exception ->
            emit(AMTResult(404, "${exception.message}", null))
        }
    }


    private fun addFaceToUser(
        userId: String,
        faceFeature: ByteArray,
        operate: Int,
    ): AMTResult<Unit> {
        val id = ByteArray(40)
        val score = FloatArray(1)
        val ret = FaceManager.getInstance().dbIdentify(faceFeature, id, score)
        if (ret != 0) {
            return AMTResult(ret, "identify failed,$ret", null)
        }
        val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))
        if (score[0] > Config.instance().faceIdentifyThreshold) {
            if (operate == RegisterOperate.ADD) {
                return AMTResult(
                    404, stringFormatSTT(
                        RegisterStatus.FACE_REGISTERED,
                        identifyUserId,
                        Config.instance().faceIdentifyThreshold,
                        score[0]
                    ), null
                )
            }
            if (operate == RegisterOperate.UPDATE && identifyUserId != userId) {
                return AMTResult(
                    404, stringFormatSTT(
                        RegisterStatus.FACE_REGISTERED,
                        identifyUserId,
                        Config.instance().faceIdentifyThreshold,
                        score[0]
                    ), null
                )
            }
        }

        val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId)
        userInfo.faceFeature = faceFeature
        BioDataUtil.instance().updateUserInfo(userInfo)
        return AMTResult(ErrorCode.SUCCESS, getString(R.string.success), null)
    }


    private fun addPalmToUser(
        userId: String,
        tempPalmFeatureList: List<TempPalmFeature>,
        operate: Int,
    ): AMTResult<Unit> {
        val id = ByteArray(40)
        val score = FloatArray(1)
        tempPalmFeatureList.forEach { tempPalmFeature ->
            if (tempPalmFeature.bioType == BioType.PALM) {
                PalmManager.getInstance().dbIdentify(tempPalmFeature.feature, id, score)
                val palmVLRecognizeThreshold = Config.instance().palmVLIdentifyThreshold
                Log.d(
                    TAG,
                    "[addPalmToUser]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold
                )
                if (score[0] > Config.instance().palmVLIdentifyThreshold) {
                    val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))
                    if (operate == RegisterOperate.ADD) {
                        return AMTResult(
                            404, stringFormatSTT(
                                RegisterStatus.PALM_REGISTERED, identifyUserId,
                                Config.instance().palmVLIdentifyThreshold, score[0]
                            ), null
                        )
                    }
                    if (operate == RegisterOperate.UPDATE && identifyUserId != userId) {
                        return AMTResult(
                            404, stringFormatSTT(
                                RegisterStatus.PALM_REGISTERED, identifyUserId,
                                Config.instance().palmVLIdentifyThreshold, score[0]
                            ), null
                        )
                    }
                }

                val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId)
                userInfo.palmFeature1 = tempPalmFeature.feature
                BioDataUtil.instance().updateUserInfo(userInfo)
            } else if (tempPalmFeature.bioType == BioType.PALM_VEIN) {
                NIRPalmManager.getInstance().dbIdentify(tempPalmFeature.feature, id, score)
                val palmVLRecognizeThreshold = Config.instance().palmVLLivenessThreshold
                Log.d(
                    TAG,
                    "[addPalmToUser]: dbIdentify score=" + score[0] + ", threshold=" + palmVLRecognizeThreshold
                )
                if (score[0] > Config.instance().palmVLIdentifyThreshold) {
                    val identifyUserId = String(id, 0, BioDataUtil.getValidLength(id))
                    if (operate == RegisterOperate.ADD) {
                        return AMTResult(
                            404, stringFormatSTT(
                                RegisterStatus.PALM_REGISTERED, identifyUserId,
                                Config.instance().palmVLIdentifyThreshold, score[0]
                            ), null
                        )
                    }
                    if (operate == RegisterOperate.UPDATE && identifyUserId != userId) {
                        return AMTResult(
                            404, stringFormatSTT(
                                RegisterStatus.PALM_REGISTERED, identifyUserId,
                                Config.instance().palmVLIdentifyThreshold, score[0]
                            ), null
                        )
                    }
                }

                val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId)
                userInfo.palmFeature2 = tempPalmFeature.feature
                BioDataUtil.instance().updateUserInfo(userInfo)
            }
        }
        return AMTResult(ErrorCode.SUCCESS, "", null)
    }
}