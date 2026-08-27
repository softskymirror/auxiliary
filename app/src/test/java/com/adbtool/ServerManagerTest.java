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
import com.system.ConfigUtils;
import org.junit.jupiter.api.*;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ServerManager 的 JUnit 测试。
 * <p>
 * 测试覆盖范围：
 * <ul>
 *   <li>Web Server 启动与停止生命周期</li>
 *   <li>三种启动模式（startAdbServer、startWebServer、startAll）</li>
 *   <li>端口冲突保护（重复启动自动释放旧端口）</li>
 *   <li>stop() 统一关闭 Web Server + AdbServer</li>
 *   <li>restart() 原子重启</li>
 *   <li>isRunning / isAdbRunning / isWebServerRunning 状态查询</li>
 *   <li>多线程并发启动与停止的线程安全性</li>
 * </ul>
 * <p>
 * 端口使用 testPort（由 ConfigUtils.ConfigLoader 从 global.json 读取，默认 6655）。
 * ADB 相关测试通过 {@code assumeTrue} 自动跳过不可用环境。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerManagerTest {

    /** 测试端口，从 ConfigUtils 获取 testPort */
    private static int TEST_PORT;

    /** ADB 环境是否可用（@BeforeAll 中检测） */
    private static boolean adbAvailable = false;

    private ServerManager manager;

    // ==================== 全局初始化 ====================

    @BeforeAll
    static void initEnvironment() {
        // 读取 testPort，默认 6655
        try {
            ConfigUtils.ConfigLoader config = new ConfigUtils.ConfigLoader();
            TEST_PORT = config.getTestPort();
        } catch (Exception e) {
            TEST_PORT = 6655;
        }
        // 检测 ADB 环境是否可用：不仅检查单例创建，还要验证 listenUSB+listenADB 能正常执行
        try {
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            Thread.sleep(500);
            adbAvailable = AdbServer.server().isRunning();
            if (!adbAvailable) {
                System.err.println("ADB 同步线程未能启动，ADB 相关测试将自动跳过");
                AdbServer.server().shutdown();
            }
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("ADB 环境不可用，ADB 相关测试将自动跳过: " + e.getMessage());
            try { AdbServer.server().shutdown(); } catch (Exception ignored) {}
            adbAvailable = false;
        }
    }

    @BeforeEach
    void setUp() {
        manager = new ServerManager();
    }

    @AfterEach
    void tearDown() {
        safeStopManager();
        waitForPortRelease();
    }

    // ==================== 辅助方法 ====================

    /** 安全停止 manager，忽略 ADB 环境异常 */
    private void safeStopManager() {
        try {
            if (manager != null) {
                manager.stop();
            }
        } catch (Exception e) {
            // ADB 环境不可用时忽略
        }
    }

    /** 等待端口释放（最多 5 秒） */
    private void waitForPortRelease() {
        for (int i = 0; i < 50; i++) {
            if (isPortAvailable(TEST_PORT)) return;
            try { Thread.sleep(100); } catch (InterruptedException ignored) { break; }
        }
    }

    /** 检测端口是否可用 */
    private boolean isPortAvailable(int port) {
        try (Socket s = new Socket("localhost", port)) {
            return false; // 能连接说明端口仍被占用
        } catch (Exception e) {
            return true; // 连接失败说明端口已释放
        }
    }

    // ==================== Web Server 启动与停止 ====================

    @Nested
    @DisplayName("Web Server 启动与停止")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WebServerLifecycleTest {

        @Test
        @Order(1)
        @DisplayName("startWebServer 启动后 isWebServerRunning 返回 true")
        void testStartWebServer() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertAll("验证启动状态",
                    () -> assertNotNull(server, "返回的 server 实例不应为 null"),
                    () -> assertTrue(manager.isWebServerRunning(), "isWebServerRunning 应返回 true"),
                    () -> assertTrue(manager.isRunning(), "isRunning 应返回 true"),
                    () -> assertSame(server, manager.getWebServer(), "getWebServer 应返回同一实例"));
        }

        @Test
        @Order(2)
        @DisplayName("stop 后 isWebServerRunning 返回 false")
        void testStopWebServer() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "启动后应为运行状态");

            manager.stop();
            assertAll("验证停止后状态",
                    () -> assertFalse(manager.isWebServerRunning(), "isWebServerRunning 应返回 false"),
                    () -> assertNull(manager.getWebServer(), "getWebServer 应返回 null"));
        }

        @Test
        @Order(3)
        @DisplayName("重复启动自动关闭旧实例，避免端口冲突")
        void testStartWebServerTwice_noPortConflict() throws Exception {
            AndroidControlServer first = manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "第一次启动应成功");

            // 第二次启动同一端口，应自动关闭旧实例
            AndroidControlServer second = manager.startWebServer(TEST_PORT);
            assertAll("验证端口冲突保护",
                    () -> assertTrue(manager.isWebServerRunning(), "第二次启动后仍应运行"),
                    () -> assertNotSame(first, second, "应返回新的 server 实例"),
                    () -> assertSame(second, manager.getWebServer(), "getWebServer 应返回新实例"));
        }

        @Test
        @Order(4)
        @DisplayName("启动后返回的 server 实例 isRunning 为 true")
        void testReturnedServerIsRunning() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertTrue(server.isRunning(), "返回的 server.isRunning() 应返回 true");

            manager.stop();
            // 等待 Netty channel 关闭
            Thread.sleep(300);
            assertFalse(server.isRunning(), "停止后 server.isRunning() 应返回 false");
        }
    }

    // ==================== 状态查询 ====================

    @Nested
    @DisplayName("状态查询方法")
    class StatusQueryTest {

        @Test
        @DisplayName("初始状态：所有 isRunning 方法返回 false")
        void testInitialState_allFalse() {
            assertAll("验证初始状态",
                    () -> assertFalse(manager.isRunning(), "isRunning 应为 false"),
                    () -> assertFalse(manager.isAdbRunning(), "isAdbRunning 应为 false"),
                    () -> assertFalse(manager.isWebServerRunning(), "isWebServerRunning 应为 false"),
                    () -> assertNull(manager.getWebServer(), "getWebServer 应为 null"));
        }

        @Test
        @DisplayName("仅启动 WebServer 后，isWebServerRunning=true 但 isAdbRunning=false")
        void testWebServerOnlyStatus() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertAll("验证仅 WebServer 运行状态",
                    () -> assertTrue(manager.isRunning(), "isRunning 应为 true"),
                    () -> assertFalse(manager.isAdbRunning(), "isAdbRunning 应为 false"),
                    () -> assertTrue(manager.isWebServerRunning(), "isWebServerRunning 应为 true"));
        }

        @Test
        @DisplayName("stop 后所有状态恢复为 false")
        void testStopResetsAllStatus() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isRunning(), "启动后应为运行状态");

            manager.stop();
            assertAll("验证停止后状态",
                    () -> assertFalse(manager.isRunning(), "isRunning 应为 false"),
                    () -> assertFalse(manager.isWebServerRunning(), "isWebServerRunning 应为 false"),
                    () -> assertFalse(manager.isAdbRunning(), "isAdbRunning 应为 false"));
        }

        @Test
        @DisplayName("多次调用 stop 不抛异常（幂等性）")
        void testMultipleStops_noException() {
            assertDoesNotThrow(() -> {
                manager.stop();
                manager.stop();
                manager.stop();
            }, "多次 stop 不应抛异常");
        }
    }

    // ==================== 服务停止 ====================

    @Nested
    @DisplayName("服务停止与资源释放")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StopServiceTest {

        @Test
        @Order(1)
        @DisplayName("stop 关闭 WebServer 并释放端口")
        void testStopReleasesPort() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "启动后应运行中");

            manager.stop();
            assertFalse(manager.isWebServerRunning(), "停止后应不再运行");
            assertNull(manager.getWebServer(), "webServer 引用应被清空");
        }

        @Test
        @Order(2)
        @DisplayName("stop 后端口释放，可重新启动新服务")
        void testStartAfterStop_reusePort() throws Exception {
            manager.startWebServer(TEST_PORT);
            manager.stop();
            waitForPortRelease();

            // 端口应已释放，可以重新启动
            AndroidControlServer newServer = manager.startWebServer(TEST_PORT);
            assertAll("验证重新启动",
                    () -> assertNotNull(newServer, "重新启动应返回非 null"),
                    () -> assertTrue(manager.isWebServerRunning(), "重新启动后应运行中"));
        }

        @Test
        @Order(3)
        @DisplayName("stop 同时关闭 AdbServer（ADB 可用时）")
        void testStopAlsoClosesAdbServer() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();
            // 等待同步线程启动
            Thread.sleep(500);

            if (manager.isAdbRunning()) {
                manager.stop();
                assertFalse(manager.isAdbRunning(),
                        "stop 后 isAdbRunning 应返回 false（AdbServer 已关闭）");
            }
        }
    }

    // ==================== 重启服务 ====================

    @Nested
    @DisplayName("服务重启")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RestartTest {

        @Test
        @Order(1)
        @DisplayName("restart 在同一端口原子重启，无端口冲突")
        void testRestart_samePort() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "首次启动应成功");

            AndroidControlServer restarted = manager.restart(TEST_PORT);
            assertAll("验证重启后状态",
                    () -> assertNotNull(restarted, "restart 应返回非 null"),
                    () -> assertTrue(manager.isWebServerRunning(), "重启后应运行中"),
                    () -> assertSame(restarted, manager.getWebServer(), "getWebServer 应返回重启后的实例"));
        }

        @Test
        @Order(2)
        @DisplayName("restart 后旧 server 实例已关闭")
        void testRestart_oldServerStopped() throws Exception {
            AndroidControlServer old = manager.startWebServer(TEST_PORT);
            assertTrue(old.isRunning(), "旧实例应运行中");

            manager.restart(TEST_PORT);
            // 等待旧 channel 关闭
            Thread.sleep(300);
            assertFalse(old.isRunning(), "旧实例在重启后应已关闭");
        }

        @Test
        @Order(3)
        @DisplayName("未启动时直接 restart 也能正常启动")
        void testRestart_withoutPriorStart() throws Exception {
            AndroidControlServer server = manager.restart(TEST_PORT);
            assertAll("验证直接 restart",
                    () -> assertNotNull(server, "restart 应返回非 null"),
                    () -> assertTrue(manager.isWebServerRunning(), "restart 后应运行中"));
        }
    }

    // ==================== ADB 服务启动 ====================

    @Nested
    @DisplayName("ADB 服务启动（需要 ADB 环境）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AdbServerTest {

        @Test
        @Order(1)
        @DisplayName("startAdbServer 启动后 isAdbRunning 返回 true")
        void testStartAdbServer() {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();
            // 等待同步线程启动
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            assertTrue(manager.isAdbRunning(), "isAdbRunning 应返回 true");
            assertTrue(manager.isRunning(), "isRunning 应返回 true");
        }

        @Test
        @Order(2)
        @DisplayName("重复调用 startAdbServer 不抛异常（自动中断旧线程）")
        void testStartAdbServerTwice_noException() {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}

            assertDoesNotThrow(() -> manager.startAdbServer(),
                    "重复调用 startAdbServer 不应抛异常");
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            assertTrue(manager.isAdbRunning(), "第二次启动后 ADB 应仍运行");
        }
    }

    // ==================== 组合启动（ADB + WebServer） ====================

    @Nested
    @DisplayName("组合启动 ADB + WebServer")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StartAllTest {

        @Test
        @Order(1)
        @DisplayName("startAll 同时启动 ADB 和 WebServer")
        void testStartAll() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            AndroidControlServer server = manager.startAll(TEST_PORT);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            assertAll("验证组合启动状态",
                    () -> assertNotNull(server, "返回的 server 不应为 null"),
                    () -> assertTrue(manager.isWebServerRunning(), "WebServer 应运行"),
                    () -> assertTrue(manager.isAdbRunning(), "ADB 应运行"),
                    () -> assertTrue(manager.isRunning(), "isRunning 应为 true"));
        }

        @Test
        @Order(2)
        @DisplayName("startAll 重复调用自动关闭旧服务，无端口冲突")
        void testStartAllTwice_noPortConflict() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            AndroidControlServer first = manager.startAll(TEST_PORT);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            AndroidControlServer second = manager.startAll(TEST_PORT);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            assertAll("验证重复 startAll",
                    () -> assertNotSame(first, second, "应返回新的 server 实例"),
                    () -> assertTrue(manager.isWebServerRunning(), "WebServer 应运行"),
                    () -> assertTrue(manager.isRunning(), "isRunning 应为 true"));
        }

        @Test
        @Order(3)
        @DisplayName("startAll 后 stop 关闭所有服务")
        void testStartAllThenStop() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAll(TEST_PORT);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            assertTrue(manager.isRunning(), "启动后应运行中");

            manager.stop();
            assertAll("验证 stop 后状态",
                    () -> assertFalse(manager.isRunning(), "isRunning 应为 false"),
                    () -> assertFalse(manager.isWebServerRunning(), "WebServer 应已停止"),
                    () -> assertFalse(manager.isAdbRunning(), "ADB 应已停止"));
        }
    }

    // ==================== 多线程测试 ====================

    @Nested
    @DisplayName("多线程并发测试")
    class MultiThreadTest {

        @Test
        @DisplayName("多线程并发启动 WebServer，最终只有一个实例运行")
        void testConcurrentStartWebServer() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicReference<Exception> errorRef = new AtomicReference<>(null);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 所有线程同时开始
                        AndroidControlServer server = manager.startWebServer(TEST_PORT);
                        if (server != null && server.isRunning()) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorRef.compareAndSet(null, e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 释放所有线程
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "所有线程应在 30 秒内完成");
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // 不应有异常（ReentrantLock 保证线程安全）
            assertNull(errorRef.get(),
                    "并发启动不应抛异常: " + (errorRef.get() != null ? errorRef.get().getMessage() : ""));
            // 最终应只有一个 server 在运行
            assertTrue(manager.isWebServerRunning(), "最终应有一个 WebServer 在运行");
            // 所有成功的线程都拿到了 running 的实例
            assertTrue(successCount.get() > 0, "至少有一个线程成功启动了 server");
        }

        @Test
        @DisplayName("多线程并发 stop 不抛异常")
        void testConcurrentStop() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "启动后应运行中");

            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        manager.stop();
                    } catch (Throwable t) {
                        errorRef.compareAndSet(null, t);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "所有线程应在 15 秒内完成");
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            assertNull(errorRef.get(),
                    "并发 stop 不应抛异常: " + (errorRef.get() != null ? errorRef.get().getMessage() : ""));
            assertFalse(manager.isWebServerRunning(), "并发停止后 WebServer 应不在运行");
        }

        @Test
        @DisplayName("多线程并发 restart 最终只有一个实例运行")
        void testConcurrentRestart() throws Exception {
            manager.startWebServer(TEST_PORT);

            int threadCount = 3;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        manager.restart(TEST_PORT);
                    } catch (Throwable t) {
                        errorRef.compareAndSet(null, t);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "所有线程应在 30 秒内完成");
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            assertNull(errorRef.get(),
                    "并发 restart 不应抛异常: " + (errorRef.get() != null ? errorRef.get().getMessage() : ""));
            assertTrue(manager.isWebServerRunning(), "最终应有一个 WebServer 在运行");
        }

        @Test
        @DisplayName("一个线程启动 WebServer，另一个线程查询状态，不抛异常")
        void testConcurrentStartAndStatusQuery() throws Exception {
            int iterations = 10;
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

            // 线程 1：反复启动和停止
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations; i++) {
                        manager.startWebServer(TEST_PORT);
                        Thread.sleep(50);
                        manager.stop();
                        Thread.sleep(50);
                    }
                } catch (Throwable t) {
                    errorRef.compareAndSet(null, t);
                } finally {
                    doneLatch.countDown();
                }
            });

            // 线程 2：反复查询状态
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterations * 5; i++) {
                        // 状态查询不应抛异常，无论当前处于什么状态
                        manager.isRunning();
                        manager.isWebServerRunning();
                        manager.isAdbRunning();
                        manager.getWebServer();
                        Thread.sleep(20);
                    }
                } catch (Throwable t) {
                    errorRef.compareAndSet(null, t);
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "所有线程应在 30 秒内完成");
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            assertNull(errorRef.get(),
                    "并发启动与状态查询不应抛异常: " + (errorRef.get() != null ? errorRef.get().getMessage() : ""));
        }
    }
}
