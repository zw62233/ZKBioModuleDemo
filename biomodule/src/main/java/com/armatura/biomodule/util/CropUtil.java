package com.armatura.biomodule.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

/**
 * @author Magic
 */
public class CropUtil {
    private static final String TAG = "CropUtil";

    public static Bitmap cropAsSquare(Bitmap srcBitmap, Rect faceRect) {
        if (srcBitmap == null) {
            throw new RuntimeException("src bitmap can't be null");
        }
        if (faceRect == null) {
            throw new RuntimeException("rect can't be null");
        }
        int imageWidth = srcBitmap.getWidth();
        int imageHeight = srcBitmap.getHeight();
        int faceWidth = faceRect.width();

        int dstWidth = faceWidth * 5 / 2;
        dstWidth = Math.min(dstWidth, imageWidth);
        int dstHeight = dstWidth;

        int addtionalX = (dstWidth - faceWidth) / 2;
        int addtionalYTop = (dstHeight - faceRect.height()) * 7 / 10;

        int startX = faceRect.left - addtionalX;
        int startY = faceRect.top - addtionalYTop;


        if (startY < 0) {
            startY = 0;
        } else if (startY + dstHeight > imageHeight) {
            startY = startY - (startY + dstHeight - imageHeight);
        }

        if (startX < 0) {
            startX = 0;
        } else if (startX + dstWidth > imageWidth) {
            startX = startX - (startX + dstWidth - imageWidth);
        }

        return Bitmap.createBitmap(srcBitmap, startX, startY, dstWidth, dstHeight, null, false);
    }


    public static Bitmap cropByFixSizeEx(Bitmap srcBitmap, Rect faceRect) {
        if (srcBitmap == null) {
            throw new RuntimeException("src bitmap can't be null");
        }
        if (faceRect == null) {
            throw new RuntimeException("rect can't be null");
        }


        int imageWidth = srcBitmap.getWidth();
        int imageHeight = srcBitmap.getHeight();

        int defaultDstWidth = 300;
        int defaultDstHeight = 400;
        float cropRatio = defaultDstWidth * 1.0F / defaultDstHeight;

        float ratio = imageWidth * 1.0F / imageHeight;

        if (faceRect.height() * 15 / 10 > imageHeight || faceRect.left < 0 || faceRect.top < 0
                || faceRect.right > imageWidth || faceRect.bottom > imageHeight) {
            int expandDstHeight = faceRect.height() * 4;
            int expandDstWidth = (int) (expandDstHeight * ratio);
            srcBitmap = expandBitmap(srcBitmap, expandDstWidth, expandDstHeight);


            int expandHeightValue = (expandDstHeight - imageHeight) / 2;
            int expandWidthValue = (expandDstWidth - imageWidth) / 2;

            imageWidth = srcBitmap.getWidth();
            imageHeight = srcBitmap.getHeight();

            int shiftHeight = expandHeightValue / 2;
            int shiftWidth = expandWidthValue / 2;
            faceRect.top += shiftHeight;
            faceRect.bottom += shiftHeight;
            faceRect.left += shiftWidth;
            faceRect.right += shiftWidth;
        }

        Bitmap crop = null;
        int cropAddX = 0;
        float increaseYTopPartRatio = 1F;
        if (faceRect.width() * 2 < imageWidth) {
            cropAddX = faceRect.width() / 2;
            increaseYTopPartRatio = 4 * 1.0F / 7;
        } else {
            cropAddX = (imageWidth - faceRect.width()) / 3;
            increaseYTopPartRatio = 6 * 1.0F / 7;
        }


        int startX = faceRect.left - cropAddX;
        int finalCropWidth = faceRect.width() + 2 * cropAddX;
        int cropX = Math.min(finalCropWidth, imageWidth);
        if (startX < 0) {
            startX = 0;
        }
        if (startX + cropX >= imageWidth) {
            int extra = Math.abs(startX + cropX - imageWidth);
            startX -= extra;
        }

        int cropY = (int) (cropX / cropRatio);
        int cropAddYExtra = cropY - faceRect.height();
        int startY = faceRect.top - (int) (cropAddYExtra * increaseYTopPartRatio);
        if (startY < 0) {
            startY = 0;
        }
        if (startY + cropY >= imageHeight) {
            int extra = Math.abs(startY + cropY - imageHeight);
            startY -= extra;
        }


        crop = Bitmap.createBitmap(srcBitmap, startX, startY, cropX, cropY, null, false);
        return crop;
    }

