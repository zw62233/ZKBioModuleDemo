package com.armatura.biomodule.fragment;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.camera.ICameraView;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.face.register.DetectFaceResponse;
import com.armatura.biomodule.pojo.face.register.Face;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.pojo.palm.register.DetectPalmResponse;
import com.armatura.biomodule.pojo.setting.CommonSettingData;
import com.armatura.biomodule.register.RegisterStatus;
import com.armatura.biomodule.register.RegisterStatusCallback;
import com.armatura.biomodule.util.HidHelper;
import com.armatura.constant.ConfigType;
import com.armatura.constant.ErrorCode;
import com.armatura.translib.AMTHidManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Magic on 2020/9/18
 */
public abstract class BaseFragment extends Fragment implements RegisterStatusCallback, ICameraView,
        SurfaceHolder.Callback {
    public final static Object CAMERA_SURFACE_HOLDER_LOCK = new Object();
    public final static Object RECT_INFO_SURFACE_HOLDER_LOCK = new Object();

    private ActivityResultLauncher<PickVisualMediaRequest> pickVisualMediaRequestActivityResultLauncher;
    private ActivityResultLauncher<String[]> activityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickVisualMediaRequestActivityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri o) {
                        onImagePicked(o);
                    }
                });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri o) {
                onFilePicked(o);
            }
        });
    }

    @Override
    public void onStatusCallback(RegisterStatus status, Object... objects) {

    }

    @Override
    public void onStatusCallback(RegisterStatus status) {

    }

    @Override
    public void onHidFailed(int ret) {

    }

    @Override
    public void onResult(String detail) {

    }

    @Override
    public void onStatusCallbackEx(RegisterStatus status, float threshold, float score) {

    }

    @Override
    public void onSuccess(byte[] feature) {

    }

    @Override
    public String getUserId() {
        return null;
    }

    @Override
    public int getRegisterOperate() {
        return 0;
    }

    @Override
    public void onRegisterFinish() {

    }


    @Override
    public void onSnapStart() {

    }

    @Override
    public void onSnapFinish(Bitmap bitmap) {

    }

    @Override
    public void toastMessage(String msg) {

    }

    @Override
    public void onFPSUpdate(int[] fps) {

    }

    @Override
    public void drawFaceInfo(DrawFaceData drawFaceData) {

    }

    @Override
    public void drawPalmInfo(PalmRecognizeData palmRecognizeData) {

    }

    @Override
    public void clearCustomInfoView() {

    }

    @Override
    public void clearVideoDataView() {

    }

    @Override
    public void drawVideoData(Bitmap bitmap) {

    }


    /**
     * open image picker
     */
    protected void pickImage() {
        PickVisualMediaRequest pickVisualMediaRequest = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        pickVisualMediaRequestActivityResultLauncher.launch(pickVisualMediaRequest);
    }

    protected abstract void onImagePicked(Uri uri);

    protected abstract void onFilePicked(Uri uri);

    /**
     * open file picker
     */
    protected void pickFile() {
        activityResultLauncher.launch(new String[]{
                "*/*"
        });
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void onInfraredDistance(int distance) {

    }

    @Override
    public void onCardInfo(CardInfo cardInfo) {

    }

    @Override
    public void onProgressUpdate(boolean increase, int value) {

    }

    protected void exitStandByMode() {
        HidHelper.INSTANCE.exitStandByMode();
    }

    protected void enterStandByMode() {
        HidHelper.INSTANCE.enterStandByMode();
    }

    @Override
    public void onIdentifyFailed(IdentifyFailedData identifyFailedData) {

    }
}
