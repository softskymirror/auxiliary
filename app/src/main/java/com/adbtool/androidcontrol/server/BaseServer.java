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

package com.adbtool.androidcontrol.server;

import com.alibaba.fastjson.JSON;
import com.adbtool.adb.AdbDevice;
import com.adbtool.adb.AdbServer;
import com.adbtool.androidcontrol.DeviceInfo;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * BaseServer - shared utility base class for LocalServer and RemoteServer.
 */
public class BaseServer {

    private static final Logger logger = Logger.getLogger(BaseServer.class);

    /**
     * Get device info as JSON string.
     * @return JSON string of all connected devices
     */
    public static String getDevicesJSON() {
        List<DeviceInfo> list = new ArrayList<>();
        try {
            for (AdbDevice device : AdbServer.server().getDevices()) {
                list.add(new DeviceInfo(device));
            }
        } catch (Exception e) {
            logger.error("Failed to get device list", e);
        }
        return JSON.toJSONString(list);
    }
}
