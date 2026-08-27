/*
 * 系统测试层 —— 连接外部已运行的真实服务器，验证端到端可用性
 *
 * 特点：不启动嵌入式服务器，连接外部已部署的真实服务
 * 前置：需先启动真实服务器，并通过 -Dprod.server=IP:PORT 指定目标
 * 运行：.\gradlew.bat app:test --tests "com.adbtool.WebSocketSystemTest" -Dprod.server=localhost:6655
 */

package com.adbtool;

import com.adbtool.protocol.TextProtocol;
import com.neovisionaries.ws.client.*;
import org.junit.jupiter.api.*;

import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 【系统测试层】生产环境冒烟测试
 * <p>
 * 连接外部已运行的 AndroidControl 服务器，验证核心功能是否可用。
 * 不启动任何嵌入式服务器，完全依赖外部真实环境。
 * <p>
 * 使用方式：
 * <pre>
 *   # 1. 先启动真实服务器
 *   .\gradlew.bat app:run
 *
 *   # 2. 另一个终端运行系统测试
 *   .\gradlew.bat app:test --tests "com.adbtool.WebSocketSystemTest" -Dprod.server=localhost:6655
 * </pre>
 * <p>
 * 验证内容：
 * <ul>
 *   <li>TCP 端口连通性</li>
 *   <li>WebSocket 握手（HTTP Upgrade）</li>
 *   <li>M_DEVICES → SM_DEVICES 业务交互</li>
 *   <li>HTTP 静态文件服务（index.html / device.html / JS / CSS）</li>
 * </ul>
 */
@DisplayName("【系统测试】生产环境冒烟测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSocketSystemTest {

    /** 外部服务器地址，通过 -Dprod.server=IP:PORT 指定 */
    private String PROD_SERVER;

    @BeforeEach
    void checkProdServer() {
        PROD_SERVER = System.getProperty("prod.server");
    }

    // ==================== 系统测试用例 ====================

    @Nested
    @DisplayName("生产环境冒烟测试（连接外部已运行服务器）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProductionSmokeTest {

        @Test
        @Order(1)
        @DisplayName("验证外部服务器 TCP 可达")
        void testExternalServerReachable() throws Exception {
            assumeTrue(PROD_SERVER != null && !PROD_SERVER.isEmpty(),
                    "未指定 -Dprod.server，跳过生产环境测试。" +
                    "用法: gradle test -Dprod.server=localhost:6655");

            try (Socket socket = new Socket()) {
                String[] parts = PROD_SERVER.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                socket.connect(new java.net.InetSocketAddress(host, port), 3000);
                assertTrue(socket.isConnected(), "应能连接到外部服务器 " + PROD_SERVER);
                System.out.println("[系统测试] 服务器 " + PROD_SERVER + " TCP 可达");
            }
        }

        @Test
        @Order(2)
        @DisplayName("外部服务器 WebSocket 握手成功")
        void testExternalWebSocketHandshake() throws Exception {
            assumeTrue(PROD_SERVER != null && !PROD_SERVER.isEmpty(),
                    "未指定 -Dprod.server，跳过");

            WebSocketFactory factory = new WebSocketFactory();
            factory.setConnectionTimeout(5000);
            WebSocket extWs = factory.createSocket("ws://" + PROD_SERVER);
            extWs.connect();
            assertTrue(extWs.isOpen(), "外部服务器 WebSocket 应连接成功");
            System.out.println("[系统测试] WebSocket 握手成功");
            extWs.disconnect();
        }

        @Test
        @Order(3)
        @DisplayName("外部服务器 M_DEVICES → SM_DEVICES 交互验证")
        void testExternalM_DEVICES() throws Exception {
            assumeTrue(PROD_SERVER != null && !PROD_SERVER.isEmpty(),
                    "未指定 -Dprod.server，跳过");

            WebSocketFactory factory = new WebSocketFactory();
            factory.setConnectionTimeout(5000);
            WebSocket extWs = factory.createSocket("ws://" + PROD_SERVER);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> responseRef = new AtomicReference<>();
            extWs.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String text) {
                    responseRef.set(text);
                    latch.countDown();
                }
            });
            extWs.connect();
            Thread.sleep(200);

            extWs.sendText("M_DEVICES://{}");
            assertTrue(latch.await(5, TimeUnit.SECONDS),
                    "外部服务器应在 5 秒内响应 M_DEVICES");

            String response = responseRef.get();
            assertNotNull(response);
            TextProtocol proto = TextProtocol.ParseWithString(response);
            assertEquals("SM_DEVICES", proto.getProtocolHeader(),
                    "外部服务器应返回 SM_DEVICES");
            assertTrue(proto.getProtocolBody().startsWith("["),
                    "设备列表应为 JSON 数组");
            System.out.println("[系统测试] 设备列表: " + proto.getProtocolBody());

            extWs.disconnect();
        }

        @Test
        @Order(4)
        @DisplayName("外部服务器 HTTP 静态文件全部返回 200")
        void testExternalHttpFiles() throws Exception {
            assumeTrue(PROD_SERVER != null && !PROD_SERVER.isEmpty(),
                    "未指定 -Dprod.server，跳过");

            String baseUrl = "http://" + PROD_SERVER;
            String[] requiredFiles = {"/", "/device.html",
                    "/static/js/index.js", "/static/js/device.js",
                    "/static/css/index.css"};

            for (String path : requiredFiles) {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL(baseUrl + path).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                assertEquals(200, code, "HTTP GET " + path + " 应返回 200");
                System.out.println("[系统测试] " + path + " → " + code + " OK");
            }
        }
    }
}
