package com.armatura.biomodule.pojo.common;

import androidx.annotation.NonNull;

/**
 * Common Json Struct
 * <p>
 * {"status": 0, "detail":"success", "data":{
 * "xxx" : {
 * "xxx":xxx
 * }
 * }
 * }
 *
 * @author magic.hu@armatura.com
 * @date 2020/08/07
 * @since 1.0.0
 */
public class CommonConfigData {
    public int status;
    public String detail;
    public Object data;

    @NonNull
    @Override
    public String toString() {
        return "CommonConfigData{" +
                "status=" + status +
                ", detail='" + detail + '\'' +
                ", data=" + data +
                '}';
    }
}
