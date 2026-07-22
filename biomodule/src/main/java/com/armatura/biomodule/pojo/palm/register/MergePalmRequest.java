package com.armatura.biomodule.pojo.palm.register;

import com.armatura.biomodule.pojo.module.CacheId;

import java.util.List;

/**
 * Created by Magic on 2020/10/15
 */
public class MergePalmRequest {
    private boolean feature = false;

    private List<CacheId> cacheIds;

    public boolean isFeature() {
        return feature;
    }

    public void setFeature(boolean feature) {
        this.feature = feature;
    }

    public List<CacheId> getCacheIds() {
        return cacheIds;
    }

    public void setCacheIds(List<CacheId> cacheIds) {
        this.cacheIds = cacheIds;
    }
}
