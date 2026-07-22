package com.daimajia.numberprogressbar;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class CustomProgressDialog extends Dialog implements View.OnClickListener {


    private NumberProgressBar numberProgressBar = null;
    private TextView titleView;
    private TextView confirmTextView;
    private TextView cancelTextView;
    private OnBtnClick onBtnClick;

    public CustomProgressDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_progress);
        initWindows();
    }


    public CustomProgressDialog(Context context, int themeResId) {
        super(context, themeResId);
        setContentView(R.layout.dialog_progress);
        initWindows();
    }

    protected CustomProgressDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setContentView(R.layout.dialog_progress);
        initWindows();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initWindows() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    private void initView() {
        numberProgressBar = findViewById(R.id.number_progress_bar);
        numberProgressBar.setMax(100);
        titleView = findViewById(R.id.dialog_progress_title);
        confirmTextView = findViewById(R.id.progress_dialog_confirm);
        confirmTextView.setOnClickListener(this);
        cancelTextView = findViewById(R.id.progress_dialog_cancel);
        cancelTextView.setOnClickListener(this);
    }

    public CustomProgressDialog setDialogTitle(CharSequence charSequence) {
        titleView.setText(charSequence);
        return this;
    }

    public CustomProgressDialog setConfirmBtnText(CharSequence charSequence) {
        confirmTextView.setText(charSequence);
        return this;
    }

    public CustomProgressDialog setCancelBtnText(CharSequence charSequence) {
        cancelTextView.setText(charSequence);
        return this;
    }

    public CustomProgressDialog setOnBtnClick(OnBtnClick onBtnClick) {
        this.onBtnClick = onBtnClick;
        return this;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.progress_dialog_confirm) {
            if (onBtnClick != null) {
                onBtnClick.onConfirmClick(this);
            }
        } else if (view.getId() == R.id.progress_dialog_cancel) {
            if (onBtnClick != null) {
                onBtnClick.onCancelClick(this);
            }
        }
    }

    public void incrementProgressBy(int diff) {
        numberProgressBar.incrementProgressBy(diff);
    }

    public void setProgress(int progress) {
        numberProgressBar.setProgress(progress);
    }


    public interface OnBtnClick {
        void onConfirmClick(CustomProgressDialog dialog);

        void onCancelClick(CustomProgressDialog dialog);
    }

}
