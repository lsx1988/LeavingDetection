package com.bignerdranch.android.leavingdetection;

import android.content.Context;

/**
 * Created by shixunliu on 6/3/17.
 */

public class Wifi {

    private static Wifi sWifi;
    private double level;

    public static Wifi get(Context context) {
        if (sWifi == null) {
            sWifi = new Wifi(context);
        }
        return sWifi;
    }

    private Wifi(Context context) {
        level = 0;
    }

    public double getLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = level;
    }
}
