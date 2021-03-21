package com.proton.temp.connector.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.proton.temp.connector.R;
import com.proton.temp.connector.bean.DeviceType;
import com.proton.temp.connector.bluetooth.utils.BleUtils;
import com.wms.ble.bean.ScanResult;
import com.wms.ble.callback.OnConnectListener;
import com.wms.ble.callback.OnSubscribeListener;
import com.wms.ble.callback.OnWriteCharacterListener;
import com.wms.ble.operator.FastBleOperator;
import com.wms.ble.operator.IBleOperator;
import com.wms.ble.operator.WmsBleOperator;
import com.wms.ble.utils.BluetoothUtils;
import com.wms.logger.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by 王梦思 on 2018-11-06.
 * <p/>
 */
public class FirewareUpdateManager {

    /**
     * 重置服务uuid
     */
    private static final String SERVICE_RESET = "f000ffd0-0451-4000-b000-000000000000";
    /**
     * 特征:重置服务特征
     */
    private static final String CHARACTOR_RESET = "f000ffd1-0451-4000-b000-000000000000";
    /**
     * 固件升级服务
     */
    private static final String SERVICE_UPDATE_FIRMWARE = "f000ffc0-0451-4000-b000-000000000000";

    /**
     * 获取oad block size
     */
    private static final String CHARACTOR_BLOCKSIZE_CALLBACK = "f000ffc5-0451-4000-b000-000000000000";

    /**
     * 固件升级特征值(可写)
     */
    private static final String CHARACTOR_UPDATE_WRITE = "f000ffc1-0451-4000-b000-000000000000";
    /**
     * 固件升级回调特征值(可订阅)
     */
    private static final String CHARACTOR_UPDATE_CALLBACK = "f000ffc2-0451-4000-b000-000000000000";


    /**
     * 固件升级状态的mac地址
     */
    private static final String UPDATE_MACADDRESS = "0A:D0:AD:0A:D0:AD";
    private FirewareAdapter firewareAdapter;

    private String filePath;
    private String macaddress;
    /**
     * 固件包文件大小
     */
    private long mFileSize;
    private IBleOperator bleOperator;
    private int buffSize = 16;
    private byte[] mFileBytes;
    private long startTime;
    private Context mContext;
    private DeviceType deviceType;
    private boolean hasResetDevice;
    private boolean isNewMode = true;//新升级模式
    private int newBlockSize;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int writeDuration = 1000;

    private boolean isFFC5Write01 = true;
    private boolean isConfigRead = false;
    private boolean isResetDevice = false;

