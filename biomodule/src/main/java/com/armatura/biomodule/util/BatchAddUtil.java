package com.armatura.biomodule.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.armatura.biomodule.bean.AMTResult;
import com.armatura.biomodule.bean.RegisterPhotoInfo;
import com.armatura.biomodule.bean.UserInfo;
import com.armatura.biomodule.databases.BioDataUtil;
import com.armatura.biomodule.pojo.common.BioType;
import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.palm.PalmFeature;
import com.armatura.biomodule.pojo.palm.register.DetectPalmRequest;
import com.armatura.biomodule.pojo.palm.register.DetectPalmResponse;
import com.armatura.biomodule.pojo.palm.register.DetectPalmResult;
import com.armatura.constant.ErrorCode;
import com.armatura.constant.ManageType;
import com.armatura.constant.StatusCode;
import com.armatura.internaldata.util.ModuleBioDataUtil;
import com.armatura.translib.AMTHidManager;
import com.armatura.uvclib.util.AMTUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import kotlin.Pair;


public class BatchAddUtil {
    private static final String TAG = "BatchAddUtil";

    public interface BatchAddListener {
        void OnFacePhotoAvailable(RegisterPhotoInfo registerPhotoInfo);

        void onBatchAddProgress(String progressMsg);

        /**
         * @param error   -1:file not exist
         *                -2:begin transaction failed
         *                -3:import person encounter problem
         *                -4:commit transaction failed
         *                -5:An error was encountered during the import process
         * @param message error detail
         */
        void onError(int error, String message);
    }


    public static void batchAddFace(String path, BatchAddListener batchAddListener) {
        addFaceByPhoto(path, batchAddListener);
    }

