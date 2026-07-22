package com.armatura.biomodule.camera.base;

import com.armatura.biomodule.camera.ICameraDataModel;
import com.armatura.biomodule.camera.ICameraView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BaseThread extends Thread implements IBaseThread {
    public final Object lock = new Object();
    public volatile boolean bRun;


    protected final List<WeakReference<ICameraView>> cameraViewRefList;
    protected final Object LIST_LOCK = new Object();

    protected final WeakReference<ICameraDataModel> mICameraModelRef;

    public BaseThread(ICameraDataModel iCameraDataModel, String name) {
        bRun = false;
        this.setName(name);
        cameraViewRefList = new ArrayList<>();
        mICameraModelRef = new WeakReference<>(iCameraDataModel);
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

    @Override
    public void Start() {
        bRun = true;
        synchronized (lock) {
            lock.notifyAll();
        }
        this.start();
    }

    @Override
    public void Stop() {
        bRun = false;
        interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
