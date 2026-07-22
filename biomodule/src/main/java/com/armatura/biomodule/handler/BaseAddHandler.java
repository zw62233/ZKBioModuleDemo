package com.armatura.biomodule.handler;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.armatura.biomodule.pojo.common.CommonConfigData;
import com.armatura.biomodule.pojo.info.SnapshotData;
import com.armatura.biomodule.pojo.setting.CommonSettingData;
import com.armatura.biomodule.register.RegisterStatusCallback;
import com.armatura.biomodule.util.BitmapUtil;
import com.armatura.biomodule.util.HidHelper;
import com.armatura.biomodule.util.JSONUtil;
import com.armatura.constant.ConfigType;
import com.armatura.constant.ErrorCode;
import com.armatura.constant.ManageType;
import com.armatura.constant.ParamIndex;
import com.armatura.constant.SnapType;
import com.armatura.translib.AMTHidManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by Magic on 2020/9/17
 */
public abstract class BaseAddHandler extends Handler {
    public static final int ADD_USER_INFO = 200;
    public final static int START_REGISTER = 201;
    public final static int STOP_REGISTER = 202;
    public static final int UPDATE_USER_INFO = 203;
    public static final int MSG_SNAP_SHOT_RGB = 204;
    public static final int MSG_SNAP_SHOT_GRAY = 205;

    public static final int MSG_REG_START = 206;
    public static final int MSG_REG_END = 207;

    public static final int EXIT_STAND_BY_MODE = 208;


    public BaseAddHandler(Looper looper) {
        super(looper);
    }


    protected Bitmap snapShot(@SnapType int snapType) {
        Bitmap bitmap = null;
        byte[] snapData = new byte[2 * 1024 * 1024];
        int[] length = new int[1];
        int ret = AMTHidManager.instance().snapShot(snapType, snapData, length);
        if (ret == 0) {
            SnapshotData snapShotData = JSONUtil.getSnapShotData(new String(snapData, 0, length[0]));
            if (snapShotData != null) {
                byte[] imageData = Base64.decode(snapShotData.getData(), Base64.NO_WRAP);
                Log.i("snapShot", "snapShot: length=" + imageData.length);
                if (snapType == SnapType.SNAP_GRAY) {
                    bitmap = BitmapUtil.createGrayBitmap(imageData,
                            snapShotData.getWidth(),
                            snapShotData.getHeight());
                } else {
                    bitmap = BitmapUtil.getBitmapFromByte(imageData);
                }
            }
        }
        return bitmap;
    }

    protected void sendRegStart(RegisterStatusCallback callback) {
        byte[] resultByteArray = new byte[1024 * 1024];
        int[] resultSize = new int[]{resultByteArray.length};
        int ret = AMTHidManager.instance().manageModuleData(ManageType.REG_START, null,
                resultByteArray, resultSize);
        if (ret == 0) {
            String result = new String(resultByteArray, 0, resultSize[0]);
            CommonConfigData commonConfigData = JSONUtil.getCommonConfigData(result);
            if (commonConfigData != null) {
                if (commonConfigData.status == 0) {
                    callback.toastMessage("reg start");
                } else {
                    callback.toastMessage(commonConfigData.detail);
                }
            } else {
                callback.toastMessage("reg start failed,data analyse failed");
            }
        } else {
            callback.toastMessage(String.format(Locale.US, "reg start failed,ret =%d", ret));
        }
    }


    protected void sendRegEnd(RegisterStatusCallback callback) {
        byte[] resultByteArray = new byte[1024 * 1024];
        int[] resultSize = new int[]{resultByteArray.length};
        int ret = AMTHidManager.instance().manageModuleData(ManageType.REG_END, null,
                resultByteArray, resultSize);
        if (ret == 0) {
            String result = new String(resultByteArray, 0, resultSize[0]);
            CommonConfigData commonConfigData = JSONUtil.getCommonConfigData(result);
            if (commonConfigData != null) {
                if (commonConfigData.status == 0) {
                    callback.toastMessage("reg end");
                } else {
                    callback.toastMessage(commonConfigData.detail);
                }
            } else {
                callback.toastMessage("reg end failed,data analyse failed");
            }
        } else {
            callback.toastMessage(String.format(Locale.US, "reg end failed,ret =%d", ret));
        }
    }


    protected void exitStandByMode() {
        HidHelper.exitStandByMode();
    }

    protected void enterStandByMode() {
        HidHelper.enterStandByMode();
    }
}
