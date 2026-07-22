package com.armatura.biomodule.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/***
 *@author Create By Magic
 *@date 2019/7/11
 **/
public class FixRectMask extends View {
    private static final String TAG = "FixRectMask";
    private Paint mPaint;


    private final Rect fixRect = new Rect();

    public FixRectMask(Context context) {
        super(context);
        initPaint(context);
    }

    public FixRectMask(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint(context);
    }

    public FixRectMask(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint(context);
    }


    private void initPaint(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(4);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            int width = right - left;
            int height = bottom - top;
            int fixRectLeft = width / 6;
            int fixRectRight = fixRectLeft * 5;
            int fixRectTop = height / 5;
            int fixRectBottom = fixRectTop * 4;
            fixRect.set(fixRectLeft, fixRectTop, fixRectRight, fixRectBottom);
            invalidate();
        }
    }

    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawRect(fixRect, mPaint);
    }


}
