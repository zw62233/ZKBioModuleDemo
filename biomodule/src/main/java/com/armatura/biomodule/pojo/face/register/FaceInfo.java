package com.armatura.biomodule.pojo.face.register;

import android.graphics.Rect;

import com.armatura.biomodule.pojo.common.Attribute;
import com.armatura.biomodule.pojo.common.FacePose;
import com.armatura.biomodule.pojo.common.Landmark;

/**
 * @author magic.hu@armatura.com
 * @date 2020/08/11
 * @since 1.0.0
 */
public class FaceInfo {
    public Attribute attribute;

    public FacePose pose;

    /**
     * face rect
     */
    public Rect rect;

    public Landmark landmark;

    /**
     * face quality
     */
    public float score;
}
