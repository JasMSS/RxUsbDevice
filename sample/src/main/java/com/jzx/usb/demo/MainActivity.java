package com.jzx.usb.demo;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.bin.david.form.annotation.SmartColumn;
import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.column.ColumnInfo;
import com.bin.david.form.listener.OnColumnClickListener;
import com.jzx.rx.usb.RxUsbDevice;
import com.jzx.rx.usb.UsbPermission;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    @com.bin.david.form.annotation.SmartTable(name = "当前已连接USB设备列表")
    public static class UsbInfo{
        @SmartColumn(name = "是否允许权限")
        public String granted;
        @SmartColumn(name = "型号", autoMerge = true)
        public String usbProductName;
        @SmartColumn(name = "设备别名")
        public String usbDeviceName;

        public UsbInfo() {
        }

        public UsbInfo(String granted, String usbProductName, String usbDeviceName) {
            this.granted = granted;
            this.usbProductName = usbProductName;
            this.usbDeviceName = usbDeviceName;
        }

        public UsbInfo(UsbPermission usbPermission) {
            UsbDevice usbDevice = usbPermission.usbDevice;
            this.usbProductName = usbDevice.getProductName();
            this.usbDeviceName = usbDevice.getDeviceName();
            this.granted = usbPermission.granted ?"Y" : "N";
        }
    }

    SmartTable<UsbInfo> smartTable;

    UsbDevice[] usbDeviceList;
    List<UsbInfo> usbInfoList;
    RxUsbDevice rxUsbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initValues();
        initViews();
        initDatas();
    }
    public void initValues(){

        rxUsbDevice = new RxUsbDevice(this);
        usbInfoList = new ArrayList<>();
        usbDeviceList = rxUsbDevice.findAllUsbDevices().values().toArray(new UsbDevice[0]);
    }

    private void initViews() {
        smartTable = findViewById(R.id.st_usb_list);
    }

    private void initDatas() {
        requestUsbPermissions();
    }


    public void requestUsbPermissions(){
        rxUsbDevice.requestEachDevices()
                .subscribe(new Consumer<UsbPermission>() {
                    @Override
                    public void accept(UsbPermission usbPermission) throws Exception {
                        if(usbPermission.usbDevice == null){
                            Toast.makeText(getApplicationContext(), "无已连接的USB设备",
                                    Toast.LENGTH_LONG).show();
                            usbInfoList.add(new UsbInfo("", "", ""));
                            smartTable.setData(usbInfoList);
                            return;
                        }else {
                            addUsbDevice(usbPermission);
                        }
                    }
                });
    }

    public void addUsbDevice(UsbPermission usbPermission){
        usbInfoList.add(new UsbInfo(usbPermission));

        if(usbInfoList.size() == usbDeviceList.length){
            requestFinished();
        }
    }

    //显示USB设备列表信息
    public void requestFinished(){
        smartTable.setData(usbInfoList);
    }
}
