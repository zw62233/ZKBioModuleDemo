package com.armatura.biomodule.pojo.face.recognize;

import androidx.annotation.NonNull;

/**
 * Created by Magic on 2020/9/11
 */
public class IdentifyInfo {
    private String groupId;
    private String name;
    private String personId;
    private float similarity;
    private String userId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @NonNull
    @Override
    public String toString() {
        return "IdentifyInfo{" +
                "groupId='" + groupId + '\'' +
                ", name='" + name + '\'' +
                ", personId='" + personId + '\'' +
                ", similarity=" + similarity +
                ", userId='" + userId + '\'' +
                '}';
    }
}
