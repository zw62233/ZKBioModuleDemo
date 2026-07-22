package com.armatura.biomodule.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.ExApplication
import kotlin.math.abs
import kotlin.math.sin

const val XXXHDPI = 480
const val XXHDPI = 360
const val XHDPI = 240
const val HDPI = 160
const val MDPI = 120

fun Context.pxToDp(px: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_PX,
        px,
        resources.displayMetrics
    )
}

fun Context.adjustPxForDpi(originalPx: Float, targetDpi: Int): Float {
    val currentDpi = resources.displayMetrics.densityDpi
    val scale = targetDpi * 1.0F / currentDpi
    return originalPx / scale
}

/**
 * Created by Magic on 2022/12/7
 * Description:
 */
class CircleDetectView : androidx.appcompat.widget.AppCompatImageView {

    companion object {
        private const val TAG = "CircleDetectView"
        val COLOR_ROUND = "#FFA800".toColorInt()
        val COLOR_WRAP_ROUND = "#C8CCCCCC".toColorInt()
        val COLOR_ERROR_RED = "#FF0000".toColorInt()
        val COLOR_WARN_ORANGE = "#FF8000".toColorInt()
        val COLOR_WHITE = "#FFFFFF".toColorInt()
        val COLOR_LIGHT_GREEN = "#FF4AE8AB".toColorInt()

        @JvmField
        val OUT_RING_1_WIDTH = with(ExApplication.instance()) {
            pxToDp(adjustPxForDpi(8F, XXHDPI))
        }

        @JvmField
        val OUT_RING_2_WIDTH = with(ExApplication.instance()) {
            pxToDp(adjustPxForDpi(12F, XXHDPI))
        }

        @JvmField
        val INDICATOR_RING_WIDTH = with(ExApplication.instance()) {
            pxToDp(adjustPxForDpi(20F, XXHDPI))
        }

        @JvmField
        val SPACE_BETWEEN_RING = with(ExApplication.instance()) {
            pxToDp(adjustPxForDpi(7F, XXHDPI))
        }

        const val FPS = 25L

        const val PROGRESS_LINE_LENGTH = 20
        const val SUCCESS_PROGRESS_LINE_LENGTH = PROGRESS_LINE_LENGTH
        const val PROGRESS_SPLIT_LINE_LENGTH = 30
    }

    enum class IndicatorState {
        NORMAL,
        GREEN,
        RED,
        ORANGE,
        PROGRESS,
        SMOOTH_PROGRESS,
    }

    private var mCurrentIndicatorState = IndicatorState.NORMAL

