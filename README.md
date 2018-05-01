# RxUsbDevice
用于USB设备的权限申请、USB设备的查找等，依赖RxJava2，程序架构思路来源于RxPermission2，即用向Activity中添加寄生Fragment，来实现一步到位的权限申请及其对应处理的业务流程。

### 安装
下载此项目源码到本地，然后用Android Studio导入源码中的module“library”到你的项目，然后记得将library的build.gradle文件中的compileSdkVersion、minSdkVersion、targetSdkVersion、versionCode等参数改成你自己的值。

### 使用
初始化RxUsbDevice:
```
   RxUsbDevice rxUsbDevice = new RxUsbDevice(MainActivity.this); //传入一个栈顶的Activity对象
```
#### 例子一：自动申请所有Usb设备权限
```
   rxUsbDevice.requestAllDevices()
           .subscribe(new Consumer<Boolean>(){
               @Override
               public void accept(Boolean granted) throw Exception{
                   if(granted){
                       System.out.println("全部通过");
                   }else {
                       System.out.println("没有全部通过");
                   }
               }
            })；
```
#### 或
```
    rxUsbDevice.requestEachDevices()
            .subscribe(new Consumer<UsbPermission>(){
                @Override
                public void accept(UsbPermission usbPermission) throw Exception{
                    if(null == usbPermission.usbDevice){
                        System.out.println("无已连接的Usb设备");
                    }else {
                        System.out.println("{" + 
                                "设备名：" + usbPermission.usbDevice.getDeviceName() + "，" +
                                "型号：" + usbPermission.usbDevice.getProductName() + "，" +
                                "权限是否申请成功：" + holder.granted) +
                                "}";
                    }
                }
            });
```
#### 例子二：申请指定Usb设备的权限
```
    //通过Usb设备型号名称获取其UsbDevice对象
    UsbDevice usbDevice1 = rxUsbDevice.findUsbDeviceByProductName("USBKey Chip USBKey Module X"); 
    //通过Usb设备的vendorId和productId获取其UsbDevice对象
    UsbDevice usbDevice2 = rxUsbDevice.findUsbDeviceById(1024, 50010);
    
    //申请权限(Lamba表达式写法)
    rxUsbDevice.requestPermission(new UsbDevice[]{usbDevice1, usbDevice2})
            .subscribe({
                granted ->
                if(granted){
                    System.out.println("全部通过");
                } else {
                    System.out.println("没有全部通过");
                }
            });
```
#### 或
```
    rxUsbDevice.requestEachPermission(new UsbDevice[]{usbDevice1, usbDevice2})
            .subscribe({
                usbPermission ->
                System.out.println("{" + 
                        "设备名：" + usbPermission.usbDevice.getDeviceName() + "，" +
                        "型号：" + usbPermission.usbDevice.getProductName() + "，" +
                        "权限是否申请成功：" + holder.granted) +
                        "}";
            });
```
