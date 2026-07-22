package com.armatura.biomodule.pojo.palm.register;

import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.module.CacheId;
import com.armatura.biomodule.pojo.palm.PalmFeature;
import com.armatura.biomodule.pojo.palm.PalmRect;
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo;

public class DetectPalmResult {

    private PalmInfo palmInfo;
    private PalmFeature feature;
    private PalmFeature featureVein;
    private PalmRect rect;
    private Image picture;
    /**
     * only for module internal register
     */
    private CacheId cacheId;

    public PalmInfo getPalmInfo() {
        return palmInfo;
    }

    public void setPalmInfo(PalmInfo palmInfo) {
        this.palmInfo = palmInfo;
    }

    public PalmFeature getFeature() {
        return feature;
    }

    public void setFeature(PalmFeature feature) {
        this.feature = feature;
    }

    public PalmFeature getFeatureVein() {
        return featureVein;
    }

    public void setFeatureVein(PalmFeature featureVein) {
        this.featureVein = featureVein;
    }

    public PalmRect getRect() {
        return rect;
    }

    public void setRect(PalmRect rect) {
        this.rect = rect;
    }

    public Image getPicture() {
        return picture;
    }

    public void setPicture(Image picture) {
        this.picture = picture;
    }


    public CacheId getCacheId() {
        return cacheId;
    }

    public void setCacheId(CacheId cacheId) {
        this.cacheId = cacheId;
    }
}