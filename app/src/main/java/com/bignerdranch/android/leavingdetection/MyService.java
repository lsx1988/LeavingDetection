package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import org.litepal.crud.DataSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MyService extends Service {

    private WifiManager wifiManager = null;
    private WifiInfo wifiInfo = null;
    private android.os.Handler mHandler = null;
    private Wifi mWifi = null;
    private String[] blank = {"a", "b", "c"};
    private double result = 0;
    private String str = null;

    private static final String TAG = "MyService";
    private ScanWifi mBinder = new ScanWifi();
    private double homeWifilevel = 0;
    private int queueSize = 3;
    private double min_mean = 0, num_mean = 0, mean_mean = 0, std_mean = 0, is_wifi_mean = 0,
            min_diff = 0, num_diff = 0, mean_diff = 0, std_diff = 0;
    private BufferedReader model = null;
    private svm_predict predict = null;
    private double isHomeWifi = 1.0;
    private double numOfWifi = 0.0;
    private double allWifiLevel = 0.0;
    private double meanOfAllWifi = 0.0;
    private double stdOfAllWifi = 0.0;
    private TimerTask mTimerTask = null;

    class ScanWifi extends Binder {
        public void startScanning(android.os.Handler handler, Wifi wifi) {
            mHandler = handler;
            mWifi = wifi;
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            predict = new svm_predict();

            Timer timer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    wifiInfo = wifiManager.getConnectionInfo();

                    homeWifilevel = wifiInfo.getRssi();

                    if (Math.abs(homeWifilevel) >=95) {
                        homeWifilevel = -95;
                        isHomeWifi = 0.0;
                    } else {
                        isHomeWifi = 1.0;
                    }

                    List<ScanResult> scanResults=wifiManager.getScanResults();//搜索到的设备列表
                    for (ScanResult scanResult : scanResults) {
                        allWifiLevel = allWifiLevel + scanResult.level;
                    }

                    Log.d(TAG, Double.toString(allWifiLevel));

                    numOfWifi = scanResults.size();
                    meanOfAllWifi = allWifiLevel / numOfWifi;

                    for (int i = 0; i < scanResults.size(); i++) {
                        stdOfAllWifi += Math.pow((double)scanResults.get(i).level - meanOfAllWifi,2);
                    }
                    stdOfAllWifi = Math.sqrt(stdOfAllWifi / numOfWifi);


                    SensorData sample = new SensorData();
                    sample.setHomeWifiLevel(homeWifilevel);
                    sample.setNumOfWifi(numOfWifi);
                    sample.setMeanOfAllWifiLevel(meanOfAllWifi);
                    sample.setStdOfAllWifiLevel(stdOfAllWifi);
                    sample.setIsHomeWifi(isHomeWifi);
                    sample.saveThrows();

                    stdOfAllWifi = 0;
                    allWifiLevel = 0;

                    if (DataSupport.count(SensorData.class) == queueSize) {

                        min_mean = 0;
                        num_mean = 0;
                        mean_mean = 0;
                        std_mean = 0;
                        is_wifi_mean = 0;
                        min_diff = 0;
                        num_diff = 0;
                        mean_diff = 0;
                        std_diff = 0;

                        min_mean = DataSupport.average(SensorData.class, "homeWifiLevel");
                        num_mean = DataSupport.average(SensorData.class, "numOfWifi");
                        mean_mean = DataSupport.average(SensorData.class, "meanOfAllWifiLevel");
                        std_mean = DataSupport.average(SensorData.class, "stdOfAllWifiLevel");
                        is_wifi_mean = DataSupport.average(SensorData.class, "isHomeWifi");

                        min_mean = min_mean / Math.pow(10, (int) (Math.log10(Math.abs(min_mean)) + 1));
                        num_mean = num_mean / Math.pow(10, (int) (Math.log10(Math.abs(num_mean)) + 1));
                        mean_mean = mean_mean / Math.pow(10, (int) (Math.log10(Math.abs(mean_mean)) + 1));
                        std_mean = std_mean / Math.pow(10, (int) (Math.log10(Math.abs(std_mean)) + 1));

                        if (is_wifi_mean != 0) {
                            is_wifi_mean = is_wifi_mean / Math.pow(10, (int) (Math.log10(Math.abs(is_wifi_mean)) + 1));
                        }

                        min_diff = DataSupport.select("homeWifiLevel").findLast(SensorData.class).getHomeWifiLevel()
                                    - DataSupport.select("homeWifiLevel").findFirst(SensorData.class).getHomeWifiLevel();
                        num_diff = DataSupport.select("numOfWifi").findLast(SensorData.class).getNumOfWifi()
                                - DataSupport.select("numOfWifi").findFirst(SensorData.class).getNumOfWifi();
                        mean_diff = DataSupport.select("meanOfAllWifiLevel").findLast(SensorData.class).getMeanOfAllWifiLevel()
                                - DataSupport.select("meanOfAllWifiLevel").findFirst(SensorData.class).getMeanOfAllWifiLevel();
                        std_diff = DataSupport.select("stdOfAllWifiLevel").findLast(SensorData.class).getStdOfAllWifiLevel()
                                - DataSupport.select("stdOfAllWifiLevel").findFirst(SensorData.class).getStdOfAllWifiLevel();

                        if (min_diff != 0 && Math.abs(min_diff) >= 1) {
                            min_diff = min_diff / Math.pow(10, (int) (Math.log10(Math.abs(min_diff)) + 1));
                        }

                        if (num_diff != 0 && Math.abs(num_diff) >= 1) {
                            num_diff = num_diff / Math.pow(10, (int) (Math.log10(Math.abs(num_diff)) + 1));
                        }

                        if (mean_diff != 0 && Math.abs(mean_diff) >= 1) {
                            mean_diff = mean_diff / Math.pow(10, (int) (Math.log10(Math.abs(mean_diff)) + 1));
                        }

                        if (std_diff != 0 && Math.abs(std_diff) >= 1) {
                            std_diff = std_diff / Math.pow(10, (int) (Math.log10(Math.abs(std_diff)) + 1));
                        }

                        str = 0 + " 1:" + min_mean + " " + "2:" + num_mean + " " + "3:" + mean_mean
                                + " " + "4:" + std_mean + " " + "5:" + is_wifi_mean + " " + "6:"
                                + min_diff + " " + "7:" + num_diff + " " + "8:" + mean_diff + " "
                                + "9:" + std_diff;
                        Log.d(TAG, str);

                        InputStream modelFile = getResources().openRawResource(R.raw.model);
                        model = new BufferedReader(new InputStreamReader(modelFile));

                        try {
                            result = predict.main(blank, str, model);
                            Message msg = new Message();
                            if (result != 1.0) {
                                Bundle data = new Bundle();
                                data.putDouble("wifiLevel", homeWifilevel);
                                msg.what = 0;
                                msg.setData(data);
                            } else {
                                msg.what = 1;
                                mTimerTask.cancel();
                            }
                            mHandler.sendMessage(msg);
                        } catch (IOException e) {
                            Log.d(TAG, "onCreate: ");
                        }

                        int id = DataSupport.findFirst(SensorData.class).getId();
                        DataSupport.delete(SensorData.class,id);
                    }
                }
            };
            timer.scheduleAtFixedRate(mTimerTask, 1000, 2000);
            Log.d(TAG, "startDownload executed");
        }
    }

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        DataSupport.deleteAll(SensorData.class);
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate executed ");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand executed");
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        mTimerTask.cancel();
        Log.d(TAG, "onDestroy executed");
        super.onDestroy();
    }

    private double UnitNormalization(Double num) {
        if (num != 0 && Math.abs(num) >= 1) {
            return num / Math.pow(10, (int) (Math.log10(Math.abs(num)) + 1));
        }
        return num;
    }
}
