package com.armatura.biomodule.bean

data class AMTResult<T>(val code: Int, val message: String, val result: T?)
