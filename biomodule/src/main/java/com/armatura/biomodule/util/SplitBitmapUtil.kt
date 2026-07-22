package com.armatura.biomodule.util

import android.graphics.Bitmap

object SplitBitmapUtil {

    fun splitBitmapVertically(bitmap: Bitmap): Pair<Bitmap, Bitmap> {
        val width = bitmap.width
        val height = bitmap.height

        val midHeight = height / 2

        val topBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, midHeight)
        val bottomBitmap = Bitmap.createBitmap(bitmap, 0, midHeight, width, height - midHeight)

        return Pair(topBitmap, bottomBitmap)
    }

}