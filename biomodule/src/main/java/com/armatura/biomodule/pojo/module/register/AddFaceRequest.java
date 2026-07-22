package com.armatura.biomodule.pojo.module.register;

import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.module.CacheId;

import java.util.List;

/**
 * Created by Magic on 2020/10/10
 */
public class AddFaceRequest {
    public String personId;
    public List<Image> images;
    public List<Features> features;
    private List<CacheId> cacheIds;


    public List<CacheId> getCacheIds() {
        return cacheIds;
    }

    public void setCacheIds(List<CacheId> cacheIds) {
        this.cacheIds = cacheIds;
    }
}
