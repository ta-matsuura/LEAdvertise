package com.leadvertise.txm.leadvertise;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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


public class MainActivity extends Activity {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private final String TAG = "LEAdvertise";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        if(mBluetoothAdapter == null) {
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
                Log.d(TAG, "stop Advertisement");
                if(mBTAdvertiser != null) {
                    mBTAdvertiser.stopAdvertising(mAdvertiseCallback);
                }
            }
        });
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        String str = "";

        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            str += "onStartFailure : " + errorCode + "\n";
            Log.d(TAG, "" + str);
        }

        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

            str += "onStartSuccess : " + settingsInEffect.toString() + "\n";
            Log.d(TAG, str);

        }
    };

    private void startAdvertising(){
        Log.d(TAG, " --- > START startAdvertising");

        if(mBluetoothAdapter == null){
            return;
        }
        if(mBluetoothManager == null){
            return;
        }

        mBTAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        final boolean isAdvertiseSupported = mBTAdvertiser != null;
        if (!isAdvertiseSupported) {
            // サポートしていないときの処理
            Log.d(TAG, "This device does not support advertisement.");
        }

        mBTAdvertiser.startAdvertising(createAdvSetting(), createAdvData(), mAdvertiseCallback);
        Log.d(TAG, " --- > END startAdvertising");
    }
    private AdvertiseSettings createAdvSetting() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTimeout(0);
        builder.setConnectable(true);
        return builder.build();
    }

    private AdvertiseData createAdvData() {
        byte[] serviceData = new byte[7];
        serviceData[0] = 0x00; // flags
        serviceData[1] = (byte) 0xBA; // transmit power
        serviceData[2] = 0x00; // http://www.
        serviceData[3] = 0x65; // e
        serviceData[4] = 0x66; // f
        serviceData[5] = 0x66; // f
        serviceData[6] = 0x08; // .org
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false);
        ParcelUuid uuid = ParcelUuid.fromString("11111111-1234-1234-1234-123456789012");
        dataBuilder.addServiceData(uuid, serviceData);

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
}
