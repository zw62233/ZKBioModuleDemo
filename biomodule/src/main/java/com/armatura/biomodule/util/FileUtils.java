package com.armatura.biomodule.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.armatura.biomodule.activity.base.ExApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Administrator on 2018/3/22 0022.
 */

public class FileUtils {
    private static final String TAG = "FileUtils";

    private static final int MAX_LOG_FILE_SIZE = 1024 * 1024 * 3;

    private final static String APP_EXTERNAL_CACHE_DIR
            = ExApplication.instance().getExternalCacheDir().getAbsolutePath();
    public static final String resultPath = APP_EXTERNAL_CACHE_DIR + "/Result";
    public static final String avatarPath = APP_EXTERNAL_CACHE_DIR + "/Result/Photo/";
    public static final String livePath = APP_EXTERNAL_CACHE_DIR + "/Result/LiveTest/";
    public static final String recordPath = APP_EXTERNAL_CACHE_DIR + "/Result/Record/";
    public static final String idCardPath = APP_EXTERNAL_CACHE_DIR + "/Result/IDCardPhoto/";
    public static final String bioPhotoPath = APP_EXTERNAL_CACHE_DIR + "/Result/BioPhoto/";
    public static final String maskOnFacePhotoPath = APP_EXTERNAL_CACHE_DIR + "/Result/maskOnFace/";
    public static final String maskOnWrongPlacePhotoPath = APP_EXTERNAL_CACHE_DIR + "/Result/maskOnWrongPlace/";
    public static final String noMaskOnFacePhotoPath = APP_EXTERNAL_CACHE_DIR + "/Result/noMaskOnFace/";
    public static final String snapShotPhotoPath = APP_EXTERNAL_CACHE_DIR + "/Result/snapShotPhoto/";
    public static final String speedPhotoPath = APP_EXTERNAL_CACHE_DIR + "/TestSpeed";
    public static final String testPhotoPath = APP_EXTERNAL_CACHE_DIR + "/Test25";
    public static final String testSpeedResultPath = APP_EXTERNAL_CACHE_DIR + "/Result/SpeedTest";

    public static final String USER_BIO_PHOTO = APP_EXTERNAL_CACHE_DIR + "/Enroll";
    public static final String SPEED_480_640 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/480_640";
    public static final String SPEED_720_960 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/720_960";
    public static final String SPEED_720_1280 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/720_1280";
    public static final String SPEED_960_1280 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/960_1280";
    public static final String SPEED_1080_1440 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/1080_1440";
    public static final String SPEED_1080_1920 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/1080_1920";
    public static final String SPEED_1440_1920 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/1440_1920";
    public static final String SPEED_1920_1920 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/1920_1920";
    public static final String SPEED_1920_2560 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/1920_2560";
    public static final String SPEED_2160_3840 = APP_EXTERNAL_CACHE_DIR + "/TestSpeed/2160_3840";

    public static ArrayList<String> getSpeedFileList() {
        ArrayList<String> speedPath = new ArrayList<>();
        speedPath.add(SPEED_480_640);
        speedPath.add(SPEED_720_960);
        speedPath.add(SPEED_720_1280);
        speedPath.add(SPEED_960_1280);
        speedPath.add(SPEED_1080_1440);
        speedPath.add(SPEED_1080_1920);
        speedPath.add(SPEED_1440_1920);
        speedPath.add(SPEED_1920_1920);
        speedPath.add(SPEED_1920_2560);
        speedPath.add(SPEED_2160_3840);
        return speedPath;
    }


