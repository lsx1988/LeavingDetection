package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.idescout.sql.SqlScoutServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Vibrator vibrator;
    private Intent serviceIntent;
    private String wifiLevel;
    private String possibility;
    private boolean isAlarm;

    @BindView(R.id.wifi_level) TextView mTextView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.possibility) TextView mPossibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SqlScoutServer.create(this, getPackageName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        mTextView.setVisibility(View.VISIBLE);
        serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @OnClick(R.id.stop_detection) void stop() {
        stopService(serviceIntent);
        DataSupport.deleteAll(SensorData.class);
        mTextView.setText(R.string.stop_run);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Log.e(TAG, String.valueOf(Thread.currentThread().getId()));
        mTextView.setVisibility(View.VISIBLE);
        mPossibility.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);

        wifiLevel = String.valueOf(event.getWifiLevel());
        possibility = String.format("%.2f", event.getPossibility()*100) + "%";
        isAlarm = event.isAlarm();

        mTextView.setText(wifiLevel);
        mPossibility.setText(possibility);

        if (isAlarm == true){
            vibrator.vibrate(2000);
        }

    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
