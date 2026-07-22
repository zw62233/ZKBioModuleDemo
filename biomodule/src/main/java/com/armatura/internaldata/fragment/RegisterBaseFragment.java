package com.armatura.internaldata.fragment;

import android.graphics.Bitmap;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.camera.ICameraView;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.util.KotlinExtentKt;

/**
 * Created by Jeremy on 2022/11/5.
 */
public class RegisterBaseFragment extends Fragment implements SurfaceHolder.Callback, ICameraView {

    protected final static Object SURFACE_HOLDER_LOCK = new Object();
    protected final static Object RECT_INFO_SURFACE_HOLDER_LOCK = new Object();

    protected <T extends AndroidViewModel> T getAndroidViewModel(@NonNull Class<T> modelClass) {
        return new ViewModelProvider(requireActivity())
                .get(modelClass);
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
    public void drawVideoData(Bitmap bitmap) {

    }

    @Override
    public void drawFaceInfo(DrawFaceData drawFaceData) {

    }

    @Override
    public void drawPalmInfo(PalmRecognizeData palmRecognizeData) {

    }

    @Override
    public void onIdentifyFailed(IdentifyFailedData identifyFailedData) {

    }

    @Override
    public void clearCustomInfoView() {

    }

    @Override
    public void clearVideoDataView() {

    }

    public void showToastMsg(String msg) {
        KotlinExtentKt.toastAnywhere(msg);
    }

    @Override
    public void onFPSUpdate(int[] fps) {

    }

    @Override
    public void onInfraredDistance(int distance) {

    }

    @Override
    public void onCardInfo(CardInfo cardInfo) {

    }
}
