package com.armatura.biomodule.common;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Magic on 2020/9/17
 */
@IntDef(
        value = {
                RegisterType.FACE,
                RegisterType.PALM,
                RegisterType.TAKE_AVATAR,
                RegisterType.RFID
        }
)
@Retention(RetentionPolicy.SOURCE)
public @interface RegisterType {
    int FACE = 1;
    int PALM = 2;
    int TAKE_AVATAR = 3;
    int RFID = 4;
}




