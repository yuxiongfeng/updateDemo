package com.proton.update.component;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.proton.temp.connector.TempConnectorManager;
import com.proton.temp.connector.bean.MQTTConfig;
import com.proton.update.BuildConfig;
import com.proton.update.bean.AliyunToken;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.constant.SpinnerStyle;
import com.scwang.smartrefresh.layout.footer.ClassicsFooter;
import com.scwang.smartrefresh.layout.header.ClassicsHeader;
import com.wms.logger.Logger;
import com.wms.utils.CommonUtils;

import org.litepal.LitePal;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.BleFactory;
import cn.com.heaton.blelibrary.ble.utils.UuidUtils;
import cn.trinea.android.common.util.PreferencesUtils;

/**
 * Created by yuxiongfeng.
 * Date: 2019/7/18
 */
public class App extends Application {
    public AliyunToken aliyunToken;//阿里云token
    private static App mInstance;
    private String version;
    private String systemInfo;

    public static App get() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        //初始化日志
        Logger.newBuilder()
                .tag("update_firmware")
                .showThreadInfo(false)
                .methodCount(1)
                .saveLogCount(7)
                .context(this)
                .deleteOnLaunch(false)
                .saveFile(BuildConfig.DEBUG)
                .isDebug(BuildConfig.DEBUG)
                .build();
        TempConnectorManager.init(this);
        //数据库初始化
        LitePal.initialize(this);
        initRefresh();
        initBle();
    }

    //初始化蓝牙
    private void initBle() {
        Ble.options()
                .setLogBleEnable(true)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setLogTAG("AndroidBLE")//设置全局蓝牙操作日志TAG
                .setAutoConnect(false)//设置是否自动连接
                .setIgnoreRepeat(false)//设置是否过滤扫描到的设备(已扫描到的不会再次扫描)
                .setConnectFailedRetryCount(3)//连接异常时（如蓝牙协议栈错误）,重新连接次数
                .setConnectTimeout(10 * 1000)//设置连接超时时长
                .setScanPeriod(12 * 1000)//设置扫描时长
                .setMaxConnectNum(7)//最大连接数量
                .setUuidService(UUID.fromString(UuidUtils.uuid16To128("fd00")))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128("fd01")))//设置可写特征的uuid
                .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128("fd02")))//设置可读特征的uuid （选填）
                .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128("fd03")))//设置可通知特征的uuid （选填，库中默认已匹配可通知特征的uuid）
                .setFactory(new BleFactory() {
                    @Override
                    public BleDevice create(String address, String name) {
                        return super.create(address, name);
                    }
                }).create(this, new Ble.InitCallback() {
            @Override
            public void success() {
                BleLog.e("MainApplication", "初始化成功");
            }

            @Override
            public void failed(int failedCode) {
                BleLog.e("MainApplication", "初始化失败：" + failedCode);
            }
        })
        ;
    }


    public String getApiUid() {
        return PreferencesUtils.getString(this,"uid", "uid");
    }


    public String getVersion() {
        if (TextUtils.isEmpty(version)) {
            version = CommonUtils.getAppVersion(this) + "&" + CommonUtils.getAppVersionCode(this);
        }
        return version;
    }

    public int getVersionCode() {
        int appVersionCode;
        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            appVersionCode = info.versionCode; //版本名
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
        return appVersionCode;
    }

    public String getSystemInfo() {
        if (TextUtils.isEmpty(systemInfo)) {
            systemInfo = android.os.Build.MODEL + "&" + android.os.Build.VERSION.RELEASE;
        }
        return systemInfo;
    }

    public void initRefresh() {
        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreater((context, layout) -> new ClassicsHeader(context).setSpinnerStyle(SpinnerStyle.Translate));
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreater((context, layout) -> new ClassicsFooter(context).setSpinnerStyle(SpinnerStyle.Translate));
    }

}
