package com.bignerdranch.android.leavingdetection;

import org.litepal.crud.DataSupport;

/**
 * Created by shixunliu on 26/3/17.
 */

public class PressureData extends DataSupport {

    private double pressure;
    private int id;

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
