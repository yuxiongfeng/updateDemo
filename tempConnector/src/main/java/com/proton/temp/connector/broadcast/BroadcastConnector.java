package com.proton.temp.connector.broadcast;

import android.os.Handler;
import android.os.Looper;

import com.proton.temp.connector.bean.DeviceBean;
import com.proton.temp.connector.bean.DeviceType;
import com.proton.temp.connector.interfaces.ConnectStatusListener;
import com.proton.temp.connector.interfaces.Connector;
import com.proton.temp.connector.interfaces.DataListener;
import com.proton.temp.connector.utils.BroadcastUtils;
import com.proton.temp.connector.utils.ConnectorSetting;
import com.wms.ble.bean.ScanResult;
import com.wms.ble.callback.OnScanListener;
import com.wms.ble.utils.BluetoothUtils;
import com.wms.ble.utils.ScanManager;
import com.wms.logger.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 王梦思 on 2018/12/4.
 * <p/>
 * 广播方式连接
 */
public class BroadcastConnector implements Connector {

    private String macaddress;
    private ConnectStatusListener connectStatusListener = new ConnectStatusListener() {
        @Override
        public void onConnectSuccess() {
        }
    };
    private DataListener dataListener = new DataListener();
    /**
     * 是否已经回调了连接成功
     */
    private boolean hasCallbackConnectSuccess;
    private int mLastPackageNumber = -1;
    private int battery;
    private boolean isCharge = true;
    private Timer mConnectStatusTimer;
    private Timer mCheckDeviceOpenTimer;
    /**
     * 上次收到数据的时间
     */
    private long mLastReceiveDataTime;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    /**
     * 连接超时时间
     */
    private long connectTimeout = ConnectorSetting.BROADCAST_CONNECT_TIMEOUT;
    /**
     * 数据接收超时时间
     */
    private long disconnectTimeout = ConnectorSetting.BROADCAST_DISCONNECT_TIMEOUT;
    private OnScanListener mScanCallback = new com.wms.ble.callback.OnScanListener() {

        @Override
        public void onScanStart() {
            checkDeviceOpen();
        }

        @Override
        public void onDeviceFound(ScanResult result) {
            DeviceType type = BroadcastUtils.parseDeviceType(result.getScanRecord());
            if (type != DeviceType.None) {
                String deviceName = result.getDevice().getName();
                DeviceBean deviceBean = new DeviceBean(result.getDevice().getAddress(), type, BroadcastUtils.getHardVersionByBroadcast(result.getScanRecord()));
                if ("OAD THEM".equals(deviceName)) {
                    deviceBean.setNeedUpdate(result.getDevice().getName().equalsIgnoreCase("OAD THEM"));
                }
                doDeviceFound(result.getScanRecord());
            }
        }
    };

    public BroadcastConnector(String macaddress) {
        this.macaddress = macaddress;
    }

    @Override
    public void connect() {
        clear();
        ScanManager.getInstance().scan(mScanCallback, Integer.MAX_VALUE);
    }

    private void doDeviceFound(byte[] scanRecord) {
        if (macaddress.equalsIgnoreCase(BroadcastUtils.getMacaddressByBroadcastNew(scanRecord))) {
            mLastReceiveDataTime = System.currentTimeMillis();
            if (!hasCallbackConnectSuccess) {
                hasCallbackConnectSuccess = true;
                connectStatusListener.onConnectSuccess();
                if (mCheckDeviceOpenTimer != null) {
                    mCheckDeviceOpenTimer.cancel();
                    mCheckDeviceOpenTimer = null;
                }
                checkConnectStatus();
                dataListener.receiveHardVersion(BroadcastUtils.getHardVersionByBroadcast(scanRecord));
            }

            int packageNum = BroadcastUtils.getPackageNumber(scanRecord);
            if (packageNum == mLastPackageNumber) return;
            if (packageNum - mLastPackageNumber != 1 && mLastPackageNumber != -1) {
                Logger.w("丢包了:", macaddress);
            }
            mLastPackageNumber = packageNum;
            Logger.w("包序:", packageNum, ",mac:", macaddress);
            //包序
            dataListener.receivePackageNumber(mLastPackageNumber);

            //温度
            dataListener.receiveCurrentTemp(BroadcastUtils.getTempByBroadcast(scanRecord, mLastPackageNumber));

            //电量
            int bat = BroadcastUtils.getBattery(scanRecord);
            if (battery != bat) {
                battery = bat;
                dataListener.receiveBattery(battery);
            }

            boolean charge = BroadcastUtils.isCharge(scanRecord);
            if (isCharge != charge) {
                isCharge = charge;
                dataListener.receiveCharge(isCharge);
            }
        }
    }

    /**
     * 检查设备是否打开
     */
    private void checkDeviceOpen() {
        final long startTime = System.currentTimeMillis();
        mCheckDeviceOpenTimer = new Timer();
        mCheckDeviceOpenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - startTime > connectTimeout && !hasCallbackConnectSuccess) {
                    mCheckDeviceOpenTimer.cancel();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            disConnect();
                            connectStatusListener.onConnectFaild();
                        }
                    });
                }
            }
        }, 0, 2000);
    }

    /**
     * 检查连接的状态
     */
    private void checkConnectStatus() {
        mConnectStatusTimer = new Timer();
        mConnectStatusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Logger.w("广播连接状态定时器");
                if ((mLastReceiveDataTime != 0 && System.currentTimeMillis() - mLastReceiveDataTime >= disconnectTimeout)
                        || !BluetoothUtils.isBluetoothOpened()) {
                    //没收到数据就回调断开
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            clear();
                            connectStatusListener.onDisconnect(false);
                        }
                    });
                }
            }
        }, 0, 5000);
    }

    @Override
    public void connect(ConnectStatusListener connectorListener, DataListener dataListener) {
        if (connectorListener != null) {
            this.connectStatusListener = connectorListener;
        }
        if (dataListener != null) {
            this.dataListener = dataListener;
        }
        connect();
    }

    @Override
    public void disConnect() {
        clear();
        ScanManager.getInstance().stop(mScanCallback);
    }

    private void clear() {
        if (mConnectStatusTimer != null) {
            mConnectStatusTimer.cancel();
        }
        if (mCheckDeviceOpenTimer != null) {
            mCheckDeviceOpenTimer.cancel();
        }
        mLastPackageNumber = -1;
        mLastReceiveDataTime = 0;
        hasCallbackConnectSuccess = false;
    }

    @Override
    public boolean isConnected() {
        Logger.w("是否连接hasCallbackConnectSuccess:" + hasCallbackConnectSuccess);
        return hasCallbackConnectSuccess;
    }

    @Override
    public void cancelConnect() {
        ScanManager.getInstance().stop(mScanCallback);
    }

    @Override
    public void setSampleRate(int sampleRate) {
    }

    @Override
    public void setConnectTimeoutTime(long time) {
        this.connectTimeout = time;
    }

    @Override
    public void setDisconnectTimeoutTime(long time) {
        this.disconnectTimeout = time;
    }
}