    private static void addFaceByPhoto(String path, BatchAddListener batchAddListener) {
        File wangLongPhoto = new File(path);
        File[] files = wangLongPhoto.listFiles();

        if (files != null && files.length > 0) {
            Log.i(TAG, String.format("There is %d files need to be register", files.length));

            for (File file : files) {
                if (file.isDirectory()) {
                    addFaceByPhoto(file.getAbsolutePath(), batchAddListener);
                    continue;
                }
                if (!file.getName().endsWith(".jpg")
                        && !file.getName().endsWith(".jpeg")
                        && !file.getName().endsWith(".png")
                        && !file.getName().endsWith(".JPG")) {
                    Log.w(TAG, "[Batch Add Face]: Not a valid format,name = " + file.getName());
                    continue;
                }

                String userPin = file.getName().substring(0, file.getName().lastIndexOf("."));
                if (BioDataUtil.isBioTemplateExist(userPin, true, false)) {
                    Log.i(TAG, String.format("[Batch Add Face]: %s already added", file.getName()));
                    continue;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                options.inJustDecodeBounds = false;
                Bitmap srcBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                //make sure bitmap is smaller than 720*1280
                Bitmap finalBitmap = null;
                if (options.outWidth > 720 || options.outHeight > 1280) {
                    finalBitmap = scaleBitmapIfNeeded(srcBitmap);
                    srcBitmap.recycle();
                } else {
                    finalBitmap = srcBitmap;
                }

                try {
                    Log.i(TAG, "[Batch add Face]: " + file.getName());
                    RegisterPhotoInfo registerPhotoInfo = new RegisterPhotoInfo();
                    registerPhotoInfo.bitmap = finalBitmap;
                    registerPhotoInfo.fileName = file.getName();
                    registerPhotoInfo.filePath = file.getAbsolutePath();
                    Log.i(TAG, "[Batch add Face] start ," + file.getAbsolutePath());
                    if (batchAddListener != null) {
                        batchAddListener.OnFacePhotoAvailable(registerPhotoInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static int byte2int(byte[] res) {
        return (res[0] & 0xff) | ((res[1] << 8) & 0xff00) | ((res[2] << 24) >>> 8) | (res[3] << 24);
    }

    public static void batchAddPalmFromFile(String testPalmDBPath, BatchAddListener batchAddListener) {
        if (TextUtils.isEmpty(testPalmDBPath)) {
            return;
        }
        File testPalmDBFile = new File(testPalmDBPath);
        if (!testPalmDBFile.exists()) {
            return;
        }
        long totalSize = testPalmDBFile.length();

        int pos = 0;
        int structLength = 128 + 4 + 4 + 2048;
        byte[] structUnit = new byte[structLength];
        int userCount = (int) (totalSize / structLength);
        int progress = 0;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(testPalmDBFile)))) {
            //each time ,read 500 yummy user
            byte[] buffer = new byte[structLength * 500];
            int readBytes;
            while ((readBytes = dis.read(buffer, 0, buffer.length)) > 0) {
                if (readBytes < structLength) {
                    break;
                }
                pos = 0;
                while (pos < readBytes) {
                    UserInfo userInfo = null;

                    System.arraycopy(buffer, pos, structUnit, 0, structLength);
                    byte[] idBytes = Arrays.copyOf(structUnit, 128);
                    int validLength = BioDataUtil.getValidLength(idBytes);
                    String palmId = new String(idBytes, 0, validLength, StandardCharsets.UTF_8);
                    String[] combineArray = palmId.split("_");
                    if (combineArray.length >= 3) {
                        userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(getPureName(combineArray[0]));
                        if (userInfo == null) {
                            userInfo = new UserInfo();
                        }
                        userInfo.userId = getPureName(combineArray[0]);
                        userInfo.name = getPureName(combineArray[1]);
                        try {
                            userInfo.avatarIndex = Integer.parseInt(combineArray[2]);
                        } catch (NumberFormatException e) {
                            userInfo.avatarIndex = 0;
                        }
                    } else {
                        userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(getPureName((palmId)));
                        if (userInfo == null) {
                            userInfo = new UserInfo();
                        }
                        userInfo.userId = getPureName(palmId);
                        userInfo.name = getPureName(palmId);
                    }
                    userInfo.personId = userInfo.userId;
                    byte[] sizeBytes = Arrays.copyOfRange(structUnit, 128, 132);
                    int size = byte2int(sizeBytes);
                    byte[] typeBytes = Arrays.copyOfRange(structUnit, 132, 136);
                    int type = byte2int(typeBytes);
                    byte[] features = Arrays.copyOfRange(structUnit, 136, 136 + size);
                    if (type == 9) {
                        userInfo.palmFeature2 = features;
                    } else {
                        userInfo.palmFeature1 = features;
                    }
                    userInfo.palm = 1;

                    BioDataUtil.instance().insertOrUpdateUserInfo(userInfo);

                    pos += structLength;

                    progress++;
                    if (progress % 10 == 0) {
                        if (batchAddListener != null) {
                            batchAddListener.onBatchAddProgress("Progress:" + progress + "/" + userCount);
                        }
                    }
                }
            }
        } catch (IOException ignore) {
        }
    }


    private static String getPureName(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            name = name.substring(0, dotIndex);
        }
        return name;
    }

    public static void batchAddPalmAndPalmVeinFromFile(String testPalmDBPath, BatchAddListener batchAddListener) {
        if (TextUtils.isEmpty(testPalmDBPath)) {
            return;
        }
        File testPalmDBFile = new File(testPalmDBPath);
        if (!testPalmDBFile.exists()) {
            return;
        }
        long totalSize = testPalmDBFile.length();

        int pos = 0;
        int structLength = 128 + 4 + 4 + 2048 + 4 + 4 + 2048;
        byte[] structUnit = new byte[structLength];
        int userCount = (int) (totalSize / structLength);
        int progress = 0;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(testPalmDBFile)))) {
            //each time ,read 500 yummy user
            byte[] buffer = new byte[structLength * 500];
            int readBytes;
            while ((readBytes = dis.read(buffer, 0, buffer.length)) > 0) {
                if (readBytes < structLength) {
                    break;
                }
                pos = 0;
                while (pos < readBytes) {
                    UserInfo userInfo = null;

                    System.arraycopy(buffer, pos, structUnit, 0, structLength);
                    byte[] idBytes = Arrays.copyOf(structUnit, 128);
                    int validLength = BioDataUtil.getValidLength(idBytes);
                    String palmId = new String(idBytes, 0, validLength, StandardCharsets.UTF_8);
                    String[] combineArray = palmId.split("_");
                    if (combineArray.length >= 3) {
                        userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(combineArray[0]);
                        if (userInfo == null) {
                            userInfo = new UserInfo();
                        }
                        userInfo.userId = combineArray[0];
                        userInfo.name = combineArray[1];
                        try {
                            userInfo.avatarIndex = Integer.parseInt(combineArray[2]);
                        } catch (NumberFormatException e) {
                            userInfo.avatarIndex = 0;
                        }
                    } else {
                        userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(palmId);
                        if (userInfo == null) {
                            userInfo = new UserInfo();
                        }
                        userInfo.userId = palmId;
                        userInfo.name = palmId;
                    }
                    userInfo.personId = userInfo.userId;
                    byte[] colorSizeBytes = Arrays.copyOfRange(structUnit, 128, 132);
                    int colorSize = byte2int(colorSizeBytes);
                    byte[] colorIndexBytes = Arrays.copyOfRange(structUnit, 132, 136);
                    int colorIndex = byte2int(colorIndexBytes);
                    userInfo.palmFeature1 = Arrays.copyOfRange(structUnit, 136, 136 + colorSize);


                    byte[] irSizeBytes = Arrays.copyOfRange(structUnit, 128, 132);
                    int irSize = byte2int(irSizeBytes);
                    byte[] irIndexBytes = Arrays.copyOfRange(structUnit, 132, 136);
                    int irIndex = byte2int(irIndexBytes);
                    userInfo.palmFeature2 = Arrays.copyOfRange(structUnit, 136, 136 + irSize);
                    userInfo.palm = 2;

                    BioDataUtil.instance().insertOrUpdateUserInfo(userInfo);

                    pos += structLength;

                    progress++;
                    if (progress % 10 == 0) {
                        if (batchAddListener != null) {
                            batchAddListener.onBatchAddProgress("Progress:" + progress + "/" + userCount);
                        }
                    }
                }
            }
        } catch (IOException ignore) {
        }
    }

