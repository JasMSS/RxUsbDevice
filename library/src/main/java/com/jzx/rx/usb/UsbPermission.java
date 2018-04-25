package com.jzx.rx.usb;

import android.hardware.usb.UsbDevice;

/**
 * Created by Jiangzx on 2018/3/26.
 */

public class UsbPermission {
    public final UsbDevice usbDevice;
    public final boolean granted;

    public UsbPermission(UsbDevice usbDevice, boolean isGranted) {
        this.usbDevice = usbDevice;
        this.granted = isGranted;
    }
}
