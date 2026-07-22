
package com.armatura.biomodule.pojo.palm;


import android.graphics.Rect;

import androidx.annotation.NonNull;

public class PalmRect {

    public int x0;
    public int y0;
    public int x1;
    public int y1;
    public int x2;
    public int y2;
    public int x3;
    public int y3;

    public PalmRect copy() {
        PalmRect palmRect = new PalmRect();
        palmRect.setX0(x0);
        palmRect.setY0(y0);
        palmRect.setX1(x1);
        palmRect.setY1(y1);
        palmRect.setX2(x2);
        palmRect.setY2(y2);
        palmRect.setX3(x3);
        palmRect.setY3(y3);
        return palmRect;
    }

    public void setX0(int x0) {
        this.x0 = x0;
    }

    public int getX0() {
        return x0;
    }

    public void setY0(int y0) {
        this.y0 = y0;
    }

    public int getY0() {
        return y0;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX1() {
        return x1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getY1() {
        return y1;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getX2() {
        return x2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public int getY2() {
        return y2;
    }

    public void setX3(int x3) {
        this.x3 = x3;
    }

    public int getX3() {
        return x3;
    }

    public void setY3(int y3) {
        this.y3 = y3;
    }

    public int getY3() {
        return y3;
    }

    public Rect getRect() {
        return new Rect(x1, y1, x3, y3);
    }

    @NonNull
    @Override
    public String toString() {
        return x0 +
                ", y0=" + y0 +
                ", x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                ", x3=" + x3 +
                ", y3=" + y3 +
                '}';
    }
}