package com.proton.update.net.api;

import java.util.HashMap;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

/**
 * Created by luochune on 2018/3/13.
 */

public interface ManagerCenterApi {

    /**
     * 根据设备类型获取设备列表
     */
    String getDeviceList = "openapi/device/all";
    String getUpdatePackage = "openapi/update/patch";

    @GET(getDeviceList)
    Observable<String> getDeviceList();

    @GET(getUpdatePackage)
    Observable<String> getUpdatePacckage(@QueryMap HashMap<String, Object> paramsMap);


}
