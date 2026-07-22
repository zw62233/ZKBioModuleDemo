package com.armatura.biomodule.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;

import com.armatura.biomodule.common.IdentifyState;
import com.armatura.biomodule.config.Config;

/**
 * Created by Magic on 2023/6/30
 * Description:
 */
public class CustomAttribute {


    @BindingAdapter("informationVisibility")
    public static void informationVisibility(View view, IdentifyState identifyState) {
        switch (identifyState) {
            case IDENTIFY_ONCE:
            case IDENTIFY_CONST:
                view.setVisibility(View.INVISIBLE);
                break;
            case STOP:
                view.setVisibility(View.VISIBLE);
                break;
        }
    }

    @BindingAdapter("informationText")
    public static void informationText(TextView view, IdentifyState identifyState) {
        switch (identifyState) {
            case STOP:
            case IDENTIFY_ONCE:
            case IDENTIFY_CONST:
                view.setText("");
                break;
        }
    }

    @BindingAdapter("identifyInfoVisibility")
    public static void identifyInfoVisibility(View view, IdentifyState identifyState) {
        switch (identifyState) {
            case IDENTIFY_ONCE:
            case IDENTIFY_CONST:
                view.setVisibility(View.VISIBLE);
                break;
            case STOP:
                view.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @BindingAdapter("layoutVisibility")
    public static void layoutVisibility(ConstraintLayout view, IdentifyState identifyState) {
        switch (identifyState) {
            case IDENTIFY_ONCE:
            case IDENTIFY_CONST:
                view.setVisibility(View.VISIBLE);
                break;
            case STOP:
                view.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @BindingAdapter("ivStandByVisibility")
    public static void ivStandByVisibility(ImageView view, IdentifyState identifyState) {
        switch (identifyState) {
            case IDENTIFY_ONCE:
            case IDENTIFY_CONST:
                view.setVisibility(View.INVISIBLE);
                break;
            case STOP:
                view.setVisibility(View.VISIBLE);
                break;
        }
    }

    @BindingAdapter("testModeVisibility")
    public static void testModeVisibility(View view, IdentifyState identifyState) {
        switch (identifyState) {
            case IDENTIFY_ONCE:
            case IDENTIFY_CONST:
                view.setVisibility(Config.instance().isTestMode ? View.VISIBLE : View.INVISIBLE);
                break;
            case STOP:
                view.setVisibility(View.INVISIBLE);
                break;
        }
    }
}
