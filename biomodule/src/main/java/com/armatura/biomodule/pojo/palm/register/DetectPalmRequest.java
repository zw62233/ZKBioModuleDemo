package com.armatura.biomodule.pojo.palm.register;

import com.armatura.biomodule.pojo.common.Image;

import java.util.List;

/**
 * Created by Magic on 2020/10/13
 */
public class DetectPalmRequest {
    private List<Image> images;
    private boolean feature = false;
    private boolean palmInfo = false;
    private boolean picture = false;

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public void setIsNeedPalmInfo(boolean isNeedPalmInfo) {
        palmInfo = isNeedPalmInfo;
    }

    public void setIsNeedPicture(boolean isNeedPicture) {
        picture = isNeedPicture;
    }

    public void setIsNeedFeature(boolean isNeedFeature) {
        feature = isNeedFeature;
    }
}
