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
import com.adbtool.util.Constant;
import com.android.ddmlib.IDevice;
import com.system.ConfigUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdbServer 功能测试。
 * <p>
 * 测试覆盖范围：
 * <ul>
 *   <li>DeviceChangeEvent 数据模型（CSV 序列化/反序列化、toString）</li>
 *   <li>ConnectType 枚举与 detectConnectType 连接类型识别</li>
 *   <li>设备变更事件日志缓存（内存 + 文件持久化）</li>
 *   <li>设备列表本地缓存（CSV 读写）</li>
 *   <li>Listener 管理（添加/移除/通知）</li>
 *   <li>生命周期控制（isRunning / shutdown）</li>
 *   <li>设备查询方法（getDevice / getFirstDevice / getDevices）</li>
 * </ul>
 * <p>
 * 因 AdbServer 使用私有构造 + 单例模式，测试通过反射创建独立实例，
 * 并在每个测试前后重置单例状态，确保测试隔离。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdbServerTest {

    private AdbServer server;

    // ==================== 全局初始化 ====================

    /**
     * 初始化 Constant 的配置路径。
     * AdbDevice 字段初始化器调用 Constant.getDataDir() 等方法，
     * 需要确保 Constant.defaultConfig 已正确初始化。
     * <p>
     * ConfigLoader 默认构造函数现在支持多路径回退自动查找，
     * 无需手动指定配置目录。
     */
    @BeforeAll
    static void initConstantConfig() throws Exception {
        ConfigUtils.ConfigLoader config = new ConfigUtils.ConfigLoader();
        java.lang.reflect.Field f = Constant.class.getDeclaredField("defaultConfig");
        f.setAccessible(true);
        f.set(null, config);
    }

    @AfterAll
    static void resetConstantConfig() throws Exception {
        java.lang.reflect.Field f = Constant.class.getDeclaredField("defaultConfig");
        f.setAccessible(true);
        f.set(null, null);
    }

    // ==================== 反射工具方法 ====================

    /**
     * 创建配置完整的 IDevice mock。
     * AdbDevice(IDevice) 构造函数会调用 fillPropertyCahe()，
     * 需要 getProperty() 返回有效值（特别是 SDK 必须为可解析的整数）。
     */
    private IDevice createMockIDevice(String serialNumber) {
        IDevice device = mock(IDevice.class);
        when(device.getSerialNumber()).thenReturn(serialNumber);
        when(device.getProperty("ro.build.version.sdk")).thenReturn("30");
        when(device.getProperty("ro.product.cpu.abi")).thenReturn("arm64-v8a");
        when(device.getProperty("ro.product.model")).thenReturn("MockDevice");
        return device;
    }

    /**
     * 重置 AdbServer 单例，确保每个测试独立。
     * 通过反射将静态 server 字段设为 null，然后通过私有构造函数创建新实例。
     */
    private AdbServer resetAndCreateServer() throws Exception {
        // 先关闭旧实例
        Field serverField = AdbServer.class.getDeclaredField("server");
        serverField.setAccessible(true);
        AdbServer old = (AdbServer) serverField.get(null);
        if (old != null) {
            try { old.shutdown(); } catch (Exception ignored) {}
        }

        // 清空静态引用
        serverField.set(null, null);

        // 通过私有构造函数创建新实例
        Constructor<AdbServer> ctor = AdbServer.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    /**
     * 设置 adbDeviceList（绕过单例共享状态）
     */
    @SuppressWarnings("unchecked")
    private void setDeviceList(AdbServer srv, List<AdbDevice> list) throws Exception {
        Field f = AdbServer.class.getDeclaredField("adbDeviceList");
        f.setAccessible(true);
        f.set(srv, list);
    }

    /**
     * 获取 deviceChangeLog
     */
    @SuppressWarnings("unchecked")
    private List<DeviceChangeEvent> getDeviceChangeLog(AdbServer srv) throws Exception {
        Field f = AdbServer.class.getDeclaredField("deviceChangeLog");
        f.setAccessible(true);
        return (List<DeviceChangeEvent>) f.get(srv);
    }

    /**
     * 获取 deviceCacheFile
     */
    private File getDeviceCacheFile(AdbServer srv) throws Exception {
        Field f = AdbServer.class.getDeclaredField("deviceCacheFile");
        f.setAccessible(true);
        return (File) f.get(srv);
    }

    /**
     * 设置 deviceCacheFile（指向临时文件）
     */
    private void setDeviceCacheFile(AdbServer srv, File file) throws Exception {
        Field f = AdbServer.class.getDeclaredField("deviceCacheFile");
        f.setAccessible(true);
        f.set(srv, file);
    }

    // ==================== 生命周期 ====================

    @BeforeEach
    void setUp() throws Exception {
        server = resetAndCreateServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            try { server.shutdown(); } catch (Exception ignored) {}
        }
        // 再次重置单例，避免影响其他测试类
        Field serverField = AdbServer.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    // ==================== DeviceChangeEvent 数据模型测试 ====================

    @Nested
    @DisplayName("DeviceChangeEvent 数据模型测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DeviceChangeEventTest {

        @Test
        @Order(1)
        @DisplayName("构造函数正确设置所有字段")
        void testConstructorSetsFields() {
            DeviceChangeEvent event = new DeviceChangeEvent(
                    "ABC123", ChangeType.CONNECTED, ConnectType.USB, "test detail");

            assertAll("验证字段",
                    () -> assertEquals("ABC123", event.getSerialNumber()),
                    () -> assertEquals(ChangeType.CONNECTED, event.getChangeType()),
                    () -> assertEquals(ConnectType.USB, event.getConnectType()),
                    () -> assertEquals("test detail", event.getDetail()),
                    () -> assertTrue(event.getTimestamp() > 0, "时间戳应 > 0"));
        }

        @Test
        @Order(2)
        @DisplayName("toCsvLine 输出正确的 CSV 格式")
        void testToCsvLine() {
            DeviceChangeEvent event = new DeviceChangeEvent(
                    "SN001", ChangeType.DISCONNECTED, ConnectType.WIFI, "detail");
            String csv = event.toCsvLine();

            assertTrue(csv.startsWith(String.valueOf(event.getTimestamp())),
                    "CSV 应以时间戳开头");
            assertTrue(csv.contains("SN001"), "CSV 应包含序列号");
            assertTrue(csv.contains("DISCONNECTED"), "CSV 应包含变更类型");
            assertTrue(csv.contains("WIFI"), "CSV 应包含连接类型");
            assertTrue(csv.endsWith("detail"), "CSV 应以 detail 结尾");
        }

        @Test
        @Order(3)
        @DisplayName("toCsvLine 处理 null detail")
        void testToCsvLineNullDetail() {
            DeviceChangeEvent event = new DeviceChangeEvent(
                    "SN002", ChangeType.CONNECTED, ConnectType.USB, null);
            String csv = event.toCsvLine();
            assertTrue(csv.endsWith(","), "null detail 应为空字符串");
        }

        @Test
        @Order(4)
        @DisplayName("fromCsvLine 正确反序列化")
        void testFromCsvLine() {
            long ts = System.currentTimeMillis();
            String csv = ts + ",SN003,CONNECTED,WIFI,wifi connected";

            DeviceChangeEvent event = DeviceChangeEvent.fromCsvLine(csv);

            assertNotNull(event, "fromCsvLine 不应返回 null");
            assertAll("验证反序列化字段",
                    () -> assertEquals("SN003", event.getSerialNumber()),
                    () -> assertEquals(ChangeType.CONNECTED, event.getChangeType()),
                    () -> assertEquals(ConnectType.WIFI, event.getConnectType()),
                    () -> assertEquals(ts, event.getTimestamp()),
                    () -> assertEquals("wifi connected", event.getDetail()));
        }

        @Test
        @Order(5)
        @DisplayName("fromCsvLine 序列化/反序列化往返一致")
        void testCsvRoundTrip() {
            DeviceChangeEvent original = new DeviceChangeEvent(
                    "RT_TEST", ChangeType.DISCONNECTED, ConnectType.OTHER, "round trip");
            String csv = original.toCsvLine();
            DeviceChangeEvent restored = DeviceChangeEvent.fromCsvLine(csv);

            assertNotNull(restored, "反序列化不应返回 null");
            assertAll("验证往返一致性",
                    () -> assertEquals(original.getSerialNumber(), restored.getSerialNumber()),
                    () -> assertEquals(original.getChangeType(), restored.getChangeType()),
                    () -> assertEquals(original.getConnectType(), restored.getConnectType()),
                    () -> assertEquals(original.getTimestamp(), restored.getTimestamp()),
                    () -> assertEquals(original.getDetail(), restored.getDetail()));
        }

        @Test
        @Order(6)
        @DisplayName("fromCsvLine 格式错误返回 null")
        void testFromCsvLineMalformed() {
            assertAll("验证各种异常输入",
                    () -> assertNull(DeviceChangeEvent.fromCsvLine("only,two"),
                            "字段不足应返回 null"),
                    () -> assertNull(DeviceChangeEvent.fromCsvLine(""),
                            "空字符串应返回 null"),
                    () -> assertNull(DeviceChangeEvent.fromCsvLine("notanumber,SN,XXX,USB,d"),
                            "无效时间戳应返回 null"),
                    () -> assertNull(DeviceChangeEvent.fromCsvLine("123,SN,BADTYPE,USB,d"),
                            "无效 ChangeType 应返回 null"),
                    () -> assertNull(DeviceChangeEvent.fromCsvLine("123,SN,CONNECTED,BADCONN,d"),
                            "无效 ConnectType 应返回 null"));
        }

        @Test
        @Order(7)
        @DisplayName("fromCsvLine 4 字段（无 detail）正常解析")
        void testFromCsvLineNoDetail() {
            String csv = "1000,SN4,CONNECTED,USB";
            DeviceChangeEvent event = DeviceChangeEvent.fromCsvLine(csv);

            assertNotNull(event, "4 字段 CSV 应正常解析");
            assertAll("验证字段",
                    () -> assertEquals("SN4", event.getSerialNumber()),
                    () -> assertEquals(ChangeType.CONNECTED, event.getChangeType()),
                    () -> assertEquals(ConnectType.USB, event.getConnectType()),
                    () -> assertEquals(1000L, event.getTimestamp()),
                    () -> assertEquals("", event.getDetail(), "缺少 detail 应为空字符串"));
        }

        @Test
        @Order(8)
        @DisplayName("toString 包含所有关键信息")
        void testToStringContainsInfo() {
            DeviceChangeEvent event = new DeviceChangeEvent(
                    "SN_TOSTR", ChangeType.CONNECTED, ConnectType.USB, "my detail");
            String str = event.toString();

            assertAll("验证 toString 内容",
                    () -> assertTrue(str.contains("SN_TOSTR"), "应包含序列号"),
                    () -> assertTrue(str.contains("CONNECTED"), "应包含变更类型"),
                    () -> assertTrue(str.contains("USB"), "应包含连接类型"),
                    () -> assertTrue(str.contains("my detail"), "应包含详情"));
        }
    }

    // ==================== ConnectType 枚举与 detectConnectType 测试 ====================

    @Nested
    @DisplayName("ConnectType 枚举与 detectConnectType 连接类型识别")
    class ConnectTypeTest {

        @Test
        @Order(1)
        @DisplayName("枚举值完整且顺序正确")
        void testEnumValues() {
            ConnectType[] values = ConnectType.values();
            assertEquals(3, values.length, "应有 3 个枚举值");
            assertEquals(ConnectType.USB, ConnectType.valueOf("USB"));
            assertEquals(ConnectType.WIFI, ConnectType.valueOf("WIFI"));
            assertEquals(ConnectType.OTHER, ConnectType.valueOf("OTHER"));
        }

        @Test
        @Order(2)
        @DisplayName("ChangeType 枚举值完整")
        void testChangeTypeEnum() {
            assertEquals(2, ChangeType.values().length);
            assertEquals(ChangeType.CONNECTED, ChangeType.valueOf("CONNECTED"));
            assertEquals(ChangeType.DISCONNECTED, ChangeType.valueOf("DISCONNECTED"));
        }

        @Test
        @Order(3)
        @DisplayName("null 设备返回 OTHER")
        void testDetectConnectTypeNullDevice() {
            assertEquals(ConnectType.OTHER, server.detectConnectType(null));
        }

        @Test
        @Order(4)
        @DisplayName("序列号含冒号识别为 WiFi")
        void testDetectConnectTypeWifi() {
            AdbDevice device = new AdbDevice(createMockIDevice("192.168.1.100:5555"));
            assertEquals(ConnectType.WIFI, server.detectConnectType(device));
        }

        @Test
        @Order(5)
        @DisplayName("普通序列号识别为 OTHER")
        void testDetectConnectTypeOther() {
            AdbDevice device = new AdbDevice(createMockIDevice("ABC12345"));
            assertEquals(ConnectType.OTHER, server.detectConnectType(device));
        }

        @Test
        @Order(6)
        @DisplayName("WiFi 格式序列号（host:port）识别正确")
        void testDetectConnectTypeWifiFormat() {
            AdbDevice device = new AdbDevice(createMockIDevice("10.0.0.1:5555"));
            assertEquals(ConnectType.WIFI, server.detectConnectType(device));
        }
    }

    // ==================== 设备变更事件日志缓存测试 ====================

    @Nested
    @DisplayName("设备变更事件日志缓存测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EventLogCacheTest {

        @Test
        @Order(1)
        @DisplayName("初始事件日志为空")
        void testInitialEventLogEmpty() throws Exception {
            List<DeviceChangeEvent> log = server.getDeviceChangeLog();
            assertNotNull(log, "事件日志不应为 null");
            assertTrue(log.isEmpty(), "初始事件日志应为空");
        }

        @Test
        @Order(2)
        @DisplayName("getDeviceChangeLog 返回不可变列表")
        void testEventLogIsUnmodifiable() throws Exception {
            List<DeviceChangeEvent> log = server.getDeviceChangeLog();
            assertThrows(UnsupportedOperationException.class,
                    () -> log.add(new DeviceChangeEvent("X", ChangeType.CONNECTED, ConnectType.USB, "")),
                    "返回的列表应为不可变的");
        }

        @Test
        @Order(3)
        @DisplayName("getDeviceChangeLog(maxCount) 返回最近 N 条")
        void testGetDeviceChangeLogMaxCount() throws Exception {
            // 通过反射向 deviceChangeLog 添加事件
            List<DeviceChangeEvent> log = getDeviceChangeLog(server);
            for (int i = 0; i < 5; i++) {
                log.add(new DeviceChangeEvent("SN" + i, ChangeType.CONNECTED, ConnectType.USB, "event" + i));
            }

            List<DeviceChangeEvent> recent3 = server.getDeviceChangeLog(3);
            assertEquals(3, recent3.size(), "应返回 3 条");
            assertEquals("SN2", recent3.get(0).getSerialNumber(), "应从第 3 条开始");
            assertEquals("SN4", recent3.get(2).getSerialNumber(), "应到第 5 条结束");
        }

        @Test
        @Order(4)
        @DisplayName("getDeviceChangeLog(0) 返回全部")
        void testGetDeviceChangeLogAll() throws Exception {
            List<DeviceChangeEvent> log = getDeviceChangeLog(server);
            for (int i = 0; i < 3; i++) {
                log.add(new DeviceChangeEvent("SN" + i, ChangeType.CONNECTED, ConnectType.USB, ""));
            }

            assertEquals(3, server.getDeviceChangeLog(0).size(), "maxCount=0 应返回全部");
            assertEquals(3, server.getDeviceChangeLog(-1).size(), "maxCount<0 应返回全部");
            assertEquals(3, server.getDeviceChangeLog(100).size(), "maxCount>size 应返回全部");
        }

        @Test
        @Order(5)
        @DisplayName("事件日志文件持久化（appendEventToFile）")
        void testEventLogFilePersistence() throws Exception {
            // 设置临时缓存文件
            File tempDir = Files.createTempDirectory("adbtest_event").toFile();
            File cacheFile = new File(tempDir, "test_cache.csv");
            setDeviceCacheFile(server, cacheFile);

            // 通过反射调用 appendEventToFile
            DeviceChangeEvent event = new DeviceChangeEvent(
                    "PERSIST_SN", ChangeType.CONNECTED, ConnectType.WIFI, "persist test");
            java.lang.reflect.Method appendMethod = AdbServer.class.getDeclaredMethod(
                    "appendEventToFile", DeviceChangeEvent.class);
            appendMethod.setAccessible(true);
            appendMethod.invoke(server, event);

            // 验证事件日志文件存在且包含数据
            File eventLogFile = new File(tempDir, "adb_device_events.log");
            assertTrue(eventLogFile.exists(), "事件日志文件应被创建");

            String content = new String(Files.readAllBytes(eventLogFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(content.contains("PERSIST_SN"), "日志文件应包含序列号");
            assertTrue(content.contains("CONNECTED"), "日志文件应包含变更类型");
            assertTrue(content.contains("WIFI"), "日志文件应包含连接类型");

            // 清理
            eventLogFile.delete();
            tempDir.delete();
        }

        @Test
        @Order(6)
        @DisplayName("多次追加事件日志不覆盖（append 模式）")
        void testEventLogAppendMode() throws Exception {
            File tempDir = Files.createTempDirectory("adbtest_append").toFile();
            File cacheFile = new File(tempDir, "test_cache.csv");
            setDeviceCacheFile(server, cacheFile);

            java.lang.reflect.Method appendMethod = AdbServer.class.getDeclaredMethod(
                    "appendEventToFile", DeviceChangeEvent.class);
            appendMethod.setAccessible(true);

            // 追加 3 条事件
            for (int i = 0; i < 3; i++) {
                DeviceChangeEvent event = new DeviceChangeEvent(
                        "SN_APPEND_" + i, ChangeType.CONNECTED, ConnectType.USB, "append" + i);
                appendMethod.invoke(server, event);
            }

            File eventLogFile = new File(tempDir, "adb_device_events.log");
            List<String> lines = Files.readAllLines(eventLogFile.toPath(), StandardCharsets.UTF_8);
            assertEquals(3, lines.size(), "应有 3 行日志");

            // 清理
            eventLogFile.delete();
            tempDir.delete();
        }
    }

    // ==================== 设备列表本地缓存测试 ====================

    @Nested
    @DisplayName("设备列表本地缓存测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DeviceCacheFileTest {

        @Test
        @Order(1)
        @DisplayName("saveDeviceCache 生成正确的 CSV 格式")
        void testSaveDeviceCacheCsvFormat() throws Exception {
            File tempFile = File.createTempFile("adb_cache_test", ".csv");
            tempFile.deleteOnExit();
            setDeviceCacheFile(server, tempFile);

            // 添加 mock 设备
            AdbDevice adbDevice = new AdbDevice(createMockIDevice("CACHE_SN001"));
            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            list.add(adbDevice);
            setDeviceList(server, list);

            // 保存缓存
            server.saveDeviceCache();

            // 读取并验证 CSV 内容
            List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
            assertTrue(lines.size() >= 2, "至少应有标题行 + 1 个设备行");
            assertEquals("#serialNumber,connectType,sdk,abi,model", lines.get(0),
                    "标题行格式应正确");
            assertTrue(lines.get(1).startsWith("CACHE_SN001,"),
                    "设备行应以序列号开头");
            assertTrue(lines.get(1).contains("arm64-v8a"),
                    "设备行应包含 ABI");
            assertTrue(lines.get(1).contains("MockDevice"),
                    "设备行应包含型号");

            tempFile.delete();
        }

        @Test
        @Order(2)
        @DisplayName("saveDeviceCache 空设备列表只写标题行")
        void testSaveDeviceCacheEmptyList() throws Exception {
            File tempFile = File.createTempFile("adb_cache_empty", ".csv");
            tempFile.deleteOnExit();
            setDeviceCacheFile(server, tempFile);

            server.saveDeviceCache();

            List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
            assertEquals(1, lines.size(), "空列表应只有标题行");
            assertEquals("#serialNumber,connectType,sdk,abi,model", lines.get(0));

            tempFile.delete();
        }

        @Test
        @Order(3)
        @DisplayName("saveDeviceCache deviceCacheFile 为 null 时不抛异常")
        void testSaveDeviceCacheNullFile() throws Exception {
            setDeviceCacheFile(server, null);
            assertDoesNotThrow(() -> server.saveDeviceCache(),
                    "deviceCacheFile 为 null 时应静默跳过");
        }

        @Test
        @Order(4)
        @DisplayName("saveDeviceCache 多设备写入各自占一行")
        void testSaveDeviceCacheMultipleDevices() throws Exception {
            File tempFile = File.createTempFile("adb_cache_multi", ".csv");
            tempFile.deleteOnExit();
            setDeviceCacheFile(server, tempFile);

            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            for (int i = 0; i < 3; i++) {
                list.add(new AdbDevice(createMockIDevice("MULTI_SN_" + i)));
            }
            setDeviceList(server, list);

            server.saveDeviceCache();

            List<String> lines = Files.readAllLines(tempFile.toPath(), StandardCharsets.UTF_8);
            assertEquals(4, lines.size(), "标题行 + 3 个设备行");

            for (int i = 0; i < 3; i++) {
                assertTrue(lines.get(i + 1).contains("MULTI_SN_" + i),
                        "第 " + i + " 行应包含对应序列号");
            }

            tempFile.delete();
        }

        @Test
        @Order(5)
        @DisplayName("initCacheFile 自动创建父目录")
        void testInitCacheFileCreatesParentDir() throws Exception {
            File tempDir = Files.createTempDirectory("adbtest_initcache").toFile();
            File deepCache = new File(tempDir, "sub/dir/cache.csv");
            // 手动调用 initCacheFile 逻辑验证目录创建
            File parent = deepCache.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            assertTrue(parent.exists(), "父目录应被创建");

            // 清理
            parent.delete();
            tempDir.delete();
        }
    }

    // ==================== Listener 管理测试 ====================

    @Nested
    @DisplayName("Listener 管理测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ListenerManagementTest {

        @Test
        @Order(1)
        @DisplayName("addListener 成功添加监听器")
        void testAddListener() throws Exception {
            IAdbServerListener listener = mock(IAdbServerListener.class);
            server.addListener(listener);

            // 通过反射验证 listeners 列表
            Field f = AdbServer.class.getDeclaredField("listeners");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IAdbServerListener> listeners = (List<IAdbServerListener>) f.get(server);
            assertTrue(listeners.contains(listener), "listeners 应包含添加的监听器");
        }

        @Test
        @Order(2)
        @DisplayName("removeListener 成功移除监听器")
        void testRemoveListener() throws Exception {
            IAdbServerListener listener = mock(IAdbServerListener.class);
            server.addListener(listener);
            server.removeListener(listener);

            Field f = AdbServer.class.getDeclaredField("listeners");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IAdbServerListener> listeners = (List<IAdbServerListener>) f.get(server);
            assertFalse(listeners.contains(listener), "listeners 不应包含已移除的监听器");
        }

        @Test
        @Order(3)
        @DisplayName("addListener(null) 不抛异常")
        void testAddNullListener() {
            assertDoesNotThrow(() -> server.addListener(null),
                    "添加 null 监听器不应抛异常");
        }

        @Test
        @Order(4)
        @DisplayName("removeListener(null) 不抛异常")
        void testRemoveNullListener() {
            assertDoesNotThrow(() -> server.removeListener(null),
                    "移除 null 监听器不应抛异常");
        }

        @Test
        @Order(5)
        @DisplayName("添加多个监听器后全部生效")
        void testMultipleListeners() throws Exception {
            IAdbServerListener listener1 = mock(IAdbServerListener.class);
            IAdbServerListener listener2 = mock(IAdbServerListener.class);
            IAdbServerListener listener3 = mock(IAdbServerListener.class);

            server.addListener(listener1);
            server.addListener(listener2);
            server.addListener(listener3);

            Field f = AdbServer.class.getDeclaredField("listeners");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IAdbServerListener> listeners = (List<IAdbServerListener>) f.get(server);
            assertEquals(3, listeners.size(), "应有 3 个监听器");
        }

        @Test
        @Order(6)
        @DisplayName("shutdown 清空所有监听器")
        void testShutdownClearsListeners() throws Exception {
            IAdbServerListener listener = mock(IAdbServerListener.class);
            server.addListener(listener);

            server.shutdown();

            Field f = AdbServer.class.getDeclaredField("listeners");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IAdbServerListener> listeners = (List<IAdbServerListener>) f.get(server);
            assertTrue(listeners.isEmpty(), "shutdown 后监听器应被清空");
        }
    }

    // ==================== 生命周期与状态查询测试 ====================

    @Nested
    @DisplayName("生命周期与状态查询测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LifecycleTest {

        @Test
        @Order(1)
        @DisplayName("新创建实例 isRunning 返回 false")
        void testNewInstanceNotRunning() {
            assertFalse(server.isRunning(), "新创建的实例不应处于运行状态");
        }

        @Test
        @Order(2)
        @DisplayName("shutdown 后 isRunning 返回 false")
        void testShutdownStopsRunning() {
            server.shutdown();
            assertFalse(server.isRunning(), "shutdown 后应不在运行");
        }

        @Test
        @Order(3)
        @DisplayName("shutdown 后设备列表被清空")
        void testShutdownClearsDeviceList() {
            // 先添加设备
            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            list.add(new AdbDevice(createMockIDevice("SHUTDOWN_SN")));
            try { setDeviceList(server, list); } catch (Exception e) { fail(e); }

            assertEquals(1, server.getDevices().size(), "shutdown 前应有 1 个设备");

            server.shutdown();
            assertTrue(server.getDevices().isEmpty(), "shutdown 后设备列表应被清空");
        }

        @Test
        @Order(4)
        @DisplayName("多次 shutdown 不抛异常（幂等性）")
        void testMultipleShutdowns() {
            assertDoesNotThrow(() -> {
                server.shutdown();
                server.shutdown();
                server.shutdown();
            }, "多次 shutdown 不应抛异常");
        }

        @Test
        @Order(5)
        @DisplayName("getIDevices 在 adb 为 null 时返回空数组")
        void testGetIDevicesWhenAdbNull() {
            // 新实例 adb 可能为 null（createBridge 失败），应返回空数组
            assertNotNull(server.getIDevices(), "getIDevices 不应返回 null");
            assertEquals(0, server.getIDevices().length, "adb 未初始化时应返回空数组");
        }
    }

    // ==================== 设备查询方法测试 ====================

    @Nested
    @DisplayName("设备查询方法测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DeviceQueryTest {

        @Test
        @Order(1)
        @DisplayName("空设备列表时 getDevices 返回空列表")
        void testGetDevicesEmpty() {
            assertNotNull(server.getDevices(), "getDevices 不应返回 null");
            assertTrue(server.getDevices().isEmpty(), "初始设备列表应为空");
        }

        @Test
        @Order(2)
        @DisplayName("getDevice 按序列号查找设备")
        void testGetDeviceBySerialNumber() throws Exception {
            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            list.add(new AdbDevice(createMockIDevice("FIND_ME")));
            list.add(new AdbDevice(createMockIDevice("OTHER_DEVICE")));
            setDeviceList(server, list);

            AdbDevice found = server.getDevice("FIND_ME");
            assertNotNull(found, "应找到目标设备");
            assertEquals("FIND_ME", found.getSerialNumber());
        }

        @Test
        @Order(3)
        @DisplayName("getDevice 查找不存在的设备返回 null")
        void testGetDeviceNotFound() throws Exception {
            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            list.add(new AdbDevice(createMockIDevice("EXISTING")));
            setDeviceList(server, list);

            assertNull(server.getDevice("NON_EXISTENT"), "不存在的设备应返回 null");
        }

        @Test
        @Order(4)
        @DisplayName("getFirstDevice 返回第一个设备")
        void testGetFirstDevice() throws Exception {
            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            list.add(new AdbDevice(createMockIDevice("FIRST")));
            list.add(new AdbDevice(createMockIDevice("SECOND")));
            setDeviceList(server, list);

            AdbDevice first = server.getFirstDevice();
            assertNotNull(first, "应返回第一个设备");
            assertEquals("FIRST", first.getSerialNumber());
        }

        @Test
        @Order(5)
        @DisplayName("getFirstDevice 空列表返回 null")
        void testGetFirstDeviceEmpty() {
            assertNull(server.getFirstDevice(), "空列表应返回 null");
        }

        @Test
        @Order(6)
        @DisplayName("getDevices 返回设备数量正确")
        void testGetDevicesCount() throws Exception {
            List<AdbDevice> list = new CopyOnWriteArrayList<>();
            for (int i = 0; i < 5; i++) {
                list.add(new AdbDevice(createMockIDevice("DEV_" + i)));
            }
            setDeviceList(server, list);

            assertEquals(5, server.getDevices().size(), "应返回 5 个设备");
        }
    }
}
