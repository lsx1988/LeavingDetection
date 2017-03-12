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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
    private int count = 0;
    private LimitQueue<ArrayList<Double>> mQueue = null;
    private int queueSize = 3;
    private double min_mean = 0, num_mean = 0, mean_mean = 0, std_mean = 0, is_wifi_mean = 0,
            min_first = 0, num_first = 0, mean_first = 0, std_first = 0, min_last = 0, num_last = 0,
            mean_last = 0, std_last = 0, min_diff = 0, num_diff = 0, mean_diff = 0, std_diff = 0;
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
            mQueue = new LimitQueue<>(queueSize);
            wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            predict = new svm_predict();

            Timer timer = new Timer();
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    wifiInfo = wifiManager.getConnectionInfo();
                    //获得信号强度值
                    homeWifilevel = wifiInfo.getRssi();
                    //根据获得的信号强度发送信息
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


                    ArrayList<Double> currentData = new ArrayList<>();
                    currentData.add(homeWifilevel); //min
                    currentData.add(numOfWifi); //num of Wifi
                    currentData.add(meanOfAllWifi); //mean
                    currentData.add(stdOfAllWifi); //std
                    currentData.add(isHomeWifi); //is Home wifi

                    mQueue.offer(currentData);

                    stdOfAllWifi = 0;
                    allWifiLevel = 0;

                    if (mQueue.size() == queueSize) {

                        count = 0;

                        min_mean = 0;
                        num_mean = 0;
                        mean_mean = 0;
                        std_mean = 0;
                        is_wifi_mean = 0;
                        min_first = 0;
                        num_first = 0;
                        mean_first = 0;
                        std_first = 0;
                        min_last = 0;
                        num_last = 0;
                        mean_last = 0;
                        std_last = 0;
                        min_diff = 0;
                        num_diff = 0;
                        mean_diff = 0;
                        std_diff = 0;

                        for (ArrayList<Double> list : mQueue.getQueue()) {
                            count++;
                            min_mean = min_mean + list.get(0);
                            num_mean = num_mean + list.get(1);
                            mean_mean = mean_mean + list.get(2);
                            std_mean = std_mean + list.get(3);
                            is_wifi_mean = is_wifi_mean + list.get(4);

                            if (count == 1) {
                                min_first = list.get(0);
                                num_first = list.get(1);
                                mean_first = list.get(2);
                                std_first = list.get(3);
                            }

                            if (count == queueSize) {
                                min_last = list.get(0);
                                num_last = list.get(1);
                                mean_last = list.get(2);
                                std_last = list.get(3);
                            }
                        }

                        min_mean = min_mean / queueSize;
                        num_mean = num_mean / queueSize;
                        mean_mean = mean_mean / queueSize;
                        std_mean = std_mean / queueSize;
                        is_wifi_mean = is_wifi_mean / queueSize;

                        //Log.d(TAG, Double.toString(Math.pow((int) (Math.log10(Math.abs(min_mean)) + 1),10)));
                        min_mean = min_mean / Math.pow(10, (int) (Math.log10(Math.abs(min_mean)) + 1));
                        num_mean = num_mean / Math.pow(10, (int) (Math.log10(Math.abs(num_mean)) + 1));
                        mean_mean = mean_mean / Math.pow(10, (int) (Math.log10(Math.abs(mean_mean)) + 1));

                        std_mean = std_mean / Math.pow(10, (int) (Math.log10(Math.abs(std_mean)) + 1));

                        if (is_wifi_mean != 0) {
                            is_wifi_mean = is_wifi_mean / Math.pow(10, (int) (Math.log10(Math.abs(is_wifi_mean)) + 1));
                        }

                        min_diff = min_last - min_first;
                        num_diff = num_last - num_first;
                        mean_diff = mean_last - mean_first;
                        std_diff = std_last - std_first;
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

                        str = 0 + " 1:" + min_mean + " " + "2:" + num_mean + " " + "3:" + mean_mean + " " + "4:" + std_mean + " " + "5:" + is_wifi_mean + " " + "6:" + min_diff + " " + "7:" + num_diff + " " + "8:" + mean_diff + " " + "9:" + std_diff;
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
}
