package com.armatura.biomodule.pojo.module.register;

/**
 * Auto-generated: 2020-09-07 15:31:21
 *
 * @author magic.hu
 */
public class AccessInfo {

    private String cardNum;
    private String password;
    private int authType;
    private int roleType;

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setAuthType(int authType) {
        this.authType = authType;
    }

    public int getAuthType() {
        return authType;
    }

    public void setRoleType(int roleType) {
        this.roleType = roleType;
    }

    public int getRoleType() {
        return roleType;
    }

}