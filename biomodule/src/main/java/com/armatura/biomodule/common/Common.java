package com.armatura.biomodule.common;

import androidx.annotation.NonNull;

import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.camera.biodata.FaceData;
import com.armatura.biomodule.pojo.common.LiveData;
import com.armatura.biomodule.pojo.face.recognize.IdentifyInfo;

public class Common {
    public static IdentifyState state = IdentifyState.STOP;


    public enum FaceLiveMode {
        DISABLE(0, "disable"),
        SINGLE_LENS_LIVENESS(1, "single-lens liveness"),
        DUAL_LENS_LIVENESS(2, "dual-lens liveness"),
        OLD_DUAL_LENS_LIVENESS(11, "dual-lens liveness"),
        OLD_ONLY_IR_LIVENESS(12, "IR liveness"),
        OLD_ONLY_VL_LIVENESS(13, "RGB liveness");


        private final int code;
        private final String description;

        FaceLiveMode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static String getStringByCode(int code) {
            for (FaceLiveMode mode : FaceLiveMode.values()) {
                if (mode.getCode() == code) {
                    return mode.getDescription();
                }
            }
            return "" + code;
        }

        public String getDescription() {
            return description;
        }

        public int getCode() {
            return code;
        }

        @NonNull
        @Override
        public String toString() {
            return code + ": " + description;
        }
    }

    public enum FaceLiveStatus {
        DISABLE(-1, "disable"),
        FAIL(1, "fake"),
        PASS(2, "pass"),
        NOT_SUPPORT(10, "liveness detection mode not support"),
        NO_NIR_IMAGE_DATA(11, "no nir image data"),
        NIR_IMAGE_ROTATE_FAILED(12, "nir image rotate failed"),
        NIR_IMAGE_CONVERT_FAILED(13, "nir image convert failed"),
        NO_FACE_DETECT_IN_NIR(30, "no face detect in nir"),
        IOU_EXCEPTION(31, "IOU exception");
        //OTHER

        private final int code;
        private final String description;

        FaceLiveStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static String getStringByCode(int code) {
            for (FaceLiveStatus error : FaceLiveStatus.values()) {
                if (error.getCode() == code) {
                    return error.getDescription();
                }
            }
            return "No Liveness Info";
        }

        public String getDescription() {
            return description;
        }

        public int getCode() {
            return code;
        }

        @NonNull
        @Override
        public String toString() {
            return code + ": " + description;
        }
    }


    public enum PalmLiveStatus {
        DISABLE(-1, "disable"),
        EXCLUDE_LIVENESS_INFO(-2, "exclude liveness info"),
        NO_PALM_DETECT(-3, "no palm detect"),
        EXCEPTION_GET_MAX_PALM(-4, "exception in IR stream when get max palm"),
        LOW_QUALITY(-5, "low quality"),
        IOU_EXCEPTION(-6, "IOU exception");
        //OTHER

        private final int code;
        private final String description;

        PalmLiveStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public static String getStringByCode(int code) {
            for (FaceLiveStatus error : FaceLiveStatus.values()) {
                if (error.getCode() == code) {
                    return error.getDescription();
                }
            }
            return "Algorithm Error:" + code;
        }

        public String getDescription() {
            return description;
        }

        public int getCode() {
            return code;
        }

        @NonNull
        @Override
        public String toString() {
            return code + ": " + description;
        }
    }

    public static class RecognizedFaceData {
        public int ori_w;
        public int ori_h;
        public long frameindex;
        public final FaceData faceData;

        public boolean isRecognized;
        public boolean bHandled;
        public UserInfo userFace;
        public IdentifyInfo identifyInfo;
        public LiveData liveData;
        public boolean isPassLiveness;

        public RecognizedFaceData() {
            isRecognized = false;
            isPassLiveness = true;
            userFace = null;
            faceData = new FaceData();
        }
    }

}
