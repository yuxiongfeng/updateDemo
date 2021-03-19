package com.proton.temp.connector.bean;

public enum DeviceType {
    /**
     * 不同版本设备，p02只有蓝牙，p03带有蓝牙和wifi，蓝牙部分和p02有部分区别
     */
    P02(2), P03(3), P04(4), P05(5), P06(6), P07(7), P08(8),P10(10), P11(11), None(-1);

    private int value;

    DeviceType(int value) {
        this.value = value;
    }

    public static DeviceType valueOf(int value) {
        switch (value) {
            case 2:
                return P02;
            case 3:
                return P03;
            case 4:
                return P04;
            case 5:
                return P05;
            case 6:
                return P06;
            case 7:
                return P07;
            case 8:
                return P08;
            case 10:
                return P10;
            case 11:
                return P11;
            default:
                return None;
        }
    }

    public int getValue() {
        return value;
    }
}
