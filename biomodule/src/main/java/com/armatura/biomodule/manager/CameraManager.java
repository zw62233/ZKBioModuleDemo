package com.armatura.biomodule.manager;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.SystemClock;
import android.util.Log;

import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.constant.CameraErrorCode;
import com.armatura.uvccameralibrary.pro.ProCameraManager;
import com.armatura.uvclib.CameraDataCallback;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static int mCameraID = ProCameraManager.COLOR_CAMERA;

    private CameraManager() {
        proCameraManager = new ProCameraManager();
    }

    private static class InstanceHolder {
        final static CameraManager instance = new CameraManager();
    }

    public static CameraManager getInstance() {
        return InstanceHolder.instance;
    }


    private final ProCameraManager proCameraManager;

    public int getCameraID() {
        return mCameraID;
    }

    /**
     * When your motherboard is connected to only one module, you can call this method to open the camera.
     */
    public int openCamera(Context context) {
        return proCameraManager.openCamera(context, mCameraID);
    }

    /**
     * Open the camera associated with USBDevice. This interface is usually used when multiple
     * modules need to be opened at the same time.
     */
    public int openCamera(Context context, UsbDevice usbDevice) {
        return proCameraManager.openCamera(context, usbDevice, mCameraID);
    }


    public void closeCamera() {
        proCameraManager.closeCamera();
    }

    public int startPreview(CameraDataCallback cameraDataCallback) {
        return proCameraManager.startPreview(cameraDataCallback);
    }

    public void stopPreview() {
        proCameraManager.stopPreview();
    }

    public int reConnectCamera(Context context, CameraDataCallback cameraDataCallback) {
        //pause watch dog
        CameraWatchDogManager.getInstance().pause();
        proCameraManager.closeCamera();
        SystemClock.sleep(1000);
        int ret = proCameraManager.openCamera(context, mCameraID);
        if (ret == CameraErrorCode.SUCCESS || ret == CameraErrorCode.CAMERA_ALREADY_OPEN) {
            //resume watch dog
            CameraWatchDogManager.getInstance().resume();
            proCameraManager.startPreview(cameraDataCallback);
        } else {
            Log.e(TAG, "Reconnect Camera failed,ret = " + ret);
        }
        return ret;
    }

    /**
     * switch camera id if support
     */
    public void switchCamera(CameraDataCallback cameraDataCallback) {
        proCameraManager.closeCamera();
        if (mCameraID == ProCameraManager.COLOR_CAMERA) {
            mCameraID = ProCameraManager.IR_CAMERA;
        } else {
            mCameraID = ProCameraManager.COLOR_CAMERA;
        }
        int ret;
        if ((ret = proCameraManager.openCamera(ExApplication.instance(), mCameraID)) == CameraErrorCode.SUCCESS) {
            Log.i(TAG, "switchCamera: open camera success");
            ret = proCameraManager.startPreview(cameraDataCallback);
            Log.i(TAG, "switchCamera: start preview ret = " + ret);
        } else {
            Log.e(TAG, "switchCamera failed,ret = " + ret + ",cameraID=" + mCameraID);
        }
    }
}