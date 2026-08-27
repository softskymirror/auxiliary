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

import com.system.ConfigUtils;

import java.io.*;

/**
 * Created by harry on 2017/4/17.
 * <p>
 * 薄代理层：所有路径配置统一委托给 {@link ConfigUtils.ConfigLoader}，
 * 配置文件由 global.json 集中管理（原 adb-devices.properties 已废弃）。
 * <p>
 * 保留原有 API 签名，10 个调用方无需改动。
 */
public class Constant {

    public static final String PROP_ABI = "ro.product.cpu.abi";
    public static final String PROP_SDK = "ro.build.version.sdk";

    /**
     * 内部默认配置加载器（懒加载，避免类加载时失败）。
     * 使用双重检查锁保证线程安全。
     */
    private static volatile ConfigUtils.ConfigLoader defaultConfig;

    private static ConfigUtils.ConfigLoader getConfig() {
        if (defaultConfig == null) {
            synchronized (Constant.class) {
                if (defaultConfig == null) {
                    defaultConfig = new ConfigUtils.ConfigLoader();
                }
            }
        }
        return defaultConfig;
    }

    /**
     * 读取资源目录（minicap/minitouch/web 等静态资源）。
     *
     * @return resource.root 对应的 File
     */
    public static File getResourceDir() {
        return getConfig().getResourceDir();
    }

    /**
     * 读取项目目录。
     *
     * @return project.root 对应的 File
     */
    public static File getProjectDir() {
        return getConfig().getProjectDir();
    }

    /**
     * 读取数据缓存根目录。
     *
     * @return data.root 对应的 File
     */
    public static File getDataDir() {
        return getConfig().getDataDir();
    }

    /**
     * 读取数据缓存指定文件。
     *
     * @param name 缓存文件名
     * @return {data.root}/{name} 对应的 File
     */
    public static File getDataCache(String name) {
        return getConfig().getDataCache(name);
    }

    /**
     * 读取资源目录指定文件。
     *
     * @param name 资源文件名
     * @return {resource.root}/{name} 对应的 File
     */
    public static File getResourceFile(String name) {
        return getConfig().getResourceFile(name);
    }

    /**
     * 获取 minicap 可执行文件路径。
     *
     * @param abi 设备 CPU 架构
     * @return minicap 二进制文件 File，资源目录不存在时返回 null
     */
    public static File getMinicap(String abi) {
        File resources = getResourceDir();
        if (resources.exists()) {
            return new File(resources, "minicap" + File.separator + "bin" +
                    File.separator + abi + File.separator + "minicap");
        }
        return null;
    }

    /**
     * 获取 minicap 共享库（.so）路径。
     *
     * @param abi 设备 CPU 架构
     * @param sdk 设备 Android SDK 版本
     * @return minicap.so 文件 File，资源目录不存在时返回 null
     */
    public static File getMinicapSo(String abi, String sdk) {
        File resources = getResourceDir();
        if (resources.exists()) {
            return new File(resources, "minicap" + File.separator + "shared" +
                    File.separator + "android-" + sdk + File.separator + abi + File.separator + "minicap.so");
        }
        return null;
    }

    /**
     * 获取 minitouch 可执行文件路径。
     *
     * @param abi 设备 CPU 架构
     * @return minitouch 二进制文件 File，资源目录不存在时返回 null
     */
    public static File getMinitouchBin(String abi) {
        File resources = getResourceDir();
        if (resources.exists()) {
            return new File(resources, "minitouch" + File.separator +
                    File.separator + abi + File.separator + "minitouch");
        }
        return null;
    }

    /**
     * 获取临时文件（使用系统临时目录）。
     *
     * @param fileName 临时文件名
     * @return {java.io.tmpdir}/AndroidControl/{fileName}
     */
    public static File getTmpFile(String fileName) {
        return getConfig().getTmpFile(fileName);
    }

}
