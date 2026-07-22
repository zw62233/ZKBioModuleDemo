package com.armatura.biomodule.view

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.armatura.biomodule.R

class CustomSettingLayout : RelativeLayout {

    lateinit var settingTitleView: TextView
    lateinit var settingHintView: TextView
    lateinit var settingValueView: TextView
    lateinit var key: String
    lateinit var ivEditIcon: ImageView
    lateinit var bottomLine: View

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
        val view = View.inflate(context, R.layout.layout_custom_setting, this)
        view.let {
            settingTitleView = findViewById(R.id.settingItemTitle)
            settingHintView = findViewById(R.id.settingItemHint)
            settingValueView = findViewById(R.id.settingItemValue)
            ivEditIcon = findViewById(R.id.ivEditIcon)
            bottomLine = findViewById(R.id.bottom_line)
        }

        context?.obtainStyledAttributes(attrs, R.styleable.CustomSettingLayout).let {
            settingTitleView.text = it?.getString(R.styleable.CustomSettingLayout_setting_title)
            settingHintView.text = it?.getString(R.styleable.CustomSettingLayout_setting_hint)
            settingValueView.text = it?.getString(R.styleable.CustomSettingLayout_setting_value)
            key = it?.getString(R.styleable.CustomSettingLayout_setting_key).toString()

            val isIcEditVisible =
                it?.getBoolean(R.styleable.CustomSettingLayout_ic_edit_visible, true)
            when (isIcEditVisible) {
                true -> ivEditIcon.visibility = View.VISIBLE
                false -> ivEditIcon.visibility = View.INVISIBLE
                else -> ivEditIcon.visibility = View.VISIBLE
            }

            val bottomLineVisible =
                it?.getBoolean(R.styleable.CustomSettingLayout_bottom_line_visible, true)
            when (bottomLineVisible) {
                true -> bottomLine.visibility = View.VISIBLE
                false -> bottomLine.visibility = View.INVISIBLE
                else -> bottomLine.visibility = View.VISIBLE
            }

            it?.recycle()
        }


    }

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            ivEditIcon.setImageResource(R.mipmap.ic_edit_0001)
        } else {
            ivEditIcon.setImageResource(R.mipmap.ic_edit_0001_gray)
        }
        super.setEnabled(enabled)
    }

    fun setSettingTitle(title: String) {
        settingTitleView.text = title
    }

    fun setSettingHint(hint: String) {
        settingHintView.text = hint
    }

    fun setSettingValue(value: String) {
        settingValueView.text = value
    }

    fun getSettingKey(): String = key

    fun getSettingValue(): String = if (TextUtils.isEmpty(settingValueView.text.toString())) {
        "0"
    } else {
        settingValueView.text.toString()
    }

    fun getSettingTitle(): String {
        return settingTitleView.text.toString()
    }

    fun getRange(): IntArray {
        if (!settingHintView.text.isNullOrEmpty()) {
            settingHintView.text.toString()
                .replace("(", "")
                .replace(")", "")
                .split("–").let {
                    return intArrayOf(it[0].toInt(), it[1].toInt())
                }
        } else {
            return intArrayOf(-1, -1)
        }
    }
}