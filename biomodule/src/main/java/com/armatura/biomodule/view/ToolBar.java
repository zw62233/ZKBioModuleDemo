package com.armatura.biomodule.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.armatura.biomodule.R;

public class ToolBar extends RelativeLayout implements View.OnClickListener {


    private ImageView ivSave;
    private ImageView deleteImageView;
    private TextView tvTitle;
    private ToolBarClickListener toolBarClickListener;

    public ToolBar(Context context) {
        super(context);
        initView(context, null);
    }

    public ToolBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ToolBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public ToolBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView(Context context, AttributeSet attr) {
        View view = inflate(context, R.layout.toolbar_with_back, this);
        ImageView ivClose = view.findViewById(R.id.iv_close);
        ivClose.setOnTouchListener(new ViewClickAnim());
        ivClose.setOnClickListener(this);
        ivSave = view.findViewById(R.id.iv_save);
        ivSave.setOnClickListener(this);
        ivSave.setOnTouchListener(new ViewClickAnim());
        tvTitle = view.findViewById(R.id.tv_toolbar_title);
        deleteImageView = view.findViewById(R.id.iv_delete);
        deleteImageView.setOnClickListener(this);
        deleteImageView.setOnTouchListener(new ViewClickAnim());
        TypedArray typedArray = null;
        try {
            typedArray = context.obtainStyledAttributes(attr, R.styleable.ToolBar);

            String toolbarTitle = typedArray.getString(R.styleable.ToolBar_toolbar_title);
            if (!TextUtils.isEmpty(toolbarTitle)) {
                tvTitle.setText(toolbarTitle);
            }

            boolean leftBtnVisible = typedArray.getBoolean(R.styleable.ToolBar_left_button_visible, true);
            ivClose.setVisibility(leftBtnVisible ? VISIBLE : INVISIBLE);

            boolean rightBtnVisible = typedArray.getBoolean(R.styleable.ToolBar_right_button_visible, true);
            ivSave.setVisibility(rightBtnVisible ? VISIBLE : INVISIBLE);
        } finally {
            if (typedArray != null) {
                typedArray.recycle();
            }
        }


    }

    private static class ViewClickAnim implements OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setAlpha(0.5F);
                v.setScaleX(1.2F);
                v.setScaleY(1.2F);
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setAlpha(1.0F);
                v.setScaleX(1.0F);
                v.setScaleY(1.0F);
            }
            return false;
        }
    }

    public void setRightIconAndListener(@DrawableRes int resId, View.OnClickListener listener) {
        ivSave.setImageResource(resId);
        ivSave.setOnClickListener(listener);
    }

    public void hideSaveIcon() {
        if (ivSave != null) {
            ivSave.setVisibility(GONE);
        }
    }

    public void showDeleteAllIcon() {
        if (deleteImageView != null) {
            deleteImageView.setVisibility(VISIBLE);
        }
    }

    public void setTitle(CharSequence title) {
        tvTitle.setText(title);
    }

    public void setToolBarClickListener(ToolBarClickListener toolBarClickListener) {
        this.toolBarClickListener = toolBarClickListener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_close) {
            if (toolBarClickListener != null) {
                toolBarClickListener.onClickLeft();
            }
        } else if (id == R.id.iv_delete || id == R.id.iv_save) {
            if (toolBarClickListener != null) {
                toolBarClickListener.onClickRight();
            }
        }
    }


    public interface ToolBarClickListener {
        void onClickLeft();

        void onClickRight();
    }

}
