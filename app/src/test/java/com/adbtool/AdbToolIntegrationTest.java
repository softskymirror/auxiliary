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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * adbtool 包集成测试。
 * <p>
 * 测试覆盖范围（模块间协作）：
 * <ul>
 *   <li>ServerManager + AndroidControlServer 生命周期协作</li>
 *   <li>AdbServer + IAdbServerListener 事件通知集成</li>
 *   <li>DeviceChangeEvent 事件日志 → 文件持久化 → 重新加载</li>
 *   <li>AdbDevice 属性缓存 + AdbUtils JSON 序列化集成</li>
 *   <li>TextProtocol 协议解析 → 消息分发链路</li>
 *   <li>AndroidControlServer 端口绑定 + TCP 连接验证</li>
 *   <li>ServerManager 状态查询与实际服务状态一致性</li>
 * </ul>
 * <p>
 * 端口使用 testPort（由 ConfigUtils.ConfigLoader 从 global.json 读取，默认 6655）。
 * ADB 相关测试通过 {@code assumeTrue} 自动跳过不可用环境。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdbToolIntegrationTest {

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
                System.err.println("ADB 同步线程未能启动，ADB 相关集成测试将自动跳过");
                AdbServer.server().shutdown();
            }
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("ADB 环境不可用，ADB 相关集成测试将自动跳过: " + e.getMessage());
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
        when(device.getProperty("ro.product.model")).thenReturn("MockDevice");
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

    // ==================== ServerManager + AndroidControlServer 集成 ====================

    @Nested
    @DisplayName("ServerManager + AndroidControlServer 生命周期集成")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ServerManagerAndroidControlTest {

        @Test
        @Order(1)
        @DisplayName("启动 WebServer 后 TCP 端口可连接")
        void testWebServerPortConnectable() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "WebServer 应运行中");

            // 验证 TCP 端口可连接
            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected(), "TCP 连接应成功");
            }
        }

        @Test
        @Order(2)
        @DisplayName("stop 后 TCP 端口不可连接")
        void testPortNotConnectableAfterStop() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isWebServerRunning(), "启动后应运行中");

            manager.stop();
            waitForPortRelease();

            // 验证端口已释放
            assertTrue(isPortAvailable(TEST_PORT), "stop 后端口应不可连接");
        }

        @Test
        @Order(3)
        @DisplayName("ServerManager.getWebServer 返回的实例与 startWebServer 一致")
        void testGetWebServerConsistency() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertSame(server, manager.getWebServer(), "getWebServer 应返回同一实例");
            assertTrue(server.isRunning(), "实例应处于运行状态");
        }

        @Test
        @Order(4)
        @DisplayName("restart 后新旧实例状态正确")
        void testRestartInstanceStates() throws Exception {
            AndroidControlServer old = manager.startWebServer(TEST_PORT);
            assertTrue(old.isRunning(), "旧实例应运行中");

            AndroidControlServer renewed = manager.restart(TEST_PORT);
            Thread.sleep(300);

            assertAll("验证重启后状态",
                    () -> assertFalse(old.isRunning(), "旧实例应已关闭"),
                    () -> assertTrue(renewed.isRunning(), "新实例应运行中"),
                    () -> assertSame(renewed, manager.getWebServer(), "getWebServer 应返回新实例"));
        }

        @Test
        @Order(5)
        @DisplayName("startAll 后 ADB + WebServer 同时运行（ADB 可用时）")
        void testStartAllBothServicesRunning() throws Exception {
            assumeTrue(adbAvailable, "ADB 环境不可用，跳过");
            AndroidControlServer server = manager.startAll(TEST_PORT);
            Thread.sleep(500);

            assertAll("验证双服务运行",
                    () -> assertNotNull(server, "server 不应为 null"),
                    () -> assertTrue(manager.isWebServerRunning(), "WebServer 应运行"),
                    () -> assertTrue(manager.isAdbRunning(), "ADB 应运行"),
                    () -> assertTrue(manager.isRunning(), "isRunning 应为 true"));

            // 验证 TCP 端口可连接
            try (Socket socket = new Socket("localhost", TEST_PORT)) {
                assertTrue(socket.isConnected(), "TCP 连接应成功");
            }
        }
    }

    // ==================== AdbServer + IAdbServerListener 事件通知集成 ====================

    @Nested
    @DisplayName("AdbServer + IAdbServerListener 事件通知集成")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AdbServerListenerIntegrationTest {

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
        @DisplayName("添加 Listener 后设备连接时收到回调")
        void testListenerReceivesConnectCallback() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AdbDevice> receivedDevice = new AtomicReference<>(null);

            IAdbServerListener listener = new IAdbServerListener() {
                @Override
                public void onAdbDeviceConnected(AdbDevice device) {
                    receivedDevice.set(device);
                    latch.countDown();
                }
                @Override
                public void onAdbDeviceDisConnected(AdbDevice device) {}
            };

            server.addListener(listener);

            // 模拟设备连接：通过反射调用 notifyListenersConnected
            AdbDevice mockDevice = new AdbDevice(createMockIDevice("LISTENER_TEST_SN"));
            java.lang.reflect.Method notifyMethod = AdbServer.class.getDeclaredMethod(
                    "notifyListenersConnected", AdbDevice.class);
            notifyMethod.setAccessible(true);
            notifyMethod.invoke(server, mockDevice);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "Listener 应在 2 秒内收到回调");
            assertNotNull(receivedDevice.get(), "回调应携带设备对象");
            assertEquals("LISTENER_TEST_SN", receivedDevice.get().getSerialNumber());
        }

        @Test
        @Order(2)
        @DisplayName("添加 Listener 后设备断开时收到回调")
        void testListenerReceivesDisconnectCallback() throws Exception {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AdbDevice> receivedDevice = new AtomicReference<>(null);

            IAdbServerListener listener = new IAdbServerListener() {
                @Override
                public void onAdbDeviceConnected(AdbDevice device) {}
                @Override
                public void onAdbDeviceDisConnected(AdbDevice device) {
                    receivedDevice.set(device);
                    latch.countDown();
                }
            };

            server.addListener(listener);

            AdbDevice mockDevice = new AdbDevice(createMockIDevice("DISCONNECT_SN"));
            java.lang.reflect.Method notifyMethod = AdbServer.class.getDeclaredMethod(
                    "notifyListenersDisconnected", AdbDevice.class);
            notifyMethod.setAccessible(true);
            notifyMethod.invoke(server, mockDevice);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "Listener 应在 2 秒内收到断开回调");
            assertEquals("DISCONNECT_SN", receivedDevice.get().getSerialNumber());
        }

        @Test
        @Order(3)
        @DisplayName("多个 Listener 同时收到通知")
        void testMultipleListenersNotified() throws Exception {
            int listenerCount = 3;
            CountDownLatch latch = new CountDownLatch(listenerCount);

            for (int i = 0; i < listenerCount; i++) {
                IAdbServerListener listener = mock(IAdbServerListener.class);
                doAnswer(inv -> {
                    latch.countDown();
                    return null;
                }).when(listener).onAdbDeviceConnected(any(AdbDevice.class));
                server.addListener(listener);
            }

            AdbDevice mockDevice = new AdbDevice(createMockIDevice("MULTI_LISTENER_SN"));
            java.lang.reflect.Method notifyMethod = AdbServer.class.getDeclaredMethod(
                    "notifyListenersConnected", AdbDevice.class);
            notifyMethod.setAccessible(true);
            notifyMethod.invoke(server, mockDevice);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "所有 Listener 都应收到通知");
        }

        @Test
        @Order(4)
        @DisplayName("移除的 Listener 不再收到通知")
        void testRemovedListenerNotNotified() throws Exception {
            IAdbServerListener listener = mock(IAdbServerListener.class);
            server.addListener(listener);
            server.removeListener(listener);

            AdbDevice mockDevice = new AdbDevice(createMockIDevice("REMOVED_LISTENER_SN"));
            java.lang.reflect.Method notifyMethod = AdbServer.class.getDeclaredMethod(
                    "notifyListenersConnected", AdbDevice.class);
            notifyMethod.setAccessible(true);
            notifyMethod.invoke(server, mockDevice);

            verify(listener, never()).onAdbDeviceConnected(any(AdbDevice.class));
        }
    }

    // ==================== DeviceChangeEvent 事件日志持久化集成 ====================

    @Nested
    @DisplayName("DeviceChangeEvent 事件日志持久化集成")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EventLogPersistenceIntegrationTest {

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
        @DisplayName("事件写入日志文件后可通过文件内容验证")
        void testEventWriteAndReadBack() throws Exception {
            File tempDir = Files.createTempDirectory("integ_event").toFile();
            File cacheFile = new File(tempDir, "cache.csv");
            setDeviceCacheFile(server, cacheFile);

            // 写入 5 条事件
            java.lang.reflect.Method appendMethod = AdbServer.class.getDeclaredMethod(
                    "appendEventToFile", DeviceChangeEvent.class);
            appendMethod.setAccessible(true);

            for (int i = 0; i < 5; i++) {
                DeviceChangeEvent event = new DeviceChangeEvent(
                        "INTEG_SN_" + i, ChangeType.CONNECTED, ConnectType.USB, "integ test " + i);
                appendMethod.invoke(server, event);
            }

            // 读取日志文件验证
            File eventLogFile = new File(tempDir, "adb_device_events.log");
            assertTrue(eventLogFile.exists(), "事件日志文件应存在");

            List<String> lines = Files.readAllLines(eventLogFile.toPath(), StandardCharsets.UTF_8);
            assertEquals(5, lines.size(), "应有 5 行日志");

            // 验证每行可反序列化
            for (int i = 0; i < 5; i++) {
                DeviceChangeEvent restored = DeviceChangeEvent.fromCsvLine(lines.get(i));
                assertNotNull(restored, "第 " + i + " 行应可反序列化");
                assertEquals("INTEG_SN_" + i, restored.getSerialNumber());
                assertEquals(ChangeType.CONNECTED, restored.getChangeType());
                assertEquals(ConnectType.USB, restored.getConnectType());
            }

            // 清理
            eventLogFile.delete();
            tempDir.delete();
        }

        @Test
        @Order(2)
        @DisplayName("设备缓存 CSV 写入后可通过文件内容验证")
        void testDeviceCacheWriteAndReadBack() throws Exception {
            File tempFile = File.createTempFile("integ_cache", ".csv");
            tempFile.deleteOnExit();
            setDeviceCacheFile(server, tempFile);

            // 添加多个 mock 设备
            List<AdbDevice> devices = new CopyOnWriteArrayList<>();
            for (int i = 0; i < 3; i++) {
                devices.add(new AdbDevice(createMockIDevice("CACHE_INTEG_" + i)));
            }
            setDeviceList(server, devices);

            server.saveDeviceCache();

            // 读取验证
            List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
            assertEquals(4, lines.size(), "标题行 + 3 个设备行");
            assertEquals("#serialNumber,connectType,sdk,abi,model", lines.get(0));

            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                assertTrue(parts[0].startsWith("CACHE_INTEG_"), "序列号应正确");
                assertEquals("arm64-v8a", parts[3], "ABI 应为 arm64-v8a");
                assertEquals("MockDevice", parts[4], "型号应正确");
            }

            tempFile.delete();
        }

        @Test
        @Order(3)
        @DisplayName("事件日志内存缓存与文件日志一致")
        void testMemoryAndFileLogConsistency() throws Exception {
            File tempDir = Files.createTempDirectory("integ_consist").toFile();
            File cacheFile = new File(tempDir, "cache.csv");
            setDeviceCacheFile(server, cacheFile);

            // 通过 recordChangeEvent 同时写内存和文件
            java.lang.reflect.Method recordMethod = AdbServer.class.getDeclaredMethod(
                    "recordChangeEvent", String.class, ChangeType.class, ConnectType.class, String.class);
            recordMethod.setAccessible(true);

            for (int i = 0; i < 3; i++) {
                recordMethod.invoke(server, "CONSIST_SN_" + i, ChangeType.CONNECTED, ConnectType.WIFI, "consist " + i);
            }

            // 验证内存缓存
            List<DeviceChangeEvent> memLog = server.getDeviceChangeLog();
            assertEquals(3, memLog.size(), "内存应有 3 条事件");

            // 验证文件日志
            File eventLogFile = new File(tempDir, "adb_device_events.log");
            List<String> fileLines = Files.readAllLines(eventLogFile.toPath(), StandardCharsets.UTF_8);
            assertEquals(3, fileLines.size(), "文件应有 3 行日志");

            // 内存和文件序列号一致
            for (int i = 0; i < 3; i++) {
                assertEquals(memLog.get(i).getSerialNumber(), "CONSIST_SN_" + i);
                assertTrue(fileLines.get(i).contains("CONSIST_SN_" + i));
            }

            // 清理
            eventLogFile.delete();
            tempDir.delete();
        }
    }

    // ==================== AdbDevice + AdbUtils JSON 序列化集成 ====================

    @Nested
    @DisplayName("AdbDevice + AdbUtils JSON 序列化集成")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DeviceJsonSerializationTest {

        @Test
        @Order(1)
        @DisplayName("设备列表序列化后 JSON 包含设备属性")
        void testDeviceListJsonContainsProperties() {
            List<AdbDevice> devices = new ArrayList<>();
            devices.add(new AdbDevice(createMockIDevice("JSON_SN_001")));
            String json = AdbUtils.devices2JSON(devices);

            assertNotNull(json);
            assertTrue(json.contains("JSON_SN_001"), "JSON 应包含序列号");
            assertTrue(json.contains("arm64-v8a"), "JSON 应包含 ABI");
            assertTrue(json.contains("MockDevice"), "JSON 应包含型号");
        }

        @Test
        @Order(2)
        @DisplayName("多设备序列化后各自独立")
        void testMultipleDevicesJsonIndependent() {
            List<AdbDevice> devices = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                devices.add(new AdbDevice(createMockIDevice("JSON_MULTI_" + i)));
            }
            String json = AdbUtils.devices2JSON(devices);

            // 验证每个设备都出现且可区分
            for (int i = 0; i < 5; i++) {
                assertTrue(json.contains("JSON_MULTI_" + i),
                        "JSON 应包含设备 JSON_MULTI_" + i);
            }
        }

        @Test
        @Order(3)
        @DisplayName("序列化结果为合法 JSON 数组格式")
        void testJsonArrayFormat() {
            List<AdbDevice> devices = new ArrayList<>();
            devices.add(new AdbDevice(createMockIDevice("FORMAT_SN")));
            String json = AdbUtils.devices2JSON(devices);

            assertTrue(json.startsWith("["), "应以 [ 开头");
            assertTrue(json.endsWith("]"), "应以 ] 结尾");
            // 尝试解析验证格式合法
            assertDoesNotThrow(() -> com.alibaba.fastjson.JSON.parseArray(json),
                    "应可被 fastjson 解析");
        }
    }

    // ==================== TextProtocol 消息分发链路集成 ====================

    @Nested
    @DisplayName("TextProtocol 消息分发链路集成")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProtocolDispatchIntegrationTest {

        @Test
        @Order(1)
        @DisplayName("M_DEVICES 协议构建→解析→Header 匹配")
        void testMDevicesProtocolChain() {
            // 构建
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_DEVICES, "");
            // 序列化为传输格式
            String wire = String.format("%s://%s", request.getProtocolHeader(), request.getProtocolBody());
            // 接收端解析
            TextProtocol parsed = TextProtocol.ParseWithString(wire);

            assertAll("验证完整链路",
                    () -> assertEquals(TextProtocol.Header.M_DEVICES, parsed.getProtocolHeader()),
                    () -> assertEquals("", parsed.getProtocolBody()));
        }

        @Test
        @Order(2)
        @DisplayName("M_WAIT 协议携带 JSON body 构建→解析→提取 sn")
        void testMWaitProtocolChain() {
            String jsonBody = "{\"sn\":\"DEVICE_001\"}";
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_WAIT, jsonBody);
            String wire = String.format("%s://%s", request.getProtocolHeader(), request.getProtocolBody());
            TextProtocol parsed = TextProtocol.ParseWithString(wire);

            assertAll("验证 M_WAIT 链路",
                    () -> assertEquals(TextProtocol.Header.M_WAIT, parsed.getProtocolHeader()),
                    () -> assertEquals(jsonBody, parsed.getProtocolBody()));

            // 提取 sn
            com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(parsed.getProtocolBody());
            assertEquals("DEVICE_001", obj.getString("sn"));
        }

        @Test
        @Order(3)
        @DisplayName("SM_DEVICES 响应协议构建→解析→JSON 反序列化")
        void testSMDevicesResponseChain() {
            // 模拟服务端构建响应
            String devicesJson = "[{\"serialNumber\":\"SN001\"},{\"serialNumber\":\"SN002\"}]";
            TextProtocol response = TextProtocol.newProtocol(TextProtocol.Header.SM_DEVICES, devicesJson);
            String wire = String.format("%s://%s", response.getProtocolHeader(), response.getProtocolBody());

            // 客户端解析
            TextProtocol parsed = TextProtocol.ParseWithString(wire);
            assertEquals(TextProtocol.Header.SM_DEVICES, parsed.getProtocolHeader());

            // 反序列化 JSON
            com.alibaba.fastjson.JSONArray arr = com.alibaba.fastjson.JSON.parseArray(parsed.getProtocolBody());
            assertEquals(2, arr.size(), "应有 2 个设备");
        }

        @Test
        @Order(4)
        @DisplayName("M_TOUCH 协议 body 含特殊字符正常传输")
        void testMTouchProtocolChain() {
            String touchData = "d:0:100:200:1:c:1";
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_TOUCH, touchData);
            String wire = String.format("%s://%s", request.getProtocolHeader(), request.getProtocolBody());
            TextProtocol parsed = TextProtocol.ParseWithString(wire);

            assertEquals(TextProtocol.Header.M_TOUCH, parsed.getProtocolHeader());
            assertEquals(touchData, parsed.getProtocolBody(), "触摸数据应完整传输");
        }

        @Test
        @Order(5)
        @DisplayName("M_KEYEVENT 协议 body 为数字字符串")
        void testMKeyEventProtocolChain() {
            String keyCode = "4"; // KEYCODE_BACK
            TextProtocol request = TextProtocol.newProtocol(TextProtocol.Header.M_KEYEVENT, keyCode);
            String wire = String.format("%s://%s", request.getProtocolHeader(), request.getProtocolBody());
            TextProtocol parsed = TextProtocol.ParseWithString(wire);

            assertEquals(TextProtocol.Header.M_KEYEVENT, parsed.getProtocolHeader());
            assertEquals("4", parsed.getProtocolBody());
            // 验证可解析为整数
            assertEquals(4, Integer.parseInt(parsed.getProtocolBody()));
        }

        @Test
        @Order(6)
        @DisplayName("所有客户端 Header 均可正确构建和解析")
        void testAllClientHeadersRoundTrip() {
            String[] headers = {
                    TextProtocol.Header.M_WAIT,
                    TextProtocol.Header.M_START,
                    TextProtocol.Header.M_WAITTING,
                    TextProtocol.Header.M_TOUCH,
                    TextProtocol.Header.M_KEYEVENT,
                    TextProtocol.Header.M_INPUT,
                    TextProtocol.Header.M_PUSH,
                    TextProtocol.Header.M_SHOT,
                    TextProtocol.Header.M_DEVICES
            };

            for (String header : headers) {
                TextProtocol p = TextProtocol.newProtocol(header, "test_body");
                String wire = String.format("%s://%s", p.getProtocolHeader(), p.getProtocolBody());
                TextProtocol parsed = TextProtocol.ParseWithString(wire);
                assertEquals(header, parsed.getProtocolHeader(),
                        "Header " + header + " 往返应一致");
                assertEquals("test_body", parsed.getProtocolBody(),
                        "Body 往返应一致");
            }
        }
    }

    // ==================== ServerManager 状态与实际服务一致性 ====================

    @Nested
    @DisplayName("ServerManager 状态与实际服务一致性")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ManagerStatusConsistencyTest {

        @Test
        @Order(1)
        @DisplayName("isWebServerRunning 与 AndroidControlServer.isRunning 一致")
        void testWebServerStatusConsistency() throws Exception {
            AndroidControlServer server = manager.startWebServer(TEST_PORT);
            assertEquals(server.isRunning(), manager.isWebServerRunning(),
                    "两个 isRunning 应一致（启动后）");

            manager.stop();
            Thread.sleep(300);
            assertFalse(manager.isWebServerRunning(), "stop 后 manager 应为 false");
        }

        @Test
        @Order(2)
        @DisplayName("isRunning 在仅 WebServer 运行时为 true")
        void testIsRunningWithWebServerOnly() throws Exception {
            manager.startWebServer(TEST_PORT);
            assertTrue(manager.isRunning(), "仅 WebServer 运行时 isRunning 应为 true");
            assertFalse(manager.isAdbRunning(), "isAdbRunning 应为 false");
            assertTrue(manager.isWebServerRunning(), "isWebServerRunning 应为 true");
        }

        @Test
        @Order(3)
        @DisplayName("stop 后 isRunning 立即返回 false")
        void testIsRunningImmediatelyFalseAfterStop() throws Exception {
            manager.startWebServer(TEST_PORT);
            manager.stop();
            assertFalse(manager.isRunning(), "stop 后 isRunning 应立即返回 false");
        }

        @Test
        @Order(4)
        @DisplayName("多次 start-stop 循环后状态正确")
        void testMultipleStartStopCycles() throws Exception {
            for (int i = 0; i < 3; i++) {
                manager.startWebServer(TEST_PORT);
                assertTrue(manager.isWebServerRunning(), "第 " + i + " 次启动后应运行中");

                manager.stop();
                waitForPortRelease();
                assertFalse(manager.isWebServerRunning(), "第 " + i + " 次停止后应不在运行");
            }
        }
    }
}
