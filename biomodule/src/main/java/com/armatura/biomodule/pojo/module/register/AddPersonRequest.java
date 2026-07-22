package com.armatura.biomodule.pojo.module.register;

import com.armatura.biomodule.pojo.common.Image;

import java.util.List;

/**
 * Created by Magic on 2020/10/10
 */
public class AddPersonRequest {
    public String personId;
    public String groupId;
    public String userId;
    public String name;
    public int age;
    public String gender;
    public String phone;
    public String email;
    public long certificateType;
    public String certificateNumber;
    public String updateTime;

    public List<Image> images;
    public List<Features> features;

    public AccessInfo accessInfo;
}
