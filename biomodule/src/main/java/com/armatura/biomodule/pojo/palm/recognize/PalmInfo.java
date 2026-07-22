package com.armatura.biomodule.pojo.palm.recognize;

import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;

import com.armatura.biomodule.bean.IdentifyInfoBoard;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.pojo.common.Label;
import com.armatura.biomodule.pojo.palm.PalmRect;
import com.armatura.biomodule.pojo.setting.VLPalmSetting;

import java.util.Locale;

/**
 * the rect of palm
 *
 * @author magic.hu@armatura.com
 * @date 2020/08/07
 * @since 1.0.0
 */
public class PalmInfo {
    private int imageQuality;
    private int templateQuality;

    /**
     * >=0 : liveness score
     * -1.0：liveness function disabled
     * -2.0：uvc data exclude liveness info
     * -3.0：no palm detect in IR stream
     * -4.0：exception in IR stream when get max palm
     * -5.0：low quality in IR stream
     * -6.0：iou exception
     */
    private float liveScore = 0F;
    private PalmRect rect;
    private transient UserInfo userInfo = null;

    public int getTemplateQuality() {
        return templateQuality;
    }

    public PalmInfo setTemplateQuality(int templateQuality) {
        this.templateQuality = templateQuality;
        return this;
    }

    public float getLiveScore() {
        return liveScore;
    }

    public void setLiveScore(float liveScore) {
        this.liveScore = liveScore;
    }

    public void setImageQuality(int imageQuality) {
        this.imageQuality = imageQuality;
    }

    public int getImageQuality() {
        return imageQuality;
    }

    public void setRect(PalmRect rect) {
        this.rect = rect;
    }

    public PalmRect getRect() {
        return rect;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public PalmInfo copy() {
        PalmInfo palmInfo = new PalmInfo();
        if (userInfo != null) {
            palmInfo.setUserInfo(this.userInfo.copy());
        }
        palmInfo.setImageQuality(imageQuality);
        palmInfo.setTemplateQuality(templateQuality);
        palmInfo.setRect(rect);
        palmInfo.setLiveScore(liveScore);
        return palmInfo;
    }

    public boolean hasIdentifyInfo() {
        return userInfo != null;
    }

    @NonNull
    @Override
    public String toString() {
        return "PalmTrackInfo{" +
                "imageQuality=" + imageQuality +
                "liveScore=" + liveScore +
                ", rect=" + rect +
                '}';
    }

    public IdentifyInfoBoard toIdentifyInfoBoard() {
        IdentifyInfoBoard identifyInfoBoard = new IdentifyInfoBoard();
        identifyInfoBoard.identifyType = Label.LABEL_PALM;
        identifyInfoBoard.avatarIndex = hasIdentifyInfo() ? userInfo.avatarIndex : -1;
        identifyInfoBoard.userName = hasIdentifyInfo() ? userInfo.name : "";
        identifyInfoBoard.identifyInfoSpannableStingBuilder = toShorString();
        return identifyInfoBoard;
    }

    private SpannableStringBuilder toShorString() {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        stringBuilder.append("Identify Mode: ")
                .append(getIdentityModeDescribe(userInfo.getIdentifyMode()))
                .append("\n");
        stringBuilder.append("Matching Score :");
        int identifyMode = userInfo.getIdentifyMode();
        switch (identifyMode) {
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL:
            case VLPalmSetting.PALM_INTERNAL:
                stringBuilder.append(String.format(Locale.US, "%.2f", userInfo.similarity));
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_IR:
                stringBuilder.append(String.format(Locale.US, "%.2f", userInfo.nirSimilarity));
                break;
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR:
                stringBuilder.append(String.format(Locale.US, "%.2f | %.2f", userInfo.similarity,
                        userInfo.nirSimilarity));
                break;
        }
        stringBuilder.append("\n");
        stringBuilder.append("ImageQuality: ").append(String.valueOf(imageQuality));
        if (Config.instance().isDisplayLivenessInfo && !Float.isNaN(liveScore)) {
            stringBuilder.append("\n").append("LiveScore: ").append(String.valueOf(liveScore));
        }
        return stringBuilder;
    }

    /**
     * -3.0：no palm detect in IR stream
     * -4.0：exception in IR stream when get max palm
     * -5.0：low quality in IR stream
     * -6.0：iou exception
     * -2.0：uvc data exclude liveness info
     * -1.0：liveness function disabled
     * >=0 : liveness score
     */
    public String getLivenessScoreDesc() {
        if (liveScore >= 0) {
            return "Liveness Score:" + liveScore;
        }
        final int ret = (int) liveScore;
        switch (ret) {
            case -3:
                return "No palm detect in IR stream";
            case -2:
                return "uvc data exclude liveness info";
            case -1:
                return "liveness function disabled";
            case -4:
                return "xception in IR stream when get max palm";
            case -5:
                return "low quality in IR stream";
            case -6:
                return "ou exception";
            default:
                return "Unknown";
        }
    }


    public String getIdentityModeDescribe(int identifyMode) {
        switch (identifyMode) {
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL:
                return "VL";
            case VLPalmSetting.PALM_TEMPLATE_MODE_IR:
                return "IR";
            case VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR:
                return "VL & IR";
            case VLPalmSetting.PALM_INTERNAL:
                return "Module";
            default:
                return "Unknown Identify Mode";
        }
    }
}
