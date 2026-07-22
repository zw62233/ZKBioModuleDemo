package com.armatura.biomodule.handler;

import android.os.Handler;
import android.os.Looper;

import com.armatura.biomodule.camera.ICameraDataModel;
import com.armatura.biomodule.camera.ICameraView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BaseDrawHandler extends Handler {
    protected final List<WeakReference<ICameraView>> cameraViewRefList;
    protected final Object LIST_LOCK = new Object();
    public final static int MSG_REMOVE_CAMERA_VIEW = 0x9999;
    public final static int MSG_ADD_CAMERA_VIEW = 0x8888;


    public BaseDrawHandler(Looper looper) {
        super(looper);
        cameraViewRefList = new ArrayList<>();
    }


    public void addICameraView(ICameraView iCameraView) {
        synchronized (LIST_LOCK) {
            Iterator<WeakReference<ICameraView>> iterator = cameraViewRefList.iterator();
            while (iterator.hasNext()) {
                WeakReference<ICameraView> next = iterator.next();
                if (next.get() == null) {
                    iterator.remove();
                    continue;
                }
                if (next.get().equals(iCameraView)) {
                    next.clear();
                    iterator.remove();
                    break;
                }
            }
            cameraViewRefList.add(new WeakReference<>(iCameraView));
        }
    }

    public void removeICameraView(ICameraView iCameraView) {
        synchronized (LIST_LOCK) {
            Iterator<WeakReference<ICameraView>> iterator = cameraViewRefList.iterator();
            while (iterator.hasNext()) {
                WeakReference<ICameraView> next = iterator.next();
                if (next.get() == null) {
                    iterator.remove();
                    continue;
                }
                if (next.get().equals(iCameraView)) {
                    next.clear();
                    iterator.remove();
                    break;
                }
            }
        }
    }
}
