package com.armatura.biomodule.register;

import static com.armatura.biomodule.databases.BioDataUtil.getValidLength;
import static com.armatura.biomodule.register.RegisterStatus.FACE_BLUR_TOO_HIGH;
import static com.armatura.biomodule.register.RegisterStatus.FACE_QUALITY_TOO_BAD;
import static com.armatura.biomodule.register.RegisterStatus.FACE_REGISTERED;
import static com.armatura.biomodule.register.RegisterStatus.FACE_ROLL_TOO_BIG;
import static com.armatura.biomodule.register.RegisterStatus.FACE_TOO_SMALL;
import static com.armatura.biomodule.register.RegisterStatus.FACE_YAW_TOO_BIG;
import static com.armatura.biomodule.register.RegisterStatus.JSON_FAILED;
import static com.armatura.biomodule.register.RegisterStatus.NO_DETECT_FACE;
import static com.armatura.biomodule.register.RegisterStatus.SEND_FAILED;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.armatura.biomodule.R;
import com.armatura.biomodule.activity.base.ExApplication;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.common.RegisterOperate;
import com.armatura.biomodule.config.Config;
import com.armatura.biomodule.databases.BioDataUtil;
import com.armatura.biomodule.dialog.CaptureRegisterDialog;
import com.armatura.biomodule.manager.FaceManager;
import com.armatura.biomodule.pojo.common.BioType;
import com.armatura.biomodule.pojo.common.CommonConfigData;
import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.face.register.DetectFaceRequest;
import com.armatura.biomodule.pojo.face.register.DetectFaceResponse;
import com.armatura.biomodule.pojo.face.register.Face;
import com.armatura.biomodule.pojo.face.register.FaceInfo;
import com.armatura.biomodule.util.BitmapUtil;
import com.armatura.biomodule.util.CropUtil;
import com.armatura.biomodule.util.JSONUtil;
import com.armatura.biomodule.util.KotlinExtentKt;
import com.armatura.translib.AMTHidManager;
import com.armatura.uvclib.util.AMTUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class RegisterHelper {

    private final byte[] result = new byte[300 * 1024];
    public static final int CAPTURE = 0;
    public static final int LOCAL_IMAGE = 1;
    public static final int NETWORK = 2;
    private static final String TAG = "RegisterHelper";
    private static final int SHOW_TOAST = 0;
    private static final int DETECT_FACE_SUCCESS = 1;
    private static RegisterHelper registerHelper;
    private IRegisterInfoUpdateCallback callback;
    private WeakReference<FragmentManager> fragmentManagerWrf;
    private final Handler handler_main;
    private final Rect enrollArea = new Rect();

    public void handleRegisterFaceResult(Bitmap originBitmap, int imageSource, String registerName,
                                         String registerResultJson, String filePath,
                                         WeakReference<Context> wrfContext
    ) {
        if (registerResultJson == null) {
            Log.e(TAG, "[onDetectFace]: registerResultJson is invalid");
            return;
        }

        CommonConfigData commonConfigData = JSONUtil.getCommonConfigData(registerResultJson);
        if (commonConfigData != null) {
            DetectFaceResponse detectFaceResponse = JSONUtil.getDetectFaceResponse(commonConfigData);
            if (detectFaceResponse != null) {
                if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
                    String tip = getAppContex().getResources().getString(R.string.reg_face_tip_no_face);
                    Log.e(TAG, String.format("[handleRegisterFaceResult]:[tip]%s,[RegisterName]%s", tip, registerName));
                    showToast(tip, wrfContext.get());
                    //release bitmap
                    AMTUtil.safeReleaseBitmap(originBitmap);
                    return;
                }
                Log.d(TAG, "[handleRegisterFaceResult]: face count=" + detectFaceResponse.faces.size());

                //set first face as max face default
                Face biggestFace = detectFaceResponse.faces.get(0);
                //find max face
                for (Face face : detectFaceResponse.faces) {
                    int w1 = Math.abs(face.faceInfo.rect.right - face.faceInfo.rect.left);
                    int w2 = Math.abs(biggestFace.faceInfo.rect.right - biggestFace.faceInfo.rect.left);
                    if (w1 > w2) {
                        biggestFace = face;
                    }
                }


                byte[] feature = biggestFace.feature.getFeature();

                Rect rectFace = new Rect(biggestFace.faceInfo.rect.left,
                        biggestFace.faceInfo.rect.top,
                        biggestFace.faceInfo.rect.right,
                        biggestFace.faceInfo.rect.bottom);

                if (imageSource == LOCAL_IMAGE || imageSource == CAPTURE) {
                    if (originBitmap == null || wrfContext == null) {
                        Log.e(TAG, "[handleRegisterFaceResult]: cameraRegisterBitmap = null or camreg_ctx = null");
                        return;
                    }
                    CamRegFaceDataUI camRegFaceDataUI = new CamRegFaceDataUI();
                    camRegFaceDataUI.gender = biggestFace.faceInfo.attribute.gender == 0 ? "F" : "M";
                    camRegFaceDataUI.age = biggestFace.faceInfo.attribute.age;
                    camRegFaceDataUI.bitmap = originBitmap;
                    camRegFaceDataUI.faceRect = rectFace;
                    camRegFaceDataUI.feature = feature;
                    camRegFaceDataUI.registerName = registerName;
                    camRegFaceDataUI.imageSource = imageSource;
                    camRegFaceDataUI.filePath = filePath;
                    camRegFaceDataUI.wrfContext = wrfContext;
                    Log.i(TAG, String.format("[handleRegisterFaceResult]: %s register success", registerName));
                    handler_main.obtainMessage(DETECT_FACE_SUCCESS, camRegFaceDataUI).sendToTarget();
                }
            } else {
                AMTUtil.safeReleaseBitmap(originBitmap);
                String tip = commonConfigData.detail;
                showToast(tip, wrfContext.get());
                Log.e(TAG, String.format("onDetectFace: %s,%s", tip, registerName));
                Log.i(TAG, "[handleRegisterFaceResult]: " + registerResultJson);
            }
        } else {
            AMTUtil.safeReleaseBitmap(originBitmap);
            String tip = getAppContex().getResources().getString(R.string.reg_face_tip_face_detect_fail);
            Log.e(TAG, String.format("onDetectFace: %s,%s", tip, registerName));
            Log.i(TAG, "[handleRegisterFaceResult]: " + registerResultJson);
            showToast(tip, wrfContext.get());
        }
    }

    private void showToast(String tip, Context context) {
//        ToastMessage toastMessage = new ToastMessage(tip, context);
//        handler_main.obtainMessage(SHOW_TOAST, toastMessage).sendToTarget();
    }

    private RegisterHelper() {
        handler_main = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                int what = msg.what;
                if (what == SHOW_TOAST) {
                    ToastMessage toastMessage = (ToastMessage) msg.obj;
                    if (toastMessage.context != null) {
                        KotlinExtentKt.toastAnywhere(toastMessage.message);
                    }
                } else if (what == DETECT_FACE_SUCCESS) {
                    final CamRegFaceDataUI camRegFaceDataUI = (CamRegFaceDataUI) msg.obj;
                    final Context context = camRegFaceDataUI.wrfContext.get();
                    final UserInfo identifyUserInfo = BioDataUtil.identifyUserInfoByFaceFeature(camRegFaceDataUI.feature);
                    if (identifyUserInfo != null) {
                        //release bitmap
                        AMTUtil.safeReleaseBitmap(camRegFaceDataUI.bitmap);
//                        if (camRegFaceDataUI.filePath != null) {
//                            File file = new File(camRegFaceDataUI.filePath);
//                            boolean delete = file.delete();
//                        }
                        if (context != null) {
                            String tip = String.format(context.getString(R.string.reg_face_tip_already_reg), camRegFaceDataUI.registerName, identifyUserInfo.similarity, identifyUserInfo.name);
                            showToast(tip, context);
                        }
                    } else {
                        //crop avatar
                        final Bitmap bitmap_face = CropUtil.cropAvatar(camRegFaceDataUI.bitmap, camRegFaceDataUI.faceRect);
                        //if register name is not null then it's batch add face else it's add a face
                        if (TextUtils.isEmpty(camRegFaceDataUI.registerName)) {
                            CaptureRegisterDialog captureRegisterDialog
                                    = CaptureRegisterDialog.Companion.newInstance(camRegFaceDataUI.imageSource != LOCAL_IMAGE,
                                    bitmap_face,
                                    new CaptureRegisterDialog.CaptureRegisterDialogClickListener() {
                                        @Override
                                        public void onClickConfirm(@NotNull DialogFragment dialogFragment, @NotNull String name, @NotNull String userPin) {
                                            UserInfo userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userPin);
                                            long id = -1;
                                            if (userInfo == null) {
                                                if (name.isEmpty()) {
                                                    //release bitmap
                                                    AMTUtil.safeReleaseBitmap(camRegFaceDataUI.bitmap);
                                                    showToast(context.getString(R.string.reg_face_tip_empty_name), context);
                                                    return;
                                                }

                                                userInfo = new UserInfo();
                                                userInfo.userId = userPin;
                                                userInfo.name = name;
                                                userInfo.age = camRegFaceDataUI.age;
                                                userInfo.gender = camRegFaceDataUI.gender;

                                                userInfo.bUpdate = true;
                                                userInfo.faceFeature = camRegFaceDataUI.feature;
                                                id = BioDataUtil.instance().insertUserInfo(userInfo);
                                            } else {
                                                if (userInfo.faceFeature != null) {
                                                    //release bitmap
                                                    AMTUtil.safeReleaseBitmap(camRegFaceDataUI.bitmap);
                                                    if (context != null) {
                                                        String tip = String.format(context.getString(R.string.reg_user_pin_already_regist), userPin);
                                                        showToast(tip, context);
                                                    }
                                                    return;
                                                }
                                                userInfo.name = name;
                                                userInfo.age = camRegFaceDataUI.age;
                                                userInfo.gender = camRegFaceDataUI.gender;
                                                userInfo.bUpdate = true;
                                                userInfo.faceFeature = camRegFaceDataUI.feature;
                                                id = BioDataUtil.instance().updateUserInfo(userInfo);
                                            }

                                            if (id == -1) {
                                                if (context != null) {
                                                    String tip = context.getString(R.string.reg_face_tip_err_database);
                                                    showToast(tip, context);
                                                }

                                                //release bitmap
                                                AMTUtil.safeReleaseBitmap(camRegFaceDataUI.bitmap);
                                                AMTUtil.safeReleaseBitmap(bitmap_face);
                                            } else {
                                                userInfo.id = id;
                                                //save avatar
                                                File file_face = new File(Config.getHostAvatarPath() + userPin + ".jpg");
                                                AMTUtil.SaveBitmap(bitmap_face, file_face);

                                                if (context != null) {
                                                    String tip = String.format(context.getString(R.string.reg_face_tip_reg_ok_name), name, userPin);
                                                    showToast(tip, context);
                                                }

                                                AMTUtil.safeReleaseBitmap(bitmap_face);
                                                if (callback != null) {
                                                    callback.onRegisterInfoUpdateComplete();
                                                }
                                            }

                                            dialogFragment.dismiss();
                                        }

                                        @Override
                                        public void onClickCancel(@NotNull DialogFragment dialogFragment, @NotNull String name, @NotNull String userPin) {
                                            dialogFragment.dismiss();
                                        }

                                        @Override
                                        public void onClickRecapture(@NotNull DialogFragment dialogFragment, @NotNull String name, @NotNull String userPin) {
                                            dialogFragment.dismiss();
                                        }
                                    });
                            captureRegisterDialog.show(fragmentManagerWrf.get());
                        } else {
                            //crop avatar
                            final Bitmap avatar = CropUtil.cropAvatar(camRegFaceDataUI.bitmap, camRegFaceDataUI.faceRect);
                            try {
                                //batch add face
                                UserInfo userInfo = new UserInfo();
                                if (!camRegFaceDataUI.registerName.isEmpty()) {

                                    userInfo.age = camRegFaceDataUI.age;
                                    userInfo.gender = camRegFaceDataUI.gender;
                                    userInfo.name = camRegFaceDataUI.registerName;
                                    String userName = userInfo.name;
                                    userInfo.userId = userName.substring(0, userName.lastIndexOf("."));
                                    userInfo.bUpdate = true;
                                    userInfo.faceFeature = camRegFaceDataUI.feature;
                                    long id = BioDataUtil.instance().insertUserInfo(userInfo);
                                    if (id == -1) {
                                        if (context != null) {
                                            String tip = context.getString(R.string.reg_face_tip_err_database);
                                            showToast(tip, context);
                                        }
                                    } else {
                                        userInfo.id = id;
                                        File file_face = new File(Config.getHostAvatarPath() + userInfo.userId + ".jpg");
                                        AMTUtil.SaveBitmap(bitmap_face, file_face);
                                        if (context != null) {
                                            String tip = String.format(context.getString(R.string.reg_face_tip_reg_ok_name), userInfo.name, userInfo.userId);
                                            showToast(tip, context);
                                        }
                                        AMTUtil.safeReleaseBitmap(bitmap_face);
                                        if (callback != null) {
                                            callback.onRegisterInfoUpdateComplete();
                                        }
                                    }
                                }
                            } finally {
                                AMTUtil.safeReleaseBitmap(camRegFaceDataUI.bitmap);
                                AMTUtil.safeReleaseBitmap(avatar);
                            }
                        }
                    }
                }
            }
        };
    }

    public static RegisterHelper instance() {
        if (registerHelper == null) {
            registerHelper = new RegisterHelper();
        }
        return registerHelper;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        if (fragmentManagerWrf != null) {
            fragmentManagerWrf.clear();
            fragmentManagerWrf = null;
        }

        fragmentManagerWrf = new WeakReference<>(fragmentManager);
    }


    public void setCallback(IRegisterInfoUpdateCallback callback) {
        this.callback = callback;
    }

    public void deleteAllUser() {
        BioDataUtil.instance().deleteAll();
    }

    /**
     * delete from database and memory
     *
     * @param userPin user pin
     * @return return true if success
     */
    public boolean deleteUserFromDatabasesAndMemory(String userPin) {
        boolean bok = false;
        synchronized (BioDataUtil.userFaces_all_List) {
            for (Iterator<UserInfo> iterator = BioDataUtil.userFaces_all_List.iterator(); iterator.hasNext(); ) {
                UserInfo userFace = iterator.next();
                if (userFace.userId.equals(userPin)) {
                    if (BioDataUtil.instance().deleteBioDataByUserPin(userFace.userId)) {
                        bok = true;
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        return bok;
    }

    public void delete_users(ArrayList<Integer> ids) {
    }

    public void registerByBitmap(Bitmap bitmap, int imageSource, Context context) {
        registerByBitmap(bitmap, imageSource, context, null, null);
    }

    public String registerByBitmap(Bitmap bitmap, int imageSource, Context context,
            /*only for batch*/String userName, String filePath) {
        WeakReference<Context> wrfContext = new WeakReference<>(context);

        DetectFaceRequest detectFaceRequest = new DetectFaceRequest();
        Image image = new Image();
        image.width = bitmap.getWidth();
        image.height = bitmap.getHeight();
        image.bioType = BioType.FACE;
        image.format = Image.Format.JPEG;
        image.data = BitmapUtil.bitmapToBase64(bitmap);
        detectFaceRequest.setImage(image);
        detectFaceRequest.setIsNeedFaceInfo(true);
        detectFaceRequest.setIsNeedPicture(false);
        detectFaceRequest.setIsNeedFeature(true);
        String data = JSONUtil.getJsonString(detectFaceRequest);

        int[] size = new int[1];
        String jsonResult = null;
        Arrays.fill(result, (byte) 0);
        int ret = AMTHidManager.instance().registerFace(data.getBytes(), result, size);
        if (ret == 0) {
            jsonResult = new String(result, 0, size[0]);
        } else {
            AMTUtil.safeReleaseBitmap(bitmap);
            Log.e(TAG, "[registerByCam]: ret = " + ret);
        }
        handleRegisterFaceResult(bitmap, imageSource, userName, jsonResult, filePath, wrfContext);
        return jsonResult;
    }

    public synchronized void registerByBitmap(Bitmap bitmap, String userId, int operate,
                                              @NotNull RegisterStatusCallback callback, boolean isNeedFilter) {
        try {
            DetectFaceRequest detectFaceRequest = new DetectFaceRequest();
            DetectFaceRequest.Filter filter = new DetectFaceRequest.Filter();
            filter.widthMinValue = 10;
            filter.heightMinValue = 10;
            detectFaceRequest.setFilter(filter);
            if (bitmap != null && !bitmap.isRecycled()) {
                Image image = new Image();
                image.bioType = BioType.FACE;
                image.format = Image.Format.JPEG;
                image.data = BitmapUtil.bitmapToBase64(bitmap);
                detectFaceRequest.setImage(image);
            }
            detectFaceRequest.setIsNeedFaceInfo(true);
            detectFaceRequest.setIsNeedPicture(false);
            detectFaceRequest.setIsNeedFeature(true);
            String data = JSONUtil.getJsonString(detectFaceRequest);

            String jsonResult = null;
            byte[] result = new byte[400 * 1024];
            int[] size = new int[]{result.length};
            int ret = AMTHidManager.instance().registerFace(data.getBytes(), result, size);
            if (ret == 0) {
                jsonResult = new String(result, 0, size[0]);
            } else {
                Log.i(TAG, "registerByBitmap: registerFace ret=" + ret);
                callback.onStatusCallback(SEND_FAILED);
                return;
            }
            CommonConfigData commonConfigData = JSONUtil.getCommonConfigData(jsonResult);
            if (commonConfigData != null) {
                Log.i(TAG, "registerByBitmap: " + commonConfigData);
                DetectFaceResponse detectFaceResponse = JSONUtil.getDetectFaceResponse(commonConfigData);
                if (detectFaceResponse != null) {
                    if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
                        callback.onStatusCallback(NO_DETECT_FACE);
                        return;
                    }

                    //set first face as max face default
                    Face biggestFace = detectFaceResponse.faces.get(0);
                    //find max face
                    for (Face face : detectFaceResponse.faces) {
                        int w1 = Math.abs(face.faceInfo.rect.right - face.faceInfo.rect.left);
                        int w2 = Math.abs(biggestFace.faceInfo.rect.right - biggestFace.faceInfo.rect.left);
                        if (w1 > w2) {
                            biggestFace = face;
                        }
                    }

                    if (isNeedFilter) {
                        Log.i(TAG, "registerByBitmap: " + biggestFace.faceInfo.toString());
                        FaceInfo faceInfo = biggestFace.faceInfo;

                        Rect faceRect = faceInfo.rect;
                        if (bitmap != null) {
                            calEnrollArea(enrollArea, bitmap.getWidth(), bitmap.getHeight());
                            boolean isBigFace = faceRect.width() > enrollArea.width() && faceRect.width() < bitmap.getWidth()
                                    && faceRect.top >= enrollArea.top && faceRect.bottom <= bitmap.getHeight()
                                    && faceRect.right > 0 && faceRect.right < bitmap.getWidth();
                            if (!isBigFace && !enrollArea.contains(faceRect)) {
                                //not big face and not in enroll area
                                callback.onStatusCallback(RegisterStatus.FACE_NOT_IN_ENROLL_AREA);
                                return;
                            }
                        }

                        //face size filter
                        int face_w = faceRect.right - faceRect.left;
                        if (face_w < Config.instance().faceWidthMinSize) {
                            callback.onStatusCallbackEx(FACE_TOO_SMALL, Config.instance().faceWidthMinSize, face_w);
                            return;
                        }

                        int face_h = faceRect.bottom - faceRect.top;
                        if (face_h < Config.instance().faceHeightMinSize) {
                            callback.onStatusCallbackEx(FACE_TOO_SMALL, Config.instance().faceHeightMinSize, face_h);
                            return;
                        }

                        if (faceInfo.score < Config.instance().faceRegistrationQuality) {
                            callback.onStatusCallbackEx(FACE_QUALITY_TOO_BAD,
                                    Config.instance().faceRegistrationQuality, faceInfo.score);
                            return;
                        }

                        if (biggestFace.blur > Config.instance().faceBlurThreshold) {
                            callback.onStatusCallbackEx(FACE_BLUR_TOO_HIGH,
                                    Config.instance().faceBlurThreshold, biggestFace.blur);
                            return;
                        }

                        if (faceInfo.pose.yaw > Config.instance().faceYawMaxThreshold
                                || faceInfo.pose.yaw < Config.instance().faceYawMinThreshold) {
                            callback.onStatusCallbackEx(FACE_YAW_TOO_BIG,
                                    Config.instance().faceYawMaxThreshold, faceInfo.pose.yaw);
                            return;
                        }

                        if (faceInfo.pose.roll > Config.instance().faceRollMaxThreshold
                                || faceInfo.pose.roll < Config.instance().faceRollMinThreshold) {
                            callback.onStatusCallbackEx(FACE_ROLL_TOO_BIG,
                                    Config.instance().faceRollMaxThreshold, faceInfo.pose.roll);
                            return;
                        }

                    }

                    byte[] feature = biggestFace.feature.getFeature();
                    byte[] id = new byte[40];
                    float[] score = new float[1];
                    if (FaceManager.getInstance().dbIdentify(feature, id, score) == 0) {
                        if (score[0] > Config.instance().faceIdentifyThreshold) {
                            if (operate == RegisterOperate.ADD) {
                                callback.onStatusCallbackEx(FACE_REGISTERED, Config.instance().faceIdentifyThreshold, score[0]);
                                return;
                            }
                            if (operate == RegisterOperate.UPDATE) {
                                String identifyUserId = new String(id, 0, getValidLength(id));
                                if (!identifyUserId.equals(userId)) {
                                    callback.onStatusCallbackEx(FACE_REGISTERED, Config.instance().faceIdentifyThreshold, score[0]);
                                    return;
                                }
                            }
                        }
                    }
                    callback.onSuccess(feature);
                } else {
                    callback.toastMessage(commonConfigData.detail);
                    callback.onStatusCallback(JSON_FAILED);
                }
            } else {
                callback.onStatusCallback(JSON_FAILED);
            }

        } finally {
            AMTUtil.safeReleaseBitmap(bitmap);
        }

    }


    public synchronized Bitmap cropFace(Bitmap bitmap) {
        Bitmap avatar = null;
        try {
            String jsonResult = null;
            DetectFaceRequest detectFaceRequest = new DetectFaceRequest();
            Image image = new Image();
            image.bioType = BioType.FACE;
            image.format = Image.Format.JPEG;
            image.data = BitmapUtil.bitmapToBase64(bitmap);
            detectFaceRequest.setImage(image);
            detectFaceRequest.setIsNeedFaceInfo(true);
            detectFaceRequest.setIsNeedPicture(false);
            detectFaceRequest.setIsNeedFeature(true);
            String data = JSONUtil.getJsonString(detectFaceRequest);


            byte[] result = new byte[400 * 1024];
            int[] size = new int[1];
            int ret = AMTHidManager.instance().registerFace(data.getBytes(), result, size);
            if (ret == 0) {
                jsonResult = new String(result, 0, size[0]);

                DetectFaceResponse detectFaceResponse = JSONUtil.getDetectFaceResponse(jsonResult);

                if (detectFaceResponse != null) {
                    if (detectFaceResponse.faces == null || detectFaceResponse.faces.isEmpty()) {
                        return null;
                    }

                    //set first face as max face default
                    //set first face as max face default
                    Face biggestFace = detectFaceResponse.faces.get(0);
                    //find max face
                    for (Face face : detectFaceResponse.faces) {
                        int w1 = Math.abs(face.faceInfo.rect.right - face.faceInfo.rect.left);
                        int w2 = Math.abs(biggestFace.faceInfo.rect.right - biggestFace.faceInfo.rect.left);
                        if (w1 > w2) {
                            biggestFace = face;
                        }
                    }

                    FaceInfo faceInfo = biggestFace.faceInfo;
                    //face size filter
                    int face_w = faceInfo.rect.right - faceInfo.rect.left;
                    int face_h = faceInfo.rect.bottom - faceInfo.rect.top;
                    if (face_w < 100 || face_h < 100) {
                        return null;
                    }


                    Rect rectFace = new Rect(biggestFace.faceInfo.rect.left,
                            biggestFace.faceInfo.rect.top,
                            biggestFace.faceInfo.rect.right,
                            biggestFace.faceInfo.rect.bottom);
                    avatar = CropUtil.cropAvatar(bitmap, rectFace);
                }
            }
        } finally {
            AMTUtil.safeReleaseBitmap(bitmap);
        }
        return avatar;
    }

    private Context getAppContex() {
        return ExApplication.instance().getApplicationContext();
    }

    private void calEnrollArea(Rect enrollArea, int width, int height) {
        int fixRectLeft = width / 6;
        int fixRectRight = fixRectLeft * 5;
        int fixRectTop = height / 5;
        int fixRectBottom = fixRectTop * 4;
        enrollArea.set(fixRectLeft, fixRectTop, fixRectRight, fixRectBottom);
    }

    public interface IRegisterInfoUpdateCallback {
        void onRegisterInfoUpdateComplete();
    }

    final static class CamRegFaceDataUI {
        String gender;
        int age = -1;
        Bitmap bitmap;
        Rect faceRect;
        byte[] feature;
        String registerName;
        int imageSource;
        String filePath;
        WeakReference<Context> wrfContext;
    }

    final static class ToastMessage {
        final String message;
        final Context context;

        public ToastMessage(String message, Context context) {
            this.message = message;
            this.context = context;
        }
    }
}
