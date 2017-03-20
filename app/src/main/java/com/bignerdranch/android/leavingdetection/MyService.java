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
    private String[] blank = {"-b","1", "a","b", "c"};
    private String[] blank_scale = {"-r","scale_para","data"};
    private double[] result = null;
    private String str = null;
    private static final String TAG = "MyService";
    private ScanWifi mBinder = new ScanWifi();
    private double homeWifilevel = 0;
    private int queueSize = 10;
    private BufferedReader model = null;
    private svm_predict predict = null;
    private double isHomeWifi = 1.0, allWifiLevel = 0.0, meanOfAllWifi = 0.0;
    private TimerTask mTimerTask = null;
    private Bundle data = null;
    private boolean  stayInside= false, stayOutside= false;

    class ScanWifi extends Binder {
        public void startScanning(android.os.Handler handler) {

            mHandler = handler;
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
                    meanOfAllWifi = allWifiLevel / scanResults.size();

                    allWifiLevel = 0;

                    SensorData sample = new SensorData();
                    sample.setHomeWifiLevel(homeWifilevel);
                    sample.setMeanOfAllWifiLevel(meanOfAllWifi);
                    sample.setIsHomeWifi(isHomeWifi);
                    sample.setStdOfAllWifiLevel(meanOfAllWifi);
                    sample.saveThrows();

                    if (DataSupport.count(SensorData.class) == queueSize) {

                        str = 0 + " 1:" + getMean("homeWifiLevel")
                                + " 2:" + getMean("meanOfAllWifiLevel")
                                + " 3:" + getMean("isHomeWifi")
                                + " 4:" + getStd("stdOfAllWifiLevel")
                                + " 5:" + getSumVar("homeWifiLevel")
                                + " 6:" + getSumVar("meanOfAllWifiLevel");
                        Log.d(TAG, str);
                        InputStream modelFile = getResources().openRawResource(R.raw.model_scale);
                        model = new BufferedReader(new InputStreamReader(modelFile));
                        try {
                            String scale_result = svm_scale.main(blank_scale,str, MyService.this);
                            Log.d(TAG, scale_result);
                            result = predict.main(blank, scale_result, model);
                            Message msg = new Message();

                            data = new Bundle();
                            data.putDouble("wifiLevel", getMean("homeWifiLevel"));
                            data.putDouble("Possibility", result[1]);

                            if (getMean("isHomeWifi") != 1.0 || result[0] == 1.0){

                                if (result[1] >= 0.95) {
                                    stayOutside = true;
                                }

                                if (stayOutside == true && stayInside == true) {
                                    data.putBoolean("alarm", true);
                                    if (result[1] >= 0.95) {
                                        stayInside = false;
                                    }
                                } else {
                                    data.putBoolean("alarm", false);
                                }
                            } else if (result[0] != 1.0) {

                                if (result[1] <= 0.1) {
                                    stayInside = true;
                                }

                                if (stayInside == true && stayOutside == true) {
                                    data.putBoolean("alarm", true);
                                    if (result[1] <= 0.1) {
                                        stayOutside = false;
                                    }
                                } else {
                                    data.putBoolean("alarm", false);
                                }
                            }
                            msg.setData(data);
                            mHandler.sendMessage(msg);
                        } catch (IOException e) {
                            Log.d(TAG, "onCreate: ");
                        }
                        int id = DataSupport.findFirst(SensorData.class).getId();
                        DataSupport.delete(SensorData.class,id);
                    }
                }
            };
            timer.scheduleAtFixedRate(mTimerTask, 0,1000);
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

    private double getSumVar(String col) {

        double average_first = 0, average_last = 0;
        List<SensorData> firstSet, lastSet;
        firstSet = DataSupport.select(col).order("id asc").limit(5).find(SensorData.class);
        lastSet = DataSupport.select(col).order("id desc").limit(5).find(SensorData.class);
        switch(col) {
            case "homeWifiLevel":
                for (SensorData data:firstSet) {
                    average_first += data.getHomeWifiLevel();
                }
                for (SensorData data:lastSet) {
                    average_last += data.getHomeWifiLevel();
                }

                break;
            case "meanOfAllWifiLevel":
                for (SensorData data:firstSet) {
                    average_first += data.getMeanOfAllWifiLevel();
                }

                for (SensorData data:lastSet) {
                    average_last += data.getMeanOfAllWifiLevel();
                }
                break;
        }
        average_first = average_first / firstSet.size();
        average_last = average_last / lastSet.size();
        return Math.abs(average_last-average_first);
    }

    private double getMean(String col) {
        double mean = DataSupport.average(SensorData.class, col);
        return mean;
    }

    private double getStd(String col) {
        List<SensorData> temp = DataSupport.select(col).find(SensorData.class);
        double mean = DataSupport.average(SensorData.class,col);
        double result = 0;
        for (int i = 0; i < temp.size() - 1; i++) {
            result += Math.pow(temp.get(i).getStdOfAllWifiLevel() - mean,2);
        }
        return Math.sqrt(result / (temp.size() - 2));
    }
}
