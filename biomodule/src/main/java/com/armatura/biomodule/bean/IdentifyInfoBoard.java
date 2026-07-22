package com.armatura.biomodule.bean;

import android.text.SpannableStringBuilder;

/**
 * Created by Magic on 2023/6/29
 * Description:
 */
public class IdentifyInfoBoard {
    public int identifyType;
    public SpannableStringBuilder identifyInfoSpannableStingBuilder;
    public int avatarIndex;

    public String userName;

    public boolean hasFaceFeature = false;
    public float faceMatchScore = 0F;

    public IdentifyInfoBoard() {
    }

    private IdentifyInfoBoard(int identifyType, SpannableStringBuilder identifyInfoSpannableStingBuilder, int avatarIndex) {
        this.identifyType = identifyType;
        this.identifyInfoSpannableStingBuilder = identifyInfoSpannableStingBuilder;
        this.avatarIndex = avatarIndex;
    }

}
