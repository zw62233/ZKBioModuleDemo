package com.armatura.biomodule.activity.base;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.armatura.biomodule.R;
import com.armatura.biomodule.dialog.CustomInputDialog;
import com.armatura.biomodule.dialog.ProgressDialogFragment;
import com.armatura.biomodule.manager.OrientationManager;
import com.armatura.biomodule.util.KotlinExtentKt;
import com.armatura.biomodule.view.CustomSettingLayout;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;


public abstract class BaseActivity extends AppCompatActivity {

    public static WeakReference<Context> applicationCtxtWeakRef;
    protected final Runnable hideSystemUIRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };
    //public DemoConfig mConfig;
    private ProgressDialogFragment mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        //initView();
        //initData();
        //initEvent();

        applicationCtxtWeakRef = new WeakReference<>(this.getApplicationContext());
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
//        Log.i(TAG, "showSystemUI: ");
//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    @Override
    protected void onResume() {
        //hideBottomUIMenu();
        super.onResume();
        hideSystemUI();
//        setRequestedOrientation(OrientationManager.INSTANCE.getOrientation());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    //    protected abstract void initData();
//
//    protected abstract void initView();
//
//    protected abstract void initEvent();

    public void toastMsg(String content) {
        KotlinExtentKt.toastAnywhere(content);
    }

    public void toastMsg(@StringRes int stringResId) {
        KotlinExtentKt.toastAnywhere(stringResId);
    }


    public void showProgressDialog(Context context, String message) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss(getSupportFragmentManager());
            mProgressDialog = null;
        }
        mProgressDialog = ProgressDialogFragment.Companion.show(getSupportFragmentManager(),
                "", message, false);
    }


    public void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss(getSupportFragmentManager());
            mProgressDialog = null;
        }
    }

    public void setProgressText(String text) {
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(text);
        }
    }


    protected void showInputNumberDialog(String title, String curValue, CustomInputDialog.OnBtnClickListener listener, int inputType) {
        CustomInputDialog customInputDialog = new CustomInputDialog(this, R.style.dialog_full_screen);
        customInputDialog.setTitle(title);
        customInputDialog.setHintContent(curValue);
        customInputDialog.setInputType(inputType);
        if (listener == null) {
            customInputDialog.setOnBtnClickListener(new CustomInputDialog.OnBtnClickListener() {
                @Override
                public void onPositiveButtonClick(@NotNull CustomInputDialog dialog, @NotNull String content) {
                    dialog.dismiss();
                }

                @Override
                public void onNegativeButtonClick(@NotNull CustomInputDialog dialog, @NotNull String content) {
                    dialog.dismiss();
                }
            });
        } else {
            customInputDialog.setOnBtnClickListener(listener);
        }
        customInputDialog.show();
    }

    protected void showInputStringDialog(String title, String curValue, CustomInputDialog.OnBtnClickListener listener) {
        CustomInputDialog customInputDialog = new CustomInputDialog(this, R.style.dialog_full_screen);
        customInputDialog.setTitle(title);
        customInputDialog.setHintContent(curValue);
        customInputDialog.setInputType(InputType.TYPE_NULL);
        if (listener == null) {
            customInputDialog.setOnBtnClickListener(new CustomInputDialog.OnBtnClickListener() {
                @Override
                public void onPositiveButtonClick(@NotNull CustomInputDialog dialog, @NotNull String content) {
                    dialog.dismiss();
                }

                @Override
                public void onNegativeButtonClick(@NotNull CustomInputDialog dialog, @NotNull String content) {
                    dialog.dismiss();
                }
            });
        } else {
            customInputDialog.setOnBtnClickListener(listener);
        }
        customInputDialog.show();
    }

    protected void showInputNumberDialog(CustomSettingLayout customSettingLayout, CustomInputDialog.OnBtnClickListener listener) {
        showInputNumberDialog(customSettingLayout.getSettingTitle(),
                customSettingLayout.getSettingValue(),
                listener,
                InputType.TYPE_NUMBER_FLAG_DECIMAL);
    }

    protected void showInputStringDialog(CustomSettingLayout customSettingLayout, CustomInputDialog.OnBtnClickListener listener) {
        showInputStringDialog(customSettingLayout.getSettingTitle(),
                customSettingLayout.getSettingValue(),
                listener);
    }


    protected <T extends AndroidViewModel> T getAndroidViewModel(@NonNull Class<T> modelClass) {
        return new ViewModelProvider(this, (ViewModelProvider.Factory) new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                .get(modelClass);
    }


}
