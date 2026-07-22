package com.armatura.biomodule.config;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 1.0 initial version
 * 1.1 Merge change from product_tdb03_maintain which can restart myself once caught exception.
 *
 * @author Terry, Raymond Wang
 * @version 1.1
 */

public class CrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private UncaughtExceptionHandler mDefaultHandler;
    private Application application;
    private final Map<String, String> infos = new HashMap<String, String>();

    private CrashHandler() {
    }

    private static final class CrashHandlerHolder {
        static final CrashHandler crashHandler = new CrashHandler();
    }

    public static CrashHandler getInstance() {
        return CrashHandlerHolder.crashHandler;
    }

    public void init(Application application) {
        this.application = application;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        Log.e(TAG, "uncaughtException: ", ex);
        handleException(ex);
        if (mDefaultHandler != null) {
            // 系统处理
            mDefaultHandler.uncaughtException(thread, ex);
        }

    }

    private void handleException(Throwable ex) {
        if (ex == null) {
            return;
        }
        saveException(ex);

    }


    public void collectDeviceInfo() {
        try {
            PackageManager pm = application.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(application.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), Objects.requireNonNull(field.get(null)).toString());
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    private void saveCrashInfoFile(Throwable ex) throws Exception {
        StringBuilder sb = new StringBuilder();
        try {
            SimpleDateFormat sDateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sDateFormat.setTimeZone(Calendar.getInstance().getTimeZone());
            String date = sDateFormat.format(new java.util.Date());
            sb.append("\r\n").append(date).append("\n");
            for (Map.Entry<String, String> entry : infos.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                sb.append(key).append("=").append(value).append("\n");
            }

            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            Throwable cause = ex.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.flush();
            printWriter.close();
            String result = writer.toString();
            sb.append(result);

            writeFile(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
            sb.append("an error occured while writing file...\r\n");
            writeFile(sb.toString());
        }
    }

    private void saveException(Throwable throwable) {
        collectDeviceInfo();
        try {
            saveCrashInfoFile(throwable);
        } catch (Exception ignore) {
        }
    }


    private void writeFile(String sb) throws Exception {
        String time = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String fileName = "crashLog-" + time + ".log";
        File dir = application.getExternalFilesDir(null);
        if (dir == null || !dir.exists()) {
            dir = application.getFilesDir();
        }

        FileOutputStream fos = new FileOutputStream(dir.getAbsolutePath() + "/" + fileName, true);
        fos.write(sb.getBytes());
        fos.flush();
        fos.close();
    }
}
