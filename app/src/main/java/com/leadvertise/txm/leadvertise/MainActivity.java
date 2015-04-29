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
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class MainActivity extends Activity {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private final String TAG = "LEAdvertise";
    private BluetoothGattServer mGattServer;
    private boolean isAdvertising;
    private AdvCallback mAdvertiseCallback;
    private GattServerCallback mGattServerCallback;

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
            mGattServerCallback = new GattServerCallback(myhandler);
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
            mGattServerCallback.setGattServer(mGattServer);
            BluetoothGattService dis = new BluetoothGattService(
                    UUID.fromString(BleUuid.CHAR_INFO),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic char_name = new BluetoothGattCharacteristic(
                    UUID.fromString(BleUuid.CHAR_NAME_STRING),
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            char_name.setValue("KTEC Corp");


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

    private MyHandler myhandler = new MyHandler() {
        @Override
        public void onReadReqCompleted(String str) {
            Log.d(TAG, "Update the result area.");
            TextView tx = (TextView)findViewById(R.id.result_area);
            tx.setText(str);

        }
        @Override
        public void onWriteReqCompleted(String str) {
            Log.d(TAG, "Update the result area.");
            TextView tx = (TextView)findViewById(R.id.result_area);
            tx.setText(str);

        }
    };
}
