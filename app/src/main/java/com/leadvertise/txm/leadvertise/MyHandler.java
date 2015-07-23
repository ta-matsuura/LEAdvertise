package com.leadvertise.txm.leadvertise;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Created by txm on 2015/04/28.
 */
public abstract class MyHandler extends Handler {
    public void handleMessage(Message msg){
        Bundle bundle = msg.getData();
        int type = bundle.getInt("msg_type");
        String str;
        switch(type) {
            case MyConstants.READ_REQ_RESULT:
                str = "Read request accepted : " + bundle.getString("value");
                onReadReqCompleted(str);
                break;
            case MyConstants.WRITE_REQ_RESULT:
                str = "What you wrote is ..." + bundle.getString("value");
                onWriteReqCompleted(str);
                break;
            case MyConstants.DISCONNECTED:
                onDisconnected();
            default:
                break;
        }

    }

    public abstract void onReadReqCompleted(String str);
    public abstract void onWriteReqCompleted(String str);
    public abstract void onDisconnected();

}
