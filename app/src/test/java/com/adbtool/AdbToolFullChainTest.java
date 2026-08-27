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

import com.adbtool.adb.AdbDevice;
import com.adbtool.adb.AdbServer;
import com.adbtool.adb.AdbServer.ChangeType;
import com.adbtool.adb.AdbServer.ConnectType;
import com.adbtool.adb.AdbServer.DeviceChangeEvent;
import com.adbtool.adb.IAdbServerListener;
import com.adbtool.protocol.TextProtocol;
import com.adbtool.server.AndroidControlServer;
import com.adbtool.util.AdbUtils;
import com.adbtool.util.Constant;
import com.android.ddmlib.IDevice;
import com.system.ConfigUtils;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * adbtool 全链路测试。
 * <p>
 * 模拟完整业务生命周期：
 * <ol>
 *   <li>Phase 1 - 启动 ADB 设备监控</li>
 *   <li>Phase 2 - 启动 WEB 服务（Netty Server）</li>
 *   <li>Phase 3 - 设备展示与控制管理（协议构建→解析→设备列表→控制指令）</li>
 *   <li>Phase 4 - 日志输出及保存（事件日志→CSV 缓存→文件持久化）</li>
 *   <li>Phase 5 - 关闭服务退出 WEB（stop→资源释放→端口回收→状态归零）</li>
 * </ol>
 * <p>
 * 每个 Phase 对应一个 @Nested 测试类，按顺序执行。
 * 同时提供一个端到端全链路测试方法，串联所有 Phase。
 * <p>
 * 端口使用 testPort（默认 6655），ADB 不可用时自动跳过。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdbToolFullChainTest {

    /** 测试端口 */
    private static int TEST_PORT;
    /** ADB 环境是否可用 */
    private static boolean adbAvailable = false;

    private ServerManager manager;

    // ==================== 全局初始化 ====================

    @BeforeAll
    static void initEnvironment() throws Exception {
        // 初始化 Constant 配置
        ConfigUtils.ConfigLoader config = new ConfigUtils.ConfigLoader();
        Field f = Constant.class.getDeclaredField("defaultConfig");
        f.setAccessible(true);
        f.set(null, config);

        // 读取测试端口
        try {
            TEST_PORT = config.getTestPort();
        } catch (Exception e) {
            TEST_PORT = 6655;
        }

        // 检测 ADB 环境
        try {
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            Thread.sleep(500);
            adbAvailable = AdbServer.server().isRunning();
            if (!adbAvailable) {
                System.err.println("ADB 同步线程未能启动，全链路测试将自动跳过");
                AdbServer.server().shutdown();
            }
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("ADB 环境不可用，全链路测试将自动跳过: " + e.getMessage());
            try { AdbServer.server().shutdown(); } catch (Exception ignored) {}
            adbAvailable = false;
        }
    }

    @AfterAll
    static void resetEnvironment() throws Exception {
        Field f = Constant.class.getDeclaredField("defaultConfig");
        f.setAccessible(true);
        f.set(null, null);
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

    private void safeStopManager() {
        try {
            if (manager != null) manager.stop();
        } catch (Exception ignored) {}
    }

    private void waitForPortRelease() {
        for (int i = 0; i < 50; i++) {
            if (isPortAvailable(TEST_PORT)) return;
            try { Thread.sleep(100); } catch (InterruptedException ignored) { break; }
        }
    }

    private boolean isPortAvailable(int port) {
        try (Socket s = new Socket("localhost", port)) {
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private IDevice createMockIDevice(String serialNumber) {
        IDevice device = mock(IDevice.class);
        when(device.getSerialNumber()).thenReturn(serialNumber);
        when(device.getProperty("ro.build.version.sdk")).thenReturn("30");
        when(device.getProperty("ro.product.cpu.abi")).thenReturn("arm64-v8a");
        when(device.getProperty("ro.product.model")).thenReturn("FullChainDevice");
        return device;
    }

    private AdbServer resetAndCreateAdbServer() throws Exception {
        Field serverField = AdbServer.class.getDeclaredField("server");
        serverField.setAccessible(true);
        AdbServer old = (AdbServer) serverField.get(null);
        if (old != null) {
            try { old.shutdown(); } catch (Exception ignored) {}
        }
        serverField.set(null, null);
        Constructor<AdbServer> ctor = AdbServer.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @SuppressWarnings("unchecked")
    private void setDeviceList(AdbServer srv, List<AdbDevice> list) throws Exception {
        Field f = AdbServer.class.getDeclaredField("adbDeviceList");
        f.setAccessible(true);
        f.set(srv, list);
    }

    private void setDeviceCacheFile(AdbServer srv, File file) throws Exception {
        Field f = AdbServer.class.getDeclaredField("deviceCacheFile");
        f.setAccessible(true);
        f.set(srv, file);
    }

    // ==================== Phase 1: 启动 ADB 设备监控 ====================

    @Nested
    @DisplayName("Phase 1: 启动 ADB 设备监控")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Phase1_StartADBTest {

        @Test
        @Order(1)
        @DisplayName("Phase1: ADB 启动后 isAdbRunning 为 true")
        void testADBStartSuccess() {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            assertTrue(manager.isAdbRunning(), "ADB 应处于运行状态");
            assertTrue(manager.isRunning(), "isRunning 应为 true");
        }

        @Test
        @Order(2)
        @DisplayName("Phase1: ADB 启动后设备列表初始化为空或已有设备")
        void testADBDeviceListInitialized() {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            List<AdbDevice> devices = AdbServer.server().getDevices();
            assertNotNull(devices, "设备列表不应为 null");
            // 设备数量取决于实际连接的设备，可能为 0
        }

        @Test
        @Order(3)
        @DisplayName("Phase1: ADB 启动后 Listener 可注册")
        void testADBListenerRegistrable() {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();

            IAdbServerListener listener = mock(IAdbServerListener.class);
            assertDoesNotThrow(() -> AdbServer.server().addListener(listener),
                    "ADB 启动后应可注册 Listener");
        }
    }

    // ==================== Phase 2: 启动 WEB 服务 ====================

    @Nested
    @DisplayName("Phase 2: 启动 WEB 服务")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Phase2_StartWEBTest {

        @Test
        @Order(1)
        @DisplayName("Phase2: WEB 启动后端口可连接")
        void testWEBStartPortConnectable() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertNotNull(server, "WEB 服务应启动成功");
            assertTrue(manager.isWebServerRunning(), "isWebServerRunning 应为 true");

            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected(), "TCP 连接应成功");
            }
        }

        @Test
        @Order(2)
        @DisplayName("Phase2: WEB 启动后 server.isRunning 为 true")
        void testWEBServerIsRunning() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertTrue(server.isRunning(), "server.isRunning() 应为 true");
        }

        @Test
        @Order(3)
        @DisplayName("Phase2: ADB + WEB 组合启动后两者均运行")
        void testStartAllBothRunning() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            AndroidControlServer server = manager.startAll(TEST_PORT);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            assertAll("验证组合启动",
                    () -> assertNotNull(server),
                    () -> assertTrue(manager.isAdbRunning(), "ADB 应运行"),
                    () -> assertTrue(manager.isWebServerRunning(), "WEB 应运行"),
                    () -> assertTrue(manager.isRunning(), "isRunning 应为 true"));

            // TCP 端口可连接
            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected());
            }
        }
    }

    // ==================== Phase 3: 设备展示与控制管理 ====================

    @Nested
    @DisplayName("Phase 3: 设备展示与控制管理")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Phase3_DeviceDisplayAndControlTest {

        @Test
        @Order(1)
        @DisplayName("Phase3: M_DEVICES 协议链路 - 请求设备列表")
        void testMDevicesProtocolChain() {
            // 模拟客户端发送 M_DEVICES 请求
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_DEVICES, "");
            String wireFormat = String.format("%s://%s",
                    request.getProtocolHeader(), request.getProtocolBody());

            // 模拟服务端接收并解析
            TextProtocol parsed = TextProtocol.ParseWithString(wireFormat);
            assertEquals(TextProtocol.Header.M_DEVICES, parsed.getProtocolHeader());

            // 模拟服务端构建响应（空设备列表）
            String devicesJson = AdbUtils.devices2JSON(new ArrayList<>());
            TextProtocol response = TextProtocol.newProtocol(
                    TextProtocol.Header.SM_DEVICES, devicesJson);

            assertEquals("[]", response.getProtocolBody(), "空设备列表应返回 []");
        }

        @Test
        @Order(2)
        @DisplayName("Phase3: M_WAIT 协议链路 - 绑定设备")
        void testMWaitProtocolChain() {
            // 模拟客户端发送 M_WAIT 请求绑定设备
            String jsonBody = "{\"sn\":\"FULLCHAIN_SN\"}";
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_WAIT, jsonBody);
            String wireFormat = String.format("%s://%s",
                    request.getProtocolHeader(), request.getProtocolBody());

            // 服务端解析
            TextProtocol parsed = TextProtocol.ParseWithString(wireFormat);
            assertEquals(TextProtocol.Header.M_WAIT, parsed.getProtocolHeader());

            // 提取 sn
            com.alibaba.fastjson.JSONObject obj =
                    com.alibaba.fastjson.JSON.parseObject(parsed.getProtocolBody());
            assertEquals("FULLCHAIN_SN", obj.getString("sn"));

            // 模拟响应 SM_OPENED
            TextProtocol response = TextProtocol.newProtocol(TextProtocol.Header.SM_OPENED, "");
            String respWire = String.format("%s://%s",
                    response.getProtocolHeader(), response.getProtocolBody());
            assertEquals("SM_OPENED://", respWire);
        }

        @Test
        @Order(3)
        @DisplayName("Phase3: M_START 协议链路 - 启动截屏服务")
        void testMStartCapProtocolChain() {
            String jsonBody = "{\"type\":\"cap\",\"config\":{\"scale\":0.5,\"rotate\":0}}";
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_START, jsonBody);
            String wireFormat = String.format("%s://%s",
                    request.getProtocolHeader(), request.getProtocolBody());

            TextProtocol parsed = TextProtocol.ParseWithString(wireFormat);
            assertEquals(TextProtocol.Header.M_START, parsed.getProtocolHeader());

            com.alibaba.fastjson.JSONObject obj =
                    com.alibaba.fastjson.JSON.parseObject(parsed.getProtocolBody());
            assertEquals("cap", obj.getString("type"));
            assertEquals(0.5f, obj.getJSONObject("config").getFloat("scale"));
        }

        @Test
        @Order(4)
        @DisplayName("Phase3: M_TOUCH 协议链路 - 发送触摸事件")
        void testMTouchProtocolChain() {
            // 触摸数据格式: down/up/x/y/pressure
            String touchData = "d:0:540:960:50";
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_TOUCH, touchData);
            String wireFormat = String.format("%s://%s",
                    request.getProtocolHeader(), request.getProtocolBody());

            TextProtocol parsed = TextProtocol.ParseWithString(wireFormat);
            assertEquals(TextProtocol.Header.M_TOUCH, parsed.getProtocolHeader());
            assertEquals(touchData, parsed.getProtocolBody(), "触摸数据应完整传输");
        }

        @Test
        @Order(5)
        @DisplayName("Phase3: M_KEYEVENT 协议链路 - 发送按键事件")
        void testMKeyEventProtocolChain() {
            // 按键码 4 = KEYCODE_BACK
            String keyCode = "4";
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_KEYEVENT, keyCode);
            String wireFormat = String.format("%s://%s",
                    request.getProtocolHeader(), request.getProtocolBody());

            TextProtocol parsed = TextProtocol.ParseWithString(wireFormat);
            assertEquals(TextProtocol.Header.M_KEYEVENT, parsed.getProtocolHeader());
            assertEquals(4, Integer.parseInt(parsed.getProtocolBody()), "按键码应可解析");
        }

        @Test
        @Order(6)
        @DisplayName("Phase3: 设备列表变更后通过 Listener 通知客户端刷新")
        void testDeviceChangeNotification() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean notified = new AtomicBoolean(false);

            IAdbServerListener listener = new IAdbServerListener() {
                @Override
                public void onAdbDeviceConnected(AdbDevice device) {
                    notified.set(true);
                    latch.countDown();
                }
                @Override
                public void onAdbDeviceDisConnected(AdbDevice device) {}
            };

            AdbServer.server().addListener(listener);

            // 模拟设备连接通知
            AdbDevice mockDevice = new AdbDevice(createMockIDevice("CHAIN_NOTIFY_SN"));
            java.lang.reflect.Method notifyMethod = AdbServer.class.getDeclaredMethod(
                    "notifyListenersConnected", AdbDevice.class);
            notifyMethod.setAccessible(true);
            notifyMethod.invoke(AdbServer.server(), mockDevice);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "应收到设备连接通知");
            assertTrue(notified.get(), "通知标志应为 true");
        }

        @Test
        @Order(7)
        @DisplayName("Phase3: 设备列表 JSON 序列化→协议封装→解析完整链路")
        void testFullDeviceListProtocolChain() {
            // 创建设备列表
            List<AdbDevice> devices = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                devices.add(new AdbDevice(createMockIDevice("CHAIN_DEV_" + i)));
            }

            // 序列化为 JSON
            String json = AdbUtils.devices2JSON(devices);
            assertTrue(json.contains("CHAIN_DEV_0"));
            assertTrue(json.contains("CHAIN_DEV_2"));

            // 封装为协议
            TextProtocol response = TextProtocol.newProtocol(TextProtocol.Header.SM_DEVICES, json);
            String wire = String.format("%s://%s",
                    response.getProtocolHeader(), response.getProtocolBody());

            // 解析协议
            TextProtocol parsed = TextProtocol.ParseWithString(wire);
            assertEquals(TextProtocol.Header.SM_DEVICES, parsed.getProtocolHeader());

            // 反序列化 JSON
            com.alibaba.fastjson.JSONArray arr =
                    com.alibaba.fastjson.JSON.parseArray(parsed.getProtocolBody());
            assertEquals(3, arr.size(), "应有 3 个设备");
        }
    }

    // ==================== Phase 4: 日志输出及保存 ====================

    @Nested
    @DisplayName("Phase 4: 日志输出及保存")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Phase4_LoggingAndPersistenceTest {

        private AdbServer server;

        @BeforeEach
        void setUp() throws Exception {
            server = resetAndCreateAdbServer();
        }

        @AfterEach
        void tearDown() {
            try { if (server != null) server.shutdown(); } catch (Exception ignored) {}
            try {
                Field f = AdbServer.class.getDeclaredField("server");
                f.setAccessible(true);
                f.set(null, null);
            } catch (Exception ignored) {}
        }

        @Test
        @Order(1)
        @DisplayName("Phase4: 设备变更事件写入内存日志缓存")
        void testEventLogMemoryCache() throws Exception {
            java.lang.reflect.Method recordMethod = AdbServer.class.getDeclaredMethod(
                    "recordChangeEvent", String.class, ChangeType.class, ConnectType.class, String.class);
            recordMethod.setAccessible(true);

            for (int i = 0; i < 10; i++) {
                recordMethod.invoke(server, "LOG_SN_" + i,
                        i % 2 == 0 ? ChangeType.CONNECTED : ChangeType.DISCONNECTED,
                        ConnectType.USB, "log test " + i);
            }

            List<DeviceChangeEvent> log = server.getDeviceChangeLog();
            assertEquals(10, log.size(), "内存日志应有 10 条");

            // 验证事件顺序
            for (int i = 0; i < 10; i++) {
                assertEquals("LOG_SN_" + i, log.get(i).getSerialNumber());
            }
        }

        @Test
        @Order(2)
        @DisplayName("Phase4: 事件日志持久化到文件")
        void testEventLogFilePersistence() throws Exception {
            File tempDir = Files.createTempDirectory("chain_log").toFile();
            File cacheFile = new File(tempDir, "cache.csv");
            setDeviceCacheFile(server, cacheFile);

            java.lang.reflect.Method recordMethod = AdbServer.class.getDeclaredMethod(
                    "recordChangeEvent", String.class, ChangeType.class, ConnectType.class, String.class);
            recordMethod.setAccessible(true);

            // 写入事件
            recordMethod.invoke(server, "PERSIST_SN", ChangeType.CONNECTED, ConnectType.WIFI, "persist");

            // 验证文件存在
            File eventLogFile = new File(tempDir, "adb_device_events.log");
            assertTrue(eventLogFile.exists(), "事件日志文件应被创建");

            String content = new String(Files.readAllBytes(eventLogFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(content.contains("PERSIST_SN"), "日志应包含序列号");
            assertTrue(content.contains("CONNECTED"), "日志应包含变更类型");
            assertTrue(content.contains("WIFI"), "日志应包含连接类型");

            // 清理
            eventLogFile.delete();
            tempDir.delete();
        }

        @Test
        @Order(3)
        @DisplayName("Phase4: 设备列表 CSV 缓存保存与读取")
        void testDeviceCacheCSVPersistence() throws Exception {
            File tempFile = File.createTempFile("chain_cache", ".csv");
            tempFile.deleteOnExit();
            setDeviceCacheFile(server, tempFile);

            // 添加设备
            List<AdbDevice> devices = new CopyOnWriteArrayList<>();
            devices.add(new AdbDevice(createMockIDevice("CSV_CHAIN_SN")));
            setDeviceList(server, devices);

            // 保存缓存
            server.saveDeviceCache();

            // 读取验证
            List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
            assertTrue(lines.size() >= 2, "至少标题行 + 1 个设备行");
            assertEquals("#serialNumber,connectType,sdk,abi,model", lines.get(0));
            assertTrue(lines.get(1).contains("CSV_CHAIN_SN"));
            assertTrue(lines.get(1).contains("arm64-v8a"));
            assertTrue(lines.get(1).contains("FullChainDevice"));

            tempFile.delete();
        }

        @Test
        @Order(4)
        @DisplayName("Phase4: shutdown 前自动保存设备缓存")
        void testShutdownAutoSaveCache() throws Exception {
            File tempFile = File.createTempFile("chain_shutdown_cache", ".csv");
            tempFile.deleteOnExit();
            setDeviceCacheFile(server, tempFile);

            // 添加设备
            List<AdbDevice> devices = new CopyOnWriteArrayList<>();
            devices.add(new AdbDevice(createMockIDevice("AUTOSAVE_SN")));
            setDeviceList(server, devices);

            // shutdown 应自动保存缓存
            server.shutdown();

            List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
            assertTrue(lines.size() >= 2, "shutdown 后缓存文件应有内容");
            assertTrue(lines.get(1).contains("AUTOSAVE_SN"), "应包含自动保存的设备");

            tempFile.delete();
        }

        @Test
        @Order(5)
        @DisplayName("Phase4: 事件日志超过 MAX_EVENT_LOG_SIZE 自动截断")
        void testEventLogMaxSizeTruncation() throws Exception {
            java.lang.reflect.Method recordMethod = AdbServer.class.getDeclaredMethod(
                    "recordChangeEvent", String.class, ChangeType.class, ConnectType.class, String.class);
            recordMethod.setAccessible(true);

            // 写入超过 200 条事件（MAX_EVENT_LOG_SIZE = 200）
            for (int i = 0; i < 250; i++) {
                recordMethod.invoke(server, "TRUNC_SN_" + i, ChangeType.CONNECTED, ConnectType.USB, "trunc");
            }

            List<DeviceChangeEvent> log = server.getDeviceChangeLog();
            assertTrue(log.size() <= 200, "日志不应超过 200 条");
            // 应保留最新的 200 条
            assertEquals("TRUNC_SN_50", log.get(0).getSerialNumber(),
                    "应保留最新的 200 条（从第 50 条开始）");
        }
    }

    // ==================== Phase 5: 关闭服务退出 WEB ====================

    @Nested
    @DisplayName("Phase 5: 关闭服务退出 WEB")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Phase5_ShutdownAndExitTest {

        @Test
        @Order(1)
        @DisplayName("Phase5: stop 后 WEB 服务关闭，端口释放")
        void testWEBShutdownPortReleased() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "启动后应运行中");

            manager.stop();
            waitForPortRelease();

            assertFalse(manager.isWebServerRunning(), "stop 后 WEB 应已关闭");
            assertTrue(isPortAvailable(TEST_PORT), "端口应已释放");
        }

        @Test
        @Order(2)
        @DisplayName("Phase5: stop 后 ADB 服务关闭")
        void testADBShutdown() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAdbServer();
            Thread.sleep(500);
            assertTrue(manager.isAdbRunning(), "ADB 应运行中");

            manager.stop();
            assertFalse(manager.isAdbRunning(), "stop 后 ADB 应已关闭");
        }

        @Test
        @Order(3)
        @DisplayName("Phase5: stop 后所有状态归零")
        void testAllStatusResetToZero() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            manager.startAll(TEST_PORT);
            Thread.sleep(500);
            assertTrue(manager.isRunning(), "启动后应运行中");

            manager.stop();
            assertAll("验证所有状态归零",
                    () -> assertFalse(manager.isRunning(), "isRunning 应为 false"),
                    () -> assertFalse(manager.isAdbRunning(), "isAdbRunning 应为 false"),
                    () -> assertFalse(manager.isWebServerRunning(), "isWebServerRunning 应为 false"),
                    () -> assertNull(manager.getWebServer(), "getWebServer 应为 null"));
        }

        @Test
        @Order(4)
        @DisplayName("Phase5: 关闭后可重新启动（端口复用）")
        void testRestartAfterFullShutdown() throws Exception {
            manager.startWebServer(TEST_PORT);
            manager.stop();
            waitForPortRelease();

            // 重新启动
            AndroidControlServer newServer = manager.startWebServer(TEST_PORT);
            assertNotNull(newServer, "重新启动应成功");
            assertTrue(manager.isWebServerRunning(), "重新启动后应运行中");

            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected(), "重新启动后端口应可连接");
            }
        }

        @Test
        @Order(5)
        @DisplayName("Phase5: 多次 stop 不抛异常（幂等性）")
        void testMultipleStopsIdempotent() {
            assertDoesNotThrow(() -> {
                manager.stop();
                manager.stop();
                manager.stop();
            }, "多次 stop 不应抛异常");
        }
    }

    // ==================== 端到端全链路测试 ====================

    @Nested
    @DisplayName("端到端全链路测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EndToEndFullChainTest {

        @Test
        @Order(1)
        @DisplayName("全链路: ADB启动→WEB启动→设备展示→日志保存→关闭退出")
        void testFullLifecycleChain() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过全链路测试");

            // ===== Phase 1: 启动 ADB =====
            manager.startAdbServer();
            Thread.sleep(500);
            assertTrue(manager.isAdbRunning(), "Phase1: ADB 应启动成功");

            // ===== Phase 2: 启动 WEB =====
            AndroidControlServer webServer = manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "Phase2: WEB 应启动成功");
            assertTrue(webServer.isRunning(), "Phase2: server.isRunning 应为 true");

            // 验证 TCP 端口可连接
            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected(), "Phase2: TCP 连接应成功");
            }

            // ===== Phase 3: 设备展示与协议链路 =====
            // 3.1 请求设备列表
            TextProtocol devicesReq = TextProtocol.newProtocol(TextProtocol.Header.M_DEVICES, "");
            String devicesWire = String.format("%s://%s",
                    devicesReq.getProtocolHeader(), devicesReq.getProtocolBody());
            TextProtocol parsedDevicesReq = TextProtocol.ParseWithString(devicesWire);
            assertEquals(TextProtocol.Header.M_DEVICES, parsedDevicesReq.getProtocolHeader());

            // 3.2 获取当前设备列表并序列化
            String devicesJson = AdbUtils.devices2JSON(AdbServer.server().getDevices());
            assertNotNull(devicesJson, "设备列表 JSON 不应为 null");

            // 3.3 构建响应协议
            TextProtocol devicesResp = TextProtocol.newProtocol(
                    TextProtocol.Header.SM_DEVICES, devicesJson);
            String respWire = String.format("%s://%s",
                    devicesResp.getProtocolHeader(), devicesResp.getProtocolBody());
            TextProtocol parsedResp = TextProtocol.ParseWithString(respWire);
            assertEquals(TextProtocol.Header.SM_DEVICES, parsedResp.getProtocolHeader());

            // 3.4 模拟 M_WAIT 绑定设备（如果有设备）
            List<AdbDevice> currentDevices = AdbServer.server().getDevices();
            if (!currentDevices.isEmpty()) {
                String sn = currentDevices.get(0).getSerialNumber();
                String waitJson = "{\"sn\":\"" + sn + "\"}";
                TextProtocol waitReq = TextProtocol.newProtocol(TextProtocol.Header.M_WAIT, waitJson);
                TextProtocol parsedWait = TextProtocol.ParseWithString(
                        String.format("%s://%s", waitReq.getProtocolHeader(), waitReq.getProtocolBody()));
                assertEquals(TextProtocol.Header.M_WAIT, parsedWait.getProtocolHeader());
            }

            // ===== Phase 4: 日志验证 =====
            // 4.1 事件日志缓存
            List<DeviceChangeEvent> eventLog = AdbServer.server().getDeviceChangeLog();
            assertNotNull(eventLog, "事件日志不应为 null");

            // 4.2 设备缓存保存验证
            AdbServer.server().saveDeviceCache();
            // 不抛异常即为成功

            // ===== Phase 5: 关闭服务退出 =====
            manager.stop();
            waitForPortRelease();

            assertAll("Phase5: 验证全部服务已关闭",
                    () -> assertFalse(manager.isRunning(), "isRunning 应为 false"),
                    () -> assertFalse(manager.isAdbRunning(), "ADB 应已关闭"),
                    () -> assertFalse(manager.isWebServerRunning(), "WEB 应已关闭"),
                    () -> assertNull(manager.getWebServer(), "webServer 应为 null"));

            // 验证端口已释放
            assertTrue(isPortAvailable(TEST_PORT), "端口应已释放");
        }

        @Test
        @Order(2)
        @DisplayName("全链路: 仅 WEB 模式（无 ADB）启动→协议→关闭")
        void testWebOnlyFullChain() throws Exception {
            // ===== Phase 1: 仅启动 WEB =====
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "WEB 应启动");
            assertFalse(manager.isAdbRunning(), "ADB 不应运行");

            // ===== Phase 2: 协议链路 =====
            TextProtocol p = TextProtocol.newProtocol(TextProtocol.Header.M_DEVICES, "");
            String wire = String.format("%s://%s", p.getProtocolHeader(), p.getProtocolBody());
            TextProtocol parsed = TextProtocol.ParseWithString(wire);
            assertEquals(TextProtocol.Header.M_DEVICES, parsed.getProtocolHeader());

            // ===== Phase 3: TCP 连接验证 =====
            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected(), "TCP 连接应成功");
            }

            // ===== Phase 4: 关闭 =====
            manager.stop();
            waitForPortRelease();
            assertFalse(manager.isRunning(), "所有服务应已关闭");
            assertTrue(isPortAvailable(TEST_PORT), "端口应已释放");
        }

        @Test
        @Order(3)
        @DisplayName("全链路: 反复启动→关闭→重启 稳定性验证")
        void testRepeatedStartStopRestart() throws Exception {
            int cycles = 5;
            for (int i = 0; i < cycles; i++) {
                // 启动
                AndroidControlServer server = manager.startWebServer(TEST_PORT);
                assertTrue(manager.isWebServerRunning(),
                        "第 " + i + " 次启动后 WEB 应运行");

                // 验证端口
                try (Socket socket = new Socket("localhost", TEST_PORT)) {
                    assertTrue(socket.isConnected(),
                            "第 " + i + " 次启动后端口应可连接");
                }

                // 关闭
                manager.stop();
                waitForPortRelease();
                assertFalse(manager.isWebServerRunning(),
                        "第 " + i + " 次关闭后 WEB 应停止");
            }
        }

        @Test
        @Order(4)
        @DisplayName("全链路: 并发设备事件通知 + 服务运行不冲突")
        void testConcurrentEventNotificationWithServerRunning() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");

            // 启动全链路
            manager.startAll(TEST_PORT);
            Thread.sleep(500);
            assertTrue(manager.isRunning(), "全链路应运行中");

            // 并发触发设备事件
            int eventCount = 10;
            CountDownLatch latch = new CountDownLatch(eventCount);
            AtomicInteger receivedCount = new AtomicInteger(0);

            IAdbServerListener listener = new IAdbServerListener() {
                @Override
                public void onAdbDeviceConnected(AdbDevice device) {
                    receivedCount.incrementAndGet();
                    latch.countDown();
                }
                @Override
                public void onAdbDeviceDisConnected(AdbDevice device) {}
            };

            AdbServer.server().addListener(listener);

            // 通过反射触发设备连接通知
            java.lang.reflect.Method notifyMethod = AdbServer.class.getDeclaredMethod(
                    "notifyListenersConnected", AdbDevice.class);
            notifyMethod.setAccessible(true);

            for (int i = 0; i < eventCount; i++) {
                AdbDevice mockDevice = new AdbDevice(createMockIDevice("CONCURRENT_" + i));
                notifyMethod.invoke(AdbServer.server(), mockDevice);
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS), "所有事件通知应被处理");
            assertEquals(eventCount, receivedCount.get(), "应收到所有事件");

            // 服务仍正常运行
            assertTrue(manager.isRunning(), "并发事件后服务应仍运行");

            // 关闭
            manager.stop();
            waitForPortRelease();
        }

        @Test
        @Order(5)
        @DisplayName("全链路: 日志持久化→服务关闭→重新打开→验证日志仍在")
        void testLogPersistenceAcrossRestart() throws Exception {
            File tempDir = Files.createTempDirectory("chain_restart").toFile();
            File cacheFile = new File(tempDir, "restart_cache.csv");

            // 第一轮：启动→写日志→关闭
            AdbServer server1 = resetAndCreateAdbServer();
            setDeviceCacheFile(server1, cacheFile);

            java.lang.reflect.Method recordMethod = AdbServer.class.getDeclaredMethod(
                    "recordChangeEvent", String.class, ChangeType.class, ConnectType.class, String.class);
            recordMethod.setAccessible(true);
            recordMethod.invoke(server1, "RESTART_SN", ChangeType.CONNECTED, ConnectType.USB, "round1");

            server1.saveDeviceCache();
            server1.shutdown();

            // 验证日志文件存在
            File eventLogFile = new File(tempDir, "adb_device_events.log");
            assertTrue(eventLogFile.exists(), "第一轮日志文件应存在");
            String content1 = new String(Files.readAllBytes(eventLogFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(content1.contains("RESTART_SN"), "应包含第一轮事件");

            // 第二轮：重新启动→追加日志→验证
            AdbServer server2 = resetAndCreateAdbServer();
            setDeviceCacheFile(server2, cacheFile);
            recordMethod.invoke(server2, "RESTART_SN_2", ChangeType.DISCONNECTED, ConnectType.WIFI, "round2");

            String content2 = new String(Files.readAllBytes(eventLogFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(content2.contains("RESTART_SN"), "第一轮事件应仍在日志中");
            assertTrue(content2.contains("RESTART_SN_2"), "第二轮事件应已追加");

            // 清理
            server2.shutdown();
            eventLogFile.delete();
            tempDir.delete();

            // 重置单例
            Field f = AdbServer.class.getDeclaredField("server");
            f.setAccessible(true);
            f.set(null, null);
        }
    }
}
