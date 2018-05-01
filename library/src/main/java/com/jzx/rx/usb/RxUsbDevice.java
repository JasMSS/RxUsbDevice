package com.jzx.rx.usb;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by Jiangzx on 2018/3/22.
 */

public class RxUsbDevice {
    static final String TAG = "RxUsbDevice";
    private static final Object TRIGGER = new Object();
    private UsbManager mUsbManager;
    private RxUsbFragment rxUsbFragment;

    public RxUsbDevice(Activity activity) {
        mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        rxUsbFragment = getRxUsbFragment(activity);
    }

    private RxUsbFragment getRxUsbFragment(Activity activity) {
        RxUsbFragment rxUsbFragment = findRxUsbFragment(activity);
        if(rxUsbFragment == null){
            rxUsbFragment = new RxUsbFragment();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager.beginTransaction()
                    .add(rxUsbFragment, TAG)
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return rxUsbFragment;
    }

    private RxUsbFragment findRxUsbFragment(@NonNull Activity activity){
        return (RxUsbFragment) activity.getFragmentManager().findFragmentByTag(TAG);
    }

    /**
     * 获取已连接的所有USB设备
     * @return 所有已连接Usb设备的集合（HashMap说明：key是设备名称，value是设备对应的UsbDevice对象）
     */
    public HashMap<String, UsbDevice> findAllUsbDevices(){
        return mUsbManager.getDeviceList();
    }

    /**
     * 在已连接USB设备中获取指定的USB设备
     * @param vendorId USB设备的vendorId
     * @param productId USB设备的productId
     */
    public UsbDevice findUsbDeviceById(int vendorId, int productId){
        HashMap<String, UsbDevice> deviceList = findAllUsbDevices();
        for(UsbDevice device : deviceList.values()){
            if(device.getVendorId() == vendorId && device.getProductId() == productId){
                return device;
            }
        }
        return null;
    }

    /**
     * 在已连接USB设备中获取指定的USB设备
     * @param usbDeviceName 设备的名称
     * @return
     */
    public UsbDevice findUsbDeviceByDeviceName(String usbDeviceName){
        HashMap<String, UsbDevice> deviceList = findAllUsbDevices();
        return deviceList.get(usbDeviceName);
    }

    /**
     * 在已连接USB设备中获取指定的USB设备
     * @param usbProductName 设备的型号名称
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public UsbDevice findUsbDeviceByProductName(String usbProductName){
        if(usbProductName == null || usbProductName.length() == 0){
            return null;
        }
        HashMap<String, UsbDevice> deviceList = findAllUsbDevices();
        for(UsbDevice device : deviceList.values()){
            if(device.getProductName().equals(usbProductName)){
                return device; 
            }
        }
        return null;
    }

    /*
	 *  自动请求所有USB设备的权限
	 */
    public Observable<Boolean> requestAllDevices(){
        Observable<Boolean> observable = null;
        try{
            UsbDevice[] usbDevices = findAllUsbDevices().values().toArray(new UsbDevice[0]);
            observable = requestPermission(usbDevices);
        }catch(IllegalArgumentException e){
            if(e.getMessage().contains("requires at least one")) {
                observable = Observable.just(true);
            }else {
                throw e;
            }
        }

        return observable;
    }

    public Observable<UsbPermission> requestEachDevices(){
        Observable<UsbPermission> observable = null;
        try{
            UsbDevice[] usbDevices = findAllUsbDevices().values().toArray(new UsbDevice[0]);
            observable = requestEachPermission(usbDevices);
        }catch(IllegalArgumentException e){
            if(e.getMessage().contains("requires at least one")) {
                observable = Observable.just(new UsbPermission(null, true));
            }else {
                throw e;
            }
        }

        return observable;
    }

    public Observable<Boolean> requestPermission(final UsbDevice... usbDevices) {
        return Observable.just(TRIGGER).compose(new ObservableTransformer<Object, Boolean>() {
            @Override
            public ObservableSource<Boolean> apply(Observable<Object> upstream) {
                return RxUsbDevice.this.requestPermission(upstream, usbDevices)
                        .buffer(usbDevices.length)
                        .flatMap(new Function<List<UsbPermission>, ObservableSource<Boolean>>() {
                            @Override
                            public ObservableSource<Boolean> apply(List<UsbPermission> permissions) throws Exception {
                                if (permissions.isEmpty()) {
                                    return Observable.empty();
                                }
                                for (UsbPermission usbPermission : permissions) {
                                    if (!usbPermission.granted) {
                                        return Observable.just(false);
                                    }
                                }
                                return Observable.just(true);
                            }
                        });
            }
        });
    }

    //逐个返回请求结果（调用多次onNext）
    public Observable<UsbPermission> requestEachPermission(final UsbDevice... usbDevices){
        //使用RxJava实现异步请求
        return Observable.just(TRIGGER).compose(new ObservableTransformer<Object, UsbPermission>() {
            @Override
            public ObservableSource<UsbPermission> apply(Observable<Object> upstream) {
                return requestPermission(upstream, usbDevices);
            }
        });
    }

    private Observable<UsbPermission> requestPermission(final Observable<?> trigger, final UsbDevice... usbDevices){
        if (usbDevices == null || usbDevices.length == 0) {
            throw new IllegalArgumentException("RxUsbDevice.requestPermission/requestEachPermission requires at least one input usbDevice");
        }
        return oneOf(trigger, pending(usbDevices))
                .flatMap(new Function<Object, ObservableSource<UsbPermission>>() {
                    @Override
                    public ObservableSource<UsbPermission> apply(Object o) throws Exception {
                        return requestPermissionImplementation(usbDevices);
                    }
                });
    }

    private Observable<?> oneOf(Observable<?> trigger, Observable<?> pending) {
        if (trigger == null) {
            return Observable.just(TRIGGER);
        }
        return Observable.merge(trigger, pending);
    }

    private Observable<?> pending(final UsbDevice... usbDevices) {
        for (UsbDevice device : usbDevices) {
            if (!rxUsbFragment.containsByDeviceName(device.getDeviceName())) {
                return Observable.empty();
            }
        }
        return Observable.just(TRIGGER);
    }

    private ObservableSource<UsbPermission> requestPermissionImplementation(UsbDevice... usbDevices) {
        List<Observable<UsbPermission>> list = new ArrayList<>(usbDevices.length);
        List<UsbDevice> unrequestedDeviceList = new ArrayList<>();
        for(UsbDevice device : usbDevices){
            rxUsbFragment.log("Requesting permission " + device.getDeviceName());
            if(isGranted(device)){
                list.add(Observable.just(new UsbPermission(device, true)));
                continue;
            }

            PublishSubject<UsbPermission> subject = rxUsbFragment.getSubjectByDeviceName(device.getDeviceName());
            if(subject == null){
                unrequestedDeviceList.add(device);
                subject = PublishSubject.create();
                rxUsbFragment.setSubjectForUsbDevice(device.getDeviceName(), subject);
            }
            list.add(subject);
        }
        if(!unrequestedDeviceList.isEmpty()){
            UsbDevice[] usbDeviceArr = unrequestedDeviceList.toArray(new UsbDevice[0]);
            rxUsbFragment.requstPermissions(usbDeviceArr);
        }

        return Observable.concat(Observable.fromIterable(list));
    }

    private boolean isGranted(UsbDevice usbDevice){
        return rxUsbFragment.isGranted(usbDevice);
    }
}
