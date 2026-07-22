package com.armatura.internaldata.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import com.armatura.biomodule.R;
import com.armatura.biomodule.activity.BioTemplateRegisterActivity;
import com.armatura.biomodule.activity.base.BaseActivity;
import com.armatura.biomodule.common.RegisterOperate;
import com.armatura.biomodule.common.RegisterType;
import com.armatura.biomodule.common.RegisterWay;
import com.armatura.internaldata.fragment.ModuleFace1V1Fragment;
import com.armatura.internaldata.fragment.ModuleRegByImageFragment;
import com.armatura.internaldata.fragment.ModuleRegBySnapShotFragment;
import com.armatura.internaldata.fragment.ModuleRegByUvcFragment;

/**
 * Created by Jeremy on 2022/11/3.
 * This page is used to demonstrate how to manage user data within the module.
 * Not all modules support this function, please consult business.
 */
public class ModuleRegisterOperateActivity extends BaseActivity {

    private static final String TAG = "MRegisterOperateActivity";
    private final static String REGISTER_TYPE = "type";
    private final static String REGISTER_USER_ID = "userId";
    private final static String REGISTER_OPERATE = "register_operate";
    private final static String REGISTER_WAY = "register_way";
    private final static int FAVE_1V1 = 11;


    public static void action(Context context, @RegisterType int type, String userId,
                              @RegisterOperate int operate, @RegisterWay int way) {
        Intent intent = new Intent();
        intent.putExtra(REGISTER_TYPE, type);
        intent.putExtra(REGISTER_USER_ID, userId);
        intent.putExtra(REGISTER_OPERATE, operate);
        intent.putExtra(REGISTER_WAY, way);
        intent.setClass(context, ModuleRegisterOperateActivity.class);
        context.startActivity(intent);
    }

    public static void actionFace1V1(Context context) {
        Intent intent = new Intent();
        intent.putExtra(REGISTER_TYPE, FAVE_1V1);
        intent.putExtra(REGISTER_WAY, FAVE_1V1);
        intent.setClass(context, ModuleRegisterOperateActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bio_template_register);
        initData();
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) {
            toastMsg("Invalid type");
            finish();
            return;
        }
        Bundle bundleExtra = intent.getExtras();
        if (bundleExtra == null) {
            toastMsg("Invalid type");
            finish();
            return;
        }
        int type = bundleExtra.getInt(REGISTER_TYPE);
        String userId = bundleExtra.getString(REGISTER_USER_ID);
        int operate = bundleExtra.getInt(REGISTER_OPERATE);
        int way = bundleExtra.getInt(REGISTER_WAY);

        String title;
        switch (type) {
            case RegisterType.FACE:
                title = getString(R.string.face);
                break;
            case RegisterType.PALM:
                title = getString(R.string.palm);
                break;
            case FAVE_1V1:
                title = getString(R.string.face_1v1);
                break;
            default:
                title = getString(R.string.un_know);
                break;
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        switch (way) {
            case RegisterWay.LOCAL_IMAGE:
                fragmentTransaction.add(R.id.main_fragment_fl,
                        ModuleRegByImageFragment.newInstance(type, title, userId, operate));
                break;
            case RegisterWay.UVC_STREAM:
                fragmentTransaction.add(R.id.main_fragment_fl,
                        ModuleRegByUvcFragment.newInstance(type, title, userId, operate));
                break;
            case RegisterWay.SNAP_SHOT:
                fragmentTransaction.add(R.id.main_fragment_fl,
                        ModuleRegBySnapShotFragment.newInstance(type, title, userId, operate));
                break;
            case FAVE_1V1:
                fragmentTransaction.add(R.id.main_fragment_fl,
                        ModuleFace1V1Fragment.Companion.newInstance(title));
                break;
            default:
                break;

        }
        fragmentTransaction.commitAllowingStateLoss();
    }
}