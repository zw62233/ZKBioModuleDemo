package com.armatura.biomodule.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceView;

import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.camera.biodata.FaceData;
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache;
import com.armatura.biomodule.common.Common;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.pojo.common.Attribute;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.face.recognize.IdentifyInfo;
import com.armatura.biomodule.pojo.palm.PalmRect;
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo;
import com.armatura.biomodule.view.CircleDetectView;

import java.util.Locale;

public class CustomDraw {

    private static Paint paint;
    private static int fontSize = 15;
    private static final int rectSize = 4;
    /**
     * crop path
     */
    private final static Path refectUIPath = new Path();

    private final static Paint mRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        mRectPaint.setStyle(Paint.Style.FILL);
        mRectPaint.setColor(Color.WHITE);
    }

    public static void clearSurface(SurfaceView outputView) {
        Canvas canvas = null;
        try {
            canvas = outputView.getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputView.getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                Log.e("SurfaceDraw", "can not unlock canvas");
            }
        }
    }

    public static void drawFaceAndPalmInfo(SurfaceView outputView, float scale, DrawFaceData drawFaceData,
                                           RecognizedBioDataCache recognizedBioDataCache, PalmInfo palmInfo,
                                           boolean useReflectDraw) {
        drawFaceAndPalmInfo(outputView, scale, drawFaceData, recognizedBioDataCache, palmInfo,
                useReflectDraw, true);
    }

    public static void drawFaceAndPalmInfo(SurfaceView outputView, float scale, DrawFaceData drawFaceData,
                                           RecognizedBioDataCache recognizedBioDataCache, PalmInfo palmInfo,
                                           boolean useReflectDraw, boolean bDrawInfoText) {
        Canvas canvas = null;
        try {
            canvas = outputView.getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            canvas.scale(scale, scale);
            if (useReflectDraw) {
                refectUIPath.reset();
                float radius = ((canvas.getWidth() / 2F)
                        - (CircleDetectView.INDICATOR_RING_WIDTH
                        + CircleDetectView.OUT_RING_1_WIDTH
                        + CircleDetectView.OUT_RING_2_WIDTH
                        + CircleDetectView.SPACE_BETWEEN_RING)) / scale;
                refectUIPath.addCircle((canvas.getWidth() / 2F) / scale,
                        (canvas.getHeight() / 2F) / scale,
                        radius,
                        Path.Direction.CW);
                canvas.clipPath(refectUIPath);
            }

            int view_width = canvas.getWidth();
            int view_height = canvas.getHeight();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (paint == null) {
                paint = new Paint();
                paint.setAntiAlias(true);
            }

            if (drawFaceData != null && !drawFaceData.faceDataList.isEmpty()) {
                for (FaceData face : drawFaceData.faceDataList) {
                    if (face == null) {
                        continue;
                    }
                    paint.reset();
                    paint.setTextSize(fontSize);
                    Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                    float fHeight = fontMetrics.bottom - fontMetrics.top;
//                    Rect rect_r = rectMapping(face.trackData.rect, drawFaceData.width, drawFaceData.height, view_width, view_height);
//                    if (rect_r.left < 0 || rect_r.top < 0) {
//                        continue;
//                    }
                    //Rect rect_r = new Rect(10,10,100,100);
                    Common.RecognizedFaceData recognizedFaceData = null;
                    if (recognizedBioDataCache != null) {
                        recognizedFaceData = recognizedBioDataCache.getRecognizedFace(face.trackData.trackId);
                    }
                    if (useReflectDraw) {
                        drawFaceCircle(canvas, face.trackData.rect);
                    } else {
                        drawRect(canvas, face.trackData.rect, face, recognizedFaceData, fHeight, view_width, view_height, bDrawInfoText);
                    }
                }
            }

            if (palmInfo != null) {
                if (useReflectDraw) {
                    drawPalmCircle(canvas, palmInfo);
                } else {
                    drawPalmRect(canvas, palmInfo);
                }
            }

        } catch (Exception ignore) {
        } finally {
            try {
                outputView.getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                Log.e("OverlayDraw", "can not unlock canvas");
            }
        }
    }


    public static void drawOverlay(DrawFaceData drawFaceData, SurfaceView outputView,
                                   int view_width, int view_height,
                                   RecognizedBioDataCache recognizedBioDataCache, PalmInfo palmInfo) {
        Canvas canvas = null;
        try {
            canvas = outputView.getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            view_width = canvas.getWidth();
            view_height = canvas.getHeight();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (paint == null) {
                paint = new Paint();
                paint.setAntiAlias(true);
            }

            if (drawFaceData != null && !drawFaceData.faceDataList.isEmpty()) {
                for (Object obj : drawFaceData.faceDataList) {
                    FaceData face = (FaceData) obj;
                    if (face == null) {
                        continue;
                    }
                    paint.reset();
                    paint.setTextSize(fontSize);
                    Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                    float fHeight = fontMetrics.bottom - fontMetrics.top;
                    Rect rect_r = rectMapping(face.trackData.rect, drawFaceData.width, drawFaceData.height, view_width, view_height);
                    if (rect_r.left < 0 || rect_r.top < 0) {
                        continue;
                    }
                    //Rect rect_r = new Rect(10,10,100,100);
                    Common.RecognizedFaceData recognizedFaceData = recognizedBioDataCache.getRecognizedFace(face.trackData.trackId);
                    drawRect(canvas, rect_r, face, recognizedFaceData, fHeight, view_width, view_height, true);
                }
            }

            if (palmInfo != null) {
                drawPalmRect(canvas, palmInfo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputView.getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                Log.e("OverlayDraw", "can not unlock canvas");
            }
        }
    }


    public static void drawRectInfoOnly(DrawFaceData drawFaceData, PalmInfo palmInfo, SurfaceView outputView, RecognizedBioDataCache recognizedBioDataCache) {
        Canvas canvas = null;
        try {
            canvas = outputView.getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            int view_width = canvas.getWidth();
            int view_height = canvas.getHeight();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (paint == null) {
                paint = new Paint();
                paint.setAntiAlias(true);
            }

            if (drawFaceData != null && !drawFaceData.faceDataList.isEmpty()) {
                for (Object obj : drawFaceData.faceDataList) {
                    FaceData face = (FaceData) obj;
                    if (face == null) {
                        continue;
                    }
                    paint.reset();
                    paint.setTextSize(fontSize);
                    Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                    float fHeight = fontMetrics.bottom - fontMetrics.top;
                    Rect rect_r = rectMapping(face.trackData.rect, drawFaceData.width, drawFaceData.height, view_width, view_height);
                    if (rect_r.left < 0 || rect_r.top < 0) {
                        continue;
                    }
                    //Rect rect_r = new Rect(10,10,100,100);
                    Common.RecognizedFaceData recognizedFaceData = recognizedBioDataCache.getRecognizedFace(face.trackData.trackId);
                    drawRect(canvas, rect_r, face, recognizedFaceData, fHeight, view_width, view_height, true);
                }
            }

            if (palmInfo != null) {
                drawPalmCircle(canvas, palmInfo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputView.getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                Log.e("OverlayDraw", "can not unlock canvas");
            }
        }
    }

    private static boolean isFirstMarkState = true;
    private static boolean lastState = false;
    private static String palmRectPaintColor = "#03A9F4";

    public static void drawPalmRect(Canvas canvas, PalmInfo palmInfo) {
        PalmRect rect = palmInfo.getRect();
        float liveScore = palmInfo.getLiveScore();
        if (liveScore == -2 || liveScore == 0) {
            palmRectPaintColor = "#FFFFFF";
        } else if (liveScore == -3) {
            palmRectPaintColor = "#ffa500";
        } else if (liveScore > 0) {
            boolean isRealPalm = palmInfo.getLiveScore() > Config.instance().palmVLLivenessThreshold;
            palmRectPaintColor = isRealPalm ? "#03A9F4" : "#FF0000";
        }
        paint.setColor(Color.parseColor(palmRectPaintColor));
        paint.setStrokeWidth(5);
        paint.setPathEffect(null);
        //paint.setPathEffect(new DashPathEffect(new float[]{15, 15, 15, 15}, 1));


        canvas.drawLine(rect.x0, rect.y0, rect.x1, rect.y1, paint);
        canvas.drawLine(rect.x1, rect.y1, rect.x2, rect.y2, paint);
        canvas.drawLine(rect.x2, rect.y2, rect.x3, rect.y3, paint);
        canvas.drawLine(rect.x3, rect.y3, rect.x0, rect.y0, paint);
    }

    public static void drawFaceCircle(Canvas canvas, Rect faceRect) {
        int centerX = faceRect.left + (faceRect.width() / 2);
        int centerY = faceRect.top + (faceRect.height() / 2);
        int radius = (Math.min(faceRect.width(), faceRect.height())) / 2;
        canvas.drawCircle(centerX, centerY, radius, mRectPaint);
    }


    public static void drawPalmCircle(Canvas canvas, PalmInfo palmInfo) {
        PalmRect rect = palmInfo.getRect();
//        float liveScore = palmInfo.getLiveScore();
//        if (liveScore == -2 || liveScore == 0) {
//            palmRectPaintColor = "#FFFFFF";
//        } else if (liveScore == -3) {
//            palmRectPaintColor = "#ffa500";
//        } else if (liveScore > 0) {
//            boolean isRealPalm = palmInfo.getLiveScore() > Config.instance().palmVLLivenessThreshold;
//            palmRectPaintColor = isRealPalm ? "#03A9F4" : "#FF0000";
//        }
//        mRectPaint.setColor(Color.parseColor(palmRectPaintColor));


        int width = rect.x0 - rect.x1;
        int height = rect.y2 - rect.y0;
        int centerX = rect.x1 + width / 2;
        int centerY = rect.y1 + height / 2;
        int radius = (width > height ? height : width) / 2;

//        if (mRadialGradient == null) {
//            mRadialGradient = new RadialGradient(
//                    centerX,
//                    centerY,
//                    radius,
//                    new int[]{Color.WHITE, Color.TRANSPARENT},
//                    new float[]{0.5F,1.0F},
//                    Shader.TileMode.CLAMP
//            );
//        }
//        mRectPaint.setShader(mRadialGradient);
        canvas.drawCircle(centerX, centerY, radius, mRectPaint);

//        paint.setColor(Color.parseColor(palmRectPaintColor));
//        paint.setStrokeWidth(5);
//        paint.setPathEffect(null);
//        //paint.setPathEffect(new DashPathEffect(new float[]{15, 15, 15, 15}, 1));
//
//
//        canvas.drawLine(rect.x0, rect.y0, rect.x1, rect.y1, paint);
//        canvas.drawLine(rect.x1, rect.y1, rect.x2, rect.y2, paint);
//        canvas.drawLine(rect.x2, rect.y2, rect.x3, rect.y3, paint);
//        canvas.drawLine(rect.x3, rect.y3, rect.x0, rect.y0, paint);
    }

    public static void drawBitmapOnly(SurfaceView cameraSurfaceView, Rect displayArea, Bitmap bitmap) {
        Canvas canvas = null;
        try {
            canvas = cameraSurfaceView.getHolder().lockCanvas();
            if (canvas != null) {
                if (bitmap.getWidth() > bitmap.getHeight()) {
                    canvas.drawBitmap(bitmap, 0, 0, null);
                } else {
                    displayArea.set(0, 0, canvas.getWidth(), canvas.getHeight());
                    canvas.drawBitmap(bitmap,
                            null, displayArea, null);
                }

            }
        } finally {
            if (canvas != null) {
                cameraSurfaceView.getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    public static void drawOverlay2(DrawFaceData faceData, Bitmap bitmap, RecognizedBioDataCache recognizedBioDataCache, PalmInfo palmInfo) {

        Canvas canvas = new Canvas(bitmap);
        int view_width = bitmap.getWidth();
        int view_height = bitmap.getHeight();
        if (view_height >= 1080) {
            fontSize = 18;
        } else if (view_height >= 720) {
            fontSize = 14;
        } else {
            fontSize = 10;
        }
        try {
            if (paint == null) {
                paint = new Paint();
                paint.setAntiAlias(true);
            }

            if (faceData != null && !faceData.faceDataList.isEmpty()) {
                for (Object obj : faceData.faceDataList) {
                    FaceData face = (FaceData) obj;
                    if (face == null) {
                        continue;
                    }
                    //Log.d("*********",faces.boxRect.toString());

                    paint.reset();
                    paint.setTextSize(fontSize);
                    Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                    float fHeight = fontMetrics.bottom - fontMetrics.top;
                    Rect rect_r = rectMapping(face.trackData.rect, faceData.width, faceData.height, view_width, view_height);
                    if (rect_r.left < 0 || rect_r.top < 0) {
                        continue;
                    }
                    Common.RecognizedFaceData recognizedFaceData = recognizedBioDataCache.getRecognizedFace(face.trackData.trackId);
                    drawRect(canvas, rect_r, face, recognizedFaceData, fHeight, view_width, view_height, true);
                }
            }

            if (palmInfo != null) {
                drawPalmCircle(canvas, palmInfo);
            } else {
                isFirstMarkState = true;
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.e("OverlayDraw", "draw custom data exception\n" + e.getMessage());
        }
    }

    private static void drawRect(Canvas canvas, Rect rect_face, FaceData faceInfo, Common.RecognizedFaceData recognizedFaceData, float fontH, float viewW, float viewH, boolean bDrawInfoText) {
        //if (faceInfo == null) return;
        paint.setStrokeWidth(rectSize);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(recognizedFaceData.isPassLiveness ? Color.WHITE : Color.RED);

        int x1 = rect_face.left;
        int y1 = rect_face.top;
        int rect_width = rect_face.width();
        int rect_height = rect_face.height();

        //draw face fragment rect
        float rw = (float) rect_width / 10;
        float rh = (float) rect_height / 10;

        Path path = new Path();
        path.moveTo(x1, y1 + rw);
        path.lineTo(x1, y1);
        path.lineTo(x1 + rw, y1);
        path.moveTo(x1 + rect_width - rw, y1);
        path.lineTo(x1 + rect_width, y1);
        path.lineTo(x1 + rect_width, y1 + rw);
        path.moveTo(x1, y1 + rect_height - rw);
        path.lineTo(x1, y1 + rect_height);
        path.lineTo(x1 + rw, y1 + rect_height);
        path.moveTo(x1 + rect_width - rw, y1 + rect_height);
        path.lineTo(x1 + rect_width, y1 + rect_height);
        path.lineTo(x1 + rect_width, y1 + rect_height - rw);
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(rectSize / 4);
        paint.setPathEffect(new DashPathEffect(new float[]{15, 15, 15, 15}, 1));
        RectF rectf = new RectF(x1, y1, x1 + rect_width, y1 + rect_height);
        canvas.drawRect(rectf, paint);
        if (!Config.instance().isShowFaceInfo) {
            return;
        }

        if (Config.instance().isShowDetailFaceInfo) {
            drawDetailFaceInfo(canvas, faceInfo, recognizedFaceData, x1, y1, rect_width,
                    fontH * 15, fontH * 13, fontH, viewW, viewH);
        } else {
            drawSimpleFaceInfo(canvas, faceInfo, recognizedFaceData, x1, y1, rect_width, fontH * 16, fontH * 2, fontH, viewW, viewH);
        }
    }

    private static final DashPathEffect dashPathEffect = new DashPathEffect(new float[]{0, 0, 0, 0}, 1);
    private static final Path path = new Path();

    private static int getInformationLineCount(Common.RecognizedFaceData recognizedFaceData) {
        int lineCount = 2;
        UserInfo userFace = recognizedFaceData == null ? null : recognizedFaceData.userFace;
        if (userFace != null) {
            lineCount++;
        }
        if (Config.instance().isShowFacePose) {
            lineCount++;
        }
        FaceData faceData = recognizedFaceData == null ? null : recognizedFaceData.faceData;
        if (faceData != null) {
            if (Config.instance().isDisplayLivenessInfo) {
                lineCount++;
                if (faceData.bHasLiveScore) {
                    lineCount++;
                }
            }
        }
        if (Config.instance().isShowExpressionAttribute) {
            lineCount++;
        }
        if (Config.instance().isShowAgeAttribute) {
            lineCount++;
        }
        if (Config.instance().isShowGenderAttribute) {
            lineCount++;
        }
        if (Config.instance().isShowMaskAttribute) {
            lineCount++;
        }
        if (Config.instance().isShowHatAttribute) {
            lineCount++;
        }
        if (Config.instance().isShowMustacheAttribute) {
            lineCount++;
        }
        if (Config.instance().isShowGlassesAttribute) {
            lineCount++;
        }
        return lineCount;
    }

    private static void drawDetailFaceInfo(Canvas canvas, FaceData faceInfo, Common.RecognizedFaceData recognizedFaceData,
                                           float faceRectLeft, float faceRectTop, float faceRectWidth,
                                           float informationCardWidth, float informationCardHeight,
                                           float fontH, float viewW, float viewH) {
        //final draw user information background
        paint.reset();
        paint.setStrokeWidth(rectSize / 4F);
        paint.setPathEffect(dashPathEffect);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E90FF"));
        paint.setAlpha(90);

        int informationLineCount = getInformationLineCount(recognizedFaceData) + 1;

        float roundLeft;
        float roundRight;
        float roundTop;
        float roundBottom;
        float lineBetween = fontH / 2;
        //top
        roundLeft = faceRectLeft;
        roundRight = roundLeft + informationCardWidth;
        roundBottom = faceRectTop - lineBetween;
        roundTop = faceRectTop - lineBetween - informationLineCount * fontH;


        if (roundTop > lineBetween - fontH) {
            canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
        } else {
            //left
            roundLeft = faceRectLeft - lineBetween - informationCardWidth;
            roundRight = roundLeft + informationCardWidth;
            roundTop = faceRectTop;
            roundBottom = roundTop + informationCardHeight;
            if (roundLeft > lineBetween - fontH) {
                canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
            } else {
                //right
                roundLeft = faceRectLeft + faceRectWidth + lineBetween;
                roundRight = roundLeft + informationCardWidth;
                roundTop = faceRectTop;
                roundBottom = faceRectTop + informationCardHeight;
                if (viewW - roundRight > lineBetween - fontH) {
                    canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
                } else {
                    //bottom
                    roundLeft = faceRectLeft;
                    roundRight = roundLeft + informationCardWidth;
                    roundTop = faceRectTop + faceRectWidth + lineBetween;
                    roundBottom = roundTop + informationCardHeight;
                    canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(new RectF(roundLeft, roundTop, roundRight, roundBottom), 20, 20, paint);
                }
            }
        }

        //user info
        paint.reset();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawPath(path, paint);
        paint.reset();
        //user info content
        paint.setTextSize(fontSize);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);
        int trackId = faceInfo.trackData.trackId;
        int toph = 2;

        paint.setTextSize(fontSize * 2);
        String personId = paint.measureText("FaceId:" + trackId) > informationCardWidth ? ("FaceId:" +
                trackId).substring(0, 7) + "..." : "FaceId:" + trackId;
        drawInfoText(canvas, personId, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);

        paint.setTextSize(fontSize);
        UserInfo userFace = recognizedFaceData == null ? null : recognizedFaceData.userFace;
        if (userFace != null) {
            String smatch = String.format(Locale.US, "Name:%s ", userFace.name);
            drawInfoText(canvas, smatch, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
        }

        if (Config.instance().isShowFacePose) {
            String s_blurpose = String.format(Locale.US, "blur:%.3f y:%.1f p:%.1f r:%.1f", faceInfo.trackData.blur, faceInfo.trackData.pose.yaw, faceInfo.trackData.pose.pitch, faceInfo.trackData.pose.roll);
            drawInfoText(canvas, s_blurpose, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
        }

        FaceData faceData = recognizedFaceData == null ? null : recognizedFaceData.faceData;
        if (faceData != null) {
            if (Config.instance().isDisplayLivenessInfo && faceData.bHasLiveScore) {
                String s_attr1 = "";
                if (Config.instance().isDisplayLivenessInfo) {
                    s_attr1 = String.format(Locale.US, "score:%s liveness:%d", faceData.liveness.livenessScore, faceData.liveness.liveness);
                } else {
                    s_attr1 = String.format(Locale.US, "score:%s", faceData.liveness.livenessScore);
                }
                drawInfoText(canvas, s_attr1, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
            }
            if (faceData.bHasAttr) {
                if (Config.instance().isShowExpressionAttribute) {
                    String genderDescription = "Expression: " + Attribute.getExpression(faceData.attribute.expression);
                    drawInfoText(canvas, genderDescription, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
                if (Config.instance().isShowAgeAttribute) {
                    String ageDescription = "Age: " + Attribute.getAssumeRange(faceData.attribute.age, 5);
                    drawInfoText(canvas, ageDescription, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
                if (Config.instance().isShowGenderAttribute) {
                    String description = "Gender: " + Attribute.getGenderDescription(faceData.attribute.gender);
                    drawInfoText(canvas, description, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
                if (Config.instance().isShowMaskAttribute) {
                    String maskDescription = "Mask: " + Attribute.getMaskStatus(faceData.attribute.respirator, faceData.attribute.respiratorLevel);
                    drawInfoText(canvas, maskDescription, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
                if (Config.instance().isShowHatAttribute) {
                    String description = "Hat: " + Attribute.getCapDescription(faceData.attribute.cap);
                    drawInfoText(canvas, description, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
                if (Config.instance().isShowMustacheAttribute) {
                    String description = "Mustache: " + Attribute.getMustacheDescription(faceData.attribute.mustache);
                    drawInfoText(canvas, description, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
                if (Config.instance().isShowGlassesAttribute) {
                    String description = "Glasses: " + Attribute.getGlassesDescription(faceData.attribute.glasses);
                    drawInfoText(canvas, description, Color.WHITE, roundLeft + 10, roundTop + toph++ * fontH, paint);
                }
            }
        }

        if (Config.instance().isDisplayLivenessInfo && faceData != null) {
            String slive = "liveness:";
            int color;
            if (faceData.liveness.livenessMode == Common.FaceLiveMode.DISABLE.getCode()) {
                //disable
                color = Color.YELLOW;
                slive += Common.FaceLiveStatus.getStringByCode(faceData.liveness.liveness);
            } else {
                int live = faceData.liveness.liveness;
                if (live == Common.FaceLiveStatus.PASS.getCode()) {
                    color = Color.GREEN;
                    slive += Common.FaceLiveStatus.getStringByCode(faceData.liveness.liveness);
                } else if (live == Common.FaceLiveStatus.FAIL.getCode()) {
                    color = Color.RED;
                    slive += Common.FaceLiveStatus.getStringByCode(faceData.liveness.liveness);
                } else if (live == 0) {
                    color = Color.WHITE;
                } else {
                    color = Color.YELLOW;
                    slive += Common.FaceLiveStatus.getStringByCode(faceData.liveness.liveness);
                }
            }
            drawInfoText(canvas, slive, color, roundLeft + 10, roundTop + toph++ * fontH, paint);
        }
    }

    private static void drawSmile(Canvas canvas, float top, float left, float right, float bottom, float fontH, boolean isSmile) {
        paint.setStrokeWidth(rectSize / 4);
        paint.setPathEffect(new DashPathEffect(new float[]{0, 0, 0, 0}, 1));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        float sR = 2 * fontH;
        if (isSmile) {
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right - sR, top, sR, paint);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(right - 3 * sR / 2, top - sR / 2, sR / 4, paint);
            canvas.drawCircle(right - sR / 2, top - sR / 2, sR / 4, paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(right - sR, top, sR, paint);
            paint.setColor(Color.BLACK);
            canvas.drawArc(new RectF(right - 5 * sR * 2 / 6, top - 2 * sR / 3, right - sR * 2 / 6, top + sR * 2 / 3), 0, 180, false, paint);
        } else {
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right - sR, top, sR, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(right - 3 * sR / 2, top - sR / 2, sR / 6, paint);
            canvas.drawCircle(right - sR / 2, top - sR / 2, sR / 6, paint);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(right - sR, top, sR, paint);
        }
        paint.reset();
    }

    private static void drawInfoText(Canvas canvas, String content, int color, float x, float y, Paint paint) {
        paint.setColor(color);
        canvas.drawText(content, x, y, paint);
    }

    public static Rect rectMapping(Rect in, int src_w, int src_h, int dest_w, int dest_h) {
        if (src_w == dest_w && src_h == dest_h) {
            return in;
        }
        Rect rect = new Rect();
        Rect in2 = new Rect(in);
        float dest_ratio = dest_w * 1.0f / dest_h;
        float dest_ratio2 = dest_h * 1.0f / dest_w;
        if (dest_ratio > 1.5 || dest_ratio2 > 1.5) {
            rect.left = in.left * dest_w / src_w;
            rect.right = in.right * dest_w / src_w;
            rect.top = in.top * dest_h / src_h;
            rect.bottom = in.bottom * dest_h / src_h;
        } else {
            if (dest_w > dest_h) {
                float nsrc_w = src_h * dest_ratio;
                float woff = (src_w - nsrc_w) / 2;
                in2.left = in.left - (int) woff;
                in2.right = in.right - (int) woff;
                rect.left = in2.left * dest_w / (int) nsrc_w;
                rect.right = in2.right * dest_w / (int) nsrc_w;
                rect.top = in2.top * dest_h / src_h;
                rect.bottom = in2.bottom * dest_h / src_h;
            } else {
                float nsrc_h = src_w * dest_ratio2;
                float hoff = (src_h - nsrc_h) / 2;
                in2.top = in.top - (int) hoff;
                in2.bottom = in.bottom - (int) hoff;
                rect.left = in2.left * dest_w / src_w;
                rect.right = in2.right * dest_w / src_w;
                rect.top = in2.top * dest_h / (int) nsrc_h;
                rect.bottom = in2.bottom * dest_h / (int) nsrc_h;
            }
        }
        return rect;
    }

    public static RectF getCropRect(int cap_w, int cap_h, int view_w, int view_h) {
        float vw_r = cap_h * 1.0f * view_w / view_h;
        float x_offset = (cap_w - vw_r) / 2.0f;
        if (x_offset < 0) {
            x_offset = 0;
            vw_r = cap_w;
        }
        RectF crop_rect = new RectF(x_offset, 0, x_offset + vw_r, cap_h);
        return crop_rect;
    }

    public static Rect rectMapping2Crop(Rect in, int src_w, int src_h, int cap_w, int cap_h, RectF crop_rect) {
        Rect cap_rect = rectMapping(in, src_w, src_h, cap_w, cap_h);
        RectF cap_rectf = new RectF(cap_rect);
        if (crop_rect.contains(cap_rectf)) {
            RectF cap_rectf2 = new RectF(cap_rectf);
            cap_rectf2.left = cap_rectf2.left - crop_rect.left;
            cap_rectf2.right = cap_rectf2.right - crop_rect.left;
            Rect cap_rect2 = new Rect();
            cap_rect2.left = (int) cap_rectf2.left;
            cap_rect2.right = (int) cap_rectf2.right;
            cap_rect2.top = (int) cap_rectf2.top;
            cap_rect2.bottom = (int) cap_rectf2.bottom;
            return cap_rect2;
        }
        return null;
    }

    private static Rect rectMapping4Portrait(Rect in, int src_w, int src_h, int cap_w, int cap_h, int view_w, int view_h) {
        Rect cap_rect = rectMapping(in, src_w, src_h, cap_w, cap_h);
        RectF cap_rectf = new RectF(cap_rect);
        float vw_r = (float) (cap_h * view_w) / view_h;
        float x_offset = (cap_w - vw_r) / 2.0f;
        RectF crop_rect = new RectF(x_offset, 0, x_offset + vw_r, cap_h);
        if (crop_rect.contains(cap_rectf)) {
            RectF cap_rectf2 = new RectF(cap_rectf);
            cap_rectf2.left = cap_rectf2.left - x_offset;
            cap_rectf2.right = cap_rectf2.right - x_offset;
            Rect cap_rect2 = new Rect();
            cap_rect2.left = (int) cap_rectf2.left;
            cap_rect2.right = (int) cap_rectf2.right;
            cap_rect2.top = (int) cap_rectf2.top;
            cap_rect2.bottom = (int) cap_rectf2.bottom;
            Rect drect = rectMapping(cap_rect2, (int) crop_rect.width(), (int) crop_rect.height(), view_w, view_h);
            return drect;
        }
        return null;
    }


    private final static Path textPath = new Path();
    private final static RectF boundsPath = new RectF();
    private final static String UNREGISTERED = "Unregistered";

    private static void drawSimpleFaceInfo(Canvas canvas, FaceData faceInfo, Common.RecognizedFaceData recognizedFaceData, float x1, float y1, float rect_width, float w, float h, float fontH, float viewW, float viewH) {
        paint.reset();
        paint.setStrokeWidth((float) rectSize / 4);
        paint.setPathEffect(new DashPathEffect(new float[]{0, 0, 0, 0}, 1));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E90FF"));
        paint.setAlpha(90);
        Path path = new Path();
        float left;
        float right;
        float top;
        float bottom;
        float distance = fontH / 2;
        UserInfo userFace;
        if (Config.MULTI_BIO_MODULE_INTERNAL_MODE == Config.instance().recognizeMode && faceInfo.bHasIdentifyInfo) {
            IdentifyInfo identifyInfo = faceInfo.identifyInfoList.get(0);
            userFace = new UserInfo();
            userFace.name = identifyInfo.getName();
        } else {
            userFace = recognizedFaceData == null ? null : recognizedFaceData.userFace;
        }
        //top
        left = x1;
        paint.setTextSize(30);
        if (userFace != null) {
            paint.getTextPath(userFace.name, 0, userFace.name.length(), 0F, 0F, textPath);
        } else {
            paint.getTextPath(UNREGISTERED, 0, UNREGISTERED.length(), 0F, 0F, textPath);
        }
        textPath.computeBounds(boundsPath, true);
        float userNameWidth = boundsPath.width();
        right = left + Math.max(w, userNameWidth) + 25;
        top = y1 - h - distance;
        bottom = y1 - distance;
        if (top > distance - fontH) {
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
            //canvas.drawCircle(left + (right - left) / 4, bottom + 5 * distance / 6, distance / 6, paint);
            //canvas.drawCircle(left + (right - left) / 3, bottom + 1 * distance / 2, distance / 4, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
            //canvas.drawCircle(left + (right - left) / 4, bottom + 5 * distance / 6, distance / 6, paint);
            //canvas.drawCircle(left + (right - left) / 3, bottom + 1 * distance / 2, distance / 4, paint);
//            if (true/*faceInfo.isRecog != -1 || faceInfo.isLiveness != -1*/) {
//                drawSmile(canvas, top, left, right, bottom, fontH, true);
//            } else {
//                drawSmile(canvas, top, left, right, bottom, fontH, false);
//            }
        } else {
            //left
            left = x1 - distance - w;
            right = left + w;
            top = y1;
            bottom = top + h;
            if (left > distance - fontH) {
                canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                canvas.drawCircle(right + distance * 5 / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                canvas.drawCircle(right, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                canvas.drawCircle(right + distance * 5 / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                canvas.drawCircle(right, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
            } else {
                //right
                left = x1 + rect_width + distance;
                right = left + w;
                top = y1;
                bottom = y1 + h;
                if (viewW - right > distance - fontH) {
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left - distance / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                    canvas.drawCircle(left - distance / 2, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left - distance / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                    canvas.drawCircle(left - distance / 2, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
                } else {
                    //bottom
                    left = x1;
                    right = left + w;
                    top = y1 + rect_width + distance;
                    bottom = top + h;
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left + (right - left) / 4, top - distance * 5 / 6, distance / 6, paint);
//                    canvas.drawCircle(left + (right - left) / 3, top - distance / 2, distance / 4, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left + (right - left) / 4, top - distance * 5 / 6, distance / 6, paint);
//                    canvas.drawCircle(left + (right - left) / 3, top - distance / 2, distance / 4, paint);
                }
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawPath(path, paint);
        paint.reset();
        paint.setTextSize(30);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);
//        String personId = paint.measureText("FaceId:" + faceInfo.face_id) > w ? ("FaceId:" +
//                faceInfo.face_id).substring(0, 7) + "..." : "FaceId:" + faceInfo.face_id;
        int toph = 1;

        if (userFace != null) {
            drawInfoText(canvas, userFace.name, Color.WHITE, left + 10, top + toph++ * fontH + 10, paint);
        } else {
            drawInfoText(canvas, "Unregistered", Color.WHITE, left + 10, top + toph++ * fontH + 10, paint);
        }

        paint.reset();
    }

    private int count = 0;

    private void drawInfoBackground() {

    }
}
