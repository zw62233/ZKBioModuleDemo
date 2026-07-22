package com.armatura.biomodule.pojo.module.record;

/**
 * Created by Magic on 2020/9/8
 */
public class AttRecord {
    private String userId;
    private float similar;
    private String imageUri;

    public AttRecord() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public float getSimilar() {
        return similar;
    }

    public void setSimilar(float similar) {
        this.similar = similar;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}
