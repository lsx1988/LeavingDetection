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

import com.idescout.sql.SqlScoutServer;

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
    private List<PressureData> pressureDataList;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SqlScoutServer.create(this, getPackageName());

        lastTimeStamp = 0;
        pressureDataList = new ArrayList<>();

        Log.d(TAG, "-------------------------------------------------------------------------"+String.valueOf(lastTimeStamp));

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        handler = trigerHandlerThread();

        registerSensor();

        Log.d(TAG, "onCreate executed ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand executed");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        handlerThread.quit();
        mSensorManager.unregisterListener(this);
        DataSupport.deleteAll(WifiData.class);
        Log.d(TAG, "onDestroy executed");
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        long currentTimeStamp = event.timestamp / 1000000;
        
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            Log.d(TAG, String.valueOf(event.values[0]));
            PressureData pressureData = new PressureData();
            pressureData.setPressure(event.values[0]);
            pressureDataList.add(pressureData);
        }

        //Log.d(TAG, String.valueOf(currentTimeStamp));

        if (currentTimeStamp - lastTimeStamp >= 1000) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                handler.post(new DataProcessThread(pressureDataList, wifiManager,getApplicationContext()));
                pressureDataList.clear();
            }

            lastTimeStamp = currentTimeStamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerSensor() {
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER
                    || sensor.getType() == Sensor.TYPE_PRESSURE) {
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