    private static Bitmap scaleBitmapIfNeeded(Bitmap bitmap) {
        int maxWidth = 720;
        int maxHeight = 1280;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private static void saveFeatureBimap(Bitmap bitmap, String fileName, String pin) {
        if (bitmap == null) {
            return;
        }
        FileUtils.saveBitmap(bitmap, fileName,
                FileUtils.USER_BIO_PHOTO + File.separator + pin + File.separator);
    }


    public static void batchAddPalmFromPictures(String palmPhotoPath, BatchAddListener batchAddListener) {
        File palmPhotoDirs = new File(palmPhotoPath);
        if (!palmPhotoDirs.exists()) {
            if (batchAddListener != null) {
                batchAddListener.onError(-1, "Path invalid!");
            }
            return;
        }

        File[] palmPhotoDirArray = palmPhotoDirs.listFiles();
        if (palmPhotoDirArray == null || palmPhotoDirArray.length == 0) {
            if (batchAddListener != null) {
                batchAddListener.onError(-1, "Path invalid!");
            }
            return;
        }

        int failedCount = 0;
        for (File file : palmPhotoDirArray) {
            String palmPhotoSubFilePath = file.getAbsolutePath();
            File palmPhotoSubFile = new File(palmPhotoSubFilePath);

            if (palmPhotoSubFile.isDirectory()) {
                batchAddPalmFromPictures(palmPhotoSubFilePath, batchAddListener);
            } else {
                if (!isImageFile(palmPhotoSubFile.getName())) {
                    continue;
                }

                if (batchAddListener != null) {
                    batchAddListener.onBatchAddProgress(palmPhotoSubFilePath);
                }
                try {
                    long ts1 = System.currentTimeMillis();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(palmPhotoSubFilePath, options);

                    options.inJustDecodeBounds = false;
                    Bitmap srcBitmap = BitmapFactory.decodeFile(palmPhotoSubFilePath, options);
                    //make sure bitmap is smaller than 720*1280
                    Bitmap finalBitmap = null;
                    if (options.outWidth > 720 || options.outHeight > 1280) {
                        finalBitmap = scaleBitmapIfNeeded(srcBitmap);
                        srcBitmap.recycle();
                    } else {
                        finalBitmap = srcBitmap;
                    }

                    String imageData = bitmapToBase64(finalBitmap);
                    String fileName = palmPhotoSubFile.getName();
                    String userId = fileName.substring(0, fileName.lastIndexOf("."));

                    Image image = new Image();
                    image.data = imageData;
                    image.width = finalBitmap.getWidth();
                    image.height = finalBitmap.getHeight();
                    image.bioType = BioType.PALM;
                    image.format = "jpeg"/*only support jpeg*/;

                    List<Image> images = new ArrayList<>();
                    images.add(image);
                    DetectPalmRequest detectPalmRequest = new DetectPalmRequest();
                    detectPalmRequest.setImages(images);
                    detectPalmRequest.setIsNeedPalmInfo(false);
                    detectPalmRequest.setIsNeedPicture(false);
                    detectPalmRequest.setIsNeedFeature(true);
                    String jsonString = JSONUtil.getJsonString(detectPalmRequest);

                    byte[] palmEnrollResult = new byte[1024 * 1024];
                    int[] size = new int[]{palmEnrollResult.length};
                    int ret = AMTHidManager.instance().registerPalm(jsonString.getBytes(), palmEnrollResult, size);
                    if (ret == ErrorCode.ERROR_NONE) {
                        String json = new String(palmEnrollResult, 0, size[0]);
                        int status = KotlinExtentKt.getStatus(json);
                        if (status == StatusCode.SUCCESS) {
                            DetectPalmResponse detectPalmResponse = JSONUtil.getDetectPalmResponse(json);
                            if (detectPalmResponse != null) {
                                List<DetectPalmResult> detectPalmResults = detectPalmResponse.palms;
                                if (detectPalmResults != null && !detectPalmResults.isEmpty()) {
                                    DetectPalmResult detectPalmResult = detectPalmResults.get(0);
                                    if (detectPalmResult == null) {
                                        return;
                                    }
                                    PalmFeature palmFeature = detectPalmResult.getFeature();
                                    if (palmFeature != null) {
                                        byte[] verifyTemplate = palmFeature.getByteVerTemplate();

                                        UserInfo userInfo = new UserInfo();
                                        userInfo.userId = userId;
                                        userInfo.name = userId;
                                        userInfo.personId = userId;
                                        userInfo.palm = 1;
                                        userInfo.palmFeature1 = verifyTemplate;

                                        if (BioDataUtil.instance().isUserExist(userId)) {
                                            BioDataUtil.instance().updateUserInfo(userInfo);
                                        } else {
                                            BioDataUtil.instance().insertUserInfo(userInfo);
                                        }
                                        saveFeatureBimap(finalBitmap, userId + "_palm_vl", userId);
                                    }
                                }
                            }
                        } else {
                            String detail = KotlinExtentKt.getDetail(json);
                            if (batchAddListener != null) {
                                batchAddListener.onError(status, detail + "\n" + fileName);
                            }
                            failedCount++;
                            if (failedCount >= 5) {
                                break;
                            }
                        }

                    } else {
                        if (batchAddListener != null) {
                            batchAddListener.onError(ret, "HID Communication Error:" + ret);
                            if (ret == ErrorCode.ERROR_NOT_OPEN) {
                                break;
                            }
                        }
                    }
                    AMTUtil.safeReleaseBitmap(finalBitmap);
                    Log.d(TAG, "[batchAddPalmFromPictures]: cost " + (System.currentTimeMillis() - ts1));
                } catch (Exception e) {
                    Log.e(TAG, "batchAddPalmFromPictures: ", e);
                }
            }
        }
    }


    /**
     * import palm and palm vein by picture
     * the picture must contain palm and palm vein
     */
    public static void batchAddPalmAndPalmVeinFromPictures(String palmPhotoPath, BatchAddListener batchAddListener) {
        File palmPhotoDirs = new File(palmPhotoPath);
        if (!palmPhotoDirs.exists()) {
            if (batchAddListener != null) {
                batchAddListener.onError(-1, "Path invalid!");
            }
            return;
        }

        File[] palmPhotoDirArray = palmPhotoDirs.listFiles();
        if (palmPhotoDirArray == null || palmPhotoDirArray.length == 0) {
            if (batchAddListener != null) {
                batchAddListener.onError(-1, "Path invalid!");
            }
            return;
        }

        int failedCount = 0;
        for (File file : palmPhotoDirArray) {
            String palmPhotoSubFilePath = file.getAbsolutePath();
            File palmPhotoSubFile = new File(palmPhotoSubFilePath);

            if (palmPhotoSubFile.isDirectory()) {
                batchAddPalmFromPictures(palmPhotoSubFilePath, batchAddListener);
            } else {
                if (!isImageFile(palmPhotoSubFile.getName())) {
                    continue;
                }

                if (batchAddListener != null) {
                    batchAddListener.onBatchAddProgress(palmPhotoSubFilePath);
                }
                try {
                    long ts1 = System.currentTimeMillis();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    Bitmap srcBitmap = BitmapFactory.decodeFile(palmPhotoSubFilePath, options);

                    Pair<Bitmap, Bitmap> bitmapBitmapPair = SplitBitmapUtil.INSTANCE.splitBitmapVertically(srcBitmap);
                    srcBitmap.recycle();

                    Bitmap vlBitmap = bitmapBitmapPair.component1();
                    Bitmap finalVLBitmap;
                    if (vlBitmap.getWidth() > 720 || vlBitmap.getHeight() > 1280) {
                        //make sure bitmap is smaller than 720*1280
                        finalVLBitmap = scaleBitmapIfNeeded(vlBitmap);
                        vlBitmap.recycle();
                    } else {
                        finalVLBitmap = vlBitmap;
                    }

                    Bitmap irBitmap = bitmapBitmapPair.component2();
                    Bitmap finalIRBitmap;
                    if (irBitmap.getWidth() > 720 || irBitmap.getHeight() > 1280) {
                        //make sure bitmap is smaller than 720*1280
                        finalIRBitmap = scaleBitmapIfNeeded(irBitmap);
                        irBitmap.recycle();
                    } else {
                        finalIRBitmap = irBitmap;
                    }


                    String vlImageData = bitmapToBase64(finalVLBitmap);
                    String irImageData = bitmapToBase64(finalIRBitmap);


                    String fileName = palmPhotoSubFile.getName();
                    String userId = fileName.substring(0, fileName.lastIndexOf("."));


                    List<Image> images = new ArrayList<>();
                    Image vlImage = new Image();
                    vlImage.data = vlImageData;
                    vlImage.width = finalVLBitmap.getWidth();
                    vlImage.height = finalVLBitmap.getHeight();
                    vlImage.bioType = BioType.PALM;
                    vlImage.format = "jpeg"/*only support jpeg*/;
                    images.add(vlImage);

                    Image irImage = new Image();
                    irImage.data = irImageData;
                    irImage.width = finalIRBitmap.getWidth();
                    irImage.height = finalIRBitmap.getHeight();
                    irImage.bioType = BioType.PALM_VEIN;
                    irImage.format = "jpeg"/*only support jpeg*/;
                    images.add(irImage);

                    DetectPalmRequest detectPalmRequest = new DetectPalmRequest();
                    detectPalmRequest.setImages(images);
                    detectPalmRequest.setIsNeedPalmInfo(false);
                    detectPalmRequest.setIsNeedPicture(false);
                    detectPalmRequest.setIsNeedFeature(true);
                    String jsonString = JSONUtil.getJsonString(detectPalmRequest);

                    byte[] palmEnrollResult = new byte[1024 * 1024];
                    int[] size = new int[]{palmEnrollResult.length};
                    int ret = AMTHidManager.instance().registerPalm(jsonString.getBytes(), palmEnrollResult, size);
                    if (ret == ErrorCode.ERROR_NONE) {
                        String json = new String(palmEnrollResult, 0, size[0]);
                        int status = KotlinExtentKt.getStatus(json);
                        if (status == StatusCode.SUCCESS) {
                            DetectPalmResponse detectPalmResponse = JSONUtil.getDetectPalmResponse(json);
                            if (detectPalmResponse != null) {
                                List<DetectPalmResult> detectPalmResults = detectPalmResponse.palms;
                                if (detectPalmResults != null && !detectPalmResults.isEmpty()) {
                                    DetectPalmResult detectPalmResult = detectPalmResults.get(0);
                                    UserInfo userInfo = new UserInfo();
                                    userInfo.userId = userId;
                                    userInfo.name = userId;
                                    userInfo.personId = userId;

                                    PalmFeature palmFeature = detectPalmResult.getFeature();
                                    if (palmFeature != null) {
                                        String bioType = palmFeature.getBioType();
                                        if (BioType.PALM.equals(bioType)) {
                                            userInfo.palmFeature1 = palmFeature.getByteVerTemplate();
                                            userInfo.palm++;
                                        }
                                    }
                                    PalmFeature palmVeinFeature = detectPalmResult.getFeatureVein();
                                    if (palmVeinFeature != null) {
                                        String bioType = palmVeinFeature.getBioType();
                                        if (BioType.PALM_VEIN.equals(bioType)) {
                                            userInfo.palmFeature2 = palmVeinFeature.getByteVerTemplate();
                                            userInfo.palm++;
                                        }
                                    }

                                    if (BioDataUtil.instance().isUserExist(userId)) {
                                        BioDataUtil.instance().updateUserInfo(userInfo);
                                    } else {
                                        BioDataUtil.instance().insertUserInfo(userInfo);
                                    }
                                    saveFeatureBimap(finalVLBitmap, userId + "_palm_vl", userId);
                                    finalVLBitmap.recycle();
                                    saveFeatureBimap(finalIRBitmap, userId + "_palm_ir", userId);
                                    finalIRBitmap.recycle();
                                }
                            }
                        } else {
                            String detail = KotlinExtentKt.getDetail(json);
                            if (batchAddListener != null) {
                                batchAddListener.onError(status, detail + "\n" + fileName);
                            }
                            failedCount++;
                            if (failedCount >= 5) {
                                break;
                            }
                        }

                    } else {
                        if (batchAddListener != null) {
                            batchAddListener.onError(ret, "HID Communication Error:" + ret);
                            if (ret == ErrorCode.ERROR_NOT_OPEN) {
                                break;
                            }
                        }
                    }
                    AMTUtil.safeReleaseBitmap(finalVLBitmap);
                    Log.d(TAG, "[batchAddPalmFromPictures]: cost " + (System.currentTimeMillis() - ts1));
                } catch (Exception e) {
                    Log.e(TAG, "batchAddPalmFromPictures: ", e);
                }
            }
        }
    }


    /**
     * char faceid[128];
     * int size; 4 byte
     * char template[2048];
     * <p>
     */
    public static void importUserInfoToModuleFromDBFile(String fileName, BatchAddListener batchAddListener) {
        File faceDBFile = new File(fileName);
        if (!faceDBFile.exists()) {
            batchAddListener.onError(-1, fileName + " not exist or no permission to read!");
            return;
        }
        byte[] manageResult = new byte[255];
        int[] manageResultSize = new int[]{manageResult.length};
        int ret = AMTHidManager.instance().manageModuleData(ManageType.BEGIN_TRANSACTION, null, manageResult, manageResultSize);
        if (ret == 0) {
            String beginTransactionResult = new String(manageResult, 0, manageResultSize[0]);
            Log.i(TAG, "batchAddFaceFromDBFile: begin transaction ret = " + ret + ",result=" + beginTransactionResult);
            if (!beginTransactionResult.contains("success")) {
                batchAddListener.onError(-2, "begin transaction failed," + beginTransactionResult);
                return;
            }
        }
        boolean isOldStruct = false;
        final byte[] faceID = new byte[128];
        final byte[] size = new byte[4];
        final byte[] type = new byte[4];
        final byte[] template = new byte[2048];
        int bufferLen;
        if (isOldStruct) {
            bufferLen = 128 + 4 + 2048;
        } else {
            bufferLen = 128 + 4 + 4 + 2048;
        }

        long totalCount = faceDBFile.length() / bufferLen;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(faceDBFile))) {
            byte[] buffer = new byte[bufferLen];
            int len;
            long index = 0L;
            boolean isAllImportSuccess = true;
            while ((len = bis.read(buffer)) != -1) {
                if (len == bufferLen) {
                    System.arraycopy(buffer, 0, faceID, 0, faceID.length);
                    System.arraycopy(buffer, 128, size, 0, size.length);

                    if (isOldStruct) {
                        System.arraycopy(buffer, 4 + 128, template, 0, template.length);
                    } else {
                        System.arraycopy(buffer, 128 + 4, type, 0, type.length);
                        System.arraycopy(buffer, 8 + 128, template, 0, template.length);
                        int t = byte2int(type);
                        if (t != 9) {
                            Log.w(TAG, "importUserInfoFromDBFile: not face template! type = " + t);
                            continue;
                        }
                    }
                    String userId = getFaceId(faceID).split("\\.")[0];
                    UserInfo userInfo = new UserInfo();
                    userInfo.name = userId;
                    userInfo.personId = userId;
                    userInfo.userId = userId;
                    userInfo.faceFeature = Arrays.copyOfRange(template, 0, byte2int(size));
                    AMTResult<Integer> amtResult = ModuleBioDataUtil.instance().importUser(userInfo);
                    if (amtResult.getCode() != ErrorCode.ERROR_NONE) {
                        Log.w(TAG, "faceId=" + userId + " add failed,ret=" + amtResult.getCode());
                        batchAddListener.onError(-5, "ID = " + userId
                                + " import failed, error = " + amtResult.getCode()
                                + ",detail = " + amtResult.getMessage());
                        isAllImportSuccess = false;
                        break;
                    }
                    index++;
                    if (index % 100 == 0) {
                        Log.i(TAG, "batchAddFaceFromDBFile: " + index + "/" + totalCount);
                    }
                    batchAddListener.onBatchAddProgress(index + "/" + totalCount);
                } else {
                    Log.w(TAG, "batchAddFaceFromDBFile: read length != expect length,data not complete");
                }
            }
            if (isAllImportSuccess) {
                //////////////////////////////////////////////////////////////////////
                ////////If all data have been add success,please COMMIT transaction///
                //////////////////////////////////////////////////////////////////////
                ret = AMTHidManager.instance().manageModuleData(ManageType.COMMIT_TRANSACTION, null, manageResult, manageResultSize);
                if (ret == 0) {
                    String result = new String(manageResult, 0, manageResultSize[0]);
                    Log.i(TAG, "batchAddFaceFromDBFile: commit transaction ret = " + ret + ",result=" + result);
                    if (!result.contains("success")) {
                        batchAddListener.onError(-4, "commit transaction failed," + result);
                    }
                } else {
                    batchAddListener.onError(-4, "commit transaction cmd send failed,ret = " + ret);
                }
            } else {
                /////////////////////////////////////////////////////////////
                ////////If any data add failed,please ROLLBACK transaction///
                /////////////////////////////////////////////////////////////
                ret = AMTHidManager.instance().manageModuleData(ManageType.ROLLBACK_TRANSACTION, null, manageResult, manageResultSize);
                if (ret == 0) {
                    String result = new String(manageResult, 0, manageResultSize[0]);
                    Log.i(TAG, "batchAddFaceFromDBFile: rollback transaction ret = " + ret + ",result=" + result);
                    if (!result.contains("success")) {
                        batchAddListener.onError(-4, "rollback transaction failed," + result);
                    }
                } else {
                    batchAddListener.onError(-4, "Rollback transaction cmd send failed! Error = " + ret);
                }
            }
        } catch (IOException e) {
            batchAddListener.onError(-1, fileName + " not exist or no permission to read!");
        }
    }


    /**
     * char faceid[128];
     * int size; 4 byte
     * char template[2048];
     * <p>
     */
    public static void importUserInfoToAppDBFromDBFile(boolean isOldStruct,
                                                       String fileName, BatchAddListener batchAddListener) {
        File faceDBFile = new File(fileName);
        if (!faceDBFile.exists()) {
            batchAddListener.onError(-1, fileName + " not exist or no permission to read!");
            return;
        }
        final byte[] faceID = new byte[128];
        final byte[] size = new byte[4];
        final byte[] type = new byte[4];
        final byte[] template = new byte[2048];
        int bufferLen;
        if (isOldStruct) {
            bufferLen = 128 + 4 + 2048;
        } else {
            bufferLen = 128 + 4 + 4 + 2048;
        }

        long totalCount = faceDBFile.length() / bufferLen;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(faceDBFile))) {
            byte[] buffer = new byte[bufferLen];
            int len;
            long index = 0L;
            while ((len = bis.read(buffer)) != -1) {
                if (len == bufferLen) {
                    System.arraycopy(buffer, 0, faceID, 0, faceID.length);
                    System.arraycopy(buffer, 128, size, 0, size.length);

                    if (isOldStruct) {
                        System.arraycopy(buffer, 4 + 128, template, 0, template.length);
                    } else {
                        System.arraycopy(buffer, 128 + 4, type, 0, type.length);
                        System.arraycopy(buffer, 8 + 128, template, 0, template.length);
                        int t = byte2int(type);
                        if (t != 9) {
                            Log.w(TAG, "importUserInfoFromDBFile: not face template! type = " + t);
                            continue;
                        }
                    }
                    String userId = getFaceId(faceID).split("\\.")[0];
                    UserInfo userInfo = new UserInfo();
                    userInfo.name = userId;
                    userInfo.personId = userId;
                    userInfo.userId = userId;
                    userInfo.faceFeature = Arrays.copyOfRange(template, 0, byte2int(size));
                    BioDataUtil.instance().insertUserInfo(userInfo);
                    index++;
                    if (index % 100 == 0) {
                        Log.i(TAG, "batchAddFaceFromDBFile: " + index + "/" + totalCount);
                    }
                    batchAddListener.onBatchAddProgress(index + "/" + totalCount);
                } else {
                    Log.w(TAG, "batchAddFaceFromDBFile: read length != expect length,data not complete");
                }
            }
        } catch (IOException e) {
            batchAddListener.onError(-1, fileName + " not exist or no permission to read!");
        }
    }


    private static int countLinesWithReader(String filePath) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(new FileReader(filePath))) {
            while (reader.skip(Long.MAX_VALUE) > 0) {
                // 这个循环确保即使文件非常大，也能跳到文件末尾
            }
            return reader.getLineNumber() + 1;
        }
    }


    /**
     * Import CSV files exported from the FaceMX mobile app.
     */
    public static void importUserInfoToAppDBFromFaceMXFile(String fileName,
                                                           BatchAddListener batchAddListener) {
        File faceDBFile = new File(fileName);
        if (!faceDBFile.exists()) {
            batchAddListener.onError(-1, fileName + " not exist or no permission to read!");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(faceDBFile))) {
            String line;

            // 跳过文件头
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                // 确保数据行有足够的列
                if (values.length >= 5) {
                    try {
                        String userPin = values[0].trim();
                        String userName = values[1].trim();
                        int templateSize = Integer.parseInt(values[2].trim());
                        String template = values[3].trim();
                        byte[] feature = Base64.decode(template, Base64.DEFAULT);

                        UserInfo userInfo = new UserInfo();
                        userInfo.name = userName;
                        userInfo.personId = userPin;
                        userInfo.userId = userPin;
                        userInfo.faceFeature = Arrays.copyOfRange(feature, 0, templateSize);
                        BioDataUtil.instance().insertUserInfo(userInfo);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    batchAddListener.onError(-1, "CSV raw over 5!");
                }
            }
        } catch (IOException e) {
            batchAddListener.onError(-1, fileName + " not exist or no permission to read!");

        }
    }

    private static String getFaceId(byte[] faceIDBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : faceIDBytes) {
            if ((char) b == '\0') {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }

    private static boolean isImageFile(String fileName) {
        String fileNameUpperCase = fileName.toUpperCase(Locale.US);
        return fileNameUpperCase.endsWith("JPG") || fileNameUpperCase.endsWith("JPEG");
    }
}
