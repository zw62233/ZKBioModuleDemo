package com.armatura.biomodule.util;

public class StringHelper {

    public static int getValidLength(byte[] id) {
        int length = id.length;
        int END_CHAR = '\0';
        for (int i = 0; i < id.length; i++) {
            if (id[i] == END_CHAR) {
                length = i;
                break;
            }
        }
        return length;
    }
}
