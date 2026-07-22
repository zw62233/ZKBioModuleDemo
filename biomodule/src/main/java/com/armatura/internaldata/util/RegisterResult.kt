package com.armatura.internaldata.util

sealed class RegisterResult<T> {

    data class Status<T>(val code: Int, var message: String, val result: T?) : RegisterResult<T>()

    data class Progress<T>(val code: Int, var message: String, val result: T?) : RegisterResult<T>()

    data class ExtractInfo<T>(val code: Int, val feature: String, val result: T?) :
        RegisterResult<T>()
}