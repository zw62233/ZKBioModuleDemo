package com.armatura.biomodule.common;

/**
 * Created by Magic on 2022/3/26
 */
public enum IdentifyState {
    IDENTIFY_CONST,
    IDENTIFY_ONCE,
    STOP;

    private boolean isIdentified = false;

    public boolean isIdentified() {
        return isIdentified;
    }

    public IdentifyState setIdentified(boolean identified) {
        isIdentified = identified;
        return this;
    }
}
