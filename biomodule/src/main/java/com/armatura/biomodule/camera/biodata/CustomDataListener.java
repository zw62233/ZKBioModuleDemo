package com.armatura.biomodule.camera.biodata;

import android.graphics.Bitmap;

import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.uvclib.model.VideoData;

public interface CustomDataListener {

    void onIdentifyFailed(IdentifyFailedData identifyFailedData);

    void onPalmRecognizeComing(PalmRecognizeData palmRecognizeData);

    void onDrawFaceDataComing(DrawFaceData drawFaceData);

    void onCardInfo(CardInfo cardInfo);

    void onVideoData(VideoData videoData);

    void onBitmap(Bitmap bitmap);

    void onInfraredDistanceChanged(int distance);
}
