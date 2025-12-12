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

import com.android.ddmlib.IDevice;
import com.google.common.base.Objects;
import com.adbtool.util.Cmd;
import com.adbtool.util.Constant;
import com.adbtool.util.FileUtil;
import org.apache.log4j.Logger;

import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfaceDescriptor;
import java.io.*;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdbDevice {
    
    private static Logger logger = Logger.getLogger(AdbDevice.class);
    
    /** Constant for ADB class. */
    private static final byte ADB_CLASS = (byte) 0xff;
    
    /** Constant for ADB sub class. */
    private static final byte ADB_SUBCLASS = 0x42;
    
    /** Constant for ADB protocol. */
    private static final byte ADB_PROTOCOL = 1;
    
    /** PropertyCahe KEY for serialNumber */
    public static final String SERIAL_NUMBER = "SERIAL";

    /** PropertyCahe KEY for DeviceCode */
    public static final String DEVICE_CODE = "CODE";
    
    /** PropertyCahe KEY for screenSize 获取的是 widthxheight的字符串 */
    public static final String SCREEN_SIZE = "SCREEN_SIZE";

    /** PropertyCahe KEY for electric quantity 获取的是 widthxheight的字符串 */
    public static final String ELECTRIC_QUANLITY = "SCREEN_SIZE";

    /** PropertyCahe Standard Value for isSIMExists*/
    public static final String IS_SIM_EXISTS="IS_SIM_EXISTS";

    /** PropertyCahe Standard Value for STANDARD_SCREEN_SIZE*/
    public static final String STANDARD_SCREEN_SIZE = "1080x2340";

    
    /** The claimed USB ADB interface. */
    private final UsbInterface iface;
    
    /** The in endpoint address. */
    private final byte inEndpoint;
    
    /** The out endpoint address. */
    private final byte outEndpoint;
    
    /** Reference From UsbDevice */
    private final UsbDevice usbDevice;
    
    /**  */
    Map<String, String> propertyCahe = new HashMap<>();
    
    /** Device Type */
    public enum Type {
        USBDevice,
        Other
    }
    
    private final Type type;

    private String code;
    
    /** IDevice */
    private IDevice iDevice;
    File data_dir=Constant.getDataDir();
    File device_list=Constant.getDataCache("devices_info");
    File app_group_list=Constant.getDataCache("apps_group_info");
    /**
     * Constructs a new ADB interface.
     *
     * @param iface
     *            The USB interface. Must not be null.
     * @param inEndpoint
     *            The in endpoint address.
     * @param outEndpoint
     *            THe out endpoint address.
     */
    public AdbDevice(UsbDevice usbDevice, UsbInterface iface, byte inEndpoint, byte outEndpoint)
    {
        if (iface == null)
            throw new IllegalArgumentException("iface must be set");
        this.type = Type.USBDevice;
        this.usbDevice = usbDevice;
        this.iface = iface;
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
    }
    
    public AdbDevice(IDevice iDevice) {
        this.type = Type.Other;
        this.usbDevice = null;
        this.iface = null;
        this.inEndpoint = 0;
        this.outEndpoint = 0;
        setIDevice(iDevice);
    }
    
    /**
     * Checks if the specified vendor is an ADB device vendor.
     *
     * @param vendorId
     *            The vendor ID to check.
     * @return True if ADB device vendor, false if not.
     */
    public static boolean isAdbVendor(short vendorId) {
        for (short adbVendorId: Vendors.VENDOR_IDS)
            if (adbVendorId == vendorId) return true;
        return false;
    }
    
    /**
     * Checks if the specified USB interface is an ADB interface.
     *
     * @param iface
     *            The interface to check.
     * @return True if interface is an ADB interface, false if not.
     */
    public static boolean isAdbInterface(UsbInterface iface) {
        UsbInterfaceDescriptor desc = iface.getUsbInterfaceDescriptor();
        return desc.bInterfaceClass() == ADB_CLASS &&
                desc.bInterfaceSubClass() == ADB_SUBCLASS &&
                desc.bInterfaceProtocol() == ADB_PROTOCOL;
    }
    
    /**
     * 获取SerialNumber，由于使用频繁，故特增加此接口
     * @return
     */
    public String getSerialNumber() {
        String serialNUmber = findPropertyCahe(SERIAL_NUMBER);
        if (serialNUmber == null) {
            if (iDevice != null) {
                serialNUmber = iDevice.getSerialNumber();
            } else if (usbDevice != null) {
                try {
                    serialNUmber = usbDevice.getSerialNumberString();
                } catch (UsbException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            propertyCahe.put(SERIAL_NUMBER, serialNUmber);
        }
        return serialNUmber;
    }
    
    public UsbDevice getUsbDevice() {
        return usbDevice;
    }
    
    public IDevice getIDevice() {
        return iDevice;
    }
    
    /**
     * 设置IDevice，同时会获取常用信息放到缓存中
     * @param iDevice
     */
    public void setIDevice(IDevice iDevice) {
        this.iDevice = iDevice;
        fillPropertyCahe();
    }
    
    public String getProperty(String name) {
        String s = findPropertyCahe(name);
        if (!Objects.deepToString(s).isEmpty()) {
            s = iDevice.getProperty(name);
        }
        return s;
    }

    /**
     * 若存在缓存数据则加载，否则重新生成缓存文件
     * @throws Exception
     */
    private void loadLocalData() throws Exception{

        if((FileUtil.checkFile(device_list)==null)) device_list.createNewFile();else fillPropertyCahe();
        if((FileUtil.checkFileDir(data_dir)==null)) data_dir.createNewFile();
    }


    /**
     * 若存在本地缓存数据文件则加载，否则重新生成缓存文件
     * @throws Exception
     */
    private void fillPropertyCahe()  {
               String sdk = iDevice.getProperty(Constant.PROP_SDK);
               int sdkv = Integer.parseInt(sdk);
               // serialNumber
               propertyCahe.put(SERIAL_NUMBER, iDevice.getSerialNumber());
               // abi & sdk
               String abi = iDevice.getProperty(Constant.PROP_ABI);
               propertyCahe.put(Constant.PROP_ABI, abi);
               propertyCahe.put(Constant.PROP_SDK, sdk);
               propertyCahe.put(DEVICE_CODE, code);
           }



    //添加或更新设备尺寸参数缓存
      public void refreshScreenSize(){
          String sdk = iDevice.getProperty(Constant.PROP_SDK);
          // android 4.3 以下没有 displays
         int sdkv = Integer.parseInt(sdk);
          String shellCmd = sdkv > 17 ? "dumpsys window displays | sed -n '3p'" : "dumpsys window";
          String str = AdbServer.executeShellCommand(iDevice, shellCmd);
        if (str != null && !str.isEmpty()) {
        Pattern pattern = Pattern.compile("init=(\\d+x\\d+)");
        Matcher m = pattern.matcher(str);
        if (m.find()) {
            if (!m.group(1).equals("16x16")) propertyCahe.put(SCREEN_SIZE, m.group(1));
            else propertyCahe.put(SCREEN_SIZE, STANDARD_SCREEN_SIZE);
            System.out.println(SERIAL_NUMBER + ":" + "SCREEN_SIZE：" + propertyCahe.get(SCREEN_SIZE));
        }
    }
}

    //添加设备电量参数缓存
    public void refreshElectricQuanlity(){
        String cmd="dumpsys battery|sed -n '11p'";
        String str=AdbServer.executeShellCommand(iDevice,cmd);
        if(str!=null&&!str.isEmpty()){
            Pattern pattern1=Pattern.compile("level: (\\d+)");
            Matcher m1 = pattern1.matcher(str);
            if (m1.find()) {
                propertyCahe.put(ELECTRIC_QUANLITY, m1.group(0));
                System.out.println(ELECTRIC_QUANLITY + ":" + propertyCahe.get(ELECTRIC_QUANLITY));
            }
        }
    }




    //添加设备是否有SIM卡及运营商
    public void refreshSIMInfo() {
        String cmd = "getprop gsm.sim.operator.numeric";
        String str = AdbServer.executeShellCommand(iDevice, cmd);
        boolean isexists=false;
        if (str != null && !str.isEmpty()) isexists=true; else isexists=false;
        propertyCahe.put(IS_SIM_EXISTS, Boolean.toString(isexists));

    }

    //获取指定已安装APP信息
    public void getInstalledAppInfo(){
        String code=findPropertyCahe(SERIAL_NUMBER);
    }

    /**
     * 判断IP地址是否合法
     * @param ip
     * @return
     */
    public static boolean isValidIPV4ByJDK(String ip) {
        try {
            return Inet4Address.getByName(ip)
                    .getHostAddress().equals(ip);
        } catch (UnknownHostException ex) {
            return false;
        }
    }


    /**
     * 在缓存中查找属性值
     * @param key
     */
    public String findPropertyCahe(String key) {
        return propertyCahe.get(key);
    }

    public String getAppGroupDir(String dir,String code){
        char [] group =code.toCharArray();
        return "("+(dir.endsWith(File.separator)?dir:dir+File.separator)+group[1]+File.separator+"*.apk"+")";
    }

    public String getAppToolDir(String dir){
        return "("+(dir.endsWith(File.separator)?dir:dir+File.separator)+"common"+File.separator+"*.apk"+")";
    }

    /**
     * 设置设备常量
     */
    public void setConstant(){
        String shellCmd="settings put system screen_off_timeout 2147483647";
        String console=AdbServer.executeShellCommand(iDevice, shellCmd);
        System.out.println("shellCmd:"+shellCmd);
        System.out.println("console:"+console);
    }

    /**
     * 关闭移动数据
     */
    public void closeData(){
        String shellCmd="svc data disable";
        String console=AdbServer.executeShellCommand(iDevice, shellCmd);
        System.out.println("shellCmd:"+shellCmd);
        System.out.println("console:"+console);
    }

    /**
     * 批量安装APPc
     */
    public void installMultiAPP(String dir,String code,boolean isAdd)  {
        String code_cmd="chcp 65001";
        String cmd="for %i in "+getAppGroupDir(dir,code)+" do "+code_cmd+"|adb -s "+iDevice.getSerialNumber()+" install %i";
        String add_cmd="for %i in "+getAppToolDir(dir)+" do "+code_cmd+"|adb -s "+iDevice.getSerialNumber()+" install %i";
        try {
            Cmd.executive(cmd);
            if(isAdd) Cmd.executive(add_cmd);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("cmd:"+cmd);
    }


    /**
     * 重置系统
     */
    public void reset()  {
        String cmd="adb -s "+iDevice.getSerialNumber()+" reboot recovery";
        try {
            Cmd.executive(cmd);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("cmd:"+cmd);
    }

    /**
     * 网络映射调用安卓CLI应用程序并执行内部命令
     * @param command
     */
    public void callADBShell(String command,String port) throws Exception {
        // 创建Socket对象，指定服务端的IP地址和端口号
        Socket socket = new Socket("127.0.0.1",Integer.parseInt(port));
        // 获取输入流和输出流 输入流和输出流是通过socket对象来进行数据传输的。
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//        从控制台读取用户输入
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        String message;
//
//        while (true) {
//            System.out.println("请输入要发送的信息（输入 'exit' 退出）：");
//            message = reader.readLine();
//
//            if (message.equalsIgnoreCase("exit")) {
//                // 如果用户输入 'exit'，发送终止标志给服务端并退出循环
//                out.println("exit");
//                break;
//            }
   // }
            // 将用户输入的信息发送给服务端
            out.println(command);
            // 接收服务端的响应并打印
            String response = in.readLine();
            System.out.println("服务端响应：" + response);
            socket.close();
        }
        // 关闭连接
    }





