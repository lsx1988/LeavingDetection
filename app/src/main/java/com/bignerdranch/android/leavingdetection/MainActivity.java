package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Vibrator vibrator;
    private Intent serviceIntent;
    private double wifiLevel;
    private double possibility;
    private double predict;
    private boolean  stayInside= false, stayOutside= false;

    @BindView(R.id.wifi_level) TextView mTextView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.possibility) TextView mPossibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        mTextView.setVisibility(View.VISIBLE);
        serviceIntent = new Intent(this, MyService.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(serviceIntent);
        EventBus.getDefault().register(this);
    }

    @OnClick(R.id.stop_detection) void stop() {
        stopService(serviceIntent);
        mTextView.setText(R.string.stop_run);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        updateUI(event, mTextView, mPossibility);
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

    private void updateUI(MessageEvent event, TextView mTextView, TextView mPossibility) {

        wifiLevel = event.getWifiLevel();
        possibility = event.getPossibility();
        predict = event.getPredict();

        mTextView.setText(String.valueOf(wifiLevel));
        mPossibility.setText(String.format("%.2f", possibility * 100) + "%");

        if (predict == 1.0) {

            if (possibility >= 0.95) {
                stayOutside = true;
            }

            if (stayOutside == true && stayInside == true) {
                vibrator.vibrate(2000);
                stayInside = false;
            }

        }else if (predict != 1.0) {

            if (possibility <= 0.1) {
                stayInside = true;
            }

            if (stayInside == true && stayOutside == true) {
                vibrator.vibrate(2000);
                stayOutside = false;
            }
        }
    }
}
