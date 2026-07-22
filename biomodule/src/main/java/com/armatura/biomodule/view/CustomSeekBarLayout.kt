package com.armatura.biomodule.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.armatura.biomodule.R

class CustomSeekBarLayout : RelativeLayout {

    lateinit var settingTitleView: TextView
    lateinit var seekBar: SeekBar
    lateinit var seekBarValueTextView: TextView
    lateinit var bottomLine: View

    var negative: Boolean = false
    var rewrite: Boolean = false

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
        View.inflate(context, R.layout.layout_custom_seek_bar, this).let {
            settingTitleView = findViewById(R.id.settingItemTitle)
            seekBarValueTextView = findViewById(R.id.seekBarValueTV)
            seekBar = findViewById(R.id.seek_bar)
            bottomLine = findViewById(R.id.bottom_line)
//            seekBar.progressDrawable.setColorFilter(Color.parseColor("#7ac143"), PorterDuff.Mode.SRC_ATOP);//设置进度条颜色、样式
        }

        context?.obtainStyledAttributes(attrs, R.styleable.CustomSettingLayout).let {
            settingTitleView.text = it?.getString(R.styleable.CustomSettingLayout_setting_title)
            negative = it?.getBoolean(R.styleable.CustomSettingLayout_negative, false)!!
            rewrite = it.getBoolean(R.styleable.CustomSettingLayout_rewrite, false)

            when (it.getBoolean(R.styleable.CustomSettingLayout_bottom_line_visible, true)) {
                false -> bottomLine.visibility = View.INVISIBLE
                else -> bottomLine.visibility = View.VISIBLE
            }
            seekBar.max = it.getInt(R.styleable.CustomSettingLayout_max, 100)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                seekBar.min = it.getInt(R.styleable.CustomSettingLayout_min, 0)
            }

            it.recycle()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (rewrite) {
                    seekBarValueTextView.text = (progress + 2).toString()
                    return
                }
                if (negative) {
                    val negativeNum = 180 - progress
                    if (progress == 180) {
                        seekBarValueTextView.text = negativeNum.toString()
                    } else {
                        seekBarValueTextView.text = "-$negativeNum"
                    }
                } else {
                    seekBarValueTextView.text = progress.toString()
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    fun setDefaultProgress(progress: Int) {
        seekBarValueTextView.text = if (negative) {
            "${seekBar.max - progress}"
        } else {
            "$progress"
        }
        seekBar.progress = progress
    }

    fun getSettingTitle(): String {
        return settingTitleView.text.toString()
    }

    fun setSettingTitle(title: String) {
        settingTitleView.text = title
    }

    fun setMax(max: Int) {
        seekBar.max = max
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setMin(min: Int) {
        seekBar.min = min
    }


    fun getSeekBarValue(): Int = if (negative) {
        (seekBar.max - seekBar.progress) * -1
    } else {
        seekBar.progress
    }

    fun isNegativeNumber(): Boolean = negative

}