package com.armatura.biomodule.common;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Jeremy on 2023/3/14.
 */
@IntDef({WatchDogType.UVC, WatchDogType.HID})
@Retention(RetentionPolicy.SOURCE)
public @interface WatchDogType {
    int UVC = 1;
    int HID = 2;
}
