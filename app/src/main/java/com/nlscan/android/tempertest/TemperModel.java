package com.nlscan.android.tempertest;

public class TemperModel {

    public native double[] currentTemper();

    public native void closeSerial();

    public native void initSerial();

    static {
        System.loadLibrary("temperGet");
    }
}
