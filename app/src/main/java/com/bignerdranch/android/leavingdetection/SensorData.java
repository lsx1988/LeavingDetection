package com.bignerdranch.android.leavingdetection;

/**
 * Created by shixunliu on 13/3/17.
 */

public class SensorData {

    private Double homeWifiLevel;

    private Double numOfWifi;

    private Double meanOfAllWifiLevel;

    private Double stdOfAllWifiLevel;

    private Double isHomeWifi;

    public Double getHomeWifiLevel() {
        return homeWifiLevel;
    }

    public void setHomeWifiLevel(Double homeWifiLevel) {
        this.homeWifiLevel = homeWifiLevel;
    }

    public Double getNumOfWifi() {
        return numOfWifi;
    }

    public void setNumOfWifi(Double numOfWifi) {
        this.numOfWifi = numOfWifi;
    }

    public Double getMeanOfAllWifiLevel() {
        return meanOfAllWifiLevel;
    }

    public void setMeanOfAllWifiLevel(Double meanOfAllWifiLevel) {
        this.meanOfAllWifiLevel = meanOfAllWifiLevel;
    }

    public Double getStdOfAllWifiLevel() {
        return stdOfAllWifiLevel;
    }

    public void setStdOfAllWifiLevel(Double stdOfAllWifiLevel) {
        this.stdOfAllWifiLevel = stdOfAllWifiLevel;
    }

    public Double getIsHomeWifi() {
        return isHomeWifi;
    }

    public void setIsHomeWifi(Double isHomeWifi) {
        this.isHomeWifi = isHomeWifi;
    }
}
