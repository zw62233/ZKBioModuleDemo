package com.armatura.biomodule.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.armatura.biomodule.R
import com.google.android.material.slider.RangeSlider

class CustomRangeSeekBarLayout : RelativeLayout {

    private lateinit var settingTitleView: TextView
    lateinit var rangeSlider: RangeSlider
    private lateinit var seekBarValueTextView: TextView
    private lateinit var bottomLine: View

    private var negative: Boolean = false
    private var rewrite: Boolean = false

    constructor(context: Context?) : super(context) {
        initView(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView(context, attrs)
    }

    private fun initView(context: Context?, attrs: AttributeSet?) {
        View.inflate(context, R.layout.layout_custom_range_seek_bar, this).let {
            settingTitleView = findViewById(R.id.settingItemTitle)
            seekBarValueTextView = findViewById(R.id.seekBarValueTV)
            rangeSlider = findViewById(R.id.seek_bar)
            bottomLine = findViewById(R.id.bottom_line)
//            seekBar.progressDrawable.setColorFilter(Color.parseColor("#7ac143"), PorterDuff.Mode.SRC_ATOP);//设置进度条颜色、样式
        }

        context?.obtainStyledAttributes(attrs, R.styleable.CustomSettingLayout).let {
            settingTitleView.text = it?.getString(R.styleable.CustomSettingLayout_setting_title)
            negative = it?.getBoolean(R.styleable.CustomSettingLayout_negative, false)!!

            when (it.getBoolean(R.styleable.CustomSettingLayout_bottom_line_visible, true)) {
                false -> bottomLine.visibility = View.INVISIBLE
                else -> bottomLine.visibility = View.VISIBLE
            }
            rangeSlider.valueTo = it.getInt(R.styleable.CustomSettingLayout_max, 100).toFloat()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                rangeSlider.valueFrom = it.getInt(R.styleable.CustomSettingLayout_min, 0).toFloat()
            }

            it.recycle()
        }
        rangeSlider.addOnChangeListener { slider, _, _ ->
            seekBarValueTextView.text =
                "${slider.values[0].toInt()}\n-\n${slider.values[1].toInt()}"
        }
    }

    fun getSettingTitle(): String {
        return settingTitleView.text.toString()
    }

    fun setMax(max: Int) {
        rangeSlider.valueTo = max.toFloat()
    }


    fun getFromValue(): Float = rangeSlider.values[0]

    fun getToValue(): Float = rangeSlider.values[1]

}