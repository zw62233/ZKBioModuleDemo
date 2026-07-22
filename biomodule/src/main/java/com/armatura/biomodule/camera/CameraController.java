package com.armatura.biomodule.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.camera.base.BaseThread;
import com.armatura.biomodule.camera.biodata.CustomDataListener;
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache;
import com.armatura.biomodule.common.WatchDogType;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.handler.BaseDrawHandler;
import com.armatura.biomodule.manager.CameraManager;
import com.armatura.biomodule.manager.CameraWatchDogManager;
import com.armatura.biomodule.manager.RescueManager;
import com.armatura.biomodule.manager.WatchDog;
import com.armatura.biomodule.pojo.common.BioType;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.pojo.setting.CommonSettingData;
import com.armatura.biomodule.util.KotlinExtentKt;
import com.armatura.biomodule.viewmodel.AMTViewModel;
import com.armatura.constant.CameraErrorCode;
import com.armatura.uvccameralibrary.pro.ProCameraManager;
import com.armatura.uvclib.CameraDataCallback;
import com.armatura.uvclib.model.VideoData;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CameraController implements CustomDataListener {
    private final static String TAG = CameraController.class.getSimpleName();
    private final ICameraDataModel mICameraModel;
    private boolean isOpen;
    private DecodeThread decodeThread;
    private VideoDataViewThread videoDataViewThread;
    private final BaseDrawHandler palmInfoDrawHandler;
    private final BaseDrawHandler faceInfoDrawHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final static int MSG_DRAW_PALM_INFO = 0x1001;
    private final static int MSG_CLEAR_PALM_INFO = 0x1002;
    private final static int MSG_ON_CARD_INFO = 0x1003;
    private final static int MSG_ON_DISTANCE_CHANGED_INFO = 0x1004;
    private final static int MSG_ON_PALM_IDENTIFY_FAILED = 0x1005;
    private final static int MSG_DRAW_FACE_INFO = 0x2001;
    private final static int MSG_CLEAR_FACE_INFO = 0x2002;
    private final static int MSG_ON_FACE_IDENTIFY_FAILED = 0x2003;
    private static volatile CameraController cameraController = null;

    public static CameraController instance() {
        if (cameraController == null) {
            synchronized (CameraController.class) {
                if (cameraController == null) {
                    cameraController = new CameraController();
                }
            }
        }
        return cameraController;
    }

    public static int getCameraID() {
        return CameraManager.getInstance().getCameraID();
    }

    private CameraController() {
        this.mICameraModel = new CameraDataModel();
        HandlerThread palmInfoViewThread = new HandlerThread("palmInfoViewThread");
        palmInfoViewThread.start();
        palmInfoDrawHandler = new PalmInfoViewHandler(palmInfoViewThread.getLooper());
        HandlerThread faceInfoViewThread = new HandlerThread("faceInfoViewThread");
        faceInfoViewThread.start();
        faceInfoDrawHandler = new FaceInfoViewHandler(faceInfoViewThread.getLooper());
        mICameraModel.setCustomDataListener(this);
    }

    public CameraDataModel getCameraModel() {
        return (CameraDataModel) mICameraModel;
    }

    public void switchCamera() {
        CameraManager.getInstance().switchCamera((CameraDataModel) mICameraModel);
    }


    public void onDestroy() {
        closeCam();
        mICameraModel.onDestroy();
        CameraManager.getInstance().closeCamera();
    }

    public boolean isOpen() {
        return isOpen;
    }

    private void toastMsg(final Context context, String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                KotlinExtentKt.toastAnywhere(msg);
            }
        });
    }

    synchronized void openCam(Context context, ICameraView cameraView, UsbDevice specificDevice) {
        if (isOpen) {
            closeCam();
        }

        if (specificDevice == null) {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            for (UsbDevice usbDevice : deviceList.values()) {
                if (AMTViewModel.isAMTDevice(usbDevice)) {
                    specificDevice = usbDevice;
                    break;
                }
            }
            if (specificDevice == null) {
                toastMsg(context, "No device found!");
                return;
            }
        }

        int ret;
        if ((ret = CameraManager.getInstance().openCamera(context)) == CameraErrorCode.SUCCESS) {
            Log.i(TAG, "openCam: success");
            isOpen = true;

            if (Config.videoStreamMode != CommonSettingData.VIDEO_STREAM_MODE_DISABLE
                    && Config.videoStreamMode != CommonSettingData.VIDEO_STREAM_MODE_IR) {
                CameraWatchDogManager.getInstance().initCameraWatchDog(WatchDogType.UVC, new WatchDog.OnWatchDogTimeoutListener() {
                    @Override
                    public void onWatchDogTimeout() {
                        Log.i(TAG, "onWatchDogTimeout: start rescue");
                        RescueManager.instance().doRescue(ExApplication.instance(),
                                "Camera Watch Dog Trigger");
                    }
                }, false);
                CameraWatchDogManager.getInstance().startFeedWatchDog(
                        WatchDogType.UVC,
                        20, 3000, 300, TimeUnit.MILLISECONDS
                );
                Log.i(TAG, "openCam: camera watch dog started");
            }
            resumeCam(cameraView);
        } else {
            toastMsg(context, "Open Camera Failed! Error," + ret);
            RescueManager.instance().doRescue(ExApplication.instance(),
                    "Camera Open Failed," + ret);
        }
    }


    synchronized public void closeCam() {
        CameraWatchDogManager.getInstance().stop();
        if (!isOpen) {
            return;
        }
        CameraManager.getInstance().closeCamera();
        if (decodeThread != null) {
            decodeThread.Stop();
        }
        if (videoDataViewThread != null) {
            videoDataViewThread.Stop();
            videoDataViewThread = null;
        }
        mICameraModel.clearData();
        isOpen = false;
    }

    public synchronized void pauseCam(ICameraView iCameraView) {
        if (!isOpen) {
            Log.w(TAG, "pauseCam: skip...camera not open yet");
            return;
        }
        if (decodeThread != null) {
            decodeThread.removeICameraView(iCameraView);
            decodeThread.Stop();
            decodeThread.interrupt();
            decodeThread = null;
        }
        if (videoDataViewThread != null) {
            videoDataViewThread.removeICameraView(iCameraView);
            videoDataViewThread.Stop();
            videoDataViewThread.interrupt();
            videoDataViewThread = null;
        }
        if (faceInfoDrawHandler != null) {
            faceInfoDrawHandler.sendEmptyMessage(MSG_CLEAR_FACE_INFO);
            faceInfoDrawHandler.obtainMessage(BaseDrawHandler.MSG_REMOVE_CAMERA_VIEW, iCameraView).sendToTarget();
        }
        if (palmInfoDrawHandler != null) {
            palmInfoDrawHandler.sendEmptyMessage(MSG_CLEAR_PALM_INFO);
            palmInfoDrawHandler.obtainMessage(BaseDrawHandler.MSG_REMOVE_CAMERA_VIEW, iCameraView).sendToTarget();
        }
        CameraManager.getInstance().stopPreview();
        CameraWatchDogManager.getInstance().pause();
        Log.i(TAG, "pauseCam: ");
    }

    public synchronized void resumeCam(ICameraView iCameraView) {
        if (!isOpen) {
            Log.w(TAG, "resumeCam: skip...camera not open yet");
            return;
        }
        if (decodeThread != null) {
            decodeThread.Stop();
            decodeThread.interrupt();
            decodeThread = null;
        }
        decodeThread = new DecodeThread(mICameraModel);
        decodeThread.Start();
        decodeThread.addICameraView(iCameraView);

        if (videoDataViewThread != null) {
            videoDataViewThread.Stop();
            videoDataViewThread.interrupt();
            videoDataViewThread = null;
        }
        videoDataViewThread = new VideoDataViewThread(mICameraModel);
        videoDataViewThread.Start();
        videoDataViewThread.addICameraView(iCameraView);

        if (faceInfoDrawHandler != null) {
            faceInfoDrawHandler.obtainMessage(BaseDrawHandler.MSG_ADD_CAMERA_VIEW, iCameraView).sendToTarget();
        }

        if (palmInfoDrawHandler != null) {
            palmInfoDrawHandler.obtainMessage(BaseDrawHandler.MSG_ADD_CAMERA_VIEW, iCameraView).sendToTarget();
        }

        int ret = CameraManager.getInstance().startPreview((CameraDataCallback) mICameraModel);
        CameraWatchDogManager.getInstance().resume();
        Log.i(TAG, "resumeCam,ret = " + ret);
    }

    int[] getFps() {
        return mICameraModel.getImgFPS();
    }

    RecognizedBioDataCache getFaceRecData() {
        return mICameraModel.getRecognizedBioDataCache();
    }

    @Override
    public void onIdentifyFailed(IdentifyFailedData identifyFailedData) {
        String bioType = identifyFailedData.getBioType();
        switch (bioType) {
            case BioType.FACE:
                faceInfoDrawHandler.obtainMessage(MSG_ON_FACE_IDENTIFY_FAILED, identifyFailedData)
                        .sendToTarget();
                break;
            case BioType.PALM:
            case BioType.PALM_VEIN:
                palmInfoDrawHandler.obtainMessage(MSG_ON_PALM_IDENTIFY_FAILED, identifyFailedData)
                        .sendToTarget();
                break;
        }
    }

    @Override
    public void onPalmRecognizeComing(PalmRecognizeData palmRecognizeData) {
        palmInfoDrawHandler.removeMessages(MSG_CLEAR_PALM_INFO);
        palmInfoDrawHandler.obtainMessage(MSG_DRAW_PALM_INFO, palmRecognizeData).sendToTarget();
        palmInfoDrawHandler.sendEmptyMessageDelayed(MSG_CLEAR_PALM_INFO, 50);
    }

    @Override
    public void onDrawFaceDataComing(DrawFaceData drawFaceData) {
        faceInfoDrawHandler.removeMessages(MSG_CLEAR_FACE_INFO);
        faceInfoDrawHandler.obtainMessage(MSG_DRAW_FACE_INFO, drawFaceData).sendToTarget();
        faceInfoDrawHandler.sendEmptyMessageDelayed(MSG_CLEAR_FACE_INFO, 130);
    }

    @Override
    public void onCardInfo(CardInfo cardInfo) {
        palmInfoDrawHandler.obtainMessage(MSG_ON_CARD_INFO, cardInfo).sendToTarget();
    }

    @Override
    public void onVideoData(VideoData videoData) {
        if (decodeThread != null) {
            decodeThread.addVideoData(videoData);
        }
    }

    @Override
    public void onBitmap(Bitmap bitmap) {
        if (videoDataViewThread != null) {
            videoDataViewThread.addBitmap(bitmap);
        }
    }

    @Override
    public void onInfraredDistanceChanged(int distance) {
        palmInfoDrawHandler.obtainMessage(MSG_ON_DISTANCE_CHANGED_INFO, distance).sendToTarget();
    }

    private static class DecodeThread extends BaseThread {
        final BitmapFactory.Options options;
        private Bitmap cacheBitmap = null;
        private int uvcFrameInvalidCount = 0;

        private final LinkedBlockingQueue<VideoData> videoDataQueue;

        DecodeThread(ICameraDataModel iCameraDataModel) {
            super(iCameraDataModel, "DecodeThread" + new Random().nextInt(10));
            options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inMutable = true;
            videoDataQueue = new LinkedBlockingQueue<>(2);
        }

        public void addVideoData(VideoData videoData) {
            videoDataQueue.offer(videoData);
        }

        @Override
        public void run() {

            while (bRun) {
                if (this.isInterrupted()) {
                    break;
                }

                ICameraDataModel iCameraDataModel = mICameraModelRef.get();
                if (iCameraDataModel == null) {
                    Log.e(TAG, "ICameraDataModel is null,break up!");
                    break;
                }


                VideoData videoData = null;
                try {
                    videoData = videoDataQueue.take();
                    //Because of the characteristics of UVC ISOC transmission,
                    // in order to ensure the stability and robustness of
                    // the application. We strongly recommend to use the
                    // SDK interface to check the data once, and deal with
                    // it when the data is illegal three times in a row.
                    if (!ProCameraManager.checkUVCFrameValid(videoData)) {
                        uvcFrameInvalidCount++;
                        if (uvcFrameInvalidCount >= 15) {
                            Log.w(TAG, "UVC Frame invalid over 15 times,enter rescue");
                            RescueManager.instance().doRescue(ExApplication.instance(),
                                    "Invalid uvc frame trigger");
                            uvcFrameInvalidCount = 0;
                        }
                        continue;
                    }
                    uvcFrameInvalidCount = 0;

                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            SystemClock.sleep(10);
                            continue;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        if (iCameraView != null) {
//                        iCameraView.onExtraInfo(iCameraDataModel.getExtraInfo());
                            iCameraView.onFPSUpdate(iCameraDataModel.getImgFPS());
                        }
                    }

                    try {
                        if (cacheBitmap != null) {
                            options.inBitmap = cacheBitmap;
                        }
                        cacheBitmap = BitmapFactory.decodeByteArray(videoData.buff, 0, videoData.size, options);
                        //NOTE!
                        //Please recycle video data after decode,This will reduce GC times
                        ///////////////////////////////////////////////////////////////////
                        videoData.recycle();
                        ///////////////////////////////////////////////////////////////////
                        if (cacheBitmap != null) {
                            iCameraDataModel.addBitmap(cacheBitmap);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "decode error: " + ex.getMessage());
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }


    private static class VideoDataViewThread extends BaseThread {
        private static final String TAG = VideoDataViewThread.class.getSimpleName();

        private final LinkedBlockingQueue<Bitmap> bitmapQueue;

        VideoDataViewThread(ICameraDataModel iCameraDataModel) {
            super(iCameraDataModel, "ViewThr——" + new Random().nextInt(10));
            bitmapQueue = new LinkedBlockingQueue<>(1);
        }

        public void addBitmap(Bitmap bitmap) {
            bitmapQueue.offer(bitmap);
        }

        @Override
        public void run() {
            while (bRun) {
                if (this.isInterrupted()) {
                    Log.e(TAG, "interrupted!");
                    break;
                }

                ICameraDataModel iCameraDataModel = mICameraModelRef.get();
                if (iCameraDataModel == null) {
                    Log.e(TAG, "ICameraDataModel is null,break up!");
                    break;
                }

                try {
                    Bitmap bitmap = bitmapQueue.take();
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            SystemClock.sleep(10);
                            continue;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.drawVideoData(bitmap);
                            }
                        } catch (IllegalArgumentException ignore) {
                            bRun = false;
                            break;
                        }
                    }
                    if (null == bitmap) {
                        SystemClock.sleep(10);
                    } else {
                        SystemClock.sleep(1);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            Log.w(TAG, Thread.currentThread().getName() + ",end");
        }
    }


    private static class FaceInfoViewHandler extends BaseDrawHandler {

        FaceInfoViewHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_REMOVE_CAMERA_VIEW: {
                    ICameraView cameraView = (ICameraView) msg.obj;
                    removeICameraView(cameraView);
                }
                break;
                case MSG_ADD_CAMERA_VIEW:
                    ICameraView cameraView = (ICameraView) msg.obj;
                    addICameraView(cameraView);
                    break;
                case MSG_DRAW_FACE_INFO:
                    DrawFaceData drawFaceData = (DrawFaceData) msg.obj;
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.drawFaceInfo(drawFaceData);
                            }
                        } catch (IllegalArgumentException ignore) {
                            break;
                        }
                    }
                    break;
                case MSG_CLEAR_FACE_INFO:
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.clearCustomInfoView();
                            }
                        } catch (IllegalArgumentException ignore) {
                            break;
                        }
                    }
                    break;
                case MSG_ON_FACE_IDENTIFY_FAILED:
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        if (iCameraView != null) {
                            iCameraView.onIdentifyFailed((IdentifyFailedData) msg.obj);
                        }
                    }
                    break;
            }
        }

    }

    private static class PalmInfoViewHandler extends BaseDrawHandler {

        PalmInfoViewHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_REMOVE_CAMERA_VIEW: {
                    ICameraView cameraView = (ICameraView) msg.obj;
                    removeICameraView(cameraView);
                }
                break;
                case MSG_ADD_CAMERA_VIEW:
                    ICameraView cameraView = (ICameraView) msg.obj;
                    addICameraView(cameraView);
                    break;
                case MSG_DRAW_PALM_INFO:
                    PalmRecognizeData palmRecognizeData = (PalmRecognizeData) msg.obj;
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.drawPalmInfo(palmRecognizeData);
                            }
                        } catch (IllegalArgumentException ignore) {
                            break;
                        }
                    }
                    break;
                case MSG_CLEAR_PALM_INFO:
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.clearCustomInfoView();
                            }
                        } catch (IllegalArgumentException ignore) {
                            break;
                        }
                    }
                    break;
                case MSG_ON_DISTANCE_CHANGED_INFO:
                    int distance = (int) msg.obj;
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.onInfraredDistance(distance);
                            }
                        } catch (IllegalArgumentException ignore) {
                            break;
                        }
                    }
                    break;
                case MSG_ON_CARD_INFO:
                    CardInfo cardInfo = (CardInfo) msg.obj;
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        try {
                            if (iCameraView != null) {
                                iCameraView.onCardInfo(cardInfo);
                            }
                        } catch (IllegalArgumentException ignore) {
                            break;
                        }
                    }
                    break;
                case MSG_ON_PALM_IDENTIFY_FAILED:
                    synchronized (LIST_LOCK) {
                        if (cameraViewRefList.isEmpty()) {
                            break;
                        }
                        WeakReference<ICameraView> iCameraViewWeakReference = cameraViewRefList.get(0);
                        ICameraView iCameraView = iCameraViewWeakReference.get();
                        if (iCameraView != null) {
                            iCameraView.onIdentifyFailed((IdentifyFailedData) msg.obj);
                        }
                    }
                    break;
            }
        }

    }
}
