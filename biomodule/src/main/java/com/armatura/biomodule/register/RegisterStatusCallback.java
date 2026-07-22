package com.armatura.biomodule.register;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.armatura.biomodule.pojo.face.register.DetectFaceResponse;
import com.armatura.biomodule.pojo.face.register.Face;
import com.armatura.biomodule.pojo.palm.register.DetectPalmResponse;

/**
 * Created by Magic on 2020/9/17
 */
public interface RegisterStatusCallback {
    void onStatusCallback(RegisterStatus status);

    void onStatusCallbackEx(RegisterStatus status, float threshold, float score);

    void onStatusCallback(RegisterStatus status, Object... objects);

    void onHidFailed(int ret);

    void onResult(String detail);

    void onSuccess(@Nullable byte[] feature);

    void toastMessage(String msg);

    String getUserId();

    int getRegisterOperate();

    void onRegisterFinish();

    void onSnapStart();

    void onSnapFinish(Bitmap bitmap);

    void onProgressUpdate(boolean increase, int value);
}