    public static Bitmap cropByFixSize(Bitmap srcBitmap, Rect faceRect) {
        if (srcBitmap == null) {
            throw new RuntimeException("src bitmap can't be null");
        }
        if (faceRect == null) {
            throw new RuntimeException("rect can't be null");
        }
        Bitmap dstBitmap = null;
        int imageWidth = srcBitmap.getWidth();
        int imageHeight = srcBitmap.getHeight();

        int defaultDstWidth = 544;
        int defaultDstHeight = 608;

        float ratio = defaultDstWidth * 1.0F / defaultDstHeight;

        int dstWidth = 0;
        int dstHeight = 0;
        int faceWidth = faceRect.width();
        boolean isScale = false;
        float faceInScreenRatio = faceWidth * 1.0000F / imageWidth;
        Log.i(TAG, "[cropByFixSize]: ratio =" + faceInScreenRatio);
        if (faceInScreenRatio > 0.65F) {
            Bitmap expandBitmap = expandBitmap(srcBitmap, imageWidth * 100 / 65, imageHeight * 100 / 65);

            dstWidth = expandBitmap.getWidth();
            dstHeight = expandBitmap.getHeight();

            int expandWidth = dstWidth - imageWidth;
            int expandHeight = dstHeight - imageHeight;

            int shiftHeight = expandHeight / 2;
            int shiftWidth = expandWidth / 2;
            Log.i(TAG, "[cropByFixSize]: [origin rect]" + faceRect);
            faceRect.top += shiftHeight;
            faceRect.bottom += shiftHeight;
            faceRect.left += shiftWidth;
            faceRect.right += shiftWidth;
            Log.i(TAG, "[cropByFixSize]: [shift rect]" + faceRect);

            int addtionalX = (dstWidth - faceRect.width()) / 2;


            int cropX = faceWidth + 2 * addtionalX;
            int cropY = (int) (cropX / ratio);
            int addtionalY = (cropY - faceRect.height()) / 2;

            int startX = faceRect.left - addtionalX;
            int startY = faceRect.top - addtionalY;

            if (startX < 0) {
                startX = 0;
            }
            if (startY < 0) {
                startY = 0;
            }

            Bitmap crop = Bitmap.createBitmap(expandBitmap, startX, startY, cropX, cropY, null, false);

            return Bitmap.createScaledBitmap(crop, defaultDstWidth, defaultDstHeight, false);
        } else {
            if (faceWidth < imageWidth / 2) {
                int addtional = (defaultDstWidth - faceWidth) / 2;
                int startX = faceRect.left - addtional;
                int startY = 0;
                if (faceInScreenRatio < 0.4F) {
                    startY = faceRect.top - addtional * 15 / 10;
                } else if (faceInScreenRatio < 0.45F) {
                    startY = faceRect.top - addtional * 18 / 10;
                } else if (faceInScreenRatio < 0.5F) {
                    dstWidth = defaultDstWidth * 12 / 10;
                    dstHeight = defaultDstHeight * 12 / 10;
                    addtional = (dstWidth - faceWidth) / 2;
                    startY = faceRect.top - addtional * 19 / 10;
                    isScale = true;
                }

                if (isScale) {
                    if (startY < 0) {
                        startY = 0;
                    } else if (startY + dstHeight > imageHeight) {
                        startY = startY - (startY + dstHeight - imageHeight);
                    }
                } else {
                    if (startY < 0) {
                        startY = 0;
                    } else if (startY + defaultDstHeight > imageHeight) {
                        startY = startY - (startY + defaultDstHeight - imageHeight);
                    }
                }

                if (isScale) {
                    if (startX < 0) {
                        startX = 0;
                    } else if (startX + dstWidth > imageWidth) {
                        startX = Math.max(startX - (startX + dstWidth - imageWidth), 0);
                    }
                } else {
                    if (startX < 0) {
                        startX = 0;
                    } else if (startX + defaultDstWidth > imageWidth) {
                        startX = Math.max(startX - (startX + defaultDstWidth - imageWidth), 0);
                    }
                }


                if (isScale) {
                    dstBitmap = Bitmap.createBitmap(srcBitmap, startX, startY, dstWidth, dstHeight, null, false);
                    dstBitmap = scaleBitmap(dstBitmap, defaultDstWidth, defaultDstHeight);
                } else {
                    dstBitmap = Bitmap.createBitmap(srcBitmap, startX, startY, defaultDstWidth, defaultDstHeight, null, false);
                }

                return dstBitmap;
            } else {
                int cropImageWidth = imageWidth;
                int cropImageHeight = (int) (cropImageWidth * (defaultDstHeight * 1.0000F / defaultDstWidth));
                int startX = 0;
                int addtional = (cropImageHeight - faceWidth) / 2;
                int startY = faceRect.top - addtional * 17 / 10;

                if (startY < 0) {
                    startY = 0;
                } else if (startY + cropImageHeight > imageHeight) {
                    startY = Math.max(startY - (startY + cropImageHeight - imageHeight), 0);

                }
                Bitmap crop = Bitmap.createBitmap(srcBitmap, startX, startY, cropImageWidth, cropImageHeight, null, false);
                return Bitmap.createScaledBitmap(crop, defaultDstWidth, defaultDstHeight, false);
            }
        }

    }

