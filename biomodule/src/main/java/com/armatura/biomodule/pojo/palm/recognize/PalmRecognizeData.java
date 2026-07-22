package com.armatura.biomodule.pojo.palm.recognize;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.armatura.biomodule.pojo.face.recognize.IdentifyInfo;
import com.armatura.biomodule.pojo.palm.PalmFeature;

import java.util.List;

/**
 * @author magic.hu@armatura.com
 * @date 2020/08/07
 * @since 1.0.0
 */
public class PalmRecognizeData {
    private PalmFeature feature;
    private PalmInfo trackInfo;
    private PalmFeature featureVein;//since pv-50 20240705
    private List<IdentifyInfo> identify;


    public PalmFeature getFeature() {
        return feature;
    }


    public void setFeature(PalmFeature feature) {
        this.feature = feature;
    }


    public PalmInfo getTrackInfo() {
        return trackInfo;
    }

    public void setTrackInfo(PalmInfo trackInfo) {
        this.trackInfo = trackInfo;
    }

    public List<IdentifyInfo> getIdentify() {
        return identify;
    }

    public void setIdentify(List<IdentifyInfo> identify) {
        this.identify = identify;
    }

    public PalmFeature getFeatureVein() {
        return featureVein;
    }

    public PalmRecognizeData setFeatureVein(PalmFeature featureVein) {
        this.featureVein = featureVein;
        return this;
    }

    public PalmRecognizeData copy() {
        PalmRecognizeData palmRecognizeData = new PalmRecognizeData();
        palmRecognizeData.setTrackInfo(trackInfo.copy());
        if (this.feature != null) {
            palmRecognizeData.feature = this.feature.copy();
        }
        if (this.featureVein != null) {
            palmRecognizeData.featureVein = this.featureVein.copy();
        }
        return palmRecognizeData;
    }

    @NonNull
    @Override
    public String toString() {
        return "PalmRecognizeData{" +
                "feature=" + feature +
                "veinFeature=" + featureVein +
                ", trackInfo=" + trackInfo +
                ", identify=" + identify +
                '}';
    }
}
