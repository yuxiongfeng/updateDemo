package com.proton.update.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.proton.temp.connector.bean.DeviceType;
import com.proton.temp.connector.bluetooth.BleConnector;
import com.proton.temp.connector.bluetooth.callback.OnScanListener;
import com.proton.update.R;
import com.proton.temp.connector.bean.DeviceBean;
import com.proton.update.bean.UpdateFirmwareBean;
import com.proton.update.databinding.ActivityMainBinding;
import com.proton.update.net.callback.NetCallBack;
import com.proton.update.net.callback.ResultPair;
import com.proton.update.net.center.DeviceCenter;
import com.proton.update.utils.Utils;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.wms.adapter.CommonViewHolder;
import com.wms.adapter.recyclerview.CommonAdapter;
import com.wms.ble.utils.BluetoothUtils;
import com.wms.logger.Logger;
import com.wms.utils.CommonUtils;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends BaseActivity<ActivityMainBinding> {
    private CommonAdapter mAdapter;
    private List<DeviceBean> mDeviceList = new ArrayList<>();

    /**
     * 是否正在扫描设备
     */
    private boolean isScanDevice;
    private OnScanListener mScanListener = new OnScanListener() {
        @Override
        public void onDeviceFound(DeviceBean device) {
            //看看当前设备是否已经添加或者已经连接了
            if (!CommonUtils.listIsEmpty(mDeviceList)) {
                for (DeviceBean tempDevice : mDeviceList) {
                    if (tempDevice.getMacaddress().equalsIgnoreCase(device.getMacaddress())) {
                        return;
                    }
                }
            }
            mDeviceList.add(device);
            binding.idRefreshLayout.finishRefresh();
            mAdapter.notifyItemInserted(mDeviceList.size());
        }

        @Override
        public void onScanStopped() {
            Logger.w("搜索设备结束");
            doSearchStoped();
            stopSearch(true);
        }

        @Override
        public void onScanCanceled() {
            Logger.w("搜索设备取消");
            stopSearch(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding.idRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        initRefreshLayout(binding.idRefreshLayout, refreshlayout -> {
            scanDevice();
        }, null);
        mAdapter = new CommonAdapter<DeviceBean>(this, mDeviceList, R.layout.item_device) {
            @Override
            public void convert(CommonViewHolder holder, DeviceBean device) {
                holder.setText(R.id.id_device_mac, getShowMac(device.getMacaddress()));
                boolean isNeedUpdate = isNeedUpdate(device);
                holder.getView(R.id.id_connect).setOnClickListener(v -> {
                    if (isNeedUpdate) {
                        //升级固件
                        startActivity(new Intent(mContext, FirewareUpdatingActivity.class)
                                .putExtra("macaddress", device.getMacaddress())
                                .putExtra("deviceType", device.getDeviceType())
                        );
                    } else {
                        showToast("不需要升级");
                    }
                });

                if (isNeedUpdate) {
                    holder.setText(R.id.id_connect, getString(R.string.string_click_update));
                } else {
                    holder.setText(R.id.id_connect, getString(R.string.string_click_use));
                }
            }
        };
        binding.idRecyclerview.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getFireware();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        scanDevice();
    }

    private void getFireware() {
        DeviceCenter.getUpdatePackage(new NetCallBack<List<UpdateFirmwareBean>>() {
            @Override
            public void noNet() {
                super.noNet();
                Toast.makeText(mContext, "当前网络不可用", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSucceed(List<UpdateFirmwareBean> data) {
                Logger.w("获取固件更新成功:" + data.size());
                scanDevice();

            }

            @Override
            public void onFailed(ResultPair resultPair) {
                super.onFailed(resultPair);
                Toast.makeText(mContext, resultPair.getRet(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 扫描设备
     */
    private void scanDevice() {
        if (isScanDevice) {
            binding.idRefreshLayout.finishRefresh();
            return;
        }
        if (binding == null) {
            binding.idRefreshLayout.finishRefresh();
            return;
        }
        if (!BluetoothUtils.isBluetoothOpened()) {
            BluetoothUtils.openBluetooth();
            return;
        }

        mDeviceList.clear();
        binding.idRecyclerview.getAdapter().notifyDataSetChanged();
        isScanDevice = true;
        BleConnector.scanDevice(mScanListener);
    }

    private void stopSearch(boolean showEmpty) {
        isScanDevice = false;
        if (showEmpty) {
            if (CommonUtils.listIsEmpty(mDeviceList)) {
                binding.txtNoData.setVisibility(View.VISIBLE);
            }
        }
    }

    private void doSearchStoped() {
        if (!CommonUtils.listIsEmpty(mDeviceList)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected int inflateContentView() {
        return R.layout.activity_main;
    }

    /**
     * 初始化下拉刷新
     */
    protected void initRefreshLayout(SmartRefreshLayout refreshLayout, OnRefreshListener onRefreshListener, OnLoadmoreListener onLoadMoreListener) {
        if (refreshLayout == null) return;
        refreshLayout.setEnableScrollContentWhenLoaded(true);
        if (onRefreshListener != null) {
            refreshLayout.setEnableRefresh(true);
            refreshLayout.setOnRefreshListener(onRefreshListener);
        }

        if (onLoadMoreListener != null) {
            refreshLayout.setOnLoadmoreListener(onLoadMoreListener);
            refreshLayout.setEnableLoadmore(true);
        } else {
            refreshLayout.setEnableLoadmore(false);
        }
        refreshLayout.setEnableAutoLoadmore(true);//开启自动加载功能（非必须）
    }

    /**
     * 截取mac地址，后五位
     */
    public static String getShowMac(String address) {
        if (TextUtils.isEmpty(address) || address.length() < 5) return "";
        return address.substring(address.length() - 5, address.length());
    }

    private boolean isNeedUpdate(DeviceBean device) {
        UpdateFirmwareBean lastFireware = LitePal.where("deviceType = ?", String.valueOf(device.getDeviceType().getValue())).findFirst(UpdateFirmwareBean.class);
        if (lastFireware == null) return device.isNeedUpdate();

        boolean isVersionLower = false;
        if ((!TextUtils.isEmpty(device.getHardVersion())
                && !TextUtils.isEmpty(lastFireware.getVersion())
                && Utils.compareVersion(device.getHardVersion(), lastFireware.getVersion()) == -1)
                || (TextUtils.isEmpty(device.getHardVersion()) && device.getDeviceType() != DeviceType.P02)) {
            isVersionLower = true;
        }
        return device.isNeedUpdate() || isVersionLower;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDeviceList.clear();
        mAdapter.notifyDataSetChanged();
        BleConnector.stopScan();
        Logger.w("关闭蓝牙扫描");
    }
}
