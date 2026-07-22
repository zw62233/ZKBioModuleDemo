package com.armatura.biomodule.pojo.palm.register;

import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.module.CacheId;
import com.armatura.biomodule.pojo.module.register.Features;

import java.util.List;

/**
 * Created by Magic on 2020/10/15
 */
public class AddPalmRequest {
    public String personId;

    private List<Image> images;

    private List<Features> features;

    private List<CacheId> cacheIds;

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public List<Features> getFeatures() {
        return features;
    }

    public void setFeatures(List<Features> features) {
        this.features = features;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public List<CacheId> getCacheIds() {
        return cacheIds;
    }

    public void setCacheIds(List<CacheId> cacheIds) {
        this.cacheIds = cacheIds;
    }
}
