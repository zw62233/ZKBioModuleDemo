package com.armatura.biomodule.pojo.face.recognize;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.armatura.biomodule.bean.IdentifyInfoBoard;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.camera.biodata.FaceData;
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache;
import com.armatura.biomodule.common.Common;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.pojo.common.Label;
import com.armatura.biomodule.pojo.common.LiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * include track data which from CameraDataCallback
 *
 * @author magic.hu@armatura.com
 * @date 2020/07/31
 * @since 1.0.0
 */
public class DrawFaceData {
    public int height;
    public int width;
    public long index;
    public List<FaceData> faceDataList;

    public boolean isIdentify() {
        if (faceDataList == null || faceDataList.isEmpty()) {
            return false;
        }
        for (int i = 0, faceDataListSize = faceDataList.size(); i < faceDataListSize; i++) {
            FaceData newFaceData = faceDataList.get(i);

            Common.RecognizedFaceData recognizedFace = RecognizedBioDataCache.instance().getRecognizedFace(newFaceData.trackData.trackId);
            //same track ,already identify
            if (recognizedFace != null && recognizedFace.isRecognized) {
                return true;
            }
        }
        return false;
    }

    public List<IdentifyInfoBoard> toMultiIdentifyInfoBoard() {
        ArrayList<IdentifyInfoBoard> faceInfoList = new ArrayList<>();
        for (int i = 0, faceDataListSize = faceDataList.size(); i < faceDataListSize; i++) {
            IdentifyInfoBoard identifyInfoHtmlString = new IdentifyInfoBoard();
            identifyInfoHtmlString.identifyType = Label.LABEL_FACE;
            FaceData newFaceData = faceDataList.get(i);
            if (Config.instance().recognizeMode == Config.MULTI_BIO_MODULE_INTERNAL_MODE && newFaceData.bHasIdentifyInfo) {
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                IdentifyInfo identifyInfo = newFaceData.identifyInfoList.get(0);
                UserInfo userInfo = new UserInfo();
                userInfo.personId = identifyInfo.getPersonId();
                userInfo.name = identifyInfo.getName();
                userInfo.userId = identifyInfo.getUserId();
                userInfo.similarity = (int) identifyInfo.getSimilarity();
                userInfo.groupId = identifyInfo.getGroupId();
                ssb.append("Matching Score :")
                        .append(String.format(Locale.US, "%.2f", userInfo.similarity))
                        .append("\n");
                if (userInfo.liveData != null) {
                    LiveData liveData = userInfo.liveData;
                    ssb.append("Liveness Score :")
                            .append(String.format(Locale.US, "%.2f", liveData.livenessScore))
                            .append("\n");
                    ssb.append("Liveness State :")
                            .append(Common.FaceLiveStatus.getStringByCode(liveData.liveness))
                            .append("\n");
                    ssb.append("Liveness Mode :")
                            .append(Common.FaceLiveMode.getStringByCode(liveData.livenessMode))
                            .append("\n");
                }
                ssb.append(newFaceData.trackData.toShortString()).append("\n");
                if (newFaceData.bHasAttr) {
                    ssb.append(newFaceData.attribute.toShortString()).append("\n");
                }
                if (newFaceData.bHasLiveScore && Config.instance().isDisplayLivenessInfo) {
                    ssb.append(newFaceData.liveness.toShortString()).append("\n");
                }
                boolean identify = isIdentify();
                identifyInfoHtmlString.avatarIndex = identify ? userInfo.avatarIndex : -1;
                identifyInfoHtmlString.userName = identify ? userInfo.name : "";
                identifyInfoHtmlString.identifyInfoSpannableStingBuilder = ssb;
                faceInfoList.add(identifyInfoHtmlString);
            } else {
                identifyInfoHtmlString.hasFaceFeature = (newFaceData.faceFeature != null);
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                Common.RecognizedFaceData recognizedFace = RecognizedBioDataCache.instance().getRecognizedFace(newFaceData.trackData.trackId);
                //same track ,already identify
                if (recognizedFace != null && recognizedFace.isRecognized) {
                    FaceData recognizedFaceData = recognizedFace.faceData;
                    //name
                    UserInfo userInfo = recognizedFace.userFace;
                    if (userInfo != null) {
                        //score
                        identifyInfoHtmlString.faceMatchScore = userInfo.similarity;
                        ssb.append("Matching Score :")
                                .append(String.format(Locale.US, "%.2f", userInfo.similarity))
                                .append("\n");
                        if (userInfo.liveData != null) {
                            LiveData liveData = userInfo.liveData;
                            ssb.append("Liveness Score :")
                                    .append(String.format(Locale.US, "%.2f", liveData.livenessScore))
                                    .append("\n");
                            ssb.append("Liveness State :")
                                    .append(Common.FaceLiveStatus.getStringByCode(liveData.liveness))
                                    .append("\n");
                            ssb.append("Liveness Mode :")
                                    .append(Common.FaceLiveMode.getStringByCode(liveData.livenessMode))
                                    .append("\n");
                        }
                    }
                    ssb.append(newFaceData.trackData.toShortString());
                    //attribute update has a interval,if not update use old info
                    if (newFaceData.bHasAttr) {
                        ssb.append("\n");
                        ssb.append(newFaceData.attribute.toShortString());
                    } else {
                        ssb.append("\n");
                        ssb.append(recognizedFaceData.attribute.toShortString());
                    }

                    if (newFaceData.bHasLiveScore && Config.instance().isDisplayLivenessInfo) {
                        ssb.append("\n");
                        ssb.append(newFaceData.liveness.toShortString());
                    } else if (recognizedFaceData.bHasLiveScore && Config.instance().isDisplayLivenessInfo) {
                        ssb.append("\n");
                        ssb.append(recognizedFaceData.liveness.toShortString());
                    }
                    boolean identify = isIdentify();
                    identifyInfoHtmlString.avatarIndex = identify ? userInfo.avatarIndex : -1;
                    identifyInfoHtmlString.userName = identify ? userInfo.name : "";
                    identifyInfoHtmlString.identifyInfoSpannableStingBuilder = ssb;
                    faceInfoList.add(identifyInfoHtmlString);
                }
            }
        }
        return faceInfoList;
    }

    private final static Map<Integer, FaceData> trackDataCacheMap = new HashMap<>(15);

    private void putTrackData(int trackId, FaceData data) {
        if (trackDataCacheMap.size() > 10) {
            Iterator<Map.Entry<Integer, FaceData>> iterator = trackDataCacheMap.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        trackDataCacheMap.put(trackId, data);
    }
}
