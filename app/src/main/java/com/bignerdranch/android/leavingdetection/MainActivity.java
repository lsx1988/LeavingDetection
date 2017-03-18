package com.bignerdranch.android.leavingdetection;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.idescout.sql.SqlScoutServer;

public class MainActivity extends AppCompatActivity {

    private MyService.ScanWifi mScanWifi;
    private TextView mTextView = null;
    private Button mIndicator = null;
    private ProgressBar mProgressBar = null;
    private Wifi mWifi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SqlScoutServer.create(this, getPackageName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWifi = Wifi.get(this);
        mTextView = (TextView) findViewById(R.id.wifi_level);
        mTextView.setVisibility(View.VISIBLE);
        mIndicator = (Button) findViewById(R.id.detection_indicator);
        mIndicator.setVisibility(View.INVISIBLE);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    public void clickBtn(View view) {

        switch (view.getId()) {
            case R.id.start_detection:
                Intent bindIntent = new Intent(this, MyService.class);
                bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);
                mTextView.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                mIndicator.setVisibility(View.INVISIBLE);
                break;
            case R.id.stop_detection:
                unbindService(mServiceConnection);
                mTextView.setText(R.string.stop_run);
        }
    }

    private ServiceConnection mServiceConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mScanWifi = (MyService.ScanWifi) iBinder;
            mScanWifi.startScanning(mHandler, mWifi);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mTextView.setText(String.valueOf(msg.getData().getDouble("wifiLevel")));
                    mTextView.setVisibility(View.VISIBLE);
                    mIndicator.setVisibility(View.INVISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    mTextView.setVisibility(View.INVISIBLE);
                    mIndicator.setVisibility(View.VISIBLE);
                    unbindService(mServiceConnection);
                default:
                    mTextView.setText(R.string.Wifi_disconnect);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }
}
