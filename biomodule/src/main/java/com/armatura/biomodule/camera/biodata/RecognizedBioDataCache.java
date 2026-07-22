package com.armatura.biomodule.camera.biodata;

import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.common.Common;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class RecognizedBioDataCache {
    private volatile static RecognizedBioDataCache instance = null;
    private final Object lock = new Object();
    private final LinkedHashMap<Integer, Common.RecognizedFaceData> recognizeRecordMap = new LinkedHashMap<>(12);
    private final LinkedList<UserInfo> userFaces_record = new LinkedList<>();

    private RecognizedBioDataCache() {
    }

    public static RecognizedBioDataCache instance() {
        if (instance == null) {
            synchronized (RecognizedBioDataCache.class) {
                if (instance == null) {
                    instance = new RecognizedBioDataCache();
                }
            }
        }
        return instance;
    }

    public void addRecordUserInfo(UserInfo userInfo) {
        synchronized (lock) {
            for (Iterator<UserInfo> iterator = userFaces_record.iterator(); iterator.hasNext(); ) {
                UserInfo userFace1 = iterator.next();
                if (userFace1.id == userInfo.id) {
                    iterator.remove();
                    break;
                }
            }

            userFaces_record.addFirst(userInfo);

            if (userFaces_record.size() > 5) {
                userFaces_record.removeLast();
            }
        }
    }


    public void clearRecordUserFace() {
        synchronized (lock) {
            userFaces_record.clear();
        }
    }

    public Common.RecognizedFaceData getRecognizedFace(int faceId) {
        synchronized (lock) {
            return recognizeRecordMap.get(faceId);
        }
    }

    public void addRecFace(int faceid, Common.RecognizedFaceData recognizedFaceData) {
        synchronized (lock) {
            recognizeRecordMap.put(faceid, recognizedFaceData);
            if (recognizeRecordMap.size() > 10) {
                Iterator<Map.Entry<Integer, Common.RecognizedFaceData>> iterator = recognizeRecordMap.entrySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }

    public void deleteFaceRecord(String userPin) {
        synchronized (lock) {
            Iterator<UserInfo> iterator = userFaces_record.iterator();
            while (iterator.hasNext()) {
                UserInfo userInfo = iterator.next();
                if (userPin.equals(userInfo.userId)) {
                    iterator.remove();
                    break;
                }
            }
            recognizeRecordMap.clear();
        }
    }

    public void clearRecFaces() {
        synchronized (lock) {
            recognizeRecordMap.clear();
            userFaces_record.clear();
        }
    }
}