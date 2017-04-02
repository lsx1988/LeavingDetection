package com.bignerdranch.android.leavingdetection;

/**
 * Created by shixunliu on 2/4/17.
 */

public class MessageStatus {

    private boolean isWalking;

    public MessageStatus(boolean isWalking) {
        this.isWalking = isWalking;
    }

    public boolean isWalking() {
        return isWalking;
    }

    public void setWalking(boolean walking) {
        isWalking = walking;
    }
}
