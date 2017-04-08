package com.bignerdranch.android.leavingdetection;

/**
 * Created by shixunliu on 2/4/17.
 */

public class MessageIfAlarm {

    private boolean isPhoneHasAlarm;

    public MessageIfAlarm(boolean isAlarm) {
        isPhoneHasAlarm = isAlarm;
    }

    public boolean isPhoneHasAlarm() {
        return isPhoneHasAlarm;
    }

    public void setPhoneHasAlarm(boolean phoneHasAlarm) {
        isPhoneHasAlarm = phoneHasAlarm;
    }

}
