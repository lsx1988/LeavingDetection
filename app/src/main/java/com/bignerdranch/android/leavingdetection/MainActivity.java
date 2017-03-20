package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.idescout.sql.SqlScoutServer;

import org.litepal.crud.DataSupport;

public class MainActivity extends AppCompatActivity {

    private MyService.ScanWifi mScanWifi;
    private TextView mTextView = null;
    private TextView mPossibility = null;
    private ProgressBar mProgressBar = null;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SqlScoutServer.create(this, getPackageName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
        mTextView = (TextView) findViewById(R.id.wifi_level);
        mTextView.setVisibility(View.VISIBLE);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mPossibility = (TextView) findViewById(R.id.possibility);
    }

    public void clickBtn(View view) {

        switch (view.getId()) {
            case R.id.start_detection:
                Intent bindIntent = new Intent(this, MyService.class);
                bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);
                mTextView.setVisibility(View.INVISIBLE);
                mPossibility.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                break;
            case R.id.stop_detection:
                unbindService(mServiceConnection);
                DataSupport.deleteAll(SensorData.class);
                mTextView.setText(R.string.stop_run);
        }
    }

    private ServiceConnection mServiceConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mScanWifi = (MyService.ScanWifi) iBinder;
            mScanWifi.startScanning(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {

            mTextView.setVisibility(View.VISIBLE);
            mPossibility.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);

            double possibility = msg.getData().getDouble("Possibility")*100;

            mTextView.setText(String.valueOf(msg.getData().getDouble("wifiLevel")));
            mPossibility.setText(String.format("%.2f", possibility) + "%");

            if (msg.getData().getBoolean("alarm") == true){
                vibrator.vibrate(2000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }
}
