/**
 * Copyright 2020 bejson.com
 */
package com.armatura.biomodule.pojo.face.register;


import com.armatura.biomodule.pojo.common.Image;

/**
 * detect face data
 */
public class DetectFaceRequest {

    private Image image;
    private boolean feature = false;
    private boolean faceInfo = false;
    private boolean picture = false;
    private Filter filter;

    public void setImage(Image image) {
        this.image = image;
    }

    public void setIsNeedFaceInfo(boolean isNeedFaceInfo) {
        faceInfo = isNeedFaceInfo;
    }

    public void setIsNeedPicture(boolean isNeedPicture) {
        picture = isNeedPicture;
    }

    public void setIsNeedFeature(boolean isNeedFeature) {
        feature = isNeedFeature;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public static class Filter {
        public int widthMinValue = 10;
        public int heightMinValue = 10;
    }
}