    private val colorBackground = context.resources.getColor(R.color.amt_bg_color)
    private var mCircleCenterX = 200F
    private var mCircleCenterY = 200F
    private var mCircleCenterRadius = 100F
    private var mWrapCircle1CenterRadius = 100F
    private var mWrapCircle2CenterRadius = 100F
    private var mPalmTargetCircleAreaRadius = 100F
    private val mCirclePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROUND
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            isAntiAlias = true
            isDither = true//
        }
    }

    private val mDashCirclePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LIGHT_GREEN
            style = Paint.Style.STROKE
            isAntiAlias = true
            isDither = true
            setPathEffect(mDashCirclePathEffect)
        }
    }
    private val mDashCirclePathEffect by lazy {
        DashPathEffect(floatArrayOf(10f, 20f), 0f)
    }
    private var mDashCircleRotate = 0f
    private val mDashCircleRotateSpeed = 2f

    private var mWrapCircle1Rotate = 1F
    private var mWrapCircle1RotateSpeed = 1
    private var mWrapCircle2Rotate = 1F
    private var mWrapCircle2RotateSpeed = 1

    private var mCurrentProgress = 0
    private var mTotalProgress = 0

    private var isLayoutChanged = false

    private var isIndicatorChanged = false

    private val ovalRectF by lazy { RectF() }

    private val gradientPositions by lazy { floatArrayOf(0.9F, 1F) }

    private var mGradient: Shader? = null

    private val mWrapCirclePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WRAP_ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
            isDither = true//
        }
    }

    private val mBackgroundPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorBackground
            style = Paint.Style.FILL
            isAntiAlias = true
            isDither = true//
        }
    }

    private val mCircleRingPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = INDICATOR_RING_WIDTH
            isAntiAlias = true
            isDither = true//
        }
    }

    private val mProgressPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = COLOR_WHITE
            strokeWidth = 2F
            isAntiAlias = true
            isDither = true//
        }
    }

    private val mSuccessProgressPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = COLOR_LIGHT_GREEN
            strokeWidth = 4F
            isAntiAlias = true
            isDither = true//
        }
    }

    constructor(context: Context) : super(context) {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }


    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = right - left
        val height = bottom - top
        mCircleCenterX = width * 1.0F / 2

        //divide by 6,let circle up a little
        val offsetY = height * 1.0F / 7
        mCircleCenterY = (height * 1.0F / 2)

        val targetPalmAreaLeft = width / 6
        val targetPalmAreaRight = targetPalmAreaLeft * 5
        mPalmTargetCircleAreaRadius = (targetPalmAreaRight - targetPalmAreaLeft) * 1.0F / 3

        //reduce 30 ,let circle smaller
        mCircleCenterRadius =
            width * 1.0F / 2 - (OUT_RING_1_WIDTH + OUT_RING_2_WIDTH + SPACE_BETWEEN_RING * 2) - 35


        mWrapCircle1CenterRadius = mCircleCenterRadius + SPACE_BETWEEN_RING
        mWrapCircle2CenterRadius = mWrapCircle1CenterRadius + SPACE_BETWEEN_RING

        ovalRectF.set(
            mCircleCenterX - mCircleCenterRadius + (INDICATOR_RING_WIDTH / 2),
            mCircleCenterY - mCircleCenterRadius + (INDICATOR_RING_WIDTH / 2),
            mCircleCenterX + mCircleCenterRadius - (INDICATOR_RING_WIDTH / 2),
            mCircleCenterY + mCircleCenterRadius - (INDICATOR_RING_WIDTH / 2)
        )
        gradientPositions[0] =
            (mCircleCenterRadius - INDICATOR_RING_WIDTH) / mCircleCenterRadius
        gradientPositions[1] = (INDICATOR_RING_WIDTH / 2) / mCircleCenterRadius


        //moving circle anim
        mHintRadius = mPalmTargetCircleAreaRadius / 2
        mHintStartX = (right - left) - mHintRadius
        mHintStartY = (bottom - top) - mHintRadius
        mHintEndX = mCircleCenterX
        mHintEndY = mCircleCenterY

        isLayoutChanged = true
    }


    fun changeIndicatorState(indicatorState: IndicatorState) {
        mCurrentIndicatorState = indicatorState
        isIndicatorChanged = true
    }

    fun updateProgress(progress: Int, totalProgress: Int) {
        mCurrentProgress = progress
        mTotalProgress = totalProgress
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.let {
            canvas.drawPaint(mBackgroundPaint)

            canvas.drawCircle(mCircleCenterX, mCircleCenterY, mCircleCenterRadius, mCirclePaint)

            drawCircleRing(canvas)
            drawPathCircleAnim1(canvas)
            drawPathCircleAnim2(canvas)

            if (mCurrentIndicatorState == IndicatorState.NORMAL
            ) {
                drawMovingHintCircle(canvas)
            }

            if (mCurrentIndicatorState == IndicatorState.PROGRESS) {
                drawCircleLine(canvas)
                drawSuccessCircleLine(canvas)
            }

            if (mCurrentIndicatorState == IndicatorState.SMOOTH_PROGRESS) {
                drawCircleSmoothLine(canvas)
                drawSuccessCircleLine(canvas)
            }

            drawTargetCircle(canvas)
        }
        postInvalidateDelayed(1000 / FPS)
    }

    private fun drawTargetCircle(canvas: Canvas) {
        canvas.withSave {
            mDashCircleRotate += mDashCircleRotateSpeed
            if (mDashCircleRotate >= 360f) {
                mDashCircleRotate = 0f
            }
            rotate(mDashCircleRotate, mCircleCenterX, mCircleCenterY)
            drawCircle(
                mCircleCenterX,
                mCircleCenterY,
                mPalmTargetCircleAreaRadius,
                mDashCirclePaint
            )
        }
    }

    private fun drawCircleRing(canvas: Canvas) {
        canvas.save()
        if (mGradient == null || isLayoutChanged || isIndicatorChanged) {
            mGradient = when (mCurrentIndicatorState) {
                IndicatorState.NORMAL -> {
                    mWrapCircle1RotateSpeed = 1
                    mWrapCircle2RotateSpeed = 2
                    RadialGradient(
                        mCircleCenterX, mCircleCenterY, mCircleCenterRadius,
                        intArrayOf(
                            Color.TRANSPARENT,
                            COLOR_WHITE
                        ),
                        floatArrayOf(gradientPositions[0], 1F), Shader.TileMode.REPEAT
                    )
                }

                IndicatorState.RED -> {
                    mWrapCircle1RotateSpeed = 0
                    mWrapCircle2RotateSpeed = 0
                    RadialGradient(
                        mCircleCenterX, mCircleCenterY, mCircleCenterRadius,
                        intArrayOf(
                            Color.TRANSPARENT,
                            COLOR_ERROR_RED
                        ),
                        floatArrayOf(gradientPositions[0], 1F), Shader.TileMode.REPEAT
                    )
                }

                IndicatorState.ORANGE -> {
                    mWrapCircle1RotateSpeed = 1
                    mWrapCircle2RotateSpeed = 2
                    RadialGradient(
                        mCircleCenterX, mCircleCenterY, mCircleCenterRadius,
                        intArrayOf(
                            Color.TRANSPARENT,
                            COLOR_WARN_ORANGE
                        ),
                        floatArrayOf(gradientPositions[0], 1F), Shader.TileMode.REPEAT
                    )
                }

                IndicatorState.GREEN -> {
                    mWrapCircle1RotateSpeed = 3
                    mWrapCircle2RotateSpeed = 6
                    RadialGradient(
                        mCircleCenterX, mCircleCenterY, mCircleCenterRadius,
                        intArrayOf(
                            Color.TRANSPARENT,
                            COLOR_LIGHT_GREEN
                        ),
                        floatArrayOf(gradientPositions[0], 1F), Shader.TileMode.REPEAT
                    )
                }

                IndicatorState.SMOOTH_PROGRESS, IndicatorState.PROGRESS -> {
                    RadialGradient(
                        mCircleCenterX, mCircleCenterY, mCircleCenterRadius,
                        intArrayOf(
                            Color.TRANSPARENT,
                            COLOR_WHITE
                        ),
                        floatArrayOf(gradientPositions[0], 1F), Shader.TileMode.REPEAT
                    )
                }
            }
            isIndicatorChanged = false
            isLayoutChanged = false
            Log.i(
                TAG,
                "drawCircleRing: speed change ,speed1=$mWrapCircle1RotateSpeed,speed2=$mWrapCircle2RotateSpeed,${mCurrentIndicatorState.name}"
            )
        }

        mCircleRingPaint.shader = mGradient
        canvas.drawArc(
            ovalRectF, 0F, 360F, true, mCircleRingPaint
        )
        canvas.restore()
    }

    fun progress() = mCurrentProgress


    private fun drawPathCircleAnim1(canvas: Canvas) {
        canvas.save()
        val perimeter: Float = 2 * 3.14f * mWrapCircle1CenterRadius
        val dashPathEffect = DashPathEffect(
            floatArrayOf(perimeter / 4, perimeter / 4), /*perimeter / 6 * 3*/0F
        )
        mWrapCirclePaint.pathEffect = dashPathEffect
        mWrapCircle1Rotate += mWrapCircle1RotateSpeed
        mWrapCircle1Rotate = if (mWrapCircle1Rotate >= 360) {
            0F
        } else {
            mWrapCircle1Rotate
        }
        canvas.rotate(
            mWrapCircle1Rotate, mCircleCenterX, mCircleCenterY
        )
        mWrapCirclePaint.strokeWidth = OUT_RING_1_WIDTH
        mWrapCirclePaint.alpha = 0xC8
        canvas.drawCircle(
            mCircleCenterX, mCircleCenterY, mWrapCircle1CenterRadius, mWrapCirclePaint
        )
        canvas.restore()
    }


    private fun drawPathCircleAnim2(canvas: Canvas) {
        canvas.save()
        val perimeter: Float = 2 * 3.14f * mWrapCircle2CenterRadius
        val dashPathEffect = DashPathEffect(
            floatArrayOf(perimeter / 6, perimeter / 6), 0F
        )
        mWrapCirclePaint.pathEffect = dashPathEffect
        mWrapCircle2Rotate -= mWrapCircle2RotateSpeed
        mWrapCircle2Rotate = if (mWrapCircle2Rotate <= -360) {
            0F
        } else {
            mWrapCircle2Rotate
        }
        canvas.rotate(
            mWrapCircle2Rotate, mCircleCenterX, mCircleCenterY
        )
        mWrapCirclePaint.strokeWidth = OUT_RING_2_WIDTH
        mWrapCirclePaint.alpha = 0xFF
        canvas.drawCircle(
            mCircleCenterX, mCircleCenterY, mWrapCircle2CenterRadius, mWrapCirclePaint
        )
        canvas.restore()
    }

    private fun drawCircleLine(canvas: Canvas) {
        canvas.save()
        canvas.translate(mCircleCenterX, mCircleCenterY)
        for (j in 0 until 360 step 1) {
            if (j % 60 == 0) {
                canvas.drawLine(
                    0F,
                    -mCircleCenterRadius,
                    0F,
                    -mCircleCenterRadius + PROGRESS_SPLIT_LINE_LENGTH,
                    mProgressPaint
                )
            } else {
                canvas.drawLine(
                    0F,
                    -mCircleCenterRadius,
                    0F,
                    -mCircleCenterRadius + PROGRESS_LINE_LENGTH,
                    mProgressPaint
                )
            }
            canvas.rotate(1F)
        }
        canvas.restore()
    }

    private fun drawCircleSmoothLine(canvas: Canvas) {
        canvas.save()
        canvas.translate(mCircleCenterX, mCircleCenterY)
        for (j in 0 until 360 step 1) {
            canvas.drawLine(
                0F,
                -mCircleCenterRadius,
                0F,
                -mCircleCenterRadius + PROGRESS_LINE_LENGTH,
                mProgressPaint
            )
            canvas.rotate(1F)
        }
        canvas.restore()
    }


    private fun drawSuccessCircleLine(canvas: Canvas) {
        val degree = (mCurrentProgress * 1.0F / mTotalProgress * 360.0f).toInt()
        canvas.save()
        canvas.translate(mCircleCenterX, mCircleCenterY)
        for (j in 0 until degree step 1) {
            canvas.drawLine(
                0F,
                -mCircleCenterRadius,
                0F,
                -mCircleCenterRadius + SUCCESS_PROGRESS_LINE_LENGTH,
                mSuccessProgressPaint
            )
            canvas.rotate(1F)
        }
        canvas.restore()
    }


    private var mHintRadius = 0F
    private var mHintStartX = 0F
    private var mHintStartY = 0F
    private var mHintEndX = 0F
    private var mHintEndY = 0F
    private var mHintT = 0F                      // 0→1 的插值进度
    private var mHintSpeedPerFrame = 0.02F       // 约 ~1.5s 完成一次
    private val mHintCirclePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            style = Paint.Style.FILL    // 实心
            alpha = 128                 // 半透明
            isDither = true
        }
    }

    private fun drawMovingHintCircle(canvas: Canvas) {
        val startX = (mHintStartX + mHintEndX)
        val startY = (mHintStartY + mHintEndY)

        val x = startX + (mHintEndX - startX) * mHintT
        val y = startY + (mHintEndY - startY) * mHintT

        val minRadius = mPalmTargetCircleAreaRadius
        val maxRadius = mPalmTargetCircleAreaRadius
        val currentRadius = minRadius + (maxRadius - minRadius) * mHintT

        canvas.drawCircle(x, y, currentRadius, mHintCirclePaint)

        mHintT += mHintSpeedPerFrame
        if (mHintT >= 1F) {
            mHintT = 0F
        }
    }
}