    public static void deleteAllFile(String path) {
        File[] files = getFiles(path);
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.exists()) {
                if (file.delete()) {
                    Log.i(TAG, "deleteAllFile: delete success");
                } else {
                    Log.i(TAG, "deleteAllFile: delete failed");
                }
            }
        }
    }

    public static File[] getFiles(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        return files;
    }

    public static String saveBitmap(Bitmap bitmap, String bitName, String parentPath) {
        File dir = new File(parentPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String path = parentPath + bitName + ".jpeg";
        File file = new File(path);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String saveBioPhoto(Bitmap bitmap, String bitName) {
        File dir = new File(bioPhotoPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String path = bioPhotoPath + bitName + ".jpeg";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String saveLiveFaceBitmap(Bitmap bitmap, String bitName) {
        File dir = new File(livePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String path = livePath + bitName + ".jpeg";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String saveIDCardPhotoBitmap(Bitmap bitmap, String bitName) {
        File dir = new File(idCardPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String path = idCardPath + bitName + ".jpeg";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }


    public static void savePersonRecord(String str, String fileName) {
        File record = new File(APP_EXTERNAL_CACHE_DIR + "/Result/Record/" + File.separator, "" + fileName + ".txt");//记录结果文件
        try {
            if (!record.exists()) {

                File dir = new File(record.getParent());
                dir.mkdirs();
                record.createNewFile();
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(record, false);
                writer.write(str + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
        }
    }

    public static void saveFingerPrint(String str, String fileName) {
        File record = new File(APP_EXTERNAL_CACHE_DIR, fileName);
        try {
            if (!record.exists()) {
                record.createNewFile();
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(record, false);
                writer.write(str + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void saveAllRecord(Context context, String str) {
        File cacheDir = context.getExternalCacheDir();
        File record = new File(cacheDir, "record.txt");
        try {
            if (!record.exists()) {

                File dir = new File(record.getParent());
                dir.mkdirs();
                record.createNewFile();
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(record, true);
                writer.write(str + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
        }
    }

    public static void saveAllRecord(Context context, String fileName, String content) {
        File cacheDir = context.getExternalCacheDir();
        File record = new File(cacheDir, fileName);
        try {
            if (!record.exists()) {

                File dir = new File(record.getParent());
                dir.mkdirs();
                record.createNewFile();
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(record, true);
                writer.write(content + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
        }
    }


    public static void saveRecord(Context context, String fileName, String str) {
        File cacheDir = context.getExternalCacheDir();
        File record = new File(cacheDir, fileName);
        try {
            if (!record.exists()) {

                File dir = new File(record.getParent());
                dir.mkdirs();
                record.createNewFile();
            } else {
                if (record.length() > MAX_LOG_FILE_SIZE) {
                    if (record.delete()) {
                        record.createNewFile();
                    }
                }
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(record, true);
                writer.write(str + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
        }
    }

    public static byte[] readFile(final String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        DataInputStream dis = null;
        ByteArrayOutputStream baos = null;

        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
            byte[] buffer = new byte[4096];
            int len = 0;
            int count = 0;
            baos = new ByteArrayOutputStream();

            while ((count = dis.read(buffer, 0, buffer.length)) > 0) {
                len += count;
                baos.write(buffer, 0, count);
            }
            if (len > 0) {
                return baos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public static void writeFile(final String filePath, byte[] data) {
        DataOutputStream dos = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            dos.write(data, 0, data.length);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeFile(final String filePath, byte[] data,int size) {
        DataOutputStream dos = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }

            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            dos.write(data, 0, size);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void checkDir() {
        checkDir(resultPath);
        checkDir(avatarPath);
        checkDir(recordPath);
        checkDir(idCardPath);
        checkDir(testSpeedResultPath);
        checkDir(livePath);
        checkDir(bioPhotoPath);
        checkDir(maskOnFacePhotoPath);
        checkDir(maskOnWrongPlacePhotoPath);
        checkDir(noMaskOnFacePhotoPath);
        checkDir(snapShotPhotoPath);
    }

    private static void checkDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }


    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static byte[] readRawFile(Context context, String fileName) {
        InputStream inputStream;
        try {
            inputStream = context.getAssets().open(fileName);
        } catch (IOException e) {
            return null;
        }
        byte[] buf = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            while (inputStream.read(buf) > 0) {
                bos.write(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    public static String saveBytes(byte[] data, String bitName, String parentPath) {
        File dir = new File(parentPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String path = parentPath + bitName;
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            out.write(data);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }


    public static void cleanDirectory(File dirFile) {
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return;
        }
        File[] files = dirFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
    }
}
