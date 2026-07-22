package com.armatura.biomodule.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.armatura.biomodule.R

class CustomSettingSpinnerLayout : RelativeLayout {

    private lateinit var settingTitleView: TextView
    private lateinit var settingHintView: TextView
    private lateinit var spinnerView: Spinner
    private lateinit var bottomLine: View

    constructor(context: Context?) : super(context) {
        initView(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initView(context, attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView(context, attrs)
    }

    private fun initView(context: Context?, attrs: AttributeSet?) {
        View.inflate(context, R.layout.layout_custom_setting_spinner, this).let {
            settingTitleView = findViewById(R.id.settingItemTitle)
            settingHintView = findViewById(R.id.settingItemHint)
            spinnerView = findViewById(R.id.spinnerFaceMatchModel)
            bottomLine = findViewById(R.id.bottom_line)
        }

        attrs?.let {
            context?.obtainStyledAttributes(attrs, R.styleable.CustomSettingLayout).let {
                settingTitleView.text = it?.getString(R.styleable.CustomSettingLayout_setting_title)
                settingHintView.text = it?.getString(R.styleable.CustomSettingLayout_setting_hint)

                val resourceId =
                    it?.getResourceId(R.styleable.CustomSettingLayout_setting_spinner_entries, 0x00)
                if (resourceId != null && resourceId != 0x00) {

                    val objects = context?.resources?.getStringArray(resourceId) as Array<String>
                    spinnerView.adapter = ArrayAdapter(
                        getContext(), R.layout.item_spinner_text_view, objects
                    )
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
    }

    fun setSettingTitle(title: String) {
        settingTitleView.text = title
    }

    fun setSettingHint(hint: String) {
        settingHintView.text = hint
    }

    fun setAdapter(adapter: SpinnerAdapter) {
        spinnerView.adapter = adapter
    }

    fun setOnItemSelectedListener(listener: AdapterView.OnItemSelectedListener) {
        spinnerView.onItemSelectedListener = listener
    }

    fun setSelection(position: Int) {
        spinnerView.setSelection(position)
    }

    fun setSelection(position: Int, animate: Boolean) {
        spinnerView.setSelection(position, animate)
    }

    fun getSelectionItemPosition() = spinnerView.selectedItemPosition

    fun getSelectionValue(): Any = spinnerView.selectedItem

    fun setEnabledSpinnerView(enable: Boolean) {
        spinnerView.isEnabled = enable
    }
}