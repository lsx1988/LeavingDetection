package com.bignerdranch.android.leavingdetection;

/**
 * Created by shixunliu on 25/3/17.
 */

public class MessageEvent {

    public double wifiLevel;
    public double possibility;
    public double predict;


    public MessageEvent(double wifiLevel, double possibility, double predict) {
        this.wifiLevel = wifiLevel;
        this.possibility = possibility;
        this.predict = predict;
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

    public double getPredict() {
        return predict;
    }

    public void setPredict(double predict) {
        this.predict = predict;
    }
}
