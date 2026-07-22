package com.armatura.biomodule.util;

import android.content.Context;

import com.armatura.biomodule.pojo.common.Attribute;
import com.armatura.biomodule.pojo.common.LiveData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CsvHelper {
    private final static String BYTE = "byte";
    private final static String CHAR = "char";
    private final static String INT = "int";
    private final static String SHORT = "short";
    private final static String FLOAT = "float";
    private final static String DOUBLE = "double";
    private final static String LONG = "long";
    private final static String STRING = "class java.lang.String";
    private final static String BOOLEAN = "boolean";

    private static CsvHelper instance;

    private File faceAttrRecordFile = null;
    private File faceRecordFile = null;
    private File palmRecordFile = null;

    private CsvHelper() {
    }

    public static CsvHelper getInstance() {
        if (instance == null) {
            instance = new CsvHelper();
        }
        return instance;
    }

    public void initAll(Context context) {
        String nowTime = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date());
        String faceCsvPath = context.getCacheDir().getAbsolutePath() + "/Result/Record/" + nowTime + ".csv";

        List<String> csvTitle = new ArrayList<>();
        csvTitle.add("UserId");
        csvTitle.add("Score");
        csvTitle.add("Photo");

        try {
            Class<?> clz = Class.forName("com.armatura.biomodule.pojo.common.LiveData");
            Field[] declaredFields = clz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                csvTitle.add(declaredField.getName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Class<?> clz = Class.forName("com.armatura.biomodule.pojo.common.Attribute");
            Field[] declaredFields = clz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                csvTitle.add(declaredField.getName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        String[] strings = new String[csvTitle.size()];
        String[] titles = csvTitle.toArray(strings);

        CsvHelper.getInstance().initFaceAttrRecord(faceCsvPath, titles);
        CsvHelper.getInstance().initFaceRecord(FileUtils.recordPath + "/face_" + nowTime + ".csv", "Result", "Time", "IdentifyPin", "InputPin", "Score", "LiveScore");
        CsvHelper.getInstance().initPalmRecord(FileUtils.recordPath + "/palm_" + nowTime + ".csv", "Result", "Time", "IdentifyPin", "InputPin", "Score", "LiveScore");
    }

    public void initFaceAttrRecord(String csvPath, String... csvTitle) {
        faceAttrRecordFile = initCsvFile(csvPath, csvTitle);
    }

    public void initFaceRecord(String csvPath, String... csvTitle) {
        faceRecordFile = initCsvFile(csvPath, csvTitle);
    }

    public void initPalmRecord(String csvPath, String... csvTitle) {
        palmRecordFile = initCsvFile(csvPath, csvTitle);
    }

    private File initCsvFile(String csvPath, String... csvTitle) {
        File csvFile = new File(csvPath);
        if (!Objects.requireNonNull(csvFile.getParentFile()).exists()) {
            csvFile.getParentFile().mkdirs();
        }
        if (!csvFile.exists()) {
            try {
                csvFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            StringBuilder titleBuilder = new StringBuilder();
            for (String s : csvTitle) {
                titleBuilder.append(s).append(",");
            }
            titleBuilder.append("\n");
            writeLine(csvFile, titleBuilder.toString());
        }
        return csvFile;
    }

    public void appendFaceAttrAndRecognizeRecord(String userId, String recognizeScore, String photoName,
                                                 LiveData liveData,
                                                 Attribute attribute) {
        StringBuilder builder = new StringBuilder();
        builder.append(userId).append(",");
        builder.append(recognizeScore).append(",");
        builder.append(photoName).append(",");
        try {
            Class<?> clz = Class.forName("com.armatura.biomodule.pojo.common.LiveData");
            Field[] declaredFields = clz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                String type = declaredField.getType().toString();
                switch (type) {
                    case FLOAT:
                        builder.append(declaredField.getFloat(liveData));
                        break;
                    case INT:
                        builder.append(declaredField.getInt(liveData));
                        break;
                    case DOUBLE:
                        builder.append(declaredField.getDouble(liveData));
                        break;
                    case STRING:
                        builder.append((String) declaredField.get(liveData));
                        break;
                    case LONG:
                        builder.append(declaredField.getLong(liveData));
                        break;

                }
                builder.append(",");
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            Class<?> clz = Class.forName("com.armatura.biomodule.pojo.common.Attribute");
            Field[] declaredFields = clz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                String type = declaredField.getType().toString();
                switch (type) {
                    case FLOAT:
                        builder.append(declaredField.getFloat(attribute));
                        break;
                    case INT:
                        if (declaredField.getName().equals("gender")) {
                            builder.append(declaredField.getInt(attribute) == 1 ? "Male" : "Female");
                        } else {
                            builder.append(declaredField.getInt(attribute));
                        }
                        break;
                    case DOUBLE:
                        builder.append(declaredField.getDouble(attribute));
                        break;
                    case STRING:
                        builder.append((String) declaredField.get(attribute));
                        break;
                    case LONG:
                        builder.append(declaredField.getLong(attribute));
                        break;

                }
                builder.append(",");
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }

        builder.append("\n");
        writeLine(faceAttrRecordFile, builder.toString());
    }

    public void appendFaceTestRecord(String... contents) {
        StringBuilder titleBuilder = new StringBuilder();
        for (String s : contents) {
            titleBuilder.append(s).append(",");
        }
        titleBuilder.append("\n");
        writeLine(faceRecordFile, titleBuilder.toString());
    }

    public void appendPalmTestRecord(String... contents) {
        StringBuilder titleBuilder = new StringBuilder();
        for (String s : contents) {
            titleBuilder.append(s).append(",");
        }
        titleBuilder.append("\n");
        writeLine(palmRecordFile, titleBuilder.toString());
    }

    private void writeLine(File file, String content) {
        Writer writer = null;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, true);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            writer = new BufferedWriter(outputStreamWriter);
            writer.write(content);
            writer.flush();
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
            if (outputStreamWriter != null) {
                try {
                    outputStreamWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}