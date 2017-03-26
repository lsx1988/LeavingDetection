package com.bignerdranch.android.leavingdetection;

import org.litepal.crud.DataSupport;

/**
 * Created by shixunliu on 26/3/17.
 */

public class PressureData extends DataSupport {

    private float pressure;
    private int id;

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