    public static Bitmap cropByImageRatio(Bitmap srcBitmap, Rect faceRect) {
        if (srcBitmap == null) {
            throw new RuntimeException("src bitmap can't be null");
        }
        if (faceRect == null) {
            throw new RuntimeException("rect can't be null");
        }
        int imageWidth = srcBitmap.getWidth();
        int imageHeight = srcBitmap.getHeight();

        float ratio = imageWidth * 1.0000F / imageHeight;

        int faceWidth = faceRect.width();
        int dstHeight = faceWidth * 5 / 2;
        dstHeight = Math.min(imageHeight, dstHeight);
        int dstWidth = (int) (dstHeight * ratio);

        int addtionalX = (dstWidth - faceWidth) / 2;
        int addtionalY = (dstHeight - faceRect.height()) / 2;

        int startX = faceRect.left - addtionalX;
        int startY = faceRect.top - addtionalY;


        if (startY < 0) {
            startY = 0;
        } else if (startY + dstHeight > imageHeight) {
            startY = startY - (startY + dstHeight - imageHeight);
        }

        if (startX < 0) {
            startX = 0;
        } else if (startX + dstWidth > imageWidth) {
            startX = startX - (startX + dstWidth - imageWidth);
        }

        return Bitmap.createBitmap(srcBitmap, startX, startY, dstWidth, dstHeight, null, false);
    }

    public static Bitmap scaleBitmap(Bitmap srcBitmap, int dstWidth, int dstHeight) {
        if (srcBitmap == null) {
            throw new RuntimeException("src bitmap can't be null");
        }
        if (dstWidth <= 0) {
            throw new RuntimeException("dstWidth must > 0");
        }
        if (dstHeight <= 0) {
            throw new RuntimeException("dstHeight must > 0");
        }
        int iw = srcBitmap.getWidth();
        int ih = srcBitmap.getHeight();

        return Bitmap.createScaledBitmap(srcBitmap, dstWidth, dstHeight, false);

    }

