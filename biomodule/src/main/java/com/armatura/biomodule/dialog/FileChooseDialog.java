package com.armatura.biomodule.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.File;

public class FileChooseDialog extends DialogFragment {
    public static final String TRANS_MODE_KEY = "transmode";
    public static final int TRANS_FIRMWARE = 0;
    public static final int TRANS_FILE = 1;
    private static final String TAG = FileChooseDialog.class.getSimpleName();
    private static final String FTYPE = ".apk";
    private static final int DIALOG_LOAD_FILE = 1000;
    NoticeDialogListener mListener;
    //In an Activity
    private File[] fileList;
    private final File mPath = new File(Environment.getExternalStorageDirectory() + "//");
    private String mChosenFile;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mChosenFile = null;
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        loadFileList();
        builder.setTitle("Choose your file");
        if (fileList == null) {
            Log.e(TAG, "Showing file picker before loading the file list");
            dialog = builder.create();
            return dialog;
        }

        int transmode = -1;
        Bundle arguments = getArguments();
        if (arguments != null) {
            transmode = arguments.getInt(TRANS_MODE_KEY);
        }

        final int finalTransmode = transmode;


        final String[] fileNames = new String[fileList.length];
        //获取文件名
        for (int i = 0; i < fileList.length; i++) {
            fileNames[i] = fileList[i].getName();
        }


        builder.setItems(fileNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File chooseFile = fileList[which];
                Log.i(TAG, String.format("FilePath:%s", chooseFile.getAbsoluteFile()));
                //you can do stuff with the file here too
                mListener.onChoseFileClick(FileChooseDialog.this, chooseFile.getAbsolutePath(), finalTransmode);
            }
        });

        dialog = builder.show();
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle( STYLE_NO_FRAME,android.R.style.Theme_Black);
    }

    public void setDataListener(NoticeDialogListener listener) {
        this.mListener = listener;
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    public String GetChosenFIle() {
        return mChosenFile;
    }

    private void loadFileList() {
        try {
            mPath.mkdirs();
        } catch (SecurityException e) {
            Log.e(TAG, "unable to write on the sd card " + e.toString());
        }
        if (mPath.exists()) {
            fileList = mPath.listFiles();
        }
    }

    public interface NoticeDialogListener {
        void onChoseFileClick(DialogFragment dialog, String filename, int transMode);
    }
}
