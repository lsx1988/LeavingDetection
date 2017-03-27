package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.idescout.sql.SqlScoutServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

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
    private double pressure;
    private boolean  stayInside= false, stayOutside= false;

    @BindView(R.id.wifi_level) TextView mTextView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.possibility) TextView mPossibility;
    @BindView(R.id.pressure_level) TextView mPressure;
    @BindView(R.id.use_pressure) CheckBox usePressure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LitePal.initialize(this);
        SqlScoutServer.create(this, getPackageName());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        mTextView.setVisibility(View.INVISIBLE);
        mPossibility.setVisibility(View.INVISIBLE);
        mPressure.setVisibility(View.INVISIBLE);
        usePressure.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @OnClick(R.id.start_detection) void start() {

        mTextView.setVisibility(View.INVISIBLE);
        mPossibility.setVisibility(View.INVISIBLE);
        usePressure.setVisibility(View.INVISIBLE);
        mPressure.setVisibility(View.INVISIBLE);

        mProgressBar.setVisibility(View.VISIBLE);

        EventBus.getDefault().register(this);
        serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("usePressure", usePressure.isChecked());
        this.startService(serviceIntent);
    }

    @OnClick(R.id.stop_detection) void stop() {
        EventBus.getDefault().unregister(this);
        stopService(serviceIntent);
        mTextView.setVisibility(View.INVISIBLE);
        mPossibility.setVisibility(View.INVISIBLE);
        mPressure.setVisibility(View.INVISIBLE);
        usePressure.setVisibility(View.VISIBLE);
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

        mTextView.setVisibility(View.VISIBLE);
        mPossibility.setVisibility(View.VISIBLE);

        if(usePressure.isChecked() == true) {
            mPressure.setVisibility(View.VISIBLE);
        }

        mProgressBar.setVisibility(View.INVISIBLE);

        wifiLevel = event.getWifiLevel();
        possibility = event.getPossibility();
        predict = event.getPredict();
        pressure = event.getPressure();

        mTextView.setText(String.valueOf(wifiLevel));
        mPossibility.setText(String.format("%.2f", possibility * 100) + "%");
        mPressure.setText(String.format("%.2f", pressure) + "Pa");

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
