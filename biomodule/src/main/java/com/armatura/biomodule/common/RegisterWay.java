package com.armatura.biomodule.common;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Magic on 2020/9/17
 */
@IntDef(
        value = {
                RegisterWay.LOCAL_IMAGE,
                RegisterWay.UVC_STREAM,
                RegisterWay.MODULE_INSIDE,
                RegisterWay.SNAP_SHOT,
                RegisterWay.MODULE_INSIDE_REGISTER_FACE_WITH_PHOTO,
                RegisterWay.MODULE_INSIDE_REGISTER_PALM_WITH_PHOTO,
                RegisterWay.MODULE_INSIDE_REGISTER_FACE_WITH_CACHE_ID,
                RegisterWay.MODULE_INSIDE_REGISTER_PALM_WITH_CACHE_ID,
                RegisterWay.RFID
        }
)
@Retention(RetentionPolicy.SOURCE)
public @interface RegisterWay {
    int LOCAL_IMAGE = 0;
    int UVC_STREAM = 1;
    int MODULE_INSIDE = 2;
    int SNAP_SHOT = 3;
    int MODULE_INSIDE_REGISTER_FACE_WITH_PHOTO = 4;
    int MODULE_INSIDE_REGISTER_PALM_WITH_PHOTO = 5;
    int MODULE_INSIDE_REGISTER_FACE_WITH_CACHE_ID = 6;
    int MODULE_INSIDE_REGISTER_PALM_WITH_CACHE_ID = 7;
    int RFID = 8;
}




