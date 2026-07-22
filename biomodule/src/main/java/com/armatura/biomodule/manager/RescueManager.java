package com.armatura.biomodule.manager;


import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.armatura.biomodule.camera.CameraController;
import com.armatura.biomodule.camera.UVCPowerManager;
import com.armatura.biomodule.util.FileUtils;
import com.armatura.constant.CameraErrorCode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * A mechanism for self-rescue when equipment malfunctions.
 */
public class RescueManager {
    private static final String TAG = "RescueManager";
    private final static int VID = 0x34c9;
    private final static int TARGET_PID_12 = 0X12;
    private final static int TARGET_PID_22 = 0X22;
    private final static int TARGET_PID_32 = 0X32;
    private int mSoftFixCount = 0;
    private final static int MAX_SOFT_FIX_COUNT = 3;
    private final SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final static String RESCUE_LOG_FILE_NAME = "rescue.log";

    private static class InstanceHolder {
        private final static RescueManager instance = new RescueManager();
    }

    public static RescueManager instance() {
        return InstanceHolder.instance;
    }

    public void doRescue(Context context, String reason) {
        Log.i(TAG, "enter rescue mode," + reason);
        FileUtils.saveAllRecord(context, RESCUE_LOG_FILE_NAME,
                dft.format(new Date()) + ":" + reason);
        //1.check device if it exist
        if (checkDeviceExist(context)) {
            Log.i(TAG, "[rescue] device exist,do reconnect");
            //exist? reconnect
            int ret = CameraManager.getInstance().reConnectCamera(context, CameraController.instance().getCameraModel());
            if (ret != CameraErrorCode.SUCCESS) {
                mSoftFixCount++;
                Log.i(TAG, "[rescue] reconnect failed, SoftFixCount = " + mSoftFixCount);
                if (mSoftFixCount >= MAX_SOFT_FIX_COUNT) {
                    Log.i(TAG, "[rescue] Maximum number of attempts reached,reset power");
                    //reset usb power
                    resetUVCPower(context);
                    mSoftFixCount = 0;
                } else {
                    Log.i(TAG, "[rescue] try reconnect again");
                    //reconnect again
                    doRescue(context, "Rescue(soft) failed(cnt=" + mSoftFixCount + "),try again");
                }
            } else {
                Log.i(TAG, "[rescue] reconnect success!");
                mSoftFixCount = 0;
            }
        } else {
            //if device not exist,reset usb power first if support
            Log.i(TAG, "[rescue] device not exist! Reset power directly!");
            resetUVCPower(context);
        }

    }

    private void resetUVCPower(Context context) {
        UVCPowerManager.INSTANCE.resetUVCPower(context);
    }


    private boolean checkDeviceExist(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            return false;
        }
        for (UsbDevice usbDevice : deviceList.values()) {
            if (usbDevice.getVendorId() == VID && (usbDevice.getProductId() >> 8 == TARGET_PID_12
                    || usbDevice.getProductId() >> 8 == TARGET_PID_22
                    || usbDevice.getProductId() >> 8 == TARGET_PID_32)) {
                return true;
            }
        }
        return false;
    }
}
