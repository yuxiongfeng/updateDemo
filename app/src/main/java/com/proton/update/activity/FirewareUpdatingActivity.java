package com.proton.update.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.proton.temp.connector.bean.DeviceType;
import com.proton.temp.connector.utils.FirewareUpdateManager;
import com.proton.update.R;
import com.proton.update.bean.UpdateFirmwareBean;
import com.proton.update.databinding.ActivityFirewareUpdatingBinding;
import com.proton.update.net.callback.NetCallBack;
import com.proton.update.net.callback.ResultPair;
import com.proton.update.net.center.DeviceCenter;
import com.proton.update.utils.FileUtils;
import com.proton.update.view.WarmDialog;
import com.wms.logger.Logger;

import org.litepal.LitePal;

import java.text.DecimalFormat;
import java.util.List;

public class FirewareUpdatingActivity extends BaseActivity<ActivityFirewareUpdatingBinding> {
    private DecimalFormat df = new java.text.DecimalFormat("#0.00");
    private String macaddress;
    private DeviceType deviceType;
    private FirewareUpdateManager updateManager;
    private boolean isUpdateSuccessed;

    @Override
    protected int inflateContentView() {
        return R.layout.activity_fireware_updating;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //屏幕常亮
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("CheckResult")
    @Override
    protected void init() {
        super.init();
        macaddress = getIntent().getStringExtra("macaddress");
        deviceType = (DeviceType) getIntent().getSerializableExtra("deviceType");
    }

    @Override
    protected void initData() {
        super.initData();
        DeviceCenter.getUpdatePackage(new NetCallBack<List<UpdateFirmwareBean>>() {
            @Override
            public void noNet() {
                startUpdate();
            }

            @Override
            public void onSucceed(List<UpdateFirmwareBean> data) {
                startUpdate();
            }

            @Override
            public void onFailed(ResultPair resultPair) {
                startUpdate();
            }
        });
    }

    private void startUpdate() {
        updateManager = new FirewareUpdateManager(getApplication(), macaddress, type -> {
            if (type == DeviceType.P02) {
                type = DeviceType.P03;
            }
            UpdateFirmwareBean firmwareBean = LitePal.where("deviceType = ?", String.valueOf(type.getValue())).findFirst(UpdateFirmwareBean.class);
            String firewarePath = "";
            if (firmwareBean != null) {
                //检查升级固件包是否存在，不存在就下载
                firewarePath = FileUtils.getFireware(DeviceType.valueOf(firmwareBean.getDeviceType()), firmwareBean.getVersion());
                Logger.w("固件路径:" + firewarePath);
            }
            return firewarePath;
        });
        updateManager.setOnFirewareUpdateListener(new FirewareUpdateManager.OnFirewareUpdateListener() {
            @Override
            public void onSuccess(DeviceType type, String macaddress) {
                runOnUiThread(() -> {
                    showToast("固件升级成功");
                    isUpdateSuccessed=true;
                    finish();
                });
            }

            @Override
            public void onFail(String msg, FirewareUpdateManager.UpdateFailType type) {
                if (isUpdateSuccessed) {
                    return;
                }
                showToast(msg);
                startActivity(new Intent(mContext, FirewareUpdateFailActivity.class)
                        .putExtra("macaddress", macaddress)
                        .putExtra("deviceType", deviceType));
                finish();
            }

            @SuppressLint("CheckResult")
            @Override
            public void onProgress(float progress) {
                binding.idProgressbarDownload.setProgress((int) (progress * 100));
                binding.tvUpdateProgress.setText(df.format(progress * 100) + "%");
            }
        });
        updateManager.update();
    }


    @Override
    protected boolean showBackBtn() {
        return false;
    }

    @Override
    public void onBackPressed() {
        new WarmDialog(this)
                .setContent(R.string.string_updating_exit_will_cause_problem)
                .hideCancelBtn()
                .setConfirmText(R.string.string_i_konw)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateManager != null) {
            updateManager.stopUpdate();
        }
    }

    @Override
    public String getTopCenterText() {
        return getString(R.string.string_update_firware);
    }
}
