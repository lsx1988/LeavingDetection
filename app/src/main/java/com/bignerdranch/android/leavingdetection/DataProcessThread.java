package com.bignerdranch.android.leavingdetection;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by shixunliu on 26/3/17.
 */

public class DataProcessThread implements Runnable {

    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private String[] blank = {"-b","1", "a","b", "c"};
    private String[] blank_scale = {"-r","scale_para","data"};
    private double[] result = null;
    private String str = null;
    private static final String TAG = "MyThread";
    private double homeWifilevel = 0;
    private int queueSize = 10;
    private BufferedReader model = null;
    private svm_predict predict = null;
    private double isHomeWifi = 1.0, allWifiLevel = 0.0, meanOfAllWifi = 0.0;
    private boolean  stayInside= false, stayOutside= false;
    private Context mContext;

    public DataProcessThread(WifiManager wifiManager, Context context) {
        this.wifiManager = wifiManager;
        this.mContext = context;
        this.predict = new svm_predict();
    }

    @Override
    public void run() {
        Log.e(TAG, String.valueOf(Thread.currentThread().getId()));
        wifiManager.startScan();
        wifiInfo = wifiManager.getConnectionInfo();
        homeWifilevel = wifiInfo.getRssi();

        if (Math.abs(homeWifilevel) >= 95) {
            homeWifilevel = -95;
            isHomeWifi = 0.0;
        } else {
            isHomeWifi = 1.0;
        }

        List<ScanResult> scanResults = wifiManager.getScanResults();//搜索到的设备列表
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

            if (stayInside == false && stayOutside == true) {
                str = 0 + " 1:" + getMean("homeWifiLevel")
                        + " 2:" + getMean("meanOfAllWifiLevel")
                        + " 3:" + getMean("isHomeWifi")
                        + " 4:" + getStd("stdOfAllWifiLevel")
                        + " 5:" + (-getSumVar("homeWifiLevel"))
                        + " 6:" + (-getSumVar("meanOfAllWifiLevel"));
            } else {
                str = 0 + " 1:" + getMean("homeWifiLevel")
                        + " 2:" + getMean("meanOfAllWifiLevel")
                        + " 3:" + getMean("isHomeWifi")
                        + " 4:" + getStd("stdOfAllWifiLevel")
                        + " 5:" + getSumVar("homeWifiLevel")
                        + " 6:" + getSumVar("meanOfAllWifiLevel");
            }
            Log.d(TAG, str);
            InputStream modelFile = mContext.getResources().openRawResource(R.raw.model_scale);
            model = new BufferedReader(new InputStreamReader(modelFile));
            try {
                String scale_result = svm_scale.main(blank_scale, str, mContext);
                Log.d(TAG, scale_result);
                result = predict.main(blank, scale_result, model);
                EventBus.getDefault().post(new MessageEvent(getMean("homeWifiLevel"), result[1], result[0]));
            } catch (IOException e) {
                Log.d(TAG, "onCreate: ");
            }
            int id = DataSupport.findFirst(SensorData.class).getId();
            DataSupport.delete(SensorData.class, id);
        }
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
