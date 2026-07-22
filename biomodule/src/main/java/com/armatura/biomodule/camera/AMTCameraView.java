package com.armatura.biomodule.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;

import com.armatura.biomodule.R;
import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyFailedData;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.common.Common;
import com.armatura.biomodule.common.IdentifyState;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo;
import com.armatura.biomodule.pojo.palm.recognize.PalmRecognizeData;
import com.armatura.biomodule.register.RegisterHelper;
import com.armatura.biomodule.util.CustomDraw;
import com.armatura.biomodule.view.CircleDetectView;

public class AMTCameraView implements ICameraView {
    private float mRectInfoScale = 1.0F;

    private static final int WHT_REGISTER_FACE = 0,
            WHT_FPS = 2,
            WHT_INFRARED_DISTANCE = 4,
            WHT_CLEAR_INFRARED_DISTANCE = 41,
            WHT_MSG_CLEAR_CARD_IDENTIFY_INFO = 5,
            WHT_MSG_CLEAR_FACE_IDENTIFY_INFO = 6,
            WHT_MSG_CLEAR_PALM_IDENTIFY_INFO = 7,
            MSG_ENTER_SAVE_POWER_MODE = 8,
            MSG_RESET_INDICATOR_VIEW = 9;

    private final static long MAX_IDLE_TIME = 10_000L;

    private final String TAG = AMTCameraView.class.getSimpleName();
    private SurfaceView cameraSurfaceView;
    private SurfaceView rectInfoSurfaceView;
    private final CameraController cameraController;
    private final ConstraintLayout previewContainer;

    private CircleDetectView circleDetectView;
    private Handler handler;
    private RegisterHelper registerHelper = null;
    private final Rect displayArea = new Rect();
    private final MutableLiveData<DrawFaceData> faceDataMutableLiveData;
    private final MutableLiveData<PalmInfo> palmInfoMutableLiveData;
    private final MutableLiveData<CardInfo> cardInfoMutableLiveData;
    private final MutableLiveData<IdentifyState> identifyStateMutableLiveData;
    private final MutableLiveData<IdentifyFailedData> identifyFailedDataMutableLiveData;
    private TextView tvFPSView;
    private TextView tvExtraInfoView;
    private TextView tvCPUTempView;

    private volatile boolean isCameraSurfaceHolderReady = false;
    private volatile boolean isRectInfoSurfaceHolderReady = false;
    private final Object CAMERA_SURFACE_LOCK = new Object();
    private final Object RECT_INFO_SURFACE_LOCK = new Object();


    private final static int NEEDED_SKIP_FRAME = 1;
    private int skipFrameCount = 0;

    public AMTCameraView(ConstraintLayout previewLayout,
                         MutableLiveData<DrawFaceData> mutableLiveData,
                         MutableLiveData<IdentifyState> identifyStateMutableLiveData,
                         MutableLiveData<CardInfo> cardInfoLiveData,
                         MutableLiveData<PalmInfo> palmRectLiveData,
                         MutableLiveData<IdentifyFailedData> identifyFailedDataMutableLiveData) {
        cameraController = CameraController.instance();
        this.previewContainer = previewLayout;
        registerHelper = RegisterHelper.instance();
        initView();
        initHandler();
        faceDataMutableLiveData = mutableLiveData;
        palmInfoMutableLiveData = palmRectLiveData;
        cardInfoMutableLiveData = cardInfoLiveData;
        this.identifyFailedDataMutableLiveData = identifyFailedDataMutableLiveData;
        this.identifyStateMutableLiveData = identifyStateMutableLiveData;
    }

    public void unInit() {
        handler.removeCallbacksAndMessages(null);
        handler = null;
        circleDetectView = null;
        cameraSurfaceView = null;
        rectInfoSurfaceView = null;
        tvFPSView = null;
        tvCPUTempView = null;
        tvExtraInfoView = null;
    }

