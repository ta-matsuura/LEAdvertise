package com.leadvertise.txm.leadvertise;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by txm on 2015/04/25.
 */
public class GattServerCallback extends BluetoothGattServerCallback{
    private String mName;
    private final String TAG = "LEAdvertise";
    private BluetoothGattServer mGattServer;
    private MyHandler mHandler;
    private int mOffset;
    private BluetoothDevice mDevice;
    ByteBuffer tempBuff = ByteBuffer. allocate(512);

    public GattServerCallback(MyHandler myhandler) {
        mHandler = myhandler;
    }

    public void setGattServer(BluetoothGattServer mGattServer) {
        this.mGattServer = mGattServer;
    }
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void onCharacteristicReadRequest (BluetoothDevice device, int requestId, int offset,
                                             BluetoothGattCharacteristic characteristic){
        Log.d(TAG, "START ---> onCharacteristicReadRequest()");
        Log.d(TAG, "requestId : " + requestId);
        Log.d(TAG, "offset : " + offset);
        Log.d(TAG, "getValue : " + characteristic.getValue());
        Log.d(TAG, "length : " + characteristic.getValue().length);


        mGattServer.sendResponse(device, requestId,
                BluetoothGatt.GATT_SUCCESS, offset,
                characteristic.getValue());

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putInt("msg_type", MyConstants.READ_REQ_RESULT);
        bundle.putString("value", characteristic.getStringValue(0));
        message.setData(bundle);
        Log.d(TAG, "send Message to handler");
        mHandler.sendMessage(message);
    }

