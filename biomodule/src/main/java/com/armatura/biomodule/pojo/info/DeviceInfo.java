package com.armatura.biomodule.pojo.info;

import androidx.annotation.NonNull;

public class DeviceInfo {
    public final static String KEY = "deviceInfo";
    public String gPlatform_VERSION;
    public String cpID;
    public String sn;
    public String hidVer;
    public String firmVer;
    public String faceVer;

    public String palmVer;

    public DeviceInfo() {
    }

    @NonNull
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "gPlatform_VERSION='" + gPlatform_VERSION + '\'' +
                ", cpID='" + cpID + '\'' +
                ", sn='" + sn + '\'' +
                ", hidVer='" + hidVer + '\'' +
                ", firmVer='" + firmVer + '\'' +
                ", faceVer='" + faceVer + '\'' +
                ", palmVer='" + palmVer + '\'' +
                '}';
    }
}
