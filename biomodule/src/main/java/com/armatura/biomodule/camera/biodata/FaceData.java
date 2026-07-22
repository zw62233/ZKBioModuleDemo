package com.armatura.biomodule.camera.biodata;

import androidx.annotation.NonNull;

import com.armatura.biomodule.pojo.common.Attribute;
import com.armatura.biomodule.pojo.common.LiveData;
import com.armatura.biomodule.pojo.common.TrackData;
import com.armatura.biomodule.pojo.face.FaceFeature;
import com.armatura.biomodule.pojo.face.recognize.IdentifyInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author magic.hu@armatura.com
 * @date 2020/07/31
 * @since 1.0.0
 */
public class FaceData {
    public TrackData trackData;
    public FaceFeature faceFeature;
    public LiveData liveness;
    public Attribute attribute;
    public List<IdentifyInfo> identifyInfoList;

    public boolean bHasFeature = false;
    public boolean bHasLiveScore = false;
    public boolean bHasAttr = false;
    public boolean bHasTrack = false;
    public boolean bHasIdentifyInfo = false;

    public FaceData() {
        trackData = new TrackData();
        liveness = new LiveData();
        attribute = new Attribute();
        identifyInfoList = new ArrayList<>();
    }

    @NonNull
    @Override
    public String toString() {
        return "FaceData{" +
                ", bHasFeature=" + bHasFeature +
                ", bHasLiveScore=" + bHasLiveScore +
                ", bHasAttr=" + bHasAttr +
                ", bHasTrack=" + bHasTrack +
                ", bHasIdentifyInfo=" + identifyInfoList +
                '}';
    }
}
