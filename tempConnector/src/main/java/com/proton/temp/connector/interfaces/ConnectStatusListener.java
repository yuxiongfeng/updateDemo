package com.proton.temp.connector.interfaces;

/**
 * Created by wangmengsi on 2018/3/15.
 * 连接器回调
 */
public abstract class ConnectStatusListener {

    /**
     * 连接成功
     */
    public void onConnectSuccess() {
    }

    /**
     * 连接失败
     */
    public void onConnectFaild() {
    }

    /**
     * 断开连接了
     *
     * @param isManual 是否是手动调用连接断开的
     */
    public void onDisconnect(boolean isManual) {
    }

    /**
     * 接收重连次数
     *
     * @param retryCount 当前重连次数
     * @param leftCount  剩余次数
     */
    public void receiveReconnectTimes(int retryCount, int leftCount) {
    }

    /**
     * mqtt连接如果底座换了贴，则回调该方法
     */
    public void receiveNotSampleDevice(String oldMac, String newMac) {
    }

    /**
     * 底座离线
     */
    public void receiveDockerOffline(boolean isOffline) {
    }
}
