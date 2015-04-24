package com.leadvertise.txm.leadvertise;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class MainActivity extends Activity {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private final String TAG = "LEAdvertise";
    private BluetoothGattServer mGattServer;
    private String mName;
    private boolean isAdvertising;
    private AdvCallback mAdvertiseCallback;

    public boolean isAdvertising() {
        return isAdvertising;
    }

    public void setAdvertising(boolean isAdvertising) {
        this.isAdvertising = isAdvertising;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        if(mBluetoothManager != null && mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAdvertising();
            }
        });
        findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBTAdvertiser != null) {
                    Log.d(TAG, "stop Advertisement");
                    stopAdvertising();
                }
            }
        });
    }

//    public AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
//
//        String str = "";
//
//        public void onStartFailure(int errorCode) {
//            super.onStartFailure(errorCode);
//            switch (errorCode){
//                case ADVERTISE_FAILED_ALREADY_STARTED:
//                    str += "onStartFailure : ADVERTISE_FAILED_ALREADY_STARTED \n";
//                    break;
//                case ADVERTISE_FAILED_DATA_TOO_LARGE:
//                    str += "onStartFailure : ADVERTISE_FAILED_DATA_TOO_LARGE \n";
//                    break;
//                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
//                    str += "onStartFailure :  ADVERTISE_FAILED_FEATURE_UNSUPPORTED \n";
//                    break;
//                case ADVERTISE_FAILED_INTERNAL_ERROR:
//                    str += "onStartFailure : ADVERTISE_FAILED_INTERNAL_ERROR \n";
//                    break;
//                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
//                    str += "onStartFailure : ADVERTISE_FAILED_TOO_MANY_ADVERTISERS \n";
//                    break;
//            }
//
//            Log.d(TAG, "" + str);
//        }
//
//        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
//            super.onStartSuccess(settingsInEffect);
//
//            str += "onStartSuccess : " + settingsInEffect.toString() + "\n";
//            Log.d(TAG, str);
//
//        }
//    };

    private void stopAdvertising(){
        if (mGattServer != null) {
            mGattServer.clearServices();
            mGattServer.close();
            mGattServer = null;
        }
        if (mBTAdvertiser != null) {
            mBTAdvertiser.stopAdvertising(mAdvertiseCallback);
            setAdvertising(false);
            mBTAdvertiser = null;
        }
    }

    private void startAdvertising(){
        Log.d(TAG, " --- > START startAdvertising");

        if(mBTAdvertiser == null) {
            mBTAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        }
        final boolean isAdvertiseSupported = mBTAdvertiser != null;

        if (!isAdvertiseSupported  || isAdvertising()) {
            // サポートしていないときの処理
            Log.d(TAG, "Not support advertisement or Already advertising.");
            return;
        }

        if (mGattServer == null) {
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
            BluetoothGattService dis = new BluetoothGattService(
                    UUID.fromString(BleUuid.CHAR_INFO),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic char_name = new BluetoothGattCharacteristic(
                    UUID.fromString(BleUuid.CHAR_NAME_STRING),
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);

            BluetoothGattCharacteristic char_onoff = new BluetoothGattCharacteristic(
                    UUID.fromString(BleUuid.CHAR_ONOFF_STRING),
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);



            dis.addCharacteristic(char_name);
            dis.addCharacteristic(char_onoff);
            mGattServer.addService(dis);
        }

        mAdvertiseCallback = new AdvCallback();
        mBTAdvertiser.startAdvertising(createAdvSetting(), createAdvData(),createScanRspData(), mAdvertiseCallback);
        Log.d(TAG, " --- > END startAdvertising");

        setAdvertising(true);
    }

    private AdvertiseSettings createAdvSetting() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTimeout(0);
        builder.setConnectable(true);
        return builder.build();
    }
    private AdvertiseData createScanRspData() {
        byte[] serviceData = new byte[6];
        serviceData[0] = 0x10;
        serviceData[1] = (byte) 0xBA;
        serviceData[2] = 0x11;
        serviceData[3] = 0x12;
        serviceData[4] = 0x13;
        serviceData[5] = 0x15;


        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        ParcelUuid uuid = ParcelUuid.fromString(BleUuid.ADV_SERVICE_DATA_UUID);
        dataBuilder.addServiceData(uuid, serviceData);

        return dataBuilder.build();
    }
    private AdvertiseData createAdvData() {

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false);
        ParcelUuid uuid = ParcelUuid.fromString(BleUuid.ADV_SERVICE_UUID);

        //UUIDとDevice名は共存できない。セットすると長過ぎるってエラーになる
        dataBuilder.addServiceUuid(uuid);
        //dataBuilder.setIncludeDeviceName(true);

        return dataBuilder.build();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback(){

        public void onCharacteristicReadRequest (BluetoothDevice device, int requestId, int offset,
                                                 BluetoothGattCharacteristic characteristic){
            Log.d(TAG, "START ---> onCharacteristicReadRequest()");
            Log.d(TAG, "CHARA UUID : " + characteristic.getUuid().toString());

            if(characteristic.getUuid().toString().equals(BleUuid.CHAR_NAME_STRING)) {
                characteristic.setValue("111111111122222222223"); //20byte
                mGattServer.sendResponse(device, requestId,
                        BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            }
        }

        public void onCharacteristicWriteRequest (BluetoothDevice device, int requestId,
                                                  BluetoothGattCharacteristic characteristic,
                                                  boolean preparedWrite, boolean responseNeeded,
                                                  int offset, byte[] value){
            Log.d(TAG, "START ---> onCharacteristicWriteRequest()");
            Log.d(TAG, "value.length = " + value.length);
            Log.d(TAG, "offset : " + offset);
            Log.d(TAG, "requestId : " + requestId);
            Log.d(TAG, "responseNeeded : " + responseNeeded);
            Log.d(TAG, "preparedWrite : " + preparedWrite);


            if (characteristic.getUuid().equals(UUID.fromString(BleUuid.CHAR_ONOFF_STRING))) {
                if (value != null && value.length > 0 && value.length < 21) {
                    try {
                        mName = new String(value, "UTF-8");
                    }catch (UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                    Log.d(TAG, "name : " + mName);

                } else {
                    Log.d(TAG, "invalid value written");
                }
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,null);
            }
        }
        public void onConnectionStateChange (BluetoothDevice device, int status, int newState){
            Log.d(TAG, "START ---> onConnectionStateChange()");
            Log.d(TAG, "status : " + status + "(0 means GATT_SUCCESS)");
            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d(TAG, "newState : STATE_CONNECTED");
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "newState : STATE_CONNECTING");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.d(TAG, "newState : STATE_DISCONNECTED");
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


        }
        public void onDescriptorWriteRequest (BluetoothDevice device, int requestId,
                                              BluetoothGattDescriptor descriptor,
                                              boolean preparedWrite, boolean responseNeeded,
                                              int offset, byte[] value){
            Log.d(TAG, "START ---> onDescriptorWriteRequest()");


        }
        public void onExecuteWrite (BluetoothDevice device, int requestId, boolean execute){
            Log.d(TAG, "START ---> onExecuteWrite() device : " + device.getName());
            Log.d(TAG, "requestId : " + requestId);
            Log.d(TAG, "execute : " + execute);
            super.onExecuteWrite(device, requestId, execute);
            //mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);

        }
        public void onMtuChanged (BluetoothDevice device, int mtu){
            Log.d(TAG, "START ---> onMtuChanged()");

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
    };

    public class BleUuid{
        static final String ADV_SERVICE_UUID = "11111111-2222-3333-4444-555555555555";
        static final String ADV_SERVICE_DATA_UUID = "11111111-6666-6666-6666-666666666666";
        static final String CHAR_INFO = "99999999-1111-4444-4444-111111111111";
        static final String CHAR_NAME_STRING = "99999999-2222-4444-4444-222222222222";
        static final String CHAR_ONOFF_STRING = "99999999-3333-4444-4444-222222222222";
    }
}
