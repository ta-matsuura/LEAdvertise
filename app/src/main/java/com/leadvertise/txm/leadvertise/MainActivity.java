package com.leadvertise.txm.leadvertise;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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
import android.widget.Toast;

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

    public BluetoothGattCharacteristic mCharaNoti;

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
            Log.d(TAG, "getAddress : " + mBluetoothAdapter.getAddress());

        }

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAdvertising();
            }
        });
        findViewById(R.id.btn_notify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                givChangeToNotify();
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

    private void givChangeToNotify() {
        if (mCharaNoti == null) {
            Log.d(TAG, "mCharaNoti  is NULL ");

            return;
        }
        Log.d(TAG, "mCharaNoti : " + mCharaNoti.getStringValue(0));

        if (mCharaNoti.getStringValue(0).equals("OFF")) {
            mCharaNoti.setValue("ON");
            Log.d(TAG, "mCharaNoti  <--- ON ");

        } else {
            mCharaNoti.setValue("OFF");
            Log.d(TAG, "mCharaNoti  <--- OFF ");
        }
        if (mGattServer != null) {
            mGattServer.notifyCharacteristicChanged(mGattServerCallback.getDevice(), mCharaNoti, true);
        }
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
            Log.d(TAG, "getAddress : " + mBluetoothAdapter.getAddress());


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
            BluetoothGattService gattService = new BluetoothGattService(
                    UUID.fromString(BleUuid.UUID_GATT_SERVICE),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic char_name = new BluetoothGattCharacteristic(
                    UUID.fromString(BleUuid.UUID_TEST_READWRITE),
                    /* 標準の設定 */
                    BluetoothGattCharacteristic.PROPERTY_READ |
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
//                    BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
                    BluetoothGattCharacteristic.PERMISSION_READ |
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            char_name.setValue("初期値だよ");

//            BluetoothGattDescriptor descriptor =
//                    new BluetoothGattDescriptor(UUID.fromString(BleUuid.UUID_TEST_DISCRIPTOR),
//                            BluetoothGattDescriptor.PERMISSION_READ);
//            descriptor.setValue(new byte[]{0x01, 0x00});
//            char_name.addDescriptor(descriptor);



            mCharaNoti = new BluetoothGattCharacteristic(
                    UUID.fromString(BleUuid.UUID_TEST_READNOTIF),
                    BluetoothGattCharacteristic.PROPERTY_READ |
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PROPERTY_WRITE);

            mCharaNoti.setValue("OFF");

            BluetoothGattDescriptor discriptorNofi =
                    new BluetoothGattDescriptor(UUID.fromString(BleUuid.UUID_CCCD),
                            BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            mCharaNoti.addDescriptor(discriptorNofi);

            gattService.addCharacteristic(char_name);
            gattService.addCharacteristic(mCharaNoti);
            //addTestCharactaristic(gattService);
            mGattServer.addService(gattService);
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
        ParcelUuid uuid = ParcelUuid.fromString(BleUuid.UUID_SERVICE_DATA);
        dataBuilder.addServiceData(uuid, serviceData);

        return dataBuilder.build();
    }
    private AdvertiseData createAdvData() {

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false);
        ParcelUuid uuid = ParcelUuid.fromString(BleUuid.UUID_SERVICE);

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
        @Override
        public void onDisconnected() {
            Toast.makeText(getApplicationContext(), "Disconnected !!", Toast.LENGTH_SHORT).show();
        }
    };

    private void addTestCharactaristic(BluetoothGattService dis) {

        BluetoothGattCharacteristic t1 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST1),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t1.setValue("KTEC Corp");
        dis.addCharacteristic(t1);

        BluetoothGattCharacteristic t2 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST2),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t2.setValue("KTEC Corp");
        dis.addCharacteristic(t2);

        BluetoothGattCharacteristic t3 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST3),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t3.setValue("KTEC Corp");
        dis.addCharacteristic(t3);

        BluetoothGattCharacteristic t4 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST4),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t4.setValue("KTEC Corp");
        dis.addCharacteristic(t4);

        BluetoothGattCharacteristic t5 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST5),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t5.setValue("KTEC Corp");
        dis.addCharacteristic(t5);

        BluetoothGattCharacteristic t6 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST6),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t6.setValue("KTEC Corp");
        dis.addCharacteristic(t6);

        BluetoothGattCharacteristic t7 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST7),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t7.setValue("KTEC Corp");
        dis.addCharacteristic(t7);

        BluetoothGattCharacteristic t8 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST8),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t8.setValue("KTEC Corp");
        dis.addCharacteristic(t8);

        BluetoothGattCharacteristic t9 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST9),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t9.setValue("KTEC Corp");
        dis.addCharacteristic(t9);

        BluetoothGattCharacteristic t10 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST10),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t10.setValue("KTEC Corp");
        dis.addCharacteristic(t10);

        BluetoothGattCharacteristic t11 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST11),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t11.setValue("KTEC Corp");
        dis.addCharacteristic(t11);

        BluetoothGattCharacteristic t12 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST12),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t12.setValue("KTEC Corp");
        dis.addCharacteristic(t12);

        BluetoothGattCharacteristic t13 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST13),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t13.setValue("KTEC Corp");
        dis.addCharacteristic(t13);

        BluetoothGattCharacteristic t14 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST14),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t14.setValue("KTEC Corp");
        dis.addCharacteristic(t14);

        BluetoothGattCharacteristic t15 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST15),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t15.setValue("KTEC Corp");
        dis.addCharacteristic(t15);

        BluetoothGattCharacteristic t16 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST16),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t16.setValue("KTEC Corp");
        dis.addCharacteristic(t16);

        BluetoothGattCharacteristic t17 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST17),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t17.setValue("KTEC Corp");
        dis.addCharacteristic(t17);

        BluetoothGattCharacteristic t18 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST18),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t18.setValue("KTEC Corp");
        dis.addCharacteristic(t18);

        BluetoothGattCharacteristic t19 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST19),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t19.setValue("KTEC Corp");
        dis.addCharacteristic(t19);

        BluetoothGattCharacteristic t20 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST20),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        t20.setValue("KTEC Corp");
        dis.addCharacteristic(t20);

        BluetoothGattCharacteristic t21 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST21),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t21.setValue("KTEC Corp");
        dis.addCharacteristic(t21);

        BluetoothGattCharacteristic t22 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST22),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t22.setValue("KTEC Corp");
        dis.addCharacteristic(t22);

        BluetoothGattCharacteristic t23 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST23),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t23.setValue("KTEC Corp");
        dis.addCharacteristic(t23);

        BluetoothGattCharacteristic t24 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST24),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t24.setValue("KTEC Corp");
        dis.addCharacteristic(t24);

        BluetoothGattCharacteristic t25 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST25),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t25.setValue("KTEC Corp");
        dis.addCharacteristic(t25);

        BluetoothGattCharacteristic t26 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST26),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t26.setValue("KTEC Corp");
        dis.addCharacteristic(t26);

        BluetoothGattCharacteristic t27 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST27),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t27.setValue("KTEC Corp");
        dis.addCharacteristic(t27);

        BluetoothGattCharacteristic t28 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST28),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t28.setValue("KTEC Corp");
        dis.addCharacteristic(t28);

        BluetoothGattCharacteristic t29 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST29),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t29.setValue("KTEC Corp");
        dis.addCharacteristic(t29);

        BluetoothGattCharacteristic t30 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST30),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t30.setValue("KTEC Corp");
        dis.addCharacteristic(t30);

        BluetoothGattCharacteristic t31 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST31),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t31.setValue("KTEC Corp");
        dis.addCharacteristic(t31);

        BluetoothGattCharacteristic t32 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST32),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t32.setValue("KTEC Corp");
        dis.addCharacteristic(t32);

        BluetoothGattCharacteristic t33 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST33),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t33.setValue("KTEC Corp");
        dis.addCharacteristic(t33);

        BluetoothGattCharacteristic t34 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST34),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t34.setValue("KTEC Corp");
        dis.addCharacteristic(t34);

        BluetoothGattCharacteristic t35 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST35),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t35.setValue("KTEC Corp");
        dis.addCharacteristic(t35);

        BluetoothGattCharacteristic t36 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST36),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t36.setValue("KTEC Corp");
        dis.addCharacteristic(t36);

        BluetoothGattCharacteristic t37 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST37),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t37.setValue("KTEC Corp");
        dis.addCharacteristic(t37);

        BluetoothGattCharacteristic t38 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST38),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t38.setValue("KTEC Corp");
        dis.addCharacteristic(t38);

        BluetoothGattCharacteristic t39 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST39),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t39.setValue("KTEC Corp");
        dis.addCharacteristic(t39);

        BluetoothGattCharacteristic t40 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST40),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t40.setValue("KTEC Corp");
        dis.addCharacteristic(t40);

        BluetoothGattCharacteristic t41 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST41),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t41.setValue("KTEC Corp");
        dis.addCharacteristic(t41);

        BluetoothGattCharacteristic t42 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST42),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t42.setValue("KTEC Corp");
        dis.addCharacteristic(t42);

        BluetoothGattCharacteristic t43 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST43),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t43.setValue("KTEC Corp");
        dis.addCharacteristic(t43);

        BluetoothGattCharacteristic t44 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST44),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t44.setValue("KTEC Corp");
        dis.addCharacteristic(t44);

        BluetoothGattCharacteristic t45 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST45),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t45.setValue("KTEC Corp");
        dis.addCharacteristic(t45);

        BluetoothGattCharacteristic t46 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST46),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t46.setValue("KTEC Corp");
        dis.addCharacteristic(t46);

        BluetoothGattCharacteristic t47 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST47),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t47.setValue("KTEC Corp");
        dis.addCharacteristic(t47);

        BluetoothGattCharacteristic t48 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST48),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t48.setValue("KTEC Corp");
        dis.addCharacteristic(t48);

        BluetoothGattCharacteristic t49 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST49),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t49.setValue("KTEC Corp");
        dis.addCharacteristic(t49);

        BluetoothGattCharacteristic t50 = new BluetoothGattCharacteristic(
                UUID.fromString(BleUuid.TEST50),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        t50.setValue("KTEC Corp");
        dis.addCharacteristic(t50);
    }
}
