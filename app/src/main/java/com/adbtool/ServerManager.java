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

package com.adbtool;

import com.adbtool.adb.AdbServer;
import com.adbtool.server.AndroidControlServer;
import org.apache.log4j.Logger;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ServerManager - 服务生命周期管理器。
 * <p>
 * 提供三种启动模式：
 * <ul>
 *   <li>{@link #startAdbServer()} - 仅启动ADB设备监控</li>
 *   <li>{@link #startWebServer(int)} - 仅启动Web Server（非阻塞）</li>
 *   <li>{@link #startAll(int)} - 先启动ADB，再启动Web Server（标准组合模式）</li>
 * </ul>
 * <p>
 * 支持 {@link #stop()} 统一关闭所有服务（Web Server + AdbServer），
 * 避免重启时端口冲突和资源泄漏。
 * <p>
 * 线程安全：所有启动/停止操作通过 {@link ReentrantLock} 保护。
 * <p>
 * 端口由调用方通过 {@code ConfigUtils.ConfigLoader} 显式传入：
 * <pre>
 *   ConfigUtils.ConfigLoader config = new ConfigUtils.ConfigLoader();
 *   int port = config.getServerPort();  // 生产环境
 *   int port = config.getTestPort();    // 测试环境
 * </pre>
 * <p>
 * 用法示例：
 * <pre>
 *   ServerManager manager = new ServerManager();
 *
 *   // 1. 仅启动ADB
 *   manager.startAdbServer();
 *
 *   // 2. 仅启动Web Server（非阻塞）
 *   manager.startWebServer(8080);
 *
 *   // 3. 启动ADB + Web Server
 *   manager.startAll(8080);
 *
 *   // 查询状态
 *   manager.isRunning();        // 任一服务在运行即为true
 *   manager.isAdbRunning();     // ADB是否在运行
 *   manager.isWebServerRunning(); // Web Server是否在运行
 *
 *   // 关闭所有服务（避免端口冲突）
 *   manager.stop();
 *
 *   // 重启（先stop再startAll）
 *   manager.restart(8080);
 * </pre>
 */
public class ServerManager {

    private static final Logger logger = Logger.getLogger(ServerManager.class);

    /** 当前持有的Web Server实例 */
    private volatile AndroidControlServer webServer;

    /** ADB服务是否正在运行 */
    private volatile boolean adbRunning = false;

    /** 线程安全锁，保护启动/停止操作 */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 启动ADB设备监控（USB + ADB监听）。
     * <p>
     * 若ADB已在运行，会先中断旧线程再重新启动，避免重复线程。
     */
    public void startAdbServer() {
        lock.lock();
        try {
            logger.info("Starting ADB device monitoring...");
            // 如果ADB已在运行，先关闭旧的
            if (AdbServer.server().isRunning()) {
                logger.info("ADB already running, shutting down old instance first...");
                AdbServer.server().shutdown();
            }
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            adbRunning = true;
            logger.info("ADB device monitoring started.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 启动Web Server（非阻塞模式）。
     * <p>
     * 若Web Server已在运行，会先关闭旧实例释放端口，避免端口冲突。
     *
     * @param port 绑定端口
     * @return 启动后的 AndroidControlServer 实例
     * @throws Exception 启动失败时抛出
     */
    public AndroidControlServer startWebServer(int port) throws Exception {
        lock.lock();
        try {
            // 如果Web Server已在运行，先关闭释放端口
            if (webServer != null && webServer.isRunning()) {
                logger.info("Web Server already running on old port, stopping it first...");
                webServer.stop();
                webServer = null;
            }
            logger.info("Starting Web Server on port " + port + " (non-blocking)...");
            webServer = new AndroidControlServer();
            webServer.start(port);
            logger.info("Web Server started on port " + port);
            return webServer;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 先启动ADB设备监控，再启动Web Server（标准组合模式）。
     *
     * @param port 绑定端口
     * @return 启动后的 AndroidControlServer 实例
     * @throws Exception 启动失败时抛出
     */
    public AndroidControlServer startAll(int port) throws Exception {
        lock.lock();
        try {
            logger.info("Starting ADB + Web Server on port " + port + "...");
            // 先关闭旧服务，避免端口冲突
            stopInternal();
            // 启动ADB
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            adbRunning = true;
            // 启动Web Server
            webServer = new AndroidControlServer();
            webServer.start(port);
            logger.info("ADB + Web Server started on port " + port);
            return webServer;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭所有服务（Web Server + AdbServer），释放端口和线程资源。
     * <p>
     * 重启前调用此方法可避免端口冲突。
     */
    public void stop() {
        lock.lock();
        try {
            logger.info("Stopping all services...");
            stopInternal();
            logger.info("All services stopped.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 内部关闭逻辑（不获取锁，供已持锁的方法调用）
     */
    private void stopInternal() {
        // 关闭Web Server
        if (webServer != null) {
            if (webServer.isRunning()) {
                webServer.stop();
                logger.info("Web Server stopped.");
            }
            webServer = null;
        }
        // 关闭AdbServer（中断同步线程 + 释放ADB资源）
        if (AdbServer.server().isRunning()) {
            AdbServer.server().shutdown();
            logger.info("AdbServer stopped.");
        }
        adbRunning = false;
    }

    /**
     * 重启所有服务：先关闭旧服务，再重新启动。
     * <p>
     * 等价于 {@code stop()} + {@code startAll(port)}，但原子执行，线程安全。
     *
     * @param port 绑定端口
     * @return 重启后的 AndroidControlServer 实例
     * @throws Exception 启动失败时抛出
     */
    public AndroidControlServer restart(int port) throws Exception {
        lock.lock();
        try {
            logger.info("Restarting all services on port " + port + "...");
            stopInternal();
            // 启动ADB
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            adbRunning = true;
            // 启动Web Server
            webServer = new AndroidControlServer();
            webServer.start(port);
            logger.info("All services restarted on port " + port);
            return webServer;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查询是否有任何服务正在运行（ADB 或 Web Server）。
     *
     * @return true 如果ADB或Web Server任一在运行
     */
    public boolean isRunning() {
        return isAdbRunning() || isWebServerRunning();
    }

    /**
     * 查询ADB设备监控是否正在运行。
     *
     * @return true 如果ADB同步线程正在运行
     */
    public boolean isAdbRunning() {
        return adbRunning && AdbServer.server().isRunning();
    }

    /**
     * 查询Web Server是否正在运行。
     *
     * @return true 如果Web Server通道处于活跃状态
     */
    public boolean isWebServerRunning() {
        return webServer != null && webServer.isRunning();
    }

    /**
     * 获取当前Web Server实例，便于外部操作。
     *
     * @return 当前运行的 AndroidControlServer 实例，未启动时返回 null
     */
    public AndroidControlServer getWebServer() {
        return webServer;
    }
}
