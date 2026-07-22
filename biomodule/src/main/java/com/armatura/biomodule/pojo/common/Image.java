package com.armatura.biomodule.pojo.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

/**
 * Auto-generated: 2020-09-07 15:31:21
 *
 * @author magic.hu
 */
public class Image {
    public static class Format {
        public final static String JPEG = "jpeg";
        public final static String GRAY = "gray";
    }


    public String bioType;
    /**
     * must be encoded by Base64
     */
    public String data;

    public String format;
    public int width;
    public int height;

    public Bitmap getBitmap() {
        Bitmap bitmap = null;
        switch (format) {
            case Format.GRAY:
                byte[] rawData = Base64.decode(data, Base64.NO_WRAP);
                bitmap = createGrayBitmap(rawData, width, height);
                break;
            case Format.JPEG:
                byte[] pixels = Base64.decode(data, Base64.NO_WRAP);
                bitmap = BitmapFactory.decodeByteArray(pixels, 0, pixels.length);
                break;
            default:
                break;
        }
        return bitmap;
    }


    private static Bitmap createGrayBitmap(byte[] values, int picW, int picH) {
        if (values == null || picW <= 0 || picH <= 0)
            return null;
        Bitmap bitmap = Bitmap
                .createBitmap(picW, picH, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[picW * picH];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = values[i] * 256 * 256 + values[i] * 256 + values[i] + 0xFF000000;
        }
        bitmap.setPixels(pixels, 0, picW, 0, 0, picW, picH);
        return bitmap;
    }
}