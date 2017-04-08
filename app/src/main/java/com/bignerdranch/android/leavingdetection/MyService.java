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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class MyService extends Service{

    private static final String TAG = "MyService";
    private WifiManager wifiManager = null;
    private SensorManager mSensorManager = null;

    private HandlerThread sensorHandlerThread;
    private Handler sensorThreadHandler;

    private HandlerThread wifiHandlerThread;
    private Handler wifiThreadHandler;

    private long lastTimeStamp;
    private List<Double> pressureDataList;
    private LimitQueue<Double> accelMeterDataQueue;

    private boolean usePressure;
    private boolean wifiscanTrigger;
    private boolean isPhoneAlarm;

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
        wifiscanTrigger = true;
        isPhoneAlarm = true;

        lastTimeStamp = 0;
        pressureDataList = new ArrayList<>();
        accelMeterDataQueue = new LimitQueue<>(100);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorHandlerThread = new HandlerThread("sensorListenThread");
        sensorHandlerThread.start();
        sensorThreadHandler = new Handler(sensorHandlerThread.getLooper());
        registerSensor(sensorThreadHandler);

        wifiHandlerThread = new HandlerThread("wifiScanThread");
        wifiHandlerThread.start();
        wifiThreadHandler = new Handler(wifiHandlerThread.getLooper());

        // when the service is started, clear all the data in DB
        DataSupport.deleteAll(WifiData.class);
        DataSupport.deleteAll(PressureData.class);

        EventBus.getDefault().register(mSensorEventListener);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        sensorHandlerThread.quit();
        wifiHandlerThread.quit();
        mSensorManager.unregisterListener(mSensorEventListener);
        Log.d(TAG, "onDestroy executed");
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentTimeStamp = event.timestamp / 1000000;

            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                Log.d(TAG, String.valueOf(event.values[0]));
                pressureDataList.add(Double.parseDouble(Float.toString(event.values[0])));
            }

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                double accel = Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2));
                Log.d(TAG, "加速度: " + accel);
                accelMeterDataQueue.offer(accel);
                if (accelMeterDataQueue.size() == 100) {
                    double var = getVar(accelMeterDataQueue);
                    Log.d(TAG, " 加速度变化:" + var);
                    if (var > 1) {
                        wifiscanTrigger = true;
                        isPhoneAlarm = false;
                        EventBus.getDefault().post(new MessageStatus(true));
                    } else {
                        wifiscanTrigger = false;
                        EventBus.getDefault().post(new MessageStatus(false));
                    }
                }
            }

            if (wifiscanTrigger == true || isPhoneAlarm == false) {
                if (currentTimeStamp - lastTimeStamp >= 1000) {
                    double sum = 0;
                    for (Double data : pressureDataList) {
                        sum += data.doubleValue();
                    }
                    PressureData pressureData = new PressureData();
                    if (usePressure == true) {
                        pressureData.setPressure(sum / pressureDataList.size());
                    } else {
                        pressureData.setPressure(0);
                    }
                    wifiThreadHandler.post(new DataProcessThread(pressureData, wifiManager, getApplicationContext()));
                    lastTimeStamp = currentTimeStamp;
                    pressureDataList.clear();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onMessageIfAlarm(MessageIfAlarm event) {
            isPhoneAlarm = event.isPhoneHasAlarm();
        }
    };

    private void registerSensor(Handler handler) {
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mSensorManager.registerListener(mSensorEventListener, sensor, 100000, handler);
            }
            if (sensor.getType() == Sensor.TYPE_PRESSURE && usePressure == true) {
                mSensorManager.registerListener(mSensorEventListener, sensor, 100000, handler);
            }
        }
    }

    private double getVar(LimitQueue<Double> queue) {
        double mean = 0;
        int count = queue.size();
        double sum = 0;
        double sum_2 = 0;

        for (double num : queue) {
            sum += Math.pow(num,2);
            sum_2 += num;
        }
        mean = sum_2 / count;
        return (sum - count * Math.pow(mean,2)) / (count - 1);
    }
}
