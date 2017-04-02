package com.bignerdranch.android.leavingdetection;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
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

    @BindView(R.id.wifi_level) TextView mWifiLevelTextView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.possibility) TextView mPossibilityTextView;
    @BindView(R.id.pressure_level) TextView mPressureTextView;
    @BindView(R.id.use_pressure) CheckBox isPressureOnCheckBox;
    @BindView(R.id.status) TextView mStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LitePal.initialize(this);
        SqlScoutServer.create(this, getPackageName());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        showPressureSelection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    /**
     *  press the start button, the app will show the progress bar to indicate the data collecting
     *  phase. Create the intent and start the service. If the service is already in operation, the
     *  activity will not activate the service again.
     */

    @OnClick(R.id.start_detection) void start() {
        showProgressBar();
        serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("usePressure", isPressureOnCheckBox.isChecked());
        this.startService(serviceIntent);
    }

    /**
     * when press the stop button, unregister the eventbus, stop the service and showup the pressure
     * selection. And only when stop button is clicked, the service will be stopped.
     */

    @OnClick(R.id.stop_detection) void stop() {
        stopService(serviceIntent);
        showPressureSelection();
    }

    /**
     * when the activity is not visible to user, unregister the eventbus.
     */

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /**
     * when receive the message, update the UI view.
     * @param event is the messageEvent send from background thread
     */

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        updateUI(event, mWifiLevelTextView, mPossibilityTextView);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageStatus(MessageStatus event) {
        boolean status = event.isWalking();
        if (status == true) {
            mStatusTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.isWalking));
            mStatusTextView.setText("You are walking");
        } else {
            mStatusTextView.setBackgroundColor(ContextCompat.getColor(this, R.color.notWalking));
            mStatusTextView.setText("Not walking");
        }
    }

    private void updateUI(MessageEvent event, TextView mWifiLevelTextView, TextView mPossibilityTextView) {

        if(isPressureOnCheckBox.isChecked() == true) {
            showWifiAndPressureData();
        } else {
            showWifiData();
        }

        wifiLevel = event.getWifiLevel();
        possibility = event.getPossibility();
        predict = event.getPredict();
        pressure = event.getPressure();

        mWifiLevelTextView.setText(String.valueOf(wifiLevel));
        mPossibilityTextView.setText(String.format("%.2f", possibility * 100) + "%");
        mPressureTextView.setText(String.format("%.2f", pressure) + "Pa");

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

    private void showPressureSelection() {
        mProgressBar.setVisibility(View.GONE);
        mWifiLevelTextView.setVisibility(View.GONE);
        mPressureTextView.setVisibility(View.GONE);
        mPossibilityTextView.setVisibility(View.GONE);
        isPressureOnCheckBox.setVisibility(View.VISIBLE);
    }

    private void showProgressBar() {
        isPressureOnCheckBox.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void showWifiData() {
        mProgressBar.setVisibility(View.GONE);
        mWifiLevelTextView.setVisibility(View.VISIBLE);
        mPossibilityTextView.setVisibility(View.VISIBLE);
    }

    private void showWifiAndPressureData() {
        mProgressBar.setVisibility(View.GONE);
        mWifiLevelTextView.setVisibility(View.VISIBLE);
        mPressureTextView.setVisibility(View.VISIBLE);
        mPossibilityTextView.setVisibility(View.VISIBLE);
    }
}
