package com.armatura.biomodule.register;

import com.armatura.biomodule.R;

/**
 * Created by Magic on 2020/9/17
 */
public enum RegisterStatus {
    NO_RESULT(R.string.hid_no_result),
    SEND_FAILED(R.string.hid_sned_failed),
    NO_DETECT_FACE(R.string.detect_no_face),
    DETECT_TOO_MUCH_FACE(R.string.detect_too_much_face),
    FACE_TOO_SMALL(R.string.face_too_small),
    FACE_QUALITY_TOO_BAD(R.string.face_quality_too_bad),
    NEEDS_TWO_FEATURE(R.string.needs_two_feature),
    HID_FAILED(R.string.hid_failed),
    STEP1_NEED_FIRST(R.string.step_1_needs_fisrt),
    STEP2_NEED_EXECUTE(R.string.step_2_needs_execute),

    FACE_BLUR_TOO_HIGH(R.string.face_quality_too_bad),
    FACE_YAW_TOO_BIG(R.string.face_yaw_pose_too_big),
    FACE_PITCH_TOO_BIG(R.string.face_pitch_pose_too_big),
    FACE_ROLL_TOO_BIG(R.string.face_roll_pose_too_big),
    FACE_REGISTERED(R.string.face_already_registered),
    FACE_ALREADY_IN_MODULE(R.string.face_already_in_module),
    PALM_NOT_DETECT(R.string.not_detect_palm),
    PALM_MERGE_FAILED(R.string.merge_palm_failed),
    PALM_REGISTERED(R.string.palm_already_register),
    PALM_ALREADY_IN_MODULE(R.string.palm_already_in_module),
    DO_NOT_MOVE_PALM(R.string.palm_do_not_move),
    DO_NOT_MOVE_FACE(R.string.face_do_not_move),
    PALM_NOT_DETECT_CLICK_ADD_CONTINUE(R.string.no_palm_detect_click_add_continue),
    NO_DETECT_FACE_CLICK_ADD_CONTINUE(R.string.no_face_detect_move_face_and_click_add),
    KEEP_YOUR_POSTURE(R.string.keep_your_posture),
    CACHE_ID_IS_READY(R.string.cache_id_ready_click_confirm),
    PREPARE_TO_ENROLL_FACE(R.string.prepare_to_enroll_face),
    PREPARE_TO_ENROLL_PALM(R.string.prepare_to_enroll_palm),
    JSON_FAILED(R.string.json_analyse_failed),
    FACE_NOT_IN_ENROLL_AREA(R.string.tips_pls_move_face_to_enroll_area),
    PALM_NOT_IN_ENROLL_AREA(R.string.tips_pls_move_palm_to_enroll_area),
    REGISTING(R.string.registering),
    ENROLL_NEED_SAME_PALM(R.string.enroll_need_same_palm),
    ENROLL_NEED_SAME_FACE(R.string.enroll_need_same_face),
    FACE_FAKE_ATTACK(R.string.face_suspected_fake_attack),
    PALM_FAKE_ATTACK(R.string.palm_suspected_fake_attack);


    public final int resId;

    RegisterStatus(int resId) {
        this.resId = resId;
    }

    public static int getStatusString(RegisterStatus registerStatus) {
        return registerStatus.resId;
//        switch (registerStatus) {
//            case NO_RESULT:
//                return NO_RESULT.resId;
//            case SEND_FAILED:
//                return SEND_FAILED.resId;
//            case NO_DETECT_FACE:
//                return NO_DETECT_FACE.resId;
//            case FACE_TOO_SMALL:
//                return FACE_TOO_SMALL.resId;
//            case FACE_QUALITY_TOO_BAD:
//                return FACE_QUALITY_TOO_BAD.resId;
//            case FACE_ROLL_TOO_BIG:
//                return FACE_ROLL_TOO_BIG.resId;
//            case FACE_PITCH_TOO_BIG:
//                return FACE_PITCH_TOO_BIG.resId;
//            case FACE_YAW_TOO_BIG:
//                return FACE_YAW_TOO_BIG.resId;
//            case FACE_REGISTERED:
//                return FACE_REGISTERED.resId;
//            case PALM_REGISTERED:
//                return PALM_REGISTERED.resId;
//            case JSON_FAILED:
//                return JSON_FAILED.resId;
//            case PALM_NOT_DETECT:
//                return PALM_NOT_DETECT.resId;
//            case PALM_MERGE_FAILED:
//                return PALM_MERGE_FAILED.resId;
//            case KEEP_YOUR_POSTURE:
//                return KEEP_YOUR_POSTURE.resId;
//            case CACHE_ID_IS_READY:
//                return CACHE_ID_IS_READY.resId;
//            case PALM_NOT_DETECT_CLICK_ADD_CONTINUE:
//                return PALM_NOT_DETECT_CLICK_ADD_CONTINUE.resId;
//            case NO_DETECT_FACE_CLICK_ADD_CONTINUE:
//                return NO_DETECT_FACE_CLICK_ADD_CONTINUE.resId;
//            case PREPARE_TO_ENROLL_FACE:
//                return PREPARE_TO_ENROLL_FACE.resId;
//            case PREPARE_TO_ENROLL_PALM:
//                return PREPARE_TO_ENROLL_PALM.resId;
//            case REGISTING:
//                return REGISTING.resId;
//            case ENROLL_NEED_SAME_PALM:
//                return ENROLL_NEED_SAME_PALM.resId;
//            case PALM_ALREADY_IN_MODULE:
//                return PALM_ALREADY_IN_MODULE.resId;
//            case FACE_ALREADY_IN_MODULE:
//                return FACE_ALREADY_IN_MODULE.resId;
//            case FACE_NOT_IN_ENROLL_AREA:
//                return FACE_NOT_IN_ENROLL_AREA.resId;
//            case FACE_FAKE_ATTACK:
//                return FACE_FAKE_ATTACK.resId;
//            case PALM_FAKE_ATTACK:
//                return PALM_FAKE_ATTACK.resId;
//            case PALM_NOT_IN_ENROLL_AREA:
//                R
//            default:
//                throw new RuntimeException("Unknown state: " + registerStatus.name());
//        }
    }
}
