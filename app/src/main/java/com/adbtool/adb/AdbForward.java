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

import org.apache.log4j.Logger;

/**
 * ADB端口转发映射数据类
 * 封装 adb forward --list 输出的解析结果
 */
public class AdbForward {
    private static final Logger logger = Logger.getLogger(AdbForward.class);

    private String serialNumber;
    private int port;
    private String localabstract;

    private boolean isForward = true;

    public AdbForward(String serialNumber, int port, String localabstract) {
        this.serialNumber = serialNumber;
        this.port = port;
        this.localabstract = localabstract;
    }

    /**
     * 从 adb forward --list 输出行解析
     * 格式: "serialNumber tcp:port localabstract:name"
     * @param str adb forward输出行
     */
    public AdbForward(String str) {
        if (str == null || str.trim().isEmpty()) {
            isForward = false;
            return;
        }
        String[] s = str.trim().split("\\s+");
        if (s.length != 3) {
            logger.warn("无法解析adb forward行，字段数不匹配: " + str);
            isForward = false;
            return;
        }

        serialNumber = s[0];

        // 解析端口: "tcp:555"
        String[] portstr = s[1].split(":");
        if (portstr.length == 2) {
            try {
                port = Integer.parseInt(portstr[1].trim());
            } catch (NumberFormatException e) {
                logger.warn("无法解析端口号: " + s[1]);
                isForward = false;
                return;
            }
        } else {
            logger.warn("无法解析端口字段: " + s[1]);
            isForward = false;
            return;
        }

        // 解析本地抽象地址: "localabstract:name"
        String[] localabstractStr = s[2].split(":");
        if (localabstractStr.length >= 2) {
            localabstract = localabstractStr[1].trim();
        } else {
            logger.warn("无法解析localabstract字段: " + s[2]);
            isForward = false;
        }
    }

    public int getPort() {
        return port;
    }

    public String getLocalabstract() {
        return localabstract;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public boolean isForward() {
        return isForward;
    }

    @Override
    public String toString() {
        return String.format("AdbForward{sn=%s, port=%d, abstract=%s, valid=%b}",
                serialNumber, port, localabstract, isForward);
    }
}