    /**
     * 当前连接的设备mac地址
     */
    private String connectDeviceMac;
    private BroadcastReceiver mBluetoothReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Logger.w("固件更新:蓝牙关闭");
                        updateFail(getString(R.string.connector_please_open_bluetooth), UpdateFailType.BLUETOOTH_NOT_OPEN);
                        break;
                }
            }
        }
    };

    public FirewareUpdateManager(Context context, String macaddress, FirewareAdapter firewareAdapter) {
        if (context == null) {
            throw new IllegalArgumentException("context can not be null");
        }
        this.macaddress = macaddress;
        this.mContext = context;
        this.firewareAdapter = firewareAdapter;
    }

    public FirewareUpdateManager update() {
        if (!BluetoothUtils.isBluetoothOpened()) {
            updateFail(getString(R.string.connector_please_open_bluetooth), UpdateFailType.BLUETOOTH_NOT_OPEN);
            BluetoothUtils.openBluetooth();
            return this;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceive, filter);
        scanDevice(macaddress);
        return this;
    }

    private void scanDevice(final String mac) {
//        bleOperator = new FastBleOperator(mContext);
        bleOperator = new WmsBleOperator(mContext);
        bleOperator.setConnectListener(new OnConnectListener() {

            @Override
            public void onConnectSuccess(final ScanResult result) {
                if (firewareAdapter == null) {
                    throw new IllegalArgumentException("you need to provide a firewareAdapter");
                }
                deviceType = BroadcastUtils.parseDeviceType(result.getScanRecord());
                filePath = firewareAdapter.getFirewarePath(deviceType);
                if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
                    updateFail(getString(R.string.connector_fireware_not_exist), UpdateFailType.FIREWARE_NOT_EXIST);
                    bleOperator.disConnect(mac);
                    return;
                }
                mFileBytes = toByteArray(filePath);
                mFileSize = mFileBytes.length;
                Logger.w("fileSize is :", mFileSize);
                connectDeviceMac = result.getDevice().getAddress();
                Logger.w("固件升级:connectDeviceMac:", connectDeviceMac);
                Logger.w("固件升级:广播包:", BleUtils.bytesToHexString(result.getScanRecord()));
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (hasResetDevice || connectDeviceMac.equals(UPDATE_MACADDRESS)) {//进入了升级模式
                            if (isNewMode) {
                                writeIdentifyInfo();//向ffc1写入带identify的固件信息
                            } else {
                                uploadData();
                            }
                        } else {
                            resetDevice();
                        }
                    }
                }, 200);
            }

            @Override
            public void onConnectFaild() {
                Logger.w("固件升级:连接失败:", mac);
                if (mac.equals(UPDATE_MACADDRESS)) {
                    updateFail(getString(R.string.connector_connect_fail), UpdateFailType.CONNECT_FAIL);
                } else {
                    scanDevice(UPDATE_MACADDRESS);
                }
            }

            @Override
            public void onDisconnect(boolean isManual) {
                if (!isManual) {
                    updateFail(getString(R.string.connector_device_disconnect), UpdateFailType.DISCONNECT);
                }
            }
        });
        bleOperator.scanAndConnect(mac);
    }

    private void writeIdentifyInfo() {
        Logger.w("start writeIdentifyInfo....");
        String fileSizeHex = Long.toHexString(mFileSize);
        String littleSize = fileSizeHex.substring(2, 4) + fileSizeHex.substring(0, 2);
        final String identify = "4F414420494D47200101FFFE0100" + littleSize.toUpperCase() + "000030303031";
        Logger.w("发送的identify信息:", identify);
        bleOperator.subscribeNotification(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, CHARACTOR_UPDATE_WRITE, new OnSubscribeListener() {
            @Override
            public void onFail() {
                super.onFail();
                Logger.w("订阅identify失败");
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                Logger.w("订阅identify成功");
                //开始写入identify
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bleOperator.writeNoRsp(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, CHARACTOR_UPDATE_WRITE, BleUtils.hexStringToBytes(identify), new OnWriteCharacterListener() {
                            @Override
                            public void onFail() {
                                super.onFail();
                                Logger.w("写入固件Identify onFail");
                            }

                            @Override
                            public void onSuccess() {
                                super.onSuccess();
                                Logger.w("写入固件Identify 成功");
                            }
                        });
                    }
                }, writeDuration);
            }

            @Override
            public void onNotify(String uuid, byte[] data) {
                super.onNotify(uuid, data);
                String identifyStr = BleUtils.bytesToHexString(data);
                Logger.w("notify identify callback:", identifyStr);//发送identify的返回
                //开始向ffc5写入相关指令
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        subscribeFFC5();
                    }
                }, writeDuration);
            }
        });
    }


    private void subscribeFFC5() {
        //订阅ffc5
        bleOperator.subscribeNotification(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, CHARACTOR_BLOCKSIZE_CALLBACK, new OnSubscribeListener() {
            @Override
            public void onFail() {
                super.onFail();
                Logger.w("订阅ffc5失败");
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                Logger.w("订阅ffc5成功");
                //开始向ffc5里面写入01
                isFFC5Write01 = true;
                writeToFFC5("01");
            }

            @Override
            public void onNotify(String uuid, byte[] bytes) {
                super.onNotify(uuid, bytes);
                if (isResetDevice) {//升级成功
                    Logger.w("升级成功，ffc5的04回调", BleUtils.bytesToHexString(bytes));
                    updateSuccess(deviceType, connectDeviceMac);
                    return;
                }
                if (isFFC5Write01) {
                    Logger.w("ffc5的01回调:", BleUtils.bytesToHexString(bytes));
                    //获取blockSize
                    String blockSize = BleUtils.bytesToHexString(bytes).substring(0, 6);
                    Logger.w("fetchBlockSize is :", blockSize);
                    newBlockSize = Integer.parseInt(blockSize.substring(4, 6) + blockSize.substring(2, 4), 16);
                    //开始写入03
                    isFFC5Write01 = false;
                    writeToFFC5("03");
                } else {
                    Logger.w("ffc5的03回调:", BleUtils.bytesToHexString(bytes));
                    if (!isConfigRead) {
                        if (BleUtils.bytesToHexString(bytes).equalsIgnoreCase("120000000000")) {
                            isConfigRead = true;
                            //向ffc2写升级固件
                            uploadData();
                        }
                    } else {
                        String countString = BleUtils.bytesToHexString(bytes);
                        String blockSizeIndex = countString.substring(4, 12);
                        Logger.w("blockSizeIndex:", blockSizeIndex);
                        byte[] blockBytes = BleUtils.hexStringToBytes(blockSizeIndex);
                        int blockIndex = Integer.parseInt(blockSizeIndex.substring(6, 8) + blockSizeIndex.substring(4, 6) + blockSizeIndex.substring(2, 4) + blockSizeIndex.substring(0, 2), 16);
                        if (blockIndex==0) {//写入完成
                            isResetDevice=true;
                            Logger.w("写入完成");
                            isResetDevice = true;
                            Logger.w("ffc5开始写入04");
                            writeToFFC5("04");
                        }else{
                            write(blockIndex, blockBytes);
                        }
                    }
                }
            }
        });
    }

    private void writeToFFC5(final String instruction) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bleOperator.writeNoRsp(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, CHARACTOR_BLOCKSIZE_CALLBACK, BleUtils.hexStringToBytes(instruction)
                        , new OnWriteCharacterListener() {
                            @Override
                            public void onFail() {
                                super.onFail();
                                Logger.w("向ffc5写入", instruction, "失败");
                            }

                            @Override
                            public void onSuccess() {
                                super.onSuccess();
                                Logger.w("向ffc5写入", instruction, "成功");
                            }
                        });
            }
        }, writeDuration);
    }

    /**
     * 上传固件包
     */
    private void uploadData() {
        bleOperator.subscribeNotification(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, CHARACTOR_UPDATE_CALLBACK, new OnSubscribeListener() {
            @Override
            public void onSuccess() {
                Logger.w("ffc2订阅成功");
                byte[] b = new byte[]{0, 0, 0, 0};
                if (isNewMode) {
                    write(0, b);
                } else {
                    write(0, null);
                }
            }

            @Override
            public void onFail() {
                Logger.w("ffc2订阅失败");
                updateFail(getString(R.string.connector_fireware_write_fail), UpdateFailType.SUBSCRIBE_FAIL);
            }

            @Override
            public void onNotify(String s, byte[] bytes) {
                String countString = BleUtils.bytesToHexString(bytes).substring(0, 4);
                Logger.w("uploadData countString is :", countString);
                int index = Integer.parseInt(countString.substring(2, 4) + countString.substring(0, 2), 16);
                Logger.w("index is :", index);
                write(index, bytes);
            }
        });
        startTime = System.currentTimeMillis();
    }

    private void write(final int index, byte[] data) {
        write(false, index, data);
    }

    private void write(boolean isWriteIdentify, final int index, byte[] data) {
        if (isNewMode) {
            buffSize = newBlockSize;
            buffSize = buffSize - data.length;
        }
        Logger.w("buffSize is :", buffSize);
        final float progress = (float) index * buffSize / mFileSize;
        byte[] temp;
        if (buffSize * index + buffSize < mFileBytes.length) {
            temp = new byte[buffSize];
            System.arraycopy(mFileBytes, buffSize * index, temp, 0, buffSize);
        } else {
            Logger.w("最后一包数据不足",buffSize);
            temp = new byte[mFileBytes.length - buffSize * index];
            System.arraycopy(mFileBytes, buffSize * index, temp, 0, mFileBytes.length - buffSize * index);
        }
        byte[] writeData;
        if (data != null) {
            writeData = new byte[data.length + temp.length];
            System.arraycopy(data, 0, writeData, 0, data.length);
            System.arraycopy(temp, 0, writeData, data.length, temp.length);
        } else {
            writeData = temp;
        }
        Logger.w("向ffc2写入的数据:", BleUtils.bytesToHexString(writeData));
        if (isNewMode) {//这里有问题，往ffc2里面写fffc1也会受到通知
            bleOperator.writeNoRsp(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, CHARACTOR_UPDATE_CALLBACK, writeData, new OnWriteCharacterListener() {
                @Override
                public void onFail() {
                    updateFail(getString(R.string.connector_fireware_write_fail), UpdateFailType.FIREWARE_WRITE_FAIL);
                }
            });
        } else {
            bleOperator.writeNoRsp(connectDeviceMac, SERVICE_UPDATE_FIRMWARE, data == null || isWriteIdentify ? CHARACTOR_UPDATE_WRITE : CHARACTOR_UPDATE_CALLBACK, writeData, new OnWriteCharacterListener() {
                @Override
                public void onFail() {
                    updateFail(getString(R.string.connector_fireware_write_fail), UpdateFailType.FIREWARE_WRITE_FAIL);
                }
            });

        }
        //这个回调不放到写入成功是因为有时候会出现最后一包不回调导致提示升级失败
        if (onFirewareUpdateListener != null) {
            onFirewareUpdateListener.onProgress(progress);
            if (mFileSize % buffSize == 0) {
                Logger.w("fileSize能够被16整除");
                if (index == mFileSize / buffSize - 1) {

                    if (mFileSize % buffSize == 0) {
                        //向FFc5写入"04"重启设备
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isResetDevice = true;
                                Logger.w("ffc5开始写入04");
                                writeToFFC5("04");
                            }
                        }, 5000);
                    }
                    Logger.w("升级总耗时:", (System.currentTimeMillis() - startTime));
                }
            } else if (index > (mFileSize / buffSize - 1)) {
                Logger.w("fileSize不能够被16整除");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isResetDevice = true;
                        Logger.w("ffc5开始写入04");
                        writeToFFC5("04");
                    }
                }, 5000);
            }
        }
    }

    private static byte[] toByteArray(String filename) {
        File f = new File(filename);
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 重置设备,设备重置成功后,设备重启，蓝牙地址会变成0A:D0:AD:0A:D0:AD
     */
    private void resetDevice() {
        bleOperator.write(connectDeviceMac, SERVICE_RESET, CHARACTOR_RESET, BleUtils.hexStringToBytes("01"), new OnWriteCharacterListener() {
            @Override
            public void onSuccess() {
                Logger.w("固件升级:重置成功");
                hasResetDevice = true;
                bleOperator.disConnect(connectDeviceMac);
                scanDevice(connectDeviceMac);
            }

            @Override
            public void onFail() {
                Logger.w("固件升级:重置失败");
                if (isNewMode) {
                    writeIdentifyInfo();//向ffc1写入带identify的固件信息
                } else {
                    uploadData();//已经进入升级模式，可以进行升级
                }

            }
        });
    }

    public void stopUpdate() {
        try {
            if (mBluetoothReceive != null) {
                mContext.unregisterReceiver(mBluetoothReceive);
                mBluetoothReceive = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bleOperator != null) {
            bleOperator.disConnect(macaddress);
            bleOperator.disConnect(UPDATE_MACADDRESS);
        }
        isFFC5Write01 = true;
        isConfigRead = false;
        isResetDevice=false;
    }

    private void updateFail(String msg, UpdateFailType type) {
        if (onFirewareUpdateListener != null) {
            onFirewareUpdateListener.onFail(msg, type);
        }
        stopUpdate();
    }

    private void updateSuccess(DeviceType type, String macaddress) {
        if (onFirewareUpdateListener != null) {
            onFirewareUpdateListener.onSuccess(type, macaddress);
        }
        stopUpdate();
    }

    public void setOnFirewareUpdateListener(OnFirewareUpdateListener onFirewareUpdateListener) {
        this.onFirewareUpdateListener = onFirewareUpdateListener;
    }

    public interface OnFirewareUpdateListener {
        /**
         * 更新成功
         */
        void onSuccess(DeviceType type, String macaddress);

        /**
         * 更新失败
         *
         * @param msg  更新失败原因文字，已做国际化(中英文)
         * @param type 更新失败类型
         */
        void onFail(String msg, UpdateFailType type);

        /**
         * 更新进度
         */
        void onProgress(float progress);
    }

    private OnFirewareUpdateListener onFirewareUpdateListener;

    private String getString(int stringRes) {
        return mContext.getString(stringRes);
    }

    public enum UpdateFailType {
        /**
         * 连接中断
         */
        DISCONNECT,
        /**
         * 固件不存在
         */
        FIREWARE_NOT_EXIST,
        /**
         * 蓝牙没有打开
         */
        BLUETOOTH_NOT_OPEN,
        /**
         * 连接失败
         */
        CONNECT_FAIL,
        /**
         * 订阅失败
         */
        SUBSCRIBE_FAIL,
        /**
         * 固件写入失败
         */
        FIREWARE_WRITE_FAIL,
    }

    public interface FirewareAdapter {
        /**
         * 获取固件的本地路径
         */
        String getFirewarePath(DeviceType type);
    }
}
