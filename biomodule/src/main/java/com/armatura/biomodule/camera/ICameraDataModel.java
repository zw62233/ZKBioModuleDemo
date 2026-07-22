package com.armatura.biomodule.camera;

import android.graphics.Bitmap;

import com.armatura.biomodule.camera.biodata.CustomDataListener;
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache;

public interface ICameraDataModel {

    void addBitmap(Bitmap bitmap);

    void setCustomDataListener(CustomDataListener customDataListener);

    RecognizedBioDataCache getRecognizedBioDataCache();

    int[] getImgFPS();

    void clearData();

    void onDestroy();
}
