package com.armatura.biomodule.pojo.face;

import android.util.Base64;

/**
 * @author magic.hu@armatura.com
 * @date 2020/08/11
 * @since 1.0.0
 */
public class FaceFeature {
    public String data;
    public int size;
    public String image;
    public String imgType;
    public String bioType;

    public byte[] getFeature() {
        if (data == null) {
            throw new NullPointerException("Face Feature data is null");
        }
        return Base64.decode(data, Base64.DEFAULT);
    }
}
