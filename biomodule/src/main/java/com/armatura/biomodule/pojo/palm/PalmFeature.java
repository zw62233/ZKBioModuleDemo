
package com.armatura.biomodule.pojo.palm;


import android.util.Base64;

public class PalmFeature {

    private String mergeTemplate;
    private int mergeTemplateSize;
    private String verTemplate;
    private int verTemplateSize;
    private String preTemplate;
    private int preTemplateSize;
    private String image;
    private String imgType;
    private String bioType;//since pv-50 20240705

    private String filename;//just for debug

    public void setMergeTemplate(String mergeTemplate) {
        this.mergeTemplate = mergeTemplate;
    }

    public String getMergeTemplate() {
        return mergeTemplate;
    }

    public void setMergeTemplateSize(int mergeTemplateSize) {
        this.mergeTemplateSize = mergeTemplateSize;
    }

    public int getMergeTemplateSize() {
        return mergeTemplateSize;
    }

    public void setVerTemplate(String verTemplate) {
        this.verTemplate = verTemplate;
    }

    public String getVerTemplate() {
        return verTemplate;
    }

    public byte[] getByteVerTemplate() {
        return Base64.decode(verTemplate, Base64.DEFAULT);
    }

    public void setVerTemplateSize(int verTemplateSize) {
        this.verTemplateSize = verTemplateSize;
    }

    public int getVerTemplateSize() {
        return verTemplateSize;
    }

    public String getPreTemplate() {
        return preTemplate;
    }

    public PalmFeature setPreTemplate(String preTemplate) {
        this.preTemplate = preTemplate;
        return this;
    }

    public int getPreTemplateSize() {
        return preTemplateSize;
    }

    public PalmFeature setPreTemplateSize(int preTemplateSize) {
        this.preTemplateSize = preTemplateSize;
        return this;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImgType() {
        return imgType;
    }

    public void setImgType(String imgType) {
        this.imgType = imgType;
    }

    public String getBioType() {
        return bioType;
    }

    public PalmFeature setBioType(String bioType) {
        this.bioType = bioType;
        return this;
    }

    public String getFileName() {
        return filename;
    }

    public void setFileName(String fileName) {
        this.filename = fileName;
    }

    public PalmFeature copy() {
        PalmFeature palmFeature = new PalmFeature();
        palmFeature.image = this.image;
        palmFeature.imgType = this.imgType;
        palmFeature.mergeTemplate = this.mergeTemplate;
        palmFeature.mergeTemplateSize = this.mergeTemplateSize;
        palmFeature.preTemplate = this.preTemplate;
        palmFeature.preTemplateSize = this.preTemplateSize;
        palmFeature.verTemplate = this.verTemplate;
        palmFeature.verTemplateSize = this.verTemplateSize;
        palmFeature.bioType = this.bioType;
        palmFeature.filename = this.filename;
        return palmFeature;
    }
}