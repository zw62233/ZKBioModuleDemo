package com.armatura.biomodule.pojo.common;

import androidx.annotation.NonNull;

import com.armatura.biomodule.common.Common;

/**
 * Created by Magic on 2020/9/28
 */
public class LiveData {
    public float livenessScore;
    /**
     * see {@link com.armatura.biomodule.common.Common.FaceLiveStatus}
     */
    public int liveness;

    public int livenessMode = Common.FaceLiveMode.DISABLE.getCode();

    //add at 20200712
    public long irFrameId;
    public float quality;

    @NonNull
    @Override
    public String toString() {
        return "LiveData{" +
                "livenessScore=" + livenessScore +
                ", liveness=" + liveness +
                ", livenessMode=" + livenessMode +
                ", irFrameId=" + irFrameId +
                ", quality=" + quality +
                '}';
    }


    public String toShortString() {
        if (livenessMode == Common.FaceLiveMode.DISABLE.getCode()) {
            return "Liveness: Disable";
        }
        return "Liveness: " + Common.FaceLiveStatus.getStringByCode(liveness);
    }

}
