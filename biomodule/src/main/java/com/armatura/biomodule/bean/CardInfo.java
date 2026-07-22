package com.armatura.biomodule.bean;

import android.text.SpannableStringBuilder;

import com.armatura.biomodule.pojo.common.Label;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "CardInfo")
public class CardInfo {
    @DatabaseField(generatedId = true)
    public long id;

    @DatabaseField(columnName = "userId", canBeNull = false)
    public String userId;

    @DatabaseField(columnName = "rawCard", canBeNull = false)
    public String rawCard;

    @DatabaseField(columnName = "card")
    public String card;

    public transient UserInfo userInfo;
    public transient boolean isIdentifySuccess;

    public IdentifyInfoBoard toIdentifyInfoBoard() {
        IdentifyInfoBoard identifyInfoBoard = new IdentifyInfoBoard();
        identifyInfoBoard.identifyType = Label.LABEL_CARD;
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if (isIdentifySuccess) {
            identifyInfoBoard.avatarIndex = userInfo.avatarIndex;
            identifyInfoBoard.userName = userInfo.name;
        }
        stringBuilder.append("Card : ").append(rawCard);
        identifyInfoBoard.identifyInfoSpannableStingBuilder = stringBuilder;
        return identifyInfoBoard;
    }


    public String getRawCard() {
        return rawCard;
    }

    public CardInfo setRawCard(String rawCard) {
        this.rawCard = rawCard;
        return this;
    }
}
