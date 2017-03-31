package com.bignerdranch.android.leavingdetection;

/**
 * Created by shixunliu on 25/3/17.
 */

public class MessageEvent {

    public double wifiLevel;
    public double possibility;
    public double predict;
    public double pressure;


    public MessageEvent(double wifiLevel, double possibility, double predict, double pressure) {
        this.wifiLevel = wifiLevel;
        this.possibility = possibility;
        this.predict = predict;
        this.pressure = pressure;
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

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }
}
