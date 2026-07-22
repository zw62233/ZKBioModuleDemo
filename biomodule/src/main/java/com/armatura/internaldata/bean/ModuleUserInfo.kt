package com.armatura.internaldata.bean

import com.armatura.biomodule.pojo.common.Image
import com.armatura.biomodule.pojo.module.register.Features

/**
 * Created by Jeremy on 2022/11/8.
 */
data class ModuleUserInfo(
    var personId: String = "",
    var features: List<Features>? = null,
    var images: List<Image>? = null
)
