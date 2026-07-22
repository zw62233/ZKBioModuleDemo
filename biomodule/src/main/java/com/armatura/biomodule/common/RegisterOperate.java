package com.armatura.biomodule.common;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Magic on 2020/9/17
 */
@IntDef(
        value = {
                RegisterOperate.ADD,
                RegisterOperate.UPDATE
        }
)
@Retention(RetentionPolicy.SOURCE)
public @interface RegisterOperate {
    int ADD = 1;
    int UPDATE = 2;
}




