package com.armatura.biomodule.pojo.palm.register;

import com.armatura.biomodule.pojo.module.CacheId;
import com.armatura.biomodule.pojo.palm.PalmFeature;

/**
 * Created by Magic on 2020/10/15
 */
public class MergePalmResult {

    private PalmFeature feature;

    private CacheId cacheId;

    public PalmFeature getFeature() {
        return feature;
    }

    public void setFeature(PalmFeature feature) {
        this.feature = feature;
    }

    public CacheId getCacheId() {
        return cacheId;
    }

    public void setCacheId(CacheId cacheId) {
        this.cacheId = cacheId;
    }
}
