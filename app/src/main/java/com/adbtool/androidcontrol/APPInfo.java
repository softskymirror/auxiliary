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

package com.adbtool.androidcontrol;

import com.alibaba.fastjson.annotation.JSONField;
import com.yeetor.adb.AdbDevice;

/**
 * 获取每一台设备指定APP的安装信息
 */
public class APPInfo {

        private String name,version,p_name;
        boolean is_install;

        public APPInfo(String name,String version,String p_name,boolean is_install){
            this.name=name;
            this.version=version;
            this.p_name=p_name;
            this.is_install=is_install;
        }

        public APPInfo(AdbDevice device) {

        }

        @JSONField(name = "name")
        public String getAppName() {
            return name;
        }

        @JSONField(name = "version")
        public String getAppVersion() {
            return version;
        }

        @JSONField(name = "p_name")
        public String getPackageName() {
            return p_name;
        }

        @JSONField(name = "install")
        public boolean isInstall() {
            return is_install;
        }
    }


