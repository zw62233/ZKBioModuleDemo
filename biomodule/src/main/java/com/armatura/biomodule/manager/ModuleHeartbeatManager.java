package com.armatura.biomodule.manager;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.armatura.constant.ErrorCode;
import com.armatura.constant.HeartbeatType;
import com.armatura.translib.AMTHidManager;

public class ModuleHeartbeatManager {
    private static final String TAG = "ModuleHeartbeatManager";

    private final Handler heartBeatHandler;
    private final static int MSG_START_HEART = 0x22201;
    private final static int MSG_STOP_HEART = 0x22202;

    private boolean bStartHeart = false;

    private final static int MAX_HEART_INTERVAL = 10_000;

    private ModuleHeartbeatManager() {
        HandlerThread heartBeatThread = new HandlerThread("HeartBeatThread");
        heartBeatThread.start();
        heartBeatHandler = new Handler(heartBeatThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case MSG_START_HEART:
                        if (bStartHeart) {
                            sendHeartBeat();
                        }
                        heartBeatHandler.removeMessages(MSG_START_HEART);
                        heartBeatHandler.sendEmptyMessageDelayed(MSG_START_HEART, MAX_HEART_INTERVAL);
                        break;
                    case MSG_STOP_HEART:
                        heartBeatHandler.removeMessages(MSG_START_HEART);
                        sendHeartBeatStop();
                        break;
                }
                return false;
            }
        });
    }

    private static final class InstanceHolder {
        static final ModuleHeartbeatManager instance = new ModuleHeartbeatManager();
    }

    public static ModuleHeartbeatManager getInstance() {
        return InstanceHolder.instance;
    }

    public void hearBeat() {
        bStartHeart = true;
        heartBeatHandler.sendEmptyMessage(MSG_START_HEART);
    }

    public void heatBeatStop() {
        bStartHeart = false;
        sendHeartBeatStop();
    }


    private void sendHeartBeat() {
        int ret = AMTHidManager.instance().heartbeat(HeartbeatType.SEND_HEART);
        Log.i(TAG, "heartBeat: " + ((ret == ErrorCode.ERROR_NONE) ? "success" : "failed," + ret));
    }


    private void sendHeartBeatStop() {
        int ret = AMTHidManager.instance().heartbeat(HeartbeatType.STOP_HEART);
        Log.i(TAG, "heartBeatStop: " + ((ret == ErrorCode.ERROR_NONE) ? "success" : "failed," + ret));
    }
}