    public static Bitmap crapAndScaleToFixSize(Bitmap srcBitmap, Rect faceRect, boolean expand) {
        if (srcBitmap == null) {
            throw new RuntimeException("src bitmap can't be null");
        }
        if (faceRect == null) {
            throw new RuntimeException("rect can't be null");
        }

        int imageWidth = srcBitmap.getWidth();
        int imageHeight = srcBitmap.getHeight();
        int faceWidth = faceRect.width();


        int cropWidth = Math.min(faceWidth * 23 / 10, imageWidth);
        int cropHeight = cropWidth;

        Bitmap cropBitmap = null;
        if (expand && cropWidth >= imageWidth) {
            cropHeight = Math.min(faceWidth * 23 / 10, imageHeight);
            cropWidth = imageWidth;

            int startX = 0;
            int addtionalY = (cropHeight - faceRect.height()) / 2;
            int startY = faceRect.top - addtionalY;

            Log.i(TAG, "[crapAndScaleToFixSize]: faceWidth=" + faceWidth);
            if (faceWidth > 500) {
                startY = faceRect.top - addtionalY * 4 / 10;
            } else if (faceWidth > 400) {
                startY = faceRect.top - addtionalY * 7 / 10;
            } else if (faceWidth > 300) {
                startY = faceRect.top - addtionalY * 11 / 10;
            } else if (faceWidth > 200) {
                startY = faceRect.top - addtionalY * 15 / 10;
            }

            if (startY < 0) {
                startY = 0;
            } else if (startY + cropHeight > imageHeight) {
                startY = startY - (startY + cropHeight - imageHeight);
            }


            cropBitmap = Bitmap.createBitmap(srcBitmap, startX, startY, cropWidth, cropHeight, null, false);

            cropBitmap = expandBitmap(cropBitmap, cropHeight, cropHeight);

        } else {
            int addtionalX = (cropWidth - faceWidth) / 2;
            int addtionalY = (cropHeight - faceRect.height()) / 2;
            int startY = faceRect.top - addtionalY;
            int startX = faceRect.left - addtionalX;

            if (faceWidth > 500) {
                startY = faceRect.top - addtionalY * 14 / 10;
            } else if (faceWidth > 400) {
                startY = faceRect.top - addtionalY * 15 / 10;
            } else if (faceWidth > 300) {
                startY = faceRect.top - addtionalY * 16 / 10;
            } else if (faceWidth > 200) {
                startY = faceRect.top - addtionalY * 15 / 10;
            }


            if (startY < 0) {
                startY = 0;
            } else if (startY + cropHeight > imageHeight) {
                startY = startY - (startY + cropHeight - imageHeight);
            }

            if (startX < 0) {
                startX = 0;
            } else if (startX + cropWidth > imageWidth) {
                startX = startX - (startX + cropWidth - imageWidth);
            }

            cropBitmap = Bitmap.createBitmap(srcBitmap, startX, startY, cropWidth, cropHeight, null, false);
        }


        return Bitmap.createScaledBitmap(cropBitmap, 300, 300, false);

    }


    private static Bitmap expandBitmap(Bitmap srcBitmap, int outWidth, int outHeight) {
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        Bitmap newbmp = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        newbmp.eraseColor(Color.parseColor("#000000"));
        Canvas canvas = new Canvas(newbmp);
        canvas.drawColor(Color.TRANSPARENT);
        int left = (outWidth - width) / 2;
        int top = (outHeight - height) / 2;
        canvas.drawBitmap(srcBitmap, left, top, null);
        return newbmp;
    }

    public static synchronized Bitmap cropAvatar(Bitmap bitmap, Rect rect) {
        Bitmap avatar = null;
        if (bitmap != null && rect != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            int faceHeight = rect.bottom - rect.top;
            int addValue = faceHeight / 3;

            rect.bottom = Math.min(height, rect.bottom + addValue);
            rect.left = Math.max(0, rect.left - addValue);
            rect.right = Math.min(width, rect.right + addValue);
            rect.top = Math.max(0, rect.top - faceHeight / 2 - addValue);

            int cropWidth = rect.right - rect.left;
            int cropHeight = rect.bottom - rect.top;

            try {
                if (!bitmap.isRecycled()) {
                    avatar = Bitmap.createBitmap(bitmap,
                            rect.left,
                            rect.top,
                            cropWidth,
                            cropHeight);
                }
            } catch (Exception | OutOfMemoryError ignore) {
            }

            rect = null;
        }

        return avatar;
    }
}
