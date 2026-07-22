package com.armatura.biomodule.pojo.common;

import com.armatura.biomodule.config.Config;

/**
 * Created by Magic on 2020/9/27
 */
public class Attribute {
    public int age;
    public int cap;
    public int expression;
    public int gender;
    public int glasses;
    public int mustache;
    public int respirator = -1;
    public int respiratorLevel = -1;

    public String toShortString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (Config.instance().isShowAgeAttribute) {
            stringBuilder.append("Age: ").append(getAssumeRange(age, 5)).append("\n");
        }
        if (Config.instance().isShowGenderAttribute) {
            stringBuilder.append("Gender: ").append(getGenderDescription(gender)).append("\n");
        }
        if (Config.instance().isShowExpressionAttribute) {
            stringBuilder.append("Expression: ").append(getExpression(expression)).append("\n");
        }
        if (Config.instance().isShowMustacheAttribute) {
            stringBuilder.append("Mustache: ").append(getMustacheDescription(mustache)).append("\n");
        }
        if (Config.instance().isShowGlassesAttribute) {
            stringBuilder.append("Glasses: ").append(getGlassesDescription(glasses)).append("\n");
        }
        if (Config.instance().isShowHatAttribute) {
            stringBuilder.append("Hat: ").append(getCapDescription(cap)).append("\n");
        }
        if (Config.instance().isShowMaskAttribute) {
            stringBuilder.append("Face Mask: ").append(getMaskStatus(respirator, respiratorLevel));
        }
        return stringBuilder.toString();
    }

    public static String getGenderDescription(int gender) {
        switch (gender) {
            case 0:
                return "Female";
            case 1:
                return "Male";
            default:
                return "Unknown";
        }
    }

    public static String getCapDescription(int cap) {
        switch (cap) {
            case 0:
                return "No hat";
            case 1:
                return "Winter hat";
            case 2:
                return "Other hat";
            default:
                return "Unknown";
        }
    }

    public static String getMaskStatus(int respirator, int respiratorLevel) {
        if (respirator == 1 && respiratorLevel == 1) {
            return "Correctly wearing";
        } else if (respirator == 1 && respiratorLevel > 1) {
            return "Incorrectly wearing";
        } else {
            return "Without wearing";
        }
    }


    public static String getGlassesDescription(int glasses) {
        switch (glasses) {
            case 0:
                return "No Glasses";
            case 1:
                return "Ordinary glasses";
            case 2:
                return "Sunglasses";
            default:
                return "Unknown";

        }
    }

    public static String getMustacheDescription(int mustache) {
        switch (mustache) {
            case 0:
                return "No";
            case 1:/**/
            case 2:
            case 3:
                return "Yes";
            default:
                return "Unknown";

        }
    }

    public static String getExpression(int expression) {
        switch (expression) {
            case 0:
                return "Calm";
            case 1:
                return "Happy";
            case 2:
                return "Angry";
            case 3:
                return "Sorrow";
            case 4:
                return "Surprise";
            default:
                return "Unknown(" + expression + ")";
        }
    }

    public static String getAssumeRange(int num, int range) {
        int min = num - range;
        int max = min + range;
        return min + "~" + max;
    }
}
