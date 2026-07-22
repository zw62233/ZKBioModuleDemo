package com.armatura.biomodule.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.armatura.biomodule.R
import com.google.android.material.switchmaterial.SwitchMaterial

class CustomSettingSwitchLayout : RelativeLayout {

    private lateinit var settingTitleView: TextView
    private lateinit var settingHintView: TextView
    private lateinit var switchBtn: SwitchMaterial
    private lateinit var bottomLine: View

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
        View.inflate(context, R.layout.layout_custom_setting_switch, this).let {
            settingTitleView = findViewById(R.id.settingItemTitle)
            settingHintView = findViewById(R.id.settingItemHint)
            switchBtn = findViewById(R.id.switchBtn)
            bottomLine = findViewById(R.id.bottom_line)
        }

        context?.obtainStyledAttributes(attrs, R.styleable.CustomSettingLayout).let {
            settingTitleView.text = it?.getString(R.styleable.CustomSettingLayout_setting_title)
            settingHintView.text = it?.getString(R.styleable.CustomSettingLayout_setting_hint)
            switchBtn.isChecked = it?.getBoolean(R.styleable.CustomSettingLayout_isChecked, false)
                ?: false
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

    fun setSettingTitle(title: String) {
        settingTitleView.text = title
    }

    fun setSettingHint(hint: String) {
        settingHintView.text = hint
    }

    fun setCheck(isCheck: Boolean) {
        switchBtn.isChecked = isCheck
    }

    fun setOnCheckChangedListener(listener: CompoundButton.OnCheckedChangeListener?) {
        switchBtn.setOnCheckedChangeListener(listener)
    }

    fun getSettingTitle(): String {
        return settingTitleView.text.toString()
    }

    fun isChecked(): Boolean = switchBtn.isChecked

    fun setSwitchBtnEnable(enable: Boolean) {
        switchBtn.isEnabled = enable
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        switchBtn.isEnabled = enabled
    }
}