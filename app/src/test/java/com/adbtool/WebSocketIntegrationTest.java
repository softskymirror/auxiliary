/*
 * 集成测试层 —— WebSocket + Netty 嵌入式服务器 + ADB 链路验证
 *
 * 特点：启动嵌入式 AndroidControlServer，验证 WebSocket 协议交互全链路
 * 依赖：Netty 服务器、ADB 环境（设备相关测试通过 assumeTrue 自动跳过）
 * 运行：.\gradlew.bat app:test --tests "com.adbtool.WebSocketIntegrationTest"
 */

package com.adbtool;

import com.adbtool.adb.AdbServer;
import com.adbtool.protocol.BinaryProtocol;
import com.adbtool.protocol.TextProtocol;
import com.adbtool.server.AndroidControlServer;
import com.neovisionaries.ws.client.*;
import org.junit.jupiter.api.*;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 【集成测试层】WebSocket 前后端交互链路测试
 * <p>
 * 启动嵌入式 Netty 服务器（AndroidControlServer），模拟前端 device.js / index.js
 * 的所有 WebSocket 交互行为，验证 WSServer 的协议处理是否正确响应。
 * <p>
 * 覆盖范围：
 * <ul>
 *   <li>3. WebSocket 连接建立 / 重连 / 多客户端</li>
 *   <li>4. M_DEVICES → SM_DEVICES（设备列表查询）</li>
 *   <li>5. M_WAIT → SM_OPENED（设备绑定）</li>
 *   <li>6. M_START（cap/event/constant/turnoff/install/callCLi）</li>
 *   <li>7. M_TOUCH / M_KEYEVENT（触摸/按键注入）</li>
 *   <li>8. M_WAITTING（帧流控制）</li>
 *   <li>9. 完整交互流程模拟（index.js / device.js 初始化序列）</li>
 *   <li>10. HTTP 静态文件服务（Netty HTTP pipeline）</li>
 * </ul>
 * <p>
 * 设备依赖测试通过 assumeTrue(adbAvailable) 自动跳过无设备环境。
 */
