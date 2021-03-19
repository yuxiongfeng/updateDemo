package com.proton.update.net.center;

import com.google.gson.reflect.TypeToken;
import com.proton.temp.connector.bean.DeviceType;
import com.proton.update.bean.UpdateFirmwareBean;
import com.proton.update.net.RetrofitHelper;
import com.proton.update.net.callback.NetCallBack;
import com.proton.update.net.callback.NetSubscriber;
import com.proton.update.net.callback.ParseResultException;
import com.proton.update.net.callback.ResultPair;
import com.proton.update.utils.FileUtils;
import com.proton.update.utils.JSONUtils;
import com.wms.logger.Logger;
import com.wms.utils.CommonUtils;

import org.litepal.LitePal;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by yuxiongfeng.
 * Date: 2019/7/18
 */
public class DeviceCenter extends DataCenter{
    /**
     * 获取固件升级包
     */
    public static void getUpdatePackage(NetCallBack<List<UpdateFirmwareBean>> netCallBack) {
        HashMap<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("all", 1);
        RetrofitHelper.getManagerCenterApi().getUpdatePacckage(paramsMap).map(json -> {
            Logger.json(json);
            ResultPair resultPair = parseResult(json);
            if (resultPair != null && resultPair.isSuccess()) {
                Type type = new TypeToken<ArrayList<UpdateFirmwareBean>>() {
                }.getType();
                List<UpdateFirmwareBean> allPackage = JSONUtils.getObj(resultPair.getData(), type);
                if (!CommonUtils.listIsEmpty(allPackage)) {
                    for (UpdateFirmwareBean packageBean : allPackage) {
                        //下载固件更新包
                        //packageBean.setVersion("V3.0.0");
                        String path = FileUtils.getFirewareDir(DeviceType.valueOf(packageBean.getDeviceType()));
                        if (!new File(path, packageBean.getVersion()).exists()) {
                            Logger.w("固件不存在");
                            FileUtils.downloadFile(packageBean.getUrl(), path, packageBean.getVersion());
                        } else {
                            Logger.w("固件存在");
                        }
                    }
                    LitePal.deleteAll(UpdateFirmwareBean.class);
                    LitePal.saveAll(allPackage);
                }
                return allPackage;
            } else {
                throw new ParseResultException(json);
            }
        }).compose(threadTrans()).subscribe(new NetSubscriber<List<UpdateFirmwareBean>>(netCallBack) {
            @Override
            public void onNext(List<UpdateFirmwareBean> updateFirmwareBean) {
                netCallBack.onSucceed(updateFirmwareBean);
            }
        });
    }
}
