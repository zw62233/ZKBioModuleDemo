package com.armatura.biomodule.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.armatura.biomodule.R;
import com.armatura.biomodule.activity.adapter.AvatarAdapter;
import com.armatura.biomodule.pojo.common.Image;
import com.armatura.biomodule.pojo.common.LiveData;
import com.armatura.biomodule.pojo.module.register.Features;
import com.armatura.biomodule.pojo.setting.VLPalmSetting;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@DatabaseTable(tableName = "UserInfo")
public class UserInfo implements Parcelable {
    /*Must specify one of id, generatedId, and generatedIdSequence with id*/
    @DatabaseField(generatedId = true)
    public long id;

    @DatabaseField(columnName = "groupId")
    public String groupId;

    @DatabaseField(columnName = "personId")
    public String personId;

    @DatabaseField(columnName = "name")
    public String name;

    @DatabaseField(columnName = "userId", canBeNull = false)
    public String userId;

    @DatabaseField(columnName = "age")
    public int age = -1;

    @DatabaseField(columnName = "gender")
    public String gender;

    @DatabaseField(columnName = "imageUri")
    public String imageUri;

    @DatabaseField(columnName = "updateTime")
    public String updateTime;


    @DatabaseField(columnName = "FaceFeature", dataType = DataType.BYTE_ARRAY)
    public transient byte[] faceFeature;

    @DatabaseField(columnName = "PalmFeature1", dataType = DataType.BYTE_ARRAY)
    public transient byte[] palmFeature1;

    @DatabaseField(columnName = "PalmFeature2", dataType = DataType.BYTE_ARRAY)
    public transient byte[] palmFeature2;

    @DatabaseField(columnName = "avatarIndex")
    public int avatarIndex = -1;

    /**
     * true if user has face template in module
     */
    public int face;

    /**
     * true if user has palm template in module
     */
    public int palm;

    private List<Image> images = null;

    private List<Features> features = null;

    public transient boolean bUpdate;
    public transient float similarity;
    public transient float nirSimilarity;
    private transient int identifyMode = VLPalmSetting.PALM_TEMPLATE_MODE_VL;

    public transient LiveData liveData;

    public UserInfo() {
        id = -1;
        name = "";
        bUpdate = false;
        similarity = -1;
        nirSimilarity = -1;
        personId = "";
    }

    public UserInfo copy() {
        UserInfo userInfo = new UserInfo();
        userInfo.userId = userId;
        userInfo.similarity = similarity;
        userInfo.nirSimilarity = nirSimilarity;
        userInfo.identifyMode = identifyMode;
        userInfo.id = id;
        userInfo.personId = personId;
        userInfo.name = name;
        userInfo.avatarIndex = avatarIndex;
        return userInfo;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public List<Features> getFeatures() {
        return features;
    }

    public void setFeatures(List<Features> features) {
        this.features = features;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }


    public int getIdentifyMode() {
        return identifyMode;
    }

    public void setIdentifyMode(int identifyMode) {
        this.identifyMode = identifyMode;
    }

    public int getAvatarDrawable() {
        if (avatarIndex == -1) {
            return R.drawable.default_avatar;
        } else {
            return AvatarAdapter.getAvatarDrawable(avatarIndex);
        }
    }


    @Override
    public String toString() {
        return "UserInfo{" +
                "id=" + id +
                ", groupId='" + groupId + '\'' +
                ", personId='" + personId + '\'' +
                ", name='" + name + '\'' +
                ", userId='" + userId + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", face=" + face +
                ", palm=" + palm +
                ", avatarIndex=" + avatarIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return hashCode() == userInfo.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, personId, name, userId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.groupId);
        dest.writeString(this.personId);
        dest.writeString(this.name);
        dest.writeString(this.userId);
        dest.writeInt(this.age);
        dest.writeString(this.gender);
        dest.writeString(this.imageUri);
        dest.writeString(this.updateTime);
        dest.writeInt(this.face);
        dest.writeInt(this.palm);
        dest.writeList(this.images);
        dest.writeList(this.features);
        dest.writeInt(this.avatarIndex);
    }

    protected UserInfo(Parcel in) {
        this.id = in.readLong();
        this.groupId = in.readString();
        this.personId = in.readString();
        this.name = in.readString();
        this.userId = in.readString();
        this.age = in.readInt();
        this.gender = in.readString();
        this.imageUri = in.readString();
        this.updateTime = in.readString();
        this.face = in.readInt();
        this.palm = in.readInt();
        this.images = new ArrayList<>();
        in.readList(this.images, Image.class.getClassLoader());
        this.features = new ArrayList<>();
        in.readList(this.features, Features.class.getClassLoader());
        this.avatarIndex = in.readInt();
    }

    public static final Parcelable.Creator<UserInfo> CREATOR = new Parcelable.Creator<UserInfo>() {
        @Override
        public UserInfo createFromParcel(Parcel source) {
            return new UserInfo(source);
        }

        @Override
        public UserInfo[] newArray(int size) {
            return new UserInfo[size];
        }
    };
}
