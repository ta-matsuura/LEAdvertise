package com.leadvertise.txm.leadvertise;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.util.Log;

/**
 * Created by txm on 2015/04/24.
 */
public class AdvCallback extends AdvertiseCallback {
    String str = "";
    private final String TAG = "LEAdvertise";

    public AdvCallback() {
    }


    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);
        switch (errorCode){
            case ADVERTISE_FAILED_ALREADY_STARTED:
                str += "onStartFailure : ADVERTISE_FAILED_ALREADY_STARTED \n";
                break;
            case ADVERTISE_FAILED_DATA_TOO_LARGE:
                str += "onStartFailure : ADVERTISE_FAILED_DATA_TOO_LARGE \n";
                break;
            case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                str += "onStartFailure :  ADVERTISE_FAILED_FEATURE_UNSUPPORTED \n";
                break;
            case ADVERTISE_FAILED_INTERNAL_ERROR:
                str += "onStartFailure : ADVERTISE_FAILED_INTERNAL_ERROR \n";
                break;
            case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                str += "onStartFailure : ADVERTISE_FAILED_TOO_MANY_ADVERTISERS \n";
                break;
        }

        Log.d(TAG, "" + str);
    }

    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);

        str += "onStartSuccess : " + settingsInEffect.toString() + "\n";
        Log.d(TAG, str);

    }
}
