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

package com.adbtool.util;

import com.alibaba.fastjson.JSON;
import com.yeetor.adb.AdbDevice;
import com.yeetor.adb.AdbServer;
import com.yeetor.androidcontrol.APPInfo;
import com.yeetor.androidcontrol.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

public class AdbUtils {

    /**
     * 将当前连接的设备列表转换为json
     * @return
     */
    public static String devices2JSON() {
        return devices2JSON(AdbServer.server().getDevices());
    }
    
    public static String devices2JSON(List<AdbDevice> devices) {
        ArrayList<DeviceInfo> list = new ArrayList<>();
        for (AdbDevice device : devices) {
            list.add(new DeviceInfo(device));
        }
        return JSON.toJSONString(list);
    }

    /**
     * 获取每台设备的APP信息封装为json数据
     */
    public static String apps2JSON(List<AdbDevice> devices) {
        ArrayList<APPInfo> list = new ArrayList<>();
        for (AdbDevice device : devices) {
            list.add(new APPInfo(device));
        }
        return JSON.toJSONString(list);
    }

//    public static void device2XML() throws Exception {
//        ArrayList<List<String>> nodes=new ArrayList<>();
//        ArrayList<List<String>> attributes=new ArrayList<>();
//        ArrayList<List<String>> values=new ArrayList<>();
//        ArrayList<List<DeviceInfo>> device_texts=new ArrayList<>();
//        XMLRange range=new XMLRange(nodes,attributes,values,device_texts);
//        File file=FileUtil.checkFile(Constant.getDataCache("devices_infos.txt"));
//        if(file==null) FileUtil.createFile(Constant.getDataCache("devices_infos.txt").getPath());
//        XMLUtil.generateXml(FileUtil.checkFile(Constant.getDataCache("devices_infos.txt")),XMLUtil.Device_Xml,);
//
//    }


    
}
