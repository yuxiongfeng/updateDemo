package com.wms.ble.utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.wms.ble.bean.ScanResult;
import com.wms.ble.callback.OnScanListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;

/**
 * Created by 王梦思 on 2018-11-08.
 * <p/>
 */
public class ScanManager {

    private static ScanManager mInstance;
    private BluetoothAdapter bluetoothAdapter;
    private List<OnScanListener> onScanListeners = new ArrayList<>();
    private boolean isScaning;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<String> searchNames = new ArrayList<>(1);
    private Map<OnScanListener, Runnable> stopMap = new HashMap<>();
    private BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Logger.w("设备--类型：",device.getType());
            Logger.w("设备--名称：",device.getType());
            Logger.w("设备--mac : ",device.getAddress());
            if (searchNames == null || searchNames.size() <= 0) {
                for (OnScanListener onScanListener : getListeners()) {
                    onScanListener.onDeviceFound(new ScanResult(device, rssi, scanRecord));
                }
                return;
            }
            for (String deviceName : searchNames) {
                if (deviceName.equalsIgnoreCase(device.getName())) {
                    for (OnScanListener onScanListener : getListeners()) {
                        onScanListener.onDeviceFound(new ScanResult(device, rssi, scanRecord));
                    }
                }
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mScanLeCallback=new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);

            if (searchNames == null || searchNames.size() <= 0) {
                for (OnScanListener onScanListener : getListeners()) {
                    onScanListener.onDeviceFound(new ScanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes()));
                }
                return;
            }
            for (String deviceName : searchNames) {
                if (deviceName.equalsIgnoreCase(result.getDevice().getName())) {
                    for (OnScanListener onScanListener : getListeners()) {
                        onScanListener.onDeviceFound(new ScanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes()));
                    }
                }
            }

        }

        @Override
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private ScanManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static ScanManager getInstance() {
        if (mInstance == null) {
            mInstance = new ScanManager();
        }
        return mInstance;
    }

    public void addScanListener(OnScanListener listener) {
        if (listener == null) return;
        if (!onScanListeners.contains(listener)) {
            onScanListeners.add(listener);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void scan(final OnScanListener listener, int scanTime, final String... names) {
        if (listener == null) return;
        for (String name : names) {
            if (!searchNames.contains(name)) {
                searchNames.add(name);
            }
        }
        addScanListener(listener);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                innerStop(listener, false);
            }
        };
        stopMap.put(listener, runnable);
        mainHandler.postDelayed(runnable, scanTime);
        Logger.w("扫描监听器大小:", onScanListeners.size() + ",isScaning:" + isScaning);
        for (OnScanListener onScanListener : getListeners()) {
            onScanListener.onScanStart();
        }
        if (isScaning) {
            return;
        }
        isScaning = true;
        bluetoothAdapter.startLeScan(mScanCallback);
    }

    public void stop(OnScanListener listener) {
        innerStop(listener, true);
    }

    private void innerStop(OnScanListener listener, boolean isManual) {
        if (listener == null) return;

        if (stopMap.containsKey(listener)) {
            mainHandler.removeCallbacks(stopMap.get(listener));
            stopMap.remove(listener);
        }

        for (OnScanListener onScanListener : getListeners()) {
            if (isManual) {
                onScanListener.onScanCanceled();
            } else {
                onScanListener.onScanStopped();
            }
        }
        onScanListeners.remove(listener);
        if (onScanListeners.size() == 0 && mScanCallback != null) {
            isScaning = false;
            bluetoothAdapter.stopLeScan(mScanCallback);
        }

        Logger.w("扫描监听器大小stop:", onScanListeners.size(), ",isScaning:", isScaning);
    }

    private List<OnScanListener> getListeners() {
        return new ArrayList<>(onScanListeners);
    }
}