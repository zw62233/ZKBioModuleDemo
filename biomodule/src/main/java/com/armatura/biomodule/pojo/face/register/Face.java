package com.armatura.biomodule.pojo.face.register;

import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.face.FaceFeature;
import com.armatura.biomodule.pojo.module.CacheId;

/**
 * Created by Magic on 2020/9/28
 */
public class Face {
    public FaceInfo faceInfo;
    public FaceFeature feature;
    public Image picture;
    public float blur;

    /**
     * only for internal register
     */
    public CacheId cacheId;
}