    public void onCharacteristicWriteRequest (BluetoothDevice device, int requestId,
                                              BluetoothGattCharacteristic characteristic,
                                              boolean preparedWrite, boolean responseNeeded,
                                              int offset, byte[] value){
        Log.d(TAG, "START ---> onCharacteristicWriteRequest()");
        Log.d(TAG, "CHARA UUID : " + characteristic.getUuid().toString());
        Log.d(TAG, "value.length = " + value.length);
        Log.d(TAG, "offset : " + offset);
        Log.d(TAG, "requestId : " + requestId);
        Log.d(TAG, "responseNeeded : " + responseNeeded);
        Log.d(TAG, "preparedWrite : " + preparedWrite);


        if (value != null && value.length > 0 && value.length < 1000) {

            if (preparedWrite) {
                if (offset == 0) { //分割チャンクの始め
                    tempBuff.clear();
                    tempBuff.put(value);
                } else if (offset == 18){ //分割チャンクの中
                    tempBuff.put(value);
                } else {  //分割されたチャンクの最後
                    byte[] tempByte;
                    tempBuff.put(value);
                    tempByte = tempBuff.array();
                    try {
                        mName = new String(tempByte, "UTF-8");
                    }catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                    characteristic.setValue(mName);
                    Log.d(TAG, "name : " + mName);
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putInt("msg_type", MyConstants.WRITE_REQ_RESULT);
                    bundle.putString("value", mName);
                    message.setData(bundle);
                    Log.d(TAG, "send Message to handler");
                    mHandler.sendMessage(message);
                }
            } else {
                try {
                    mName = new String(value, "UTF-8");
                }catch (UnsupportedEncodingException e){
                    e.printStackTrace();
                }
                characteristic.setValue(mName);
                Log.d(TAG, "name : " + mName);
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putInt("msg_type", MyConstants.WRITE_REQ_RESULT);
                bundle.putString("value", mName);
                message.setData(bundle);
                Log.d(TAG, "send Message to handler");
                mHandler.sendMessage(message);
            }

        } else {
            Log.d(TAG, "invalid value written");
        }
        mOffset = offset;
        if (responseNeeded) {
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

    }
    public void onConnectionStateChange (BluetoothDevice device, int status, int newState){
        Log.d(TAG, "START ---> onConnectionStateChange()");
        Log.d(TAG, "status : " + status + "(0 means GATT_SUCCESS)");
        if (status == 0) {
            mDevice = device;
        }
        switch(newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.d(TAG, "toString : "  + device.toString());
                Log.d(TAG, "getName : "  + device.getName());
                Log.d(TAG, "getAddress : "  + device.getAddress());
                Log.d(TAG, "getUuids : "  + device.getUuids());
                Log.d(TAG, "getType : "  + device.getType());
                Log.d(TAG, "newState : STATE_CONNECTED");

                mGattServer.connect(device, false);
                //mGattServer.cancelConnection(device);
                break;
            case BluetoothProfile.STATE_CONNECTING:
                Log.d(TAG, "newState : STATE_CONNECTING");
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.d(TAG, "newState : STATE_DISCONNECTED");
                mDevice = null;
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                Log.d(TAG, "newState : STATE_DISCONNECTING");
                break;
            default:
                break;
        } // end of switch
    }
    public void onDescriptorReadRequest (BluetoothDevice device, int requestId, int offset,
                                         BluetoothGattDescriptor descriptor){
        Log.d(TAG, "START ---> onDescriptorReadRequest()");
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);



    }
    public void onDescriptorWriteRequest (BluetoothDevice device, int requestId,
                                          BluetoothGattDescriptor descriptor,
                                          boolean preparedWrite, boolean responseNeeded,
                                          int offset, byte[] value){
        Log.d(TAG, "START ---> onDescriptorWriteRequest()");
        String myString = "";
        descriptor.setValue(value);

        try {
            myString = new String(value, "UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        Log.d(TAG, "value : " + myString);
        Log.d(TAG, "offset : " + offset);
        Log.d(TAG, "preparedWrite : " + preparedWrite);
        Log.d(TAG, "responseNeeded : " + responseNeeded);


        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);



    }
    public void onExecuteWrite (BluetoothDevice device, int requestId, boolean execute){
        Log.d(TAG, "START ---> onExecuteWrite() device : " + device.getName());
        Log.d(TAG, "requestId : " + requestId);
        Log.d(TAG, "execute : " + execute);
        super.onExecuteWrite(device, requestId, execute);
        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, mOffset, null);

    }
    public void onMtuChanged (BluetoothDevice device, int mtu){
        // これはAPI22(Android 5.1)から呼ばれる
        Log.d(TAG, "START ---> onMtuChanged() device : " + device.getName() + " MTU(int) : " + mtu);

    }
    public void onNotificationSent (BluetoothDevice device, int status){
        Log.d(TAG, "START ---> onNotificationSent()");

    }
    public void onServiceAdded (int status, BluetoothGattService service){
        Log.d(TAG, "START ---> onServiceAdded()");
        switch(status) {
            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                Log.d(TAG, "status : GATT_CONNECTION_CONGESTED");
                break;
            case BluetoothGatt.GATT_FAILURE:
                Log.d(TAG, "status : GATT_FAILURE");
                break;
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                Log.d(TAG, "status : GATT_INSUFFICIENT_AUTHENTICATION");
                break;
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                Log.d(TAG, "status : GATT_INSUFFICIENT_ENCRYPTION");
                break;
            case BluetoothGatt.GATT_INVALID_OFFSET:
                Log.d(TAG, "status : GATT_INVALID_OFFSET");
                break;
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                Log.d(TAG, "status : GATT_READ_NOT_PERMITTED");
                break;
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                Log.d(TAG, "status : GATT_REQUEST_NOT_SUPPORTED");
                break;
            case BluetoothGatt.GATT_SUCCESS:
                Log.d(TAG, "status : GATT_SUCCESS");
                Log.d(TAG, "service uuid : " + service.getUuid().toString());
                break;
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                Log.d(TAG, "status : GATT_WRITE_NOT_PERMITTED");
                break;
            default:
                break;

        } // end of switch
    }

}
