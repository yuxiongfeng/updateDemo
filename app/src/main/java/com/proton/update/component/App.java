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
    }

    public String getApiUid() {
        return PreferencesUtils.getString(this, "uid", "uid");
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
