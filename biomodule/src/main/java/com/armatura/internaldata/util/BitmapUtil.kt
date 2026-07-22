package com.armatura.internaldata.util

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Created by Jeremy on 2023/3/8.
 */
object BitmapUtil {
    fun scaleBitmap(inputBitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(inputBitmap, targetWidth, targetHeight, true)
    }

    fun rotateBitmap(bitmap: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}