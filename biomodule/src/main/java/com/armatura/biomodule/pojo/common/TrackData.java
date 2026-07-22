package com.armatura.biomodule.pojo.common;

import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.armatura.biomodule.config.Config;

import java.util.Locale;

/**
 * Created by Magic on 2020/9/28
 */
public class TrackData {
    public float blur;
    public int label;
    public Landmark landmark = new Landmark();
    public final FacePose pose = new FacePose();
    public final Rect rect = new Rect();
    public String snapType;                         //"enter", "leave", "single", "timing"
    public int trackId;

    @NonNull
    @Override
    public String toString() {
        return "TrackData{" +
                "blur=" + blur +
                ", label=" + label +
                ", pose=" + pose +
                ", rect=" + rect +
                ", snapType='" + snapType + '\'' +
                ", trackId=" + trackId +
                '}';
    }

    public String toShortString() {
        if (Config.instance().isShowFacePose) {
            return String.format(Locale.US, "Blur: %.3f\nY: %.1f P: %.1f R: %.1f", blur,
                    pose.yaw, pose.pitch, pose.roll);
        } else {
            return "";
        }
    }
}
