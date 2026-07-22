package com.armatura.biomodule.camera;

import android.content.Context;
import android.graphics.Bitmap;

import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;

public interface ICameraView {

    void drawVideoData(Bitmap bitmap);

    void drawFaceInfo(DrawFaceData drawFaceData);

    void drawPalmInfo(PalmRecognizeData palmRecognizeData);


    void onIdentifyFailed(IdentifyFailedData identifyFailedData);

    void clearCustomInfoView();

    void clearVideoDataView();


    void onFPSUpdate(int[] fps);

    void onInfraredDistance(int distance);

    void onCardInfo(CardInfo cardInfo);
}
