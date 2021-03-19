package com.proton.update.utils;

import android.app.Activity;

import android.content.Context;
import android.os.Build;

import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import com.proton.temp.connector.bean.DeviceType;
import com.wms.utils.CommonUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by wangmengsi on 2018/2/26.
 */

public class Utils {
    private static DecimalFormat mTempFormatter;

    public static void setStatusBarTextColor(Activity activity, boolean isDark) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Window window = activity.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

            MIUISetStatusBarLightMode(activity.getWindow(), isDark);
            FlymeSetStatusBarLightMode(activity.getWindow(), isDark);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置状态栏字体图标为深色，需要MIUIV6以上
     *
     * @param window 需要设置的窗口
     * @param dark   是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    public static boolean MIUISetStatusBarLightMode(Window window, boolean dark) {
        boolean result = false;
        if (window != null) {
            Class clazz = window.getClass();
            try {
                int darkModeFlag = 0;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
                if (dark) {
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag);//状态栏透明且黑色字体
                } else {
                    extraFlagField.invoke(window, 0, darkModeFlag);//清除黑色字体
                }
                result = true;
            } catch (Exception e) {
            }
        }
        return result;
    }

    /**
     * 设置状态栏图标为深色和魅族特定的文字风格
     * 可以用来判断是否为Flyme用户
     *
     * @param window 需要设置的窗口
     * @param dark   是否把状态栏字体及图标颜色设置为深色
     * @return boolean 成功执行返回true
     */
    public static boolean FlymeSetStatusBarLightMode(Window window, boolean dark) {
        boolean result = false;
        if (window != null) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);
                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (dark) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }
                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
                result = true;
            } catch (Exception e) {
            }
        }
        return result;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result.toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isMobilePhone(String phoneNum) {
        return !TextUtils.isEmpty(phoneNum)
                && phoneNum.length() == 11
                && phoneNum.startsWith("1");
    }


    /**
     * 检测有效是否有效
     *
     * @param emai
     * @return
     */
    public static boolean isEmail(String emai) {
        if (emai == null)
            return false;
        String regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p;
        Matcher m;
        p = Pattern.compile(regEx1);
        m = p.matcher(emai);
        if (m.matches())
            return true;
        else
            return false;
    }


    public static String encrypt(String input, String key) {
        input += input + "proton521";
        byte[] crypted = null;

        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            crypted = cipher.doFinal(input.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(crypted));
    }

    /**
     * 获取进程名称
     */
    public static String getProcessName(Context context) {
        android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return CommonUtils.getAppPackageName(context);
        List<android.app.ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (android.app.ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return null;
    }

    /**
     * 截取mac地址，后五位
     */
    public static String getShowMac(String address) {
        if (TextUtils.isEmpty(address) || address.length() < 5) return "";
        return address.substring(address.length() - 5, address.length());
    }





    /**
     * 根据浮点温度值转为两位精度格式的字符串
     */
    public static String formatTempToStr(float temp) {
        if (mTempFormatter == null) {
            mTempFormatter = new DecimalFormat("##0.00");
        }
        return mTempFormatter.format(temp);
    }



    public static String getReportJsonPath(long startTime) {
        return FileUtils.getJson_filepath() + "/" + startTime + ".json";
    }
    /**
     * m
     * 摄氏度转华氏度
     */
    public static float selsiusToFahrenheit(float celsius) {
        return formatTemp(((9.0f / 5) * celsius + 32));
    }

    /**
     * 华氏度转摄氏度
     */
    public static float fahrenheitToCelsius(float fahrenhei) {
        return formatTemp((fahrenhei - 32) * (5.0f / 9));
    }

    /**
     * 获取低电量prefrence的key
     */
    public static String getLowPowerSharedPreferencesKey(String macaddress) {
        return "low_power:" + macaddress;
    }

    /**
     * 获取高温提醒prefrence的key
     */
    public static String getHighTempWarmSharedPreferencesKey(String macaddress) {
        return "hight_warm:" + macaddress;
    }

    /**
     * 获取低温提醒prefrence的key
     */
    public static String getLowTempWarmSharedPreferencesKey(String macaddress) {
        return "low_warm:" + macaddress;
    }

    /**
     * 设置页面的透明度
     *
     * @param bgAlpha 1表示不透明
     */
    public static void setBackgroundAlpha(Activity activity, float bgAlpha) {
        if (activity == null) return;
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = bgAlpha;
        if (bgAlpha == 1) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);//不移除该Flag的话,在有视频的页面上的视频会出现黑屏的bug
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);//此行代码主要是解决在华为手机上半透明效果无效的bug
        }
        activity.getWindow().setAttributes(lp);
    }

    /**
     * 将float 温度值转为两位小数的温度值
     */
    public static float formatTemp(double temp) {
        if (mTempFormatter == null) {
            mTempFormatter = new DecimalFormat("##0.00");
        }
        return Float.valueOf(mTempFormatter.format(temp));
    }


    /**
     * 隐藏键盘
     */
    public static void hideKeyboard(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null && view.getWindowToken() != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static DeviceType getDeviceType(int type) {
        if (type == 2) {
            return DeviceType.P02;
        } else if (type == 3) {
            return DeviceType.P03;
        } else if (type == 4) {
            return DeviceType.P04;
        } else if (type == 5) {
            return DeviceType.P05;
        } else {
            return DeviceType.None;
        }
    }



    /**
     * 版本号比较
     *
     * @return 0代表相等，1代表version1大于version2，-1代表version1小于version2
     */
    public static int compareVersion(String version1, String version2) {
        try {
            if (version1.startsWith("V") || version1.startsWith("v")) {
                version1 = version1.substring(1);
            }
            if (version2.startsWith("V") || version2.startsWith("v")) {
                version2 = version2.substring(1);
            }
            if (version1.equals(version2)) {
                return 0;
            }
            String[] version1Array = version1.split("\\.");
            String[] version2Array = version2.split("\\.");
            int index = 0;
            // 获取最小长度值
            int minLen = Math.min(version1Array.length, version2Array.length);
            int diff = 0;
            // 循环判断每位的大小
            while (index < minLen
                    && (diff = Integer.parseInt(version1Array[index])
                    - Integer.parseInt(version2Array[index])) == 0) {
                index++;
            }
            if (diff == 0) {
                // 如果位数不一致，比较多余位数
                for (int i = index; i < version1Array.length; i++) {
                    if (Integer.parseInt(version1Array[i]) > 0) {
                        return 1;
                    }
                }

                for (int i = index; i < version2Array.length; i++) {
                    if (Integer.parseInt(version2Array[i]) > 0) {
                        return -1;
                    }
                }
                return 0;
            } else {
                return diff > 0 ? 1 : -1;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return 0;
    }


}