    private void initView() {
        circleDetectView = previewContainer.findViewById(R.id.circle_reflect_view);
        this.cameraSurfaceView = previewContainer.findViewById(R.id.camera_preview_surface_view);
        this.rectInfoSurfaceView = previewContainer.findViewById(R.id.rect_info_surface_view);
        this.tvFPSView = previewContainer.findViewById(R.id.tv_fps);
        this.tvCPUTempView = previewContainer.findViewById(R.id.tv_cpu_temp);
        this.tvExtraInfoView = previewContainer.findViewById(R.id.tv_extra_info);
        this.cameraSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.e(TAG, "surfaceCreated: ++++++++++++++++++++++++++");
                synchronized (CAMERA_SURFACE_LOCK) {
                    isCameraSurfaceHolderReady = true;
                    surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
                Log.e(TAG, "surfaceChanged: >>>>>>>>>>>>>>>>>>>>>>>>>>" + " width=" + width + " height=" + height);
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                Log.e(TAG, "surfaceDestroyed: -----------------------------");
                synchronized (CAMERA_SURFACE_LOCK) {
                    isCameraSurfaceHolderReady = false;
                }
            }
        });
        this.rectInfoSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                synchronized (RECT_INFO_SURFACE_LOCK) {
                    isRectInfoSurfaceHolderReady = true;
                    holder.setFormat(PixelFormat.TRANSLUCENT);
                }
                Log.i(TAG, "rect info surface view surfaceCreated: ");
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "rect info surface view surfaceChanged: ");
                synchronized (RECT_INFO_SURFACE_LOCK) {
                    mRectInfoScale = width / 720F;
                    Log.i(TAG, "surfaceChanged: mRectInfoScale=" + mRectInfoScale);
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                synchronized (RECT_INFO_SURFACE_LOCK) {
                    isRectInfoSurfaceHolderReady = false;
                }
                Log.i(TAG, "rect info surface view surfaceDestroyed: ");
            }
        });
        this.rectInfoSurfaceView.setZOrderOnTop(true);
    }

    private void initHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what;
                switch (what) {
                    case WHT_REGISTER_FACE:
                        Bitmap bitmap = (Bitmap) msg.obj;
                        registerHelper.registerByBitmap(bitmap, RegisterHelper.CAPTURE, ExApplication.instance());
                        break;
                    case WHT_FPS:
                        if (!Config.instance().isNeedShowFPS && tvFPSView.getVisibility() != View.GONE) {
                            tvFPSView.setVisibility(View.GONE);
                        }
                        if (Config.instance().isNeedShowFPS) {
                            if (tvFPSView.getVisibility() != View.VISIBLE) {
                                tvFPSView.setVisibility(View.VISIBLE);
                            }
                            tvFPSView.setText("FPS:" + msg.arg1 + "+" + msg.arg2 + "=" + (msg.arg1 + msg.arg2));
                        }
                        break;
                    case WHT_INFRARED_DISTANCE:
                        tvExtraInfoView.setText((String) msg.obj);
                        break;
                    case WHT_CLEAR_INFRARED_DISTANCE:
                        tvExtraInfoView.setText("");
                        break;
                    case WHT_MSG_CLEAR_CARD_IDENTIFY_INFO:
                        cardInfoMutableLiveData.setValue(null);
                        break;
                    case WHT_MSG_CLEAR_FACE_IDENTIFY_INFO:
                        faceDataMutableLiveData.setValue(null);
                        break;
                    case WHT_MSG_CLEAR_PALM_IDENTIFY_INFO:
                        palmInfoMutableLiveData.setValue(null);
                        break;
                    case MSG_ENTER_SAVE_POWER_MODE:
                        if (Config.instance().powerSaveMode) {
                            identifyStateMutableLiveData.postValue(IdentifyState.STOP);
                        }
                        break;
                    case MSG_RESET_INDICATOR_VIEW:
                        circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.NORMAL);
                        break;
                }
            }
        };
    }

    public void autoOpen(Context context, UsbDevice usbDevice) {
        cameraController.openCam(context, this, usbDevice);
    }

    public void enterSavePowerModeWhenIdle() {
        handler.removeMessages(MSG_ENTER_SAVE_POWER_MODE);
        handler.sendEmptyMessageDelayed(MSG_ENTER_SAVE_POWER_MODE, MAX_IDLE_TIME);
    }


    public void updateCPUTemp(String cpuTempValue) {
        if (!Config.instance().isDisplayCPUTempInfo && tvCPUTempView.getVisibility() != View.GONE) {
            tvCPUTempView.setVisibility(View.GONE);
            return;
        }
        if (Config.instance().isDisplayCPUTempInfo) {
            if (tvCPUTempView.getVisibility() != View.VISIBLE) {
                tvCPUTempView.setVisibility(View.VISIBLE);
            }
            tvCPUTempView.setText(cpuTempValue);
        }
    }

    public void close() {
        cameraController.closeCam();
    }


    @Override
    public void drawVideoData(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        synchronized (CAMERA_SURFACE_LOCK) {
            if (!isCameraSurfaceHolderReady) {
                Log.w(TAG, "drawView failed,surface holder destroyed");
                return;
            }
            CustomDraw.drawBitmapOnly(cameraSurfaceView, displayArea, bitmap);
        }
    }

    @Override
    public void drawFaceInfo(DrawFaceData drawFaceData) {
        if (Common.state == IdentifyState.STOP) {
            return;
        }
        handler.removeMessages(MSG_ENTER_SAVE_POWER_MODE);
        handler.sendEmptyMessageDelayed(MSG_ENTER_SAVE_POWER_MODE, MAX_IDLE_TIME);
        if (Common.state == IdentifyState.IDENTIFY_ONCE && !Common.state.isIdentified()) {
            if (drawFaceData != null && drawFaceData.isIdentify()) {
                faceDataMutableLiveData.postValue(drawFaceData);
                handler.removeMessages(WHT_MSG_CLEAR_FACE_IDENTIFY_INFO);
                handler.sendEmptyMessageDelayed(WHT_MSG_CLEAR_FACE_IDENTIFY_INFO, Config.instance().identifyInfoStayTime);
                Common.state.setIdentified(true);
            }
        }

        if (Common.state == IdentifyState.IDENTIFY_CONST) {
            if (drawFaceData != null) {
                faceDataMutableLiveData.postValue(drawFaceData);
                handler.removeMessages(WHT_MSG_CLEAR_FACE_IDENTIFY_INFO);
                handler.sendEmptyMessageDelayed(WHT_MSG_CLEAR_FACE_IDENTIFY_INFO, Config.instance().identifyInfoStayTime);
            }
        }
        //only biometric info
        if (drawFaceData != null) {
            synchronized (RECT_INFO_SURFACE_LOCK) {
                if (!isRectInfoSurfaceHolderReady) {
                    Log.w(TAG, "drawView failed,surface holder destroyed");
                    return;
                }
//                changeCircleDetectViewIndicator(palmInfo);
                CustomDraw.drawFaceAndPalmInfo(rectInfoSurfaceView, mRectInfoScale, drawFaceData,
                        cameraController.getFaceRecData(), null,
                        Config.shouldUseCircleIndicatorView());
            }
        }
    }

    @Override
    public void drawPalmInfo(PalmRecognizeData palmRecognizeData) {
        if (Config.shouldUseCircleIndicatorView()) {
            // In the video stream disabled mode,
            // skip the first frame as it may contain previous recognition data
            if (skipFrameCount++ < NEEDED_SKIP_FRAME) {
                return;
            }
        }

        if (Common.state == IdentifyState.STOP) {
            return;
        }
        handler.removeMessages(MSG_ENTER_SAVE_POWER_MODE);
        handler.sendEmptyMessageDelayed(MSG_ENTER_SAVE_POWER_MODE, MAX_IDLE_TIME);

        if (Common.state == IdentifyState.IDENTIFY_ONCE && !Common.state.isIdentified()) {
            if (palmRecognizeData != null && palmRecognizeData.getTrackInfo().hasIdentifyInfo()) {
                palmInfoMutableLiveData.postValue(palmRecognizeData.getTrackInfo());
                handler.removeMessages(WHT_MSG_CLEAR_PALM_IDENTIFY_INFO);
                handler.sendEmptyMessageDelayed(WHT_MSG_CLEAR_PALM_IDENTIFY_INFO, Config.instance().identifyInfoStayTime);
                Common.state.setIdentified(true);
            }
        }

        if (Common.state == IdentifyState.IDENTIFY_CONST) {
            if (palmRecognizeData != null && palmRecognizeData.getTrackInfo().hasIdentifyInfo()) {
                palmInfoMutableLiveData.postValue(palmRecognizeData.getTrackInfo());
                handler.removeMessages(WHT_MSG_CLEAR_PALM_IDENTIFY_INFO);
                handler.sendEmptyMessageDelayed(WHT_MSG_CLEAR_PALM_IDENTIFY_INFO, Config.instance().identifyInfoStayTime);
            }
        }


        PalmInfo palmInfo = null;
        if (palmRecognizeData != null) {
            palmInfo = palmRecognizeData.getTrackInfo();
        }
        //only biometric info
        changeCircleDetectViewIndicator(palmInfo);
        if (palmInfo != null) {
            synchronized (RECT_INFO_SURFACE_LOCK) {
                if (!isRectInfoSurfaceHolderReady) {
                    Log.w(TAG, "drawView failed,surface holder destroyed");
                    return;
                }
                CustomDraw.drawFaceAndPalmInfo(rectInfoSurfaceView, mRectInfoScale, null,
                        cameraController.getFaceRecData(), palmInfo,
                        Config.shouldUseCircleIndicatorView());
            }
        }
    }

    @Override
    public void onIdentifyFailed(IdentifyFailedData identifyFailedData) {
        identifyFailedDataMutableLiveData.postValue(identifyFailedData);
    }

    @Override
    public void clearCustomInfoView() {
        clearRectInfoSurface();
    }

    @Override
    public void clearVideoDataView() {
        clearVideoInfoSurface();
        skipFrameCount = 0;
    }

    private void changeCircleDetectViewIndicator(PalmInfo palmInfo) {
        if (palmInfo != null) {
            int imageQuality = palmInfo.getImageQuality();
            if (imageQuality != 0
                    && imageQuality < Config.instance().palmImageQualityThreshold) {
                circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.ORANGE);
                return;
            }

            float liveScore = palmInfo.getLiveScore();
            if ((liveScore == -3) || liveScore > 0 && liveScore < Config.instance().palmVLLivenessThreshold) {
                circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.RED);
                return;
            }
            circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.GREEN);
        }
        handler.removeMessages(MSG_RESET_INDICATOR_VIEW);
        handler.sendEmptyMessageDelayed(MSG_RESET_INDICATOR_VIEW, 500);
    }

    private void clearRectInfoSurface() {
        synchronized (RECT_INFO_SURFACE_LOCK) {
            if (!isRectInfoSurfaceHolderReady) {
                Log.w(TAG, "drawView failed,surface holder destroyed");
                return;
            }
            CustomDraw.clearSurface(rectInfoSurfaceView);
        }
    }

    private void clearVideoInfoSurface() {
        synchronized (CAMERA_SURFACE_LOCK) {
            if (!isCameraSurfaceHolderReady) {
                Log.w(TAG, "drawView failed,surface holder destroyed");
                return;
            }
            CustomDraw.clearSurface(cameraSurfaceView);
        }
    }

    @Override
    public void onFPSUpdate(int[] fps) {
        handler.obtainMessage(WHT_FPS, fps[0], fps[1]).sendToTarget();
    }

    @Override
    public void onInfraredDistance(int distance) {
        if (distance < 50) {
            circleDetectView.changeIndicatorState(CircleDetectView.IndicatorState.RED);
            handler.removeMessages(MSG_RESET_INDICATOR_VIEW);
            handler.sendEmptyMessageDelayed(MSG_RESET_INDICATOR_VIEW, 500);
        }
        handler.removeMessages(WHT_CLEAR_INFRARED_DISTANCE);
        handler.obtainMessage(WHT_INFRARED_DISTANCE, distance + "mm").sendToTarget();
        handler.sendEmptyMessageDelayed(WHT_CLEAR_INFRARED_DISTANCE, 1000);
    }

    @Override
    public void onCardInfo(CardInfo cardInfo) {
        if (Common.state == IdentifyState.STOP) {
            return;
        }
        handler.removeMessages(MSG_ENTER_SAVE_POWER_MODE);
        handler.sendEmptyMessageDelayed(MSG_ENTER_SAVE_POWER_MODE, MAX_IDLE_TIME);

        if (Common.state == IdentifyState.IDENTIFY_ONCE && !Common.state.isIdentified()) {
            if (cardInfo.isIdentifySuccess) {
                Common.state.setIdentified(true);
            }
            cardInfoMutableLiveData.postValue(cardInfo);
            handler.removeMessages(WHT_MSG_CLEAR_CARD_IDENTIFY_INFO);
            handler.sendEmptyMessageDelayed(WHT_MSG_CLEAR_CARD_IDENTIFY_INFO, Config.instance().identifyInfoStayTime);
        }

        if (Common.state == IdentifyState.IDENTIFY_CONST) {
            cardInfoMutableLiveData.postValue(cardInfo);
            handler.removeMessages(WHT_MSG_CLEAR_CARD_IDENTIFY_INFO);
            handler.sendEmptyMessageDelayed(WHT_MSG_CLEAR_CARD_IDENTIFY_INFO, Config.instance().identifyInfoStayTime);
        }

    }

}
