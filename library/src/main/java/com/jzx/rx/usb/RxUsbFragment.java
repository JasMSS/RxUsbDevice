package com.jzx.rx.usb;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;

import static android.content.Context.USB_SERVICE;

/**
 * Created by Jiangzx on 2018/3/22.
 */

public class RxUsbFragment extends Fragment{
    public static final String ACTION_USB_PERMISSION ="com.ukey.USB_PERMISSION";
    private UsbManager mUsbManager;
    private Map<String, PublishSubject<UsbPermission>> mSubjects = new HashMap<>();
    private MyBroadcastReceiver mReceiver;
    private boolean mLogging;

    private static class MyBroadcastReceiver extends BroadcastReceiver{
        WeakReference<RxUsbFragment> reference;
        public MyBroadcastReceiver(RxUsbFragment rxUsbFragment) {
            reference = new WeakReference<RxUsbFragment>(rxUsbFragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(reference.get() != null){
                if(intent.getAction().equalsIgnoreCase(ACTION_USB_PERMISSION)) {
                    if (intent.hasExtra(UsbManager.EXTRA_DEVICE)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                        reference.get().onUsbPermissionResult(device, granted);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initReceiver();
        mUsbManager = (UsbManager)getActivity().getSystemService(USB_SERVICE);
    }

    private void initReceiver(){
        if(mReceiver == null) {
            mReceiver = new MyBroadcastReceiver(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_USB_PERMISSION);
            getActivity().registerReceiver(mReceiver, intentFilter);
        }
    }

    public void requstPermissions(final UsbDevice... usbDevices){
        if(usbDevices != null && usbDevices.length > 0) {
            for (UsbDevice device : usbDevices) {
                if(mUsbManager.hasPermission(device)){
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    intent.putExtra(UsbManager.EXTRA_DEVICE, device);
                    intent.putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true);
                    getActivity().sendBroadcast(intent);
                }else {
                    PendingIntent mPermissionIntent = PendingIntent.getBroadcast(getActivity(),
                            0, new Intent(ACTION_USB_PERMISSION), 0);
                    mUsbManager.requestPermission(device, mPermissionIntent);
                }
            }
        }
    }

    public boolean isGranted(UsbDevice usbDevice) {
        return mUsbManager.hasPermission(usbDevice);
    }

    public boolean containsByDeviceName(String usbDeviceName){
        return mSubjects.containsKey(usbDeviceName);
    }

    public PublishSubject<UsbPermission> getSubjectByDeviceName(String usbDeviceName){
        return mSubjects.get(usbDeviceName);
    }

    public PublishSubject<UsbPermission> setSubjectForUsbDevice(String usbDeviceName, PublishSubject<UsbPermission> subject){
        return mSubjects.put(usbDeviceName, subject);
    }

    public void onUsbPermissionResult(UsbDevice usbDevice, boolean granted){
        if(usbDevice != null) {
            log("onUsbPermissionResult  " + usbDevice.getDeviceName());
            PublishSubject<UsbPermission> subject = getSubjectByDeviceName(usbDevice.getDeviceName());
            if (subject == null) {
                // No subject found
                Log.e(RxUsbDevice.TAG, "RxUsbFragment.onUsbPermissionResult invoked but didn't find the corresponding permission request.");
                return;
            }
            mSubjects.remove(usbDevice.getDeviceName());
            subject.onNext(new UsbPermission(usbDevice, granted));
            subject.onComplete();
        }
    }

    @Override
    public void onDestroy() {
        if(mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    public void setLogging(boolean logging){
        mLogging = logging;
    }

    public void log(String info){
        if(mLogging){
            Log.d(RxUsbDevice.TAG, info);
        }
    }
}
