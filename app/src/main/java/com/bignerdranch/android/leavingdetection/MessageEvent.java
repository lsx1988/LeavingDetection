package com.bignerdranch.android.leavingdetection;

/**
 * Created by shixunliu on 25/3/17.
 */

public class MessageEvent {

    public double wifiLevel;
    public double possibility;
    public boolean isAlarm;

    public MessageEvent(double wifiLevel, double possibility, boolean isAlarm) {
        this.wifiLevel = wifiLevel;
        this.possibility = possibility;
        this.isAlarm = isAlarm;
    }

    public double getWifiLevel() {
        return wifiLevel;
    }

    public void setWifiLevel(double wifiLevel) {
        this.wifiLevel = wifiLevel;
    }

    public double getPossibility() {
        return possibility;
    }

    public void setPossibility(double possibility) {
        this.possibility = possibility;
    }

    public boolean isAlarm() {
        return isAlarm;
    }

    public void setAlarm(boolean alarm) {
        isAlarm = alarm;
    }
}
