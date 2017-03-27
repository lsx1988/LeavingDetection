package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class MyService extends Service implements SensorEventListener {

    private WifiManager wifiManager = null;
    private SensorManager mSensorManager = null;
    private static final String TAG = "MyService";
    private Handler handler;
    private HandlerThread handlerThread;
    private long lastTimeStamp;
    private List<Double> pressureDataList;
    private boolean usePressure;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        usePressure = (boolean) intent.getExtras().get("usePressure");
        lastTimeStamp = 0;
        pressureDataList = new ArrayList<>();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        handler = trigerHandlerThread();
        registerSensor();
        DataSupport.deleteAll(WifiData.class);
        DataSupport.deleteAll(PressureData.class);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        handlerThread.quit();
        mSensorManager.unregisterListener(this);
        Log.d(TAG, "onDestroy executed");
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        long currentTimeStamp = event.timestamp / 1000000;
        
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            Log.d(TAG, String.valueOf(event.values[0]));
            pressureDataList.add(Double.parseDouble(Float.toString(event.values[0])));
        }

        //Log.d(TAG, String.valueOf(currentTimeStamp));

        if (currentTimeStamp - lastTimeStamp >= 1000) {
            double sum = 0;
            for (Double data: pressureDataList) {
                sum += data.doubleValue();
            }
            PressureData pressureData = new PressureData();
            if (usePressure == true) {
                pressureData.setPressure(sum / pressureDataList.size());
            } else {
                pressureData.setPressure(0);
            }
            handler.post(new DataProcessThread(pressureData, wifiManager,getApplicationContext()));
            lastTimeStamp = currentTimeStamp;
            pressureDataList.clear();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerSensor() {
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mSensorManager.registerListener(this, sensor, 100000);
            }
            if (sensor.getType() == Sensor.TYPE_PRESSURE && usePressure == true) {
                mSensorManager.registerListener(this, sensor, 100000);
            }
        }
    }

    private Handler trigerHandlerThread() {
        handlerThread = new HandlerThread("wifiThread");
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }
}
