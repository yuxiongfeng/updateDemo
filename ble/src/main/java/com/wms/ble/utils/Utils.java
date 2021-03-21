package com.wms.ble.utils;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by wangmengsi on 2018/3/15.
 */
public class Utils {

    public static final String PREFIX = "esp/";
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String getTopicByMacAddress(String macaddress) {
        return PREFIX + macaddress.toUpperCase().replace(":", "");
    }

    public static String getWillTopicByMacAddress(String macaddress) {
        return PREFIX + macaddress.toUpperCase().replace(":", "") + "/lastwill";
    }

    public static String getPatchDisconnectTopicByMacAddress(String macaddress) {
        return PREFIX + macaddress.toUpperCase().replace(":", "") + "/state";
    }

    public static String getMacAddressByTopic(String topic) {
        if (!TextUtils.isEmpty(topic)) {
            return parseBssid2Mac(topic.substring(PREFIX.length(), 12 + PREFIX.length()));
        }
        return "";
    }

    public static String parseBssid2Mac(String bssid) {
        StringBuilder macbuilder = new StringBuilder();
        for (int i = 0; i < bssid.length() / 2; i++) {
            macbuilder.append(bssid, i * 2, i * 2 + 2).append(":");
        }
        macbuilder.delete(macbuilder.length() - 1, macbuilder.length());
        return macbuilder.toString();
    }

    public static String getString(String jsonStr, String key) {
        JSONObject jObj = getJOSNObj(jsonStr);
        if (jObj == null) {
            return "";
        }
        try {
            return jObj.getString(key);
        } catch (JSONException e) {
            Logger.w(e.toString());
        }
        return "";
    }

    public static JSONObject getJOSNObj(String jsonStr) {
        try {
            return new JSONObject(jsonStr);
        } catch (JSONException e) {
            Logger.w(e.toString());
        }
        return null;
    }


}
