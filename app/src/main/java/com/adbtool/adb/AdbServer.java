/*
 *
 * MIT License
 *
 * Copyright (c) 2017 朱辉 https://blog.yeetor.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.adbtool.adb;

import com.android.ddmlib.*;
import com.adbtool.util.Constant;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.log4j.Logger;

import javax.usb.*;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class AdbServer {
    private static Logger logger = Logger.getLogger(AdbServer.class);
    private static volatile AdbServer server;
    private String adbPath = null;
    private String adbPlatformTools = "platform-tools";
    /** 缓存已解析的 ADB 可执行文件完整路径，避免 getADBPath() 重复追加 */
    private volatile String resolvedAdbPath = null;
    
    List<AdbDevice> adbDeviceList = new CopyOnWriteArrayList<>();
    /** 事件监听器列表，使用 CopyOnWriteArrayList 保证线程安全 */
    List<IAdbServerListener> listeners = new CopyOnWriteArrayList<>();
    /** 同步锁，保护adbDeviceList的读写操作 */
    private final Object deviceListLock = new Object();

    AndroidDebugBridge adb = null;
    private boolean success = false;
    /** ADB设备同步线程引用，用于shutdown时中断 */
    private volatile Thread syncThread;
    /** 标记ADB监听是否运行中 */
    private volatile boolean adbRunning = false;

    /** 设备变更事件日志缓存（最近 MAX_EVENT_LOG_SIZE 条） */
    private final List<DeviceChangeEvent> deviceChangeLog = new CopyOnWriteArrayList<>();
    private static final int MAX_EVENT_LOG_SIZE = 200;

    /** 设备列表本地缓存文件 */
    private File deviceCacheFile;

    /** 日期格式化（线程安全） */
    private static final SimpleDateFormat EVENT_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 设备连接类型
     */
    public enum ConnectType {
        /** USB 有线连接 */
        USB,
        /** WiFi / 网络连接 */
        WIFI,
        /** 其他连接方式（如蓝牙等） */
        OTHER
    }

    /**
     * 设备变更事件类型
     */
    public enum ChangeType {
        CONNECTED,
        DISCONNECTED
    }

    /**
     * 设备变更事件记录
     */
    public static class DeviceChangeEvent {
        private final String serialNumber;
        private final ChangeType changeType;
        private final ConnectType connectType;
        private final long timestamp;
        private final String detail;

        public DeviceChangeEvent(String serialNumber, ChangeType changeType, ConnectType connectType, String detail) {
            this.serialNumber = serialNumber;
            this.changeType = changeType;
            this.connectType = connectType;
            this.timestamp = System.currentTimeMillis();
            this.detail = detail;
        }

        /**
         * 带自定义时间戳的构造函数（用于从 CSV 反序列化）
         */
        private DeviceChangeEvent(String serialNumber, ChangeType changeType, ConnectType connectType, long timestamp, String detail) {
            this.serialNumber = serialNumber;
            this.changeType = changeType;
            this.connectType = connectType;
            this.timestamp = timestamp;
            this.detail = detail;
        }

        public String getSerialNumber() { return serialNumber; }
        public ChangeType getChangeType() { return changeType; }
        public ConnectType getConnectType() { return connectType; }
        public long getTimestamp() { return timestamp; }
        public String getDetail() { return detail; }

        @Override
        public String toString() {
            String time;
            synchronized (EVENT_TIME_FORMAT) {
                time = EVENT_TIME_FORMAT.format(new Date(timestamp));
            }
            return String.format("[%s] %s %s (%s) - %s", time, changeType, serialNumber, connectType, detail);
        }

        /**
         * 序列化为 CSV 行，用于本地持久化
         */
        public String toCsvLine() {
            return timestamp + "," + serialNumber + "," + changeType + "," + connectType + "," + (detail != null ? detail : "");
        }

        /**
         * 从 CSV 行反序列化
         */
        public static DeviceChangeEvent fromCsvLine(String line) {
            try {
                String[] parts = line.split(",", 5);
                if (parts.length < 4) return null;
                long ts = Long.parseLong(parts[0]);
                String sn = parts[1];
                ChangeType ct = ChangeType.valueOf(parts[2]);
                ConnectType connType = ConnectType.valueOf(parts[3]);
                String detail = parts.length >= 5 ? parts[4] : "";
                return new DeviceChangeEvent(sn, ct, connType, ts, detail);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static AdbServer server() {
        if (server == null) {
            synchronized (AdbServer.class) {
                if (server == null) {
                    server = new AdbServer();
                }
            }
        }
        return server;
    }

    private AdbServer() {
        initCacheFile();
        init();
    }

    /**
     * 初始化设备缓存文件路径
     */
    private void initCacheFile() {
        try {
            deviceCacheFile = Constant.getDataCache("adb_device_cache.csv");
            File parent = deviceCacheFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception e) {
            logger.warn("初始化设备缓存文件路径失败", e);
            deviceCacheFile = null;
        }
    }
    
    /**
     * 监听USB设备的状态
     */
    public void listenUSB() {
        adbDeviceList = new CopyOnWriteArrayList<>();
        
        UsbServices services = null;
        try {
            services = UsbHostManager.getUsbServices();
        } catch (UsbException e) {
            logger.error("获取USB服务失败", e);
            return;
        }
        services.addUsbServicesListener(new MyUSBListener());
        logger.info("已开启USB设备监听...");
    }
    
    /**
     * 监听ADB
     */
    public void listenADB() {
        // 如果 ADB bridge 已断开，重新连接
        reconnect();
        // 如果已有同步线程在运行，先中断
        if (syncThread != null && syncThread.isAlive()) {
            logger.info("中断已有的ADB同步线程...");
            syncThread.interrupt();
        }
        syncThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    refreshAdbDeviceList();
                }
                logger.info("ADB设备同步线程已退出");
            }
        }, "ADB-Device-Sync");
        syncThread.setDaemon(true);
        syncThread.start();
        adbRunning = true;
        logger.info("已开启ADB设备同步线程...");
    }
     
    /**
     * USB设备连接时调用
     */
    private void onUsbDeviceConnected(UsbDevice usbDevice) {
        logger.info(String.format("USB设备连接：idProduct(0x%x) idVendor(0x%x)", usbDevice.getUsbDeviceDescriptor().idProduct(), usbDevice.getUsbDeviceDescriptor().idVendor()));
        List<AdbDevice> devices = checkAdbDevices(usbDevice);
        devices.forEach(adbDevice -> onAdbDeviceConnected(adbDevice));
    }
    
    /**
     * USB设备断开时调用
     */
    private void onUsbDeviceDisConnected(UsbDevice usbDevice) {
        logger.info(String.format("USB设备断开：idProduct(0x%x) idVendor(0x%x)", usbDevice.getUsbDeviceDescriptor().idProduct(), usbDevice.getUsbDeviceDescriptor().idVendor()));
        List<AdbDevice> devices = checkAdbDevices(usbDevice);
        devices.forEach(adbDevice -> onAdbDeviceDisConnected(adbDevice));
    }
    
    /**
     * 发现安卓设备时调用
     */
    private void onAdbDeviceConnected(AdbDevice adbDevice) {
        // USB 事件仅作为“立即刷新”通知，避免与 listenADB 线程冲突
        refreshAdbDeviceList();
    }
    
    /**
     * 发现安卓设备断开时调用
     */
    private void onAdbDeviceDisConnected(AdbDevice adbDevice) {
        if (adbDevice == null) return;
        Iterator<AdbDevice> it = adbDeviceList.iterator();
        while (it.hasNext()) {
            AdbDevice device = it.next();
            if (adbDevice.getUsbDevice() != null && adbDevice.getUsbDevice() == device.getUsbDevice()) {
                logger.info("Android设备断开：" + adbDevice.getSerialNumber());
                adbDeviceList.remove(device);
                recordChangeEvent(adbDevice.getSerialNumber(), ChangeType.DISCONNECTED, detectConnectType(adbDevice), "USB disconnect");
                saveDeviceCache();
                notifyListenersDisconnected(device);
            }
        }
    }
    
    /**
     * 检测该UsbDevice是否是安卓设备
     * @param usbDevice
     * @return 检测到的安卓设备
     */
    private List<AdbDevice> checkAdbDevices(UsbDevice usbDevice) {
        
        List<AdbDevice> adbDevices = new ArrayList<>();
        
        UsbDeviceDescriptor deviceDesc = usbDevice.getUsbDeviceDescriptor();
        
        // Ignore devices from Non-ADB vendors
        // 这步不要，要不然杂牌手机就没法检测到
        // if (!AdbDevice.isAdbVendor(deviceDesc.idVendor())) return adbDevices;
        
        // Check interfaces of device
        UsbConfiguration config = usbDevice.getActiveUsbConfiguration();
        for (UsbInterface iface: (List<UsbInterface>) config.getUsbInterfaces())
        {
            List<UsbEndpoint> endpoints = iface.getUsbEndpoints();
        
            // Ignore interface if it does not have two endpoints
            if (endpoints.size() != 2) continue;
        
            // Ignore interface if it does not match the ADB specs
            if (!AdbDevice.isAdbInterface(iface)) continue;
        
            UsbEndpointDescriptor ed1 =
                    endpoints.get(0).getUsbEndpointDescriptor();
            UsbEndpointDescriptor ed2 =
                    endpoints.get(1).getUsbEndpointDescriptor();
        
            // Ignore interface if endpoints are not bulk endpoints
            if (((ed1.bmAttributes() & UsbConst.ENDPOINT_TYPE_BULK) == 0) ||
                    ((ed2.bmAttributes() & UsbConst.ENDPOINT_TYPE_BULK) == 0))
                continue;
        
            // Determine which endpoint is in and which is out. If both
            // endpoints are in or out then ignore the interface
            byte a1 = ed1.bEndpointAddress();
            byte a2 = ed2.bEndpointAddress();
            byte in, out;
            if (((a1 & UsbConst.ENDPOINT_DIRECTION_IN) != 0) &&
                    ((a2 & UsbConst.ENDPOINT_DIRECTION_IN) == 0)) {
                in = a1;
                out = a2;
            } else if (((a2 & UsbConst.ENDPOINT_DIRECTION_IN) != 0) &&
                    ((a1 & UsbConst.ENDPOINT_DIRECTION_IN) == 0)) {
                out = a1;
                in = a2;
            } else { 
                continue;
            }
            
            adbDevices.add(new AdbDevice(usbDevice, iface, in, out));
        }
        return adbDevices;
    }
    
    /**
     * 与adb同步设备状态
     * why？有可能设备是通过wifi或bt连接，这样usb接口是检测不到的
     */
    private void refreshAdbDeviceList() {
        if (this.adbDeviceList == null) return;
        List<AdbDevice> tmpAdbDeviceList = new ArrayList<>(this.adbDeviceList);
        IDevice[] iDevices = getIDevices();
        boolean changed = false;

        // 添加新的adb设备
        for (IDevice iDevice : iDevices) {
            boolean exists = false;
            for (AdbDevice adbDev : tmpAdbDeviceList) {
                if (adbDev.getIDevice() != null
                        && adbDev.getIDevice().getSerialNumber().equals(iDevice.getSerialNumber())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                AdbDevice device = new AdbDevice(iDevice);
                ConnectType connType = detectConnectType(device);
                logger.info("Android设备连接：" + device.getSerialNumber() + " (" + connType + ")");
                this.adbDeviceList.add(device);
                recordChangeEvent(device.getSerialNumber(), ChangeType.CONNECTED, connType, "ADB sync detected");
                changed = true;
                notifyListenersConnected(device);
            }
        }
        // 移除已断开的设备
        for (AdbDevice adbDev : tmpAdbDeviceList) {
            if (adbDev.getIDevice() == null) continue;
            boolean exists = false;
            for (IDevice iDevice : iDevices) {
                if (adbDev.getIDevice().getSerialNumber().equals(iDevice.getSerialNumber())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                adbDeviceList.remove(adbDev);
                ConnectType connType = detectConnectType(adbDev);
                logger.info("Android设备断开：" + adbDev.getSerialNumber() + " (" + connType + ")");
                recordChangeEvent(adbDev.getSerialNumber(), ChangeType.DISCONNECTED, connType, "ADB sync removed");
                changed = true;
                notifyListenersDisconnected(adbDev);
            }
        }
        if (changed) {
            saveDeviceCache();
        }
    }
    
    /**
     * 获取ADB可执行文件路径（结果缓存，避免重复追加）
     */
    private String getADBPath(){
        if (resolvedAdbPath != null) {
            return resolvedAdbPath;
        }
        if (adbPath == null) {
            adbPath = System.getenv("ANDROID_SDK_ROOT");
            if (adbPath != null) {
                adbPath += File.separator + adbPlatformTools;
            } else {
                resolvedAdbPath = "adb";
                return resolvedAdbPath;
            }
        }
        resolvedAdbPath = adbPath + File.separator + "adb";
        return resolvedAdbPath;
    }

    private void init() {
        //ADB若已经启动，将报错：AndroidDebugBridge.init() has already been called.
        try {
            AndroidDebugBridge.init(false);
        } catch (IllegalStateException e) {
            logger.info("AndroidDebugBridge 已初始化，跳过重复初始化");
        }
        adb = AndroidDebugBridge.createBridge(getADBPath(), true);
        if (adb != null) {
            if (waitForDeviceList()) {
                success = true;
            }
        }
        // 启动时加载本地设备缓存
        loadDeviceCache();
    }

    private boolean waitForDeviceList() {
        int maxWaittingTime = 100;
        int interval = 10;
        while (!adb.hasInitialDeviceList()) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                break;
            }
            maxWaittingTime -= 1;
            if (maxWaittingTime == 0) {
                disconnectAdb();
                return false;
            }
        }
        return true;
    }

    void disconnectAdb() {
        if (adb != null) {
            adb = null;
        }
        success = false;
    }

    /**
     * 重新连接 ADB bridge（shutdown 后可重新初始化）
     */
    private void reconnect() {
        if (adb == null) {
            try {
                adb = AndroidDebugBridge.createBridge(getADBPath(), true);
                if (adb != null) {
                    waitForDeviceList();
                    success = true;
                }
            } catch (Exception e) {
                logger.error("重新连接 ADB 失败", e);
            }
        }
    }
    
    /**
     * 获取ADB命令返回的设备列表
     * @return IDevices
     */
    public IDevice[] getIDevices() {
        if (adb == null) return new IDevice[0];
        return adb.getDevices();
    }
    
    public List<AdbDevice> getDevices() {
        return this.adbDeviceList;
    }
    
    public AdbDevice getDevice(String serialNumber) {
        for (AdbDevice device : adbDeviceList) {
            if (device.getSerialNumber().equals(serialNumber)) {
                return device;
            }
        }
        return null;
    }

    public AdbDevice getFirstDevice() {
        if (adbDeviceList.size() > 0) {
            return adbDeviceList.get(0);
        }
        return null;
    }

    public static String executeShellCommand(IDevice device, String command) {
        CollectingOutputReceiver output = new CollectingOutputReceiver();

        // 前置检查：设备是否在线（避免 ddmlib 连接过期时无意义的调用）
        if (device == null || !device.isOnline()) {
            logger.warn("设备不在线，跳过Shell命令: " + command);
            return output.getOutput();
        }

        try {
            device.executeShellCommand(command, output, 0);
        } catch (TimeoutException e) {
            logger.warn("Shell命令执行超时: " + command, e);
        } catch (AdbCommandRejectedException e) {
            logger.warn("ADB命令被拒绝: " + command + " | " + e.getMessage());
        } catch (ShellCommandUnresponsiveException e) {
            logger.warn("Shell命令无响应: " + command, e);
        } catch (IOException e) {
            logger.warn("Shell命令IO异常: " + command, e);
        }
        return output.getOutput();
    }

    /**
     * TODO: 添加自定义adb命令，原因是安卓手表的传输速度太慢，导致adb push超时错误
     * @param device
     * @return
     */
    public String executePushFile(IDevice device, String src, String dst) {
        final File adbFile = new File(AdbServer.server().adbPath);
        final SettableFuture future = SettableFuture.create();
        (new Thread(new Runnable() {
            public void run() {
                ProcessBuilder pb = new ProcessBuilder(new String[]{adbFile.getPath(), "-s", device.getSerialNumber(), "push", src, dst});
                pb.redirectErrorStream(true);
                Process p = null;
                try {
                    p = pb.start();
                } catch (IOException e) {
                    future.setException(e);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    String line;
                    try {
                        while((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        future.set(sb.toString());
                        return;
                    } catch (IOException ex) {
                        future.setException(ex);
                        return;
                    }
                } finally {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        future.setException(ex);
                    }

                }
            }
        }, "ADB-Push-File")).start();
        String s = "";
        try {
            s = (String) future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
        return s;
    }

    private ListenableFuture<List<AdbForward>> executeGetForwardList() {
        final File adbFile = new File(AdbServer.server().adbPath);
        final SettableFuture future = SettableFuture.create();
        (new Thread(new Runnable() {
            public void run() {
                ProcessBuilder pb = new ProcessBuilder(new String[]{adbFile.getPath(), "forward", "--list"});
                pb.redirectErrorStream(true);
                Process p = null;

                try {
                    p = pb.start();
                } catch (IOException e) {
                    future.setException(e);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

                try {
                    String line;
                    try {
                        List<AdbForward> list = new ArrayList<AdbForward>();
                        while((line = br.readLine()) != null) {
                            //64b2b4d9 tcp:555 localabstract:shit
                            AdbForward forward = new AdbForward(line);
                            if (forward.isForward()) {
                                list.add(forward);
                            }
                        }
                        future.set(list);
                        return;
                    } catch (IOException ex) {
                        future.setException(ex);
                        return;
                    }
                } finally {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        future.setException(ex);
                    }

                }
            }
        }, "ADB-Forward-List")).start();
        return future;
    }

    public AdbForward[] getForwardList() {
        ListenableFuture<List<AdbForward>> future = executeGetForwardList();
        try {
            List<AdbForward> s = future.get(1, TimeUnit.SECONDS);
            AdbForward[] ret = new AdbForward[s.size()];
            s.toArray(ret);
            return ret;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new AdbForward[0];
    }
    
    public void addListener(IAdbServerListener listener) {
        if (listener != null && listeners != null) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(IAdbServerListener listener) {
        if (listener != null && listeners != null) {
            this.listeners.remove(listener);
        }
    }

    /**
     * 关闭ADB服务器，释放资源
     */
    public void shutdown() {
        logger.info("正在关闭AdbServer...");
        // 关闭前保存设备缓存
        saveDeviceCache();
        // 中断 ADB 同步线程
        if (syncThread != null && syncThread.isAlive()) {
            syncThread.interrupt();
            logger.info("ADB同步线程已中断");
        }
        syncThread = null;
        adbRunning = false;
        disconnectAdb();
        if (adbDeviceList != null) {
            adbDeviceList.clear();
        }
        listeners.clear();
        // 清除 resolvedAdbPath 以便下次启动重新解析
        resolvedAdbPath = null;
    }

    /**
     * 检查ADB服务器是否正在运行
     * @return true 如果ADB同步线程正在运行
     */
    public boolean isRunning() {
        return adbRunning && syncThread != null && syncThread.isAlive();
    }

    /**
     * 重启ADB连接（不影响Web服务和USB监听）。
     * <p>
     * 用于解决 ddmlib 与 ADB 服务器连接不同步的问题：
     * 保存缓存 → 中断旧同步线程 → 重建 ADB bridge → 启动新同步线程。
     * <p>
     * 保留 USB 监听和 listeners，避免影响 Web 服务。
     */
    public void restartAdb() {
        logger.info("正在重启ADB连接...");
        // 1. 保存设备缓存
        saveDeviceCache();
        // 2. 中断旧的同步线程
        if (syncThread != null && syncThread.isAlive()) {
            syncThread.interrupt();
            try {
                syncThread.join(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        syncThread = null;
        adbRunning = false;
        // 3. 断开旧的 ADB bridge
        disconnectAdb();
        // 4. 清空设备列表（旧 IDevice 引用已失效）
        if (adbDeviceList != null) {
            adbDeviceList.clear();
        }
        // 5. 重建 ADB bridge（获取全新的 IDevice 实例）
        reconnect();
        // 6. 启动新的同步线程
        syncThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    refreshAdbDeviceList();
                }
                logger.info("ADB设备同步线程已退出");
            }
        }, "ADB-Device-Sync");
        syncThread.setDaemon(true);
        syncThread.start();
        adbRunning = true;
        logger.info("ADB连接重启完成");
    }

    /**
     * 检测设备的连接类型（USB / WiFi / Other）
     * 判断逻辑：
     * 1. 如果 AdbDevice 持有 UsbDevice 引用 → USB
     * 2. 如果 serialNumber 包含 ":" (如 "192.168.1.100:5555") → WiFi
     * 3. 其他 → OTHER
     */
    public ConnectType detectConnectType(AdbDevice device) {
        if (device == null) return ConnectType.OTHER;
        if (device.getUsbDevice() != null) {
            return ConnectType.USB;
        }
        String sn = device.getSerialNumber();
        if (sn != null && sn.contains(":")) {
            return ConnectType.WIFI;
        }
        return ConnectType.OTHER;
    }

    /**
     * 记录设备变更事件到内存日志缓存
     */
    private void recordChangeEvent(String serialNumber, ChangeType changeType, ConnectType connectType, String detail) {
        DeviceChangeEvent event = new DeviceChangeEvent(serialNumber, changeType, connectType, detail);
        deviceChangeLog.add(event);
        logger.info("设备变更事件: " + event);
        while (deviceChangeLog.size() > MAX_EVENT_LOG_SIZE) {
            deviceChangeLog.remove(0);
        }
        appendEventToFile(event);
    }

    /**
     * 获取设备变更事件日志（最近 N 条）
     * @param maxCount 最大返回条数，<=0 表示全部
     */
    public List<DeviceChangeEvent> getDeviceChangeLog(int maxCount) {
        if (maxCount <= 0 || maxCount >= deviceChangeLog.size()) {
            return Collections.unmodifiableList(new ArrayList<>(deviceChangeLog));
        }
        int fromIndex = deviceChangeLog.size() - maxCount;
        return Collections.unmodifiableList(new ArrayList<>(deviceChangeLog.subList(fromIndex, deviceChangeLog.size())));
    }

    /**
     * 获取设备变更事件日志（全部）
     */
    public List<DeviceChangeEvent> getDeviceChangeLog() {
        return getDeviceChangeLog(0);
    }

    // ==================== 设备列表本地缓存 ====================

    /**
     * 保存当前设备列表到本地 CSV 文件
     * 格式: serialNumber,connectType,sdk,abi,model
     */
    public void saveDeviceCache() {
        if (deviceCacheFile == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(deviceCacheFile, false))) {
            writer.println("#serialNumber,connectType,sdk,abi,model");
            for (AdbDevice device : adbDeviceList) {
                String sn = device.getSerialNumber() != null ? device.getSerialNumber() : "";
                ConnectType ct = detectConnectType(device);
                String sdk = device.getProperty(Constant.PROP_SDK);
                String abi = device.getProperty(Constant.PROP_ABI);
                String model = device.getProperty("ro.product.model");
                writer.println(sn + "," + ct + "," +
                        (sdk != null ? sdk : "") + "," +
                        (abi != null ? abi : "") + "," +
                        (model != null ? model : ""));
            }
            logger.debug("设备列表已保存到本地缓存: " + deviceCacheFile.getPath());
        } catch (Exception e) {
            logger.warn("保存设备列表到本地缓存失败", e);
        }
    }

    /**
     * 从本地 CSV 文件加载设备缓存（仅加载元数据，不重建 IDevice 连接）
     */
    private void loadDeviceCache() {
        if (deviceCacheFile == null || !deviceCacheFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(deviceCacheFile))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                String[] parts = line.split(",", 5);
                if (parts.length >= 1) {
                    count++;
                }
            }
            logger.info("从本地缓存加载了 " + count + " 个设备记录: " + deviceCacheFile.getPath());
        } catch (Exception e) {
            logger.warn("加载设备本地缓存失败", e);
        }
    }

    /**
     * 将变更事件追加写入本地事件日志文件
     */
    private void appendEventToFile(DeviceChangeEvent event) {
        if (deviceCacheFile == null) return;
        File eventLogFile = new File(deviceCacheFile.getParent(), "adb_device_events.log");
        try (PrintWriter writer = new PrintWriter(new FileWriter(eventLogFile, true))) {
            writer.println(event.toCsvLine());
        } catch (Exception e) {
            logger.warn("追加设备变更事件到日志文件失败", e);
        }
    }

    // ==================== Listener 通知辅助方法 ====================

    private void notifyListenersConnected(AdbDevice device) {
        for (IAdbServerListener listener : listeners) {
            try {
                listener.onAdbDeviceConnected(device);
            } catch (Exception e) {
                logger.warn("Listener onAdbDeviceConnected 回调异常", e);
            }
        }
    }

    private void notifyListenersDisconnected(AdbDevice device) {
        for (IAdbServerListener listener : listeners) {
            try {
                listener.onAdbDeviceDisConnected(device);
            } catch (Exception e) {
                logger.warn("Listener onAdbDeviceDisConnected 回调异常", e);
            }
        }
    }

    class MyUSBListener implements UsbServicesListener {
        
        @Override
        public void usbDeviceAttached(UsbServicesEvent usbServicesEvent) {
            UsbDevice device = usbServicesEvent.getUsbDevice();
            if (!device.isUsbHub()) {
                onUsbDeviceConnected(device);
            }
        }
        
        @Override
        public void usbDeviceDetached(UsbServicesEvent usbServicesEvent) {
            UsbDevice device = usbServicesEvent.getUsbDevice();
            if (!device.isUsbHub()) {
                onUsbDeviceDisConnected(device);
            }
        }
    }
}
