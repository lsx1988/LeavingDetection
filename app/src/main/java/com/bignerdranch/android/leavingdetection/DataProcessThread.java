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

    private WifiManager mWifiManager;
    private WifiInfo wifiInfo;
    private String[] blank = {"-b","1", "a","b", "c"};
    private String[] blank_scale = {"-r","scale_para","data"};
    private double[] result = null;
    private String str = null;
    private static final String TAG = "MyThread";
    private int queueSize = 10;
    private BufferedReader model = null;
    private svm_predict predict = null;
    private Context mContext;
    private PressureData mPressureData;
    private boolean isPressureExit = false;
    private InputStream modelFile;

    public DataProcessThread(PressureData pressureData, WifiManager wifiManager, Context context) {
        this.mWifiManager = wifiManager;
        this.mContext = context;
        this.predict = new svm_predict();
        this.mPressureData = pressureData;
        if(pressureData.getPressure() != 0) {
            this.isPressureExit = true;
        }
    }

    @Override
    public void run() {
        Log.e(TAG, String.valueOf(Thread.currentThread().getId()));

        saveWifiData(mWifiManager);
        if (isPressureExit == true) {
            savePressureData(mPressureData);
        }


        if (DataSupport.count(WifiData.class) == queueSize) {

            if (isPressureExit == false) {
                str = 0 + " 1:" + getMean("homeWifiLevel", WifiData.class)
                        + " 2:" + getMean("meanOfAllWifiLevel", WifiData.class)
                        + " 3:" + getMean("isHomeWifi", WifiData.class)
                        + " 4:" + getStd("stdOfAllWifiLevel")
                        + " 5:" + (getSumVar("homeWifiLevel"))
                        + " 6:" + (getSumVar("meanOfAllWifiLevel"));
            } else {
                str = 0 + " 1:" + getMean("pressure", PressureData.class)
                        + " 2:" + getMax("pressure")
                        + " 3:" + getMin("pressure")
                        + " 4:" + getVar("pressure")
                        + " 5:" + getEnergy("pressure")
                        + " 6:" + getDiff("pressure")
                        + " 7:" + getHarr_1("pressure")[0]
                        + " 8:" + getHarr_1("pressure")[1]
                        + " 9:" + getHarr_1("pressure")[2]
                        + " 10:" + getHarr_1("pressure")[3]
                        + " 11:" + getHarr_1("pressure")[4]
                        + " 12:" + getHarr_2("pressure")[0]
                        + " 13:" + getHarr_2("pressure")[1]
                        + " 14:" + getHarr_2("pressure")[2]
                        + " 15:" + getHarr_2("pressure")[3]
                        + " 16:" + getHarr_2("pressure")[4]
                        + " 17:" + getHarr_2("pressure")[5]
                        + " 18:" + getMean("homeWifiLevel", WifiData.class)
                        + " 19:" + getMean("meanOfAllWifiLevel", WifiData.class)
                        + " 20:" + getMean("isHomeWifi", WifiData.class)
                        + " 21:" + getStd("stdOfAllWifiLevel")
                        + " 22:" + getSumVar("homeWifiLevel")
                        + " 23:" + getSumVar("meanOfAllWifiLevel");
            }
            Log.d(TAG, str);
            if(isPressureExit == false) {
                modelFile = mContext.getResources().openRawResource(R.raw.model_wifi);
            } else {
                modelFile = mContext.getResources().openRawResource(R.raw.model_wifi_pressure);
            }

            model = new BufferedReader(new InputStreamReader(modelFile));
            try {
                String scale_result = svm_scale.main(blank_scale, str, isPressureExit, mContext);
                Log.d(TAG, scale_result);
                result = predict.main(blank, scale_result, model);
                if (isPressureExit == true) {
                    EventBus.getDefault().post(new MessageEvent(getMean("homeWifiLevel", WifiData.class), result[1], result[0], getMean("pressure", PressureData.class)));
                } else {
                    EventBus.getDefault().post(new MessageEvent(getMean("homeWifiLevel", WifiData.class), result[1], result[0], -1));
                }
            } catch (IOException e) {
                Log.d(TAG, "onCreate: ");
            }
            int id = DataSupport.findFirst(WifiData.class).getId();
            DataSupport.delete(WifiData.class, id);
            if(isPressureExit == true) {
                id = DataSupport.findFirst(PressureData.class).getId();
                DataSupport.delete(PressureData.class, id);
            }

        }
    }



    private double getSumVar(String col) {

        double average_first = 0, average_last = 0;
        List<WifiData> firstSet, lastSet;
        firstSet = DataSupport.select(col).order("id asc").limit(5).find(WifiData.class);
        lastSet = DataSupport.select(col).order("id desc").limit(5).find(WifiData.class);
        switch(col) {
            case "homeWifiLevel":
                for (WifiData data:firstSet) {
                    average_first += data.getHomeWifiLevel();
                }
                for (WifiData data:lastSet) {
                    average_last += data.getHomeWifiLevel();
                }

                break;
            case "meanOfAllWifiLevel":
                for (WifiData data:firstSet) {
                    average_first += data.getMeanOfAllWifiLevel();
                }

                for (WifiData data:lastSet) {
                    average_last += data.getMeanOfAllWifiLevel();
                }
                break;
        }
        average_first = average_first / firstSet.size();
        average_last = average_last / lastSet.size();
        return Math.abs(average_last-average_first);
    }

    private double getMean(String col, Class className) {
        double mean = DataSupport.average(className, col);
        return mean;
    }

    private double getStd(String col) {
        List<WifiData> temp = DataSupport.select(col).find(WifiData.class);
        double mean = DataSupport.average(WifiData.class,col);
        double result = 0;
        for (int i = 0; i < temp.size() - 1; i++) {
            result += Math.pow(temp.get(i).getStdOfAllWifiLevel() - mean,2);
        }
        return Math.sqrt(result / (temp.size() - 2));
    }

    private double getMax(String col) {
        return DataSupport.max(PressureData.class, col, double.class);
    }

    private double getMin(String col) {
        return DataSupport.min(PressureData.class, col, double.class);
    }

    private double getVar(String col) {
        double mean = getMean(col, PressureData.class);
        double count = DataSupport.count(PressureData.class);

        List<PressureData> temp = DataSupport.select(col).find(PressureData.class);
        double sum = 0;
        for (int i = 0; i < temp.size(); i++) {
            sum += Math.pow(temp.get(i).getPressure(),2);
        }

        return (sum - count * Math.pow(mean,2)) / (count - 1);
    }

    private double getEnergy(String col) {
        double count = DataSupport.count(PressureData.class);
        List<PressureData> temp = DataSupport.select(col).find(PressureData.class);
        double sum = 0;
        for (int i = 0; i < temp.size(); i++) {
            sum += Math.pow(temp.get(i).getPressure(),2);
        }
        return sum / count;
    }

    private double getDiff(String col) {
        double first = DataSupport.findFirst(PressureData.class).getPressure();
        double last = DataSupport.select(col).order("id desc").limit(1).find(PressureData.class).get(0).getPressure();
        return Math.abs(last-first);
    }

    private double[] getHarr_1 (String col) {
        double[] result = new double[5];
        int[] step = {1,3,5,7,9};
        double sumFirst = 0;
        double sumLast = 0;
        for(int i = 0; i < step.length; i++) {
            List<PressureData> firstSet = DataSupport.select(col).order("id asc").limit(step[i]).find(PressureData.class);
            for(PressureData data: firstSet){
                sumFirst += data.getPressure();
            }

            List<PressureData> lastSet = DataSupport.select(col).order("id desc").limit(step[i]).find(PressureData.class);
            for(PressureData data: lastSet){
                sumLast += data.getPressure();
            }

            result[i] = sumLast - sumFirst;
        }

        return result;
    }

    private double[] getHarr_2 (String col) {
        int count = DataSupport.count(PressureData.class);
        double[] result = new double[6];
        int[] step = {2,4,6,8,10,12};
        double sumFirst = 0;
        double sumLast = 0;
        for(int i = 0; i < step.length; i++) {

            int num = count / step[i];

            List<PressureData> firstSet = DataSupport.select(col).order("id asc").limit(num).find(PressureData.class);
            for(PressureData data: firstSet){
                sumFirst += data.getPressure();
            }

            List<PressureData> lastSet = DataSupport.select(col).order("id desc").limit(num).find(PressureData.class);
            for(PressureData data: lastSet){
                sumLast += data.getPressure();
            }

            result[i] = sumLast - sumFirst;
        }

        return result;
    }



    private void savePressureData(PressureData pressureData) {
        pressureData.saveThrows();
    }

    private void saveWifiData(WifiManager WifiManager) {

        WifiInfo wifiInfo = null;
        double isHomeWifi = 0;
        double allWifiLevel = 0;
        double meanOfAllWifi = 0;
        double homeWifiLevel = 0;

        mWifiManager.startScan();
        wifiInfo = mWifiManager.getConnectionInfo();


        homeWifiLevel = wifiInfo.getRssi();
        if (Math.abs(homeWifiLevel) >= 95) {
            homeWifiLevel = -95;
            isHomeWifi = 0.0;
        } else {
            isHomeWifi = 1.0;
        }

        List<ScanResult> scanResults = mWifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            allWifiLevel = allWifiLevel + scanResult.level;
        }
        meanOfAllWifi = allWifiLevel / scanResults.size();

        WifiData sample = new WifiData();
        sample.setHomeWifiLevel(homeWifiLevel);
        sample.setMeanOfAllWifiLevel(meanOfAllWifi);
        sample.setIsHomeWifi(isHomeWifi);
        sample.setStdOfAllWifiLevel(meanOfAllWifi);
        sample.saveThrows();
    }
}