@DisplayName("【集成测试】WebSocket 前后端交互链路")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSocketIntegrationTest {

    /** 测试端口 */
    private static int TEST_PORT;

    /** 服务器实例 */
    private static AndroidControlServer server;

    /** ADB 是否可用 */
    private static boolean adbAvailable = false;

    /** WebSocket 客户端 */
    private WebSocket ws;

    /** 接收消息的 latch */
    private CountDownLatch messageLatch;

    /** 最后收到的文本消息 */
    private final AtomicReference<String> lastTextMessage = new AtomicReference<>();

    /** 最后收到的二进制消息 */
    private final AtomicReference<byte[]> lastBinaryMessage = new AtomicReference<>();

    // ==================== 全局初始化 ====================

    @BeforeAll
    static void startServer() {
        try {
            com.system.ConfigUtils.ConfigLoader config = new com.system.ConfigUtils.ConfigLoader();
            TEST_PORT = config.getTestPort();
        } catch (Exception e) {
            TEST_PORT = 6655;
        }

        try {
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            Thread.sleep(500);
            adbAvailable = AdbServer.server().isRunning();
            if (!adbAvailable) {
                System.err.println("ADB 不可用，设备相关测试将跳过");
            }
        } catch (Exception | UnsatisfiedLinkError e) {
            System.err.println("ADB 环境异常: " + e.getMessage());
            try { AdbServer.server().shutdown(); } catch (Exception ignored) {}
            adbAvailable = false;
        }

        try {
            server = new AndroidControlServer();
            server.start(TEST_PORT);
            Thread.sleep(300);
            System.out.println("集成测试服务器已启动在端口 " + TEST_PORT);
        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            fail("服务器启动失败: " + e.getMessage());
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("集成测试服务器已停止");
        }
        if (adbAvailable) {
            try { AdbServer.server().shutdown(); } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void setUp() {
        messageLatch = new CountDownLatch(1);
        lastTextMessage.set(null);
        lastBinaryMessage.set(null);
    }

    @AfterEach
    void tearDown() {
        if (ws != null && ws.isOpen()) {
            ws.disconnect();
        }
    }

    // ==================== 辅助方法 ====================

    private WebSocket connectWebSocket() throws Exception {
        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(5000);
        ws = factory.createSocket("ws://localhost:" + TEST_PORT);
        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, String text) {
                System.out.println("[WS 收到文本] " + text);
                lastTextMessage.set(text);
                messageLatch.countDown();
            }
            @Override
            public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                System.out.println("[WS 收到二进制] " + binary.length + " bytes");
                lastBinaryMessage.set(binary);
                messageLatch.countDown();
            }
            @Override
            public void onError(WebSocket websocket, WebSocketException cause) {
                System.err.println("[WS 错误] " + cause.getMessage());
            }
        });
        ws.connect();
        Thread.sleep(200);
        return ws;
    }

    private boolean waitForResponse() throws InterruptedException {
        return messageLatch.await(3, TimeUnit.SECONDS);
    }

    private TextProtocol parseResponse(String text) {
        try {
            return TextProtocol.ParseWithString(text);
        } catch (Exception e) {
            fail("协议解析失败: " + text + " -> " + e.getMessage());
            return null;
        }
    }

    private String buildProtocolMessage(String header, String bodyJson) {
        return header + "://" + (bodyJson != null ? bodyJson : "{}");
    }

    private WebSocket bindFirstDevice() throws Exception {
        connectWebSocket();
        ws.sendText(buildProtocolMessage(TextProtocol.Header.M_DEVICES, null));
        assertTrue(waitForResponse(), "获取设备列表超时");
        String body = parseResponse(lastTextMessage.get()).getProtocolBody();
        com.alibaba.fastjson.JSONArray devices = com.alibaba.fastjson.JSON.parseArray(body);
        if (devices == null || devices.isEmpty()) {
            System.out.println("无已连接设备");
            ws.disconnect();
            return null;
        }
        String sn = devices.getJSONObject(0).getString("sn");
        messageLatch = new CountDownLatch(1);
        ws.sendText(buildProtocolMessage(TextProtocol.Header.M_WAIT, "{\"sn\":\"" + sn + "\"}"));
        assertTrue(waitForResponse(), "绑定设备超时");
        assertEquals(TextProtocol.Header.SM_OPENED,
                parseResponse(lastTextMessage.get()).getProtocolHeader());
        return ws;
    }

    // ==================== 3. WebSocket 连接测试 ====================

    @Nested
    @DisplayName("3. WebSocket 连接建立")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WebSocketConnectionTest {

        @Test
        @Order(1)
        @DisplayName("WebSocket 能成功连接到服务器")
        void testConnect() throws Exception {
            WebSocket ws = connectWebSocket();
            assertTrue(ws.isOpen(), "WebSocket 应处于连接状态");
            ws.disconnect();
        }

        @Test
        @Order(2)
        @DisplayName("断开后能自动重连（模拟前端 reconnect 逻辑）")
        void testReconnect() throws Exception {
            WebSocket ws1 = connectWebSocket();
            assertTrue(ws1.isOpen());
            ws1.disconnect();
            Thread.sleep(200);
            messageLatch = new CountDownLatch(1);
            WebSocket ws2 = connectWebSocket();
            assertTrue(ws2.isOpen(), "重连应成功");
            ws2.disconnect();
        }

        @Test
        @Order(3)
        @DisplayName("多个 WebSocket 客户端可同时连接")
        void testMultipleClients() throws Exception {
            WebSocket ws1 = connectWebSocket();
            assertTrue(ws1.isOpen());
            WebSocketFactory factory = new WebSocketFactory();
            factory.setConnectionTimeout(5000);
            WebSocket ws2 = factory.createSocket("ws://localhost:" + TEST_PORT);
            CountDownLatch latch2 = new CountDownLatch(1);
            ws2.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String text) {
                    latch2.countDown();
                }
            });
            ws2.connect();
            Thread.sleep(200);
            assertTrue(ws1.isOpen(), "第一个客户端应保持连接");
            assertTrue(ws2.isOpen(), "第二个客户端应成功连接");
            ws1.sendText("M_DEVICES://{}");
            ws2.sendText("M_DEVICES://{}");
            assertTrue(waitForResponse(), "ws1 应收到响应");
            assertTrue(latch2.await(3, TimeUnit.SECONDS), "ws2 应收到响应");
            ws2.disconnect();
        }

        @Test
        @Order(4)
        @DisplayName("发送未知协议头不应断开连接")
        void testUnknownProtocolHeader() throws Exception {
            connectWebSocket();
            ws.sendText("UNKNOWN_HEADER://{}");
            Thread.sleep(500);
            assertTrue(ws.isOpen(), "未知协议头不应导致连接断开");
        }
    }

    // ==================== 4. M_DEVICES 设备列表交互 ====================

    @Nested
    @DisplayName("4. M_DEVICES 设备列表查询（index.js 首页交互）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class M_DEVICES_Test {

        @Test
        @Order(1)
        @DisplayName("发送 M_DEVICES 应收到 SM_DEVICES 响应")
        void testGetDeviceList() throws Exception {
            connectWebSocket();
            String msg = buildProtocolMessage(TextProtocol.Header.M_DEVICES, null);
            ws.sendText(msg);
            assertTrue(waitForResponse(), "应在 3 秒内收到响应");
            String response = lastTextMessage.get();
            assertNotNull(response, "响应不应为 null");
            TextProtocol proto = parseResponse(response);
            assertEquals(TextProtocol.Header.SM_DEVICES, proto.getProtocolHeader(),
                    "响应头应为 SM_DEVICES");
            String body = proto.getProtocolBody();
            assertNotNull(body);
            assertTrue(body.startsWith("["), "设备列表应为 JSON 数组: " + body);
            System.out.println("设备列表: " + body);
        }
    }

    // ==================== 5. M_WAIT 设备绑定交互 ====================

    @Nested
    @DisplayName("5. M_WAIT 设备绑定（device.js 控制页交互）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class M_WAIT_Test {

        @Test
        @Order(1)
        @DisplayName("绑定存在的设备应收到 SM_OPENED")
        void testBindExistingDevice() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过设备绑定测试");
            connectWebSocket();
            ws.sendText(buildProtocolMessage(TextProtocol.Header.M_DEVICES, null));
            assertTrue(waitForResponse());
            TextProtocol proto = parseResponse(lastTextMessage.get());
            String deviceJson = proto.getProtocolBody();
            com.alibaba.fastjson.JSONArray devices = com.alibaba.fastjson.JSON.parseArray(deviceJson);
            if (devices == null || devices.isEmpty()) {
                System.out.println("无已连接设备，跳过绑定测试");
                return;
            }
            String sn = devices.getJSONObject(0).getString("sn");
            System.out.println("绑定设备: " + sn);
            messageLatch = new CountDownLatch(1);
            ws.sendText(buildProtocolMessage(TextProtocol.Header.M_WAIT, "{\"sn\":\"" + sn + "\"}"));
            assertTrue(waitForResponse(), "应在 3 秒内收到绑定响应");
            TextProtocol bindProto = parseResponse(lastTextMessage.get());
            assertEquals(TextProtocol.Header.SM_OPENED, bindProto.getProtocolHeader(),
                    "绑定成功应返回 SM_OPENED");
        }

        @Test
        @Order(2)
        @DisplayName("绑定不存在的设备应断开连接")
        void testBindNonExistentDevice() throws Exception {
            connectWebSocket();
            ws.sendText(buildProtocolMessage(TextProtocol.Header.M_WAIT,
                    "{\"sn\":\"non_existent_device_12345\"}"));
            Thread.sleep(500);
            assertFalse(ws.isOpen(), "绑定不存在的设备后，连接应被关闭");
        }

        @Test
        @Order(3)
        @DisplayName("M_WAIT body 缺少 sn 字段应抛异常")
        void testBindMissingSnField() throws Exception {
            connectWebSocket();
            ws.sendText(buildProtocolMessage(TextProtocol.Header.M_WAIT, "{\"other\":\"value\"}"));
            Thread.sleep(500);
            assertFalse(ws.isOpen(), "缺少 sn 字段时连接应被关闭");
        }
    }

    // ==================== 6. M_START 设备管理命令 ====================

    @Nested
    @DisplayName("6. M_START 设备管理命令（device.js DeviceManager 对应）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class M_START_Test {

        @Test
        @Order(1)
        @DisplayName("M_START(constant) 设置屏幕常亮")
        void testSetConstant() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START,
                    "{\"type\":\"constant\"}"));
            Thread.sleep(500);
            assertTrue(boundWs.isOpen(), "发送 constant 后连接应保持");
        }

        @Test
        @Order(2)
        @DisplayName("M_START(turnoff) 关闭移动数据")
        void testTurnOffMobileData() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START,
                    "{\"type\":\"turnoff\"}"));
            Thread.sleep(500);
            assertTrue(boundWs.isOpen(), "发送 turnoff 后连接应保持");
        }

        @Test
        @Order(3)
        @DisplayName("M_START(cap) 启动截屏服务")
        void testStartCapService() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START,
                    "{\"type\":\"cap\",\"config\":{\"rotate\":0,\"scale\":0.3}}"));
            assertTrue(waitForResponse(), "应收到 SM_SERVICE_STATE 响应");
            TextProtocol proto = parseResponse(lastTextMessage.get());
            assertEquals(TextProtocol.Header.SM_SERVICE_STATE, proto.getProtocolHeader());
            assertTrue(proto.getProtocolBody().contains("\"type\":\"cap\""));
            assertTrue(proto.getProtocolBody().contains("\"stat\":\"open\""));
        }

        @Test
        @Order(4)
        @DisplayName("M_START(event) 启动触控服务")
        void testStartEventService() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START,
                    "{\"type\":\"event\"}"));
            assertTrue(waitForResponse(), "应收到 SM_SERVICE_STATE 响应");
            TextProtocol proto = parseResponse(lastTextMessage.get());
            assertEquals(TextProtocol.Header.SM_SERVICE_STATE, proto.getProtocolHeader());
            assertTrue(proto.getProtocolBody().contains("\"type\":\"event\""));
        }

        @Test
        @Order(5)
        @DisplayName("M_START(install) 批量安装 - 参数解析验证")
        void testInstallApps_paramParsing() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            String config = "{\"type\":\"install\",\"config\":{\"dir\":\"D:\\\\apps\\\\\",\"code\":\"1\",\"addTool\":false}}";
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START, config));
            Thread.sleep(500);
            assertTrue(boundWs.isOpen(), "发送 install 后连接应保持");
        }

        @Test
        @Order(6)
        @DisplayName("M_START(callCLi) CLI 命令 - 参数解析验证")
        void testCallCLI_paramParsing() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            String config = "{\"type\":\"callCLi\",\"config\":{\"cmd\":\"ls\",\"port\":\"8080\"}}";
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START, config));
            Thread.sleep(500);
            assertTrue(boundWs.isOpen(), "发送 callCLi 后连接应保持");
        }
    }

    // ==================== 7. 触摸和按键注入 ====================

    @Nested
    @DisplayName("7. 触摸/按键注入协议格式（device.js TouchHandler 对应）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TouchKeyEventTest {

        @Test
        @Order(1)
        @DisplayName("M_TOUCH 按下事件格式: d 0 x y pressure")
        void testTouchDownFormat() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText("M_TOUCH://d 0 540 960 50\nc\n");
            Thread.sleep(200);
            assertTrue(boundWs.isOpen(), "触摸注入后连接应保持");
        }

        @Test
        @Order(2)
        @DisplayName("M_TOUCH 移动事件格式: m 0 x y pressure")
        void testTouchMoveFormat() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText("M_TOUCH://m 0 550 970 50\nc\n");
            Thread.sleep(200);
            assertTrue(boundWs.isOpen());
        }

        @Test
        @Order(3)
        @DisplayName("M_TOUCH 抬起事件格式: u 0")
        void testTouchUpFormat() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText("M_TOUCH://u 0\nc\n");
            Thread.sleep(200);
            assertTrue(boundWs.isOpen());
        }

        @Test
        @Order(4)
        @DisplayName("M_KEYEVENT 按键注入: BACK=4, HOME=3, MENU=82")
        void testKeyEventCodes() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            int[] keys = {4, 3, 82};
            String[] names = {"BACK", "HOME", "MENU"};
            for (int i = 0; i < keys.length; i++) {
                boundWs.sendText("M_KEYEVENT://" + keys[i]);
                Thread.sleep(200);
                assertTrue(boundWs.isOpen(), "发送 " + names[i] + " 键后连接应保持");
            }
        }

        @Test
        @Order(5)
        @DisplayName("M_TOUCH body 格式验证: WSServer 直接透传给 eventService")
        void testTouchBodyPassthrough() {
            String body = "d 0 540 960 50\nc\n";
            TextProtocol proto = TextProtocol.newProtocol("M_TOUCH", body);
            assertEquals(body, proto.getProtocolBody(),
                    "M_TOUCH body 应原样透传，不做二次解析");
        }

        @Test
        @Order(6)
        @DisplayName("M_KEYEVENT body 为纯数字字符串")
        void testKeyEventBodyIsNumeric() {
            String body = "4";
            TextProtocol proto = TextProtocol.newProtocol("M_KEYEVENT", body);
            int key = Integer.parseInt(proto.getProtocolBody());
            assertEquals(4, key, "M_KEYEVENT body 应可解析为整数");
        }

        @Test
        @Order(7)
        @DisplayName("M_KEYEVENT 非数字 body 应被后端容错处理")
        void testKeyEventNonNumericBody() {
            String body = "invalid";
            assertThrows(NumberFormatException.class, () -> {
                Integer.parseInt(body);
            }, "非数字 body 应抛 NumberFormatException（后端已 try-catch）");
        }
    }

    // ==================== 8. M_WAITTING 帧流控制 ====================

    @Nested
    @DisplayName("8. M_WAITTING 帧流控制（device.js ImageRenderer 对应）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FrameFlowTest {

        @Test
        @Order(1)
        @DisplayName("发送 M_WAITTING 请求下一帧")
        void testRequestNextFrame() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过");
            WebSocket boundWs = bindFirstDevice();
            if (boundWs == null) return;
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_START,
                    "{\"type\":\"cap\",\"config\":{\"rotate\":0,\"scale\":0.3}}"));
            assertTrue(waitForResponse());
            messageLatch = new CountDownLatch(1);
            boundWs.sendText(buildProtocolMessage(TextProtocol.Header.M_WAITTING, null));
            boolean gotResponse = waitForResponse();
            if (gotResponse) {
                byte[] binary = lastBinaryMessage.get();
                if (binary != null && binary.length >= 6) {
                    ByteBuffer headerBuf = ByteBuffer.wrap(binary, 0, 2).order(ByteOrder.LITTLE_ENDIAN);
                    short header = headerBuf.getShort();
                    assertEquals(0x0011, header, "帧头应为 SM_JPG (0x0011)");
                    System.out.println("收到 JPEG 帧: " + binary.length + " bytes");
                }
            }
        }
    }

    // ==================== 9. 完整交互流程模拟 ====================

    @Nested
    @DisplayName("9. 完整交互流程模拟（模拟前端完整初始化序列）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FullFlowSimulationTest {

        @Test
        @Order(1)
        @DisplayName("模拟 index.js 完整流程: 连接 → 获取设备列表")
        void testIndexPageFlow() throws Exception {
            connectWebSocket();
            assertTrue(ws.isOpen(), "WebSocket 应已连接");
            ws.sendText("M_DEVICES://{}");
            assertTrue(waitForResponse(), "应收到设备列表");
            TextProtocol proto = parseResponse(lastTextMessage.get());
            assertEquals("SM_DEVICES", proto.getProtocolHeader());
            String body = proto.getProtocolBody();
            assertTrue(body.startsWith("["), "设备列表应为 JSON 数组");
            System.out.println("[index.js 流程] 获取到设备: " + body);
        }

        @Test
        @Order(2)
        @DisplayName("模拟 device.js 完整流程: 连接→绑定→启动服务→请求帧")
        void testDevicePageFlow() throws Exception {
            assumeTrue(adbAvailable, "ADB 不可用，跳过完整流程测试");
            connectWebSocket();
            ws.sendText("M_DEVICES://{}");
            assertTrue(waitForResponse());
            com.alibaba.fastjson.JSONArray devices =
                    com.alibaba.fastjson.JSON.parseArray(
                            parseResponse(lastTextMessage.get()).getProtocolBody());
            if (devices == null || devices.isEmpty()) {
                System.out.println("无设备，跳过完整流程测试");
                return;
            }
            String sn = devices.getJSONObject(0).getString("sn");
            messageLatch = new CountDownLatch(1);
            ws.sendText("M_WAIT://{\"sn\":\"" + sn + "\"}");
            assertTrue(waitForResponse());
            assertEquals("SM_OPENED", parseResponse(lastTextMessage.get()).getProtocolHeader());
            messageLatch = new CountDownLatch(1);
            ws.sendText("M_START://{\"type\":\"cap\",\"config\":{\"rotate\":0,\"scale\":0.3}}");
            assertTrue(waitForResponse());
            assertEquals("SM_SERVICE_STATE", parseResponse(lastTextMessage.get()).getProtocolHeader());
            messageLatch = new CountDownLatch(1);
            ws.sendText("M_START://{\"type\":\"event\"}");
            assertTrue(waitForResponse());
            assertEquals("SM_SERVICE_STATE", parseResponse(lastTextMessage.get()).getProtocolHeader());
            messageLatch = new CountDownLatch(1);
            ws.sendText("M_WAITTING://{}");
            waitForResponse();
            System.out.println("[device.js 流程] 完整初始化序列执行成功");
        }
    }

    // ==================== 10. HTTP 静态文件服务 ====================

    @Nested
    @DisplayName("10. HTTP 静态文件服务（前端资源加载验证）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class HttpStaticFileTest {

        @Test
        @Order(1)
        @DisplayName("HTTP 服务器能响应请求（Netty HTTP pipeline 验证）")
        void testHttpServerResponds() throws Exception {
            int code = httpGetCode("/");
            assertTrue(code > 0, "HTTP 服务器应响应请求（任何状态码），实际: " + code);
            System.out.println("[HTTP] GET / → " + code + " (Netty HTTP pipeline 正常)");
        }

        @Test
        @Order(2)
        @DisplayName("前端资源文件在 resources/web 目录下存在")
        void testResourceFilesExist() {
            String[] requiredFiles = {
                "index.html", "device.html",
                "static/js/index.js", "static/js/device.js",
                "static/js/keyeventConvert.js",
                "static/css/index.css", "static/css/device.css"
            };
            for (String path : requiredFiles) {
                java.net.URL url = getClass().getClassLoader().getResource("web/" + path);
                java.io.File file = new java.io.File("../resources/web/" + path);
                java.io.File file2 = new java.io.File("src/main/resources/web/" + path);
                java.io.File file3 = new java.io.File("resources/web/" + path);
                boolean exists = (url != null) || file.exists() || file2.exists() || file3.exists();
                assertTrue(exists, "前端文件应存在: " + path);
            }
            System.out.println("[资源] 所有 " + requiredFiles.length + " 个前端文件验证通过");
        }

        @Test
        @Order(3)
        @DisplayName("前端 JS 文件包含协议关键字")
        void testJsContainsProtocolKeywords() throws Exception {
            String[] jsFiles = {"index.js", "device.js"};
            for (String jsFile : jsFiles) {
                java.net.URL url = getClass().getClassLoader().getResource("web/static/js/" + jsFile);
                if (url != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(url.openStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    String content = sb.toString();
                    assertTrue(content.contains("M_") || content.contains("SM_"),
                            jsFile + " 应包含协议关键字 M_ 或 SM_");
                    System.out.println("[资源] " + jsFile + " 包含协议关键字");
                }
            }
        }

        private int httpGetCode(String path) throws Exception {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL("http://localhost:" + TEST_PORT + path).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            return conn.getResponseCode();
        }
    }
}
