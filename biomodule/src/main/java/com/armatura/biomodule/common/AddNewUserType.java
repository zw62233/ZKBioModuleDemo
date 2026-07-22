package com.armatura.biomodule.common;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Magic on 2020/9/17
 */
@IntDef(
        value = {
                AddNewUserType.LOCALE,
                AddNewUserType.MODULE_INSIDE
        }
)
@Retention(RetentionPolicy.SOURCE)
public @interface AddNewUserType {
    int LOCALE = 1;
    int MODULE_INSIDE = 2;
}




