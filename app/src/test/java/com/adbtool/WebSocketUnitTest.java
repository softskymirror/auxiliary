/*
 * 单元测试层 —— TextProtocol / BinaryProtocol 纯协议解析验证
 *
 * 特点：不依赖 Netty 服务器、不依赖 ADB 环境
 * 运行：.\gradlew.bat app:test --tests "com.adbtool.WebSocketUnitTest"
 */

package com.adbtool;

import com.adbtool.protocol.BinaryProtocol;
import com.adbtool.protocol.TextProtocol;
import org.junit.jupiter.api.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【单元测试层】协议解析纯逻辑测试
 * <p>
 * 验证 TextProtocol（Header://Body 文本协议）和 BinaryProtocol（二进制帧）的
 * 编解码格式，对应前端 Protocol.parse() 和 handleBinary() 的解析逻辑。
 * <p>
 * 不启动任何服务器，不依赖 ADB 环境，可在任何环境运行。
 */
@DisplayName("【单元测试】协议解析（Protocol Parse / Binary Frame）")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSocketUnitTest {

    // ==================== 辅助方法 ====================

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

    // ==================== 1. TextProtocol 文本协议解析 ====================

    @Nested
    @DisplayName("1. TextProtocol 协议解析（前端 Protocol.parse 对应）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TextProtocolParsingTest {

        @Test
        @Order(1)
        @DisplayName("解析 M_DEVICES 请求格式")
        void testParseM_DEVICES() {
            String validMsg = "M_DEVICES://{}";
            TextProtocol proto = parseResponse(validMsg);
            assertEquals("M_DEVICES", proto.getProtocolHeader());
            assertEquals("{}", proto.getProtocolBody());
        }

        @Test
        @Order(2)
        @DisplayName("解析 M_WAIT 设备绑定请求")
        void testParseM_WAIT() {
            String msg = buildProtocolMessage("M_WAIT", "{\"sn\":\"emulator-5554\"}");
            TextProtocol proto = parseResponse(msg);
            assertEquals("M_WAIT", proto.getProtocolHeader());
            assertTrue(proto.getProtocolBody().contains("emulator-5554"));
        }

        @Test
        @Order(3)
        @DisplayName("解析 M_START 各种子命令")
        void testParseM_START_types() {
            String[] types = {"cap", "event", "constant", "turnoff", "install", "ring", "reset", "callCLi"};
            for (String type : types) {
                String msg = buildProtocolMessage("M_START", "{\"type\":\"" + type + "\"}");
                TextProtocol proto = parseResponse(msg);
                assertEquals("M_START", proto.getProtocolHeader());
                assertTrue(proto.getProtocolBody().contains("\"type\":\"" + type + "\""),
                        "M_START body 应包含 type=" + type);
            }
        }

        @Test
        @Order(4)
        @DisplayName("解析 M_TOUCH 触摸协议")
        void testParseM_TOUCH() {
            String msg = "M_TOUCH://d 0 540 960 50\nc\n";
            TextProtocol proto = parseResponse(msg);
            assertEquals("M_TOUCH", proto.getProtocolHeader());
            assertEquals("d 0 540 960 50\nc\n", proto.getProtocolBody());
        }

        @Test
        @Order(5)
        @DisplayName("解析 M_KEYEVENT 按键协议")
        void testParseM_KEYEVENT() {
            String msg = "M_KEYEVENT://4";
            TextProtocol proto = parseResponse(msg);
            assertEquals("M_KEYEVENT", proto.getProtocolHeader());
            assertEquals("4", proto.getProtocolBody());
        }

        @Test
        @Order(6)
        @DisplayName("解析 M_WAITTING 帧请求")
        void testParseM_WAITTING() {
            String msg = "M_WAITTING://{}";
            TextProtocol proto = parseResponse(msg);
            assertEquals("M_WAITTING", proto.getProtocolHeader());
        }

        @Test
        @Order(7)
        @DisplayName("无效协议格式应抛出异常")
        void testParseInvalidProtocol() {
            assertThrows(Exception.class, () -> {
                TextProtocol.ParseWithString("INVALID_NO_SEPARATOR");
            });
        }

        @Test
        @Order(8)
        @DisplayName("SM_OPENED 响应体为空字符串")
        void testSM_OPENED_BodyEmpty() {
            TextProtocol proto = TextProtocol.newProtocol("SM_OPENED", "");
            assertEquals("", proto.getProtocolBody(), "SM_OPENED body 应为空字符串");
            assertEquals("SM_OPENED://", proto.getProtocolHeader() + "://" + proto.getProtocolBody());
        }

        @Test
        @Order(9)
        @DisplayName("SM_SERVICE_STATE 响应体包含 type 和 stat 字段")
        void testSM_SERVICE_STATE_Format() {
            java.util.HashMap<String, String> map = new java.util.HashMap<>();
            map.put("type", "cap");
            map.put("stat", "open");
            String json = com.alibaba.fastjson.JSON.toJSONString(map);
            TextProtocol proto = TextProtocol.newProtocol("SM_SERVICE_STATE", json);

            com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(proto.getProtocolBody());
            assertEquals("cap", obj.getString("type"));
            assertEquals("open", obj.getString("stat"));
        }
    }

    // ==================== 2. BinaryProtocol 二进制帧格式 ====================

    @Nested
    @DisplayName("2. BinaryProtocol 二进制帧格式（前端 handleBinary 对应）")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BinaryProtocolFormatTest {

        @Test
        @Order(1)
        @DisplayName("SM_JPG 帧头为 0x0011（小端序）")
        void testSM_JPG_Header() {
            byte[] head = new byte[2];
            head[0] = (byte) (BinaryProtocol.Header.SM_JPG & 0xff);
            head[1] = (byte) ((BinaryProtocol.Header.SM_JPG >> 8) & 0xff);

            ByteBuffer buf = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
            short headerType = buf.getShort();
            assertEquals(0x0011, headerType, "SM_JPG 帧头应为 0x0011");
        }

        @Test
        @Order(2)
        @DisplayName("帧长度字段为 4 字节小端序")
        void testFrameLengthEncoding() {
            int dataLen = 1024;
            byte[] lenBuf = new byte[4];
            lenBuf[0] = (byte) (dataLen & 0xff);
            lenBuf[1] = (byte) ((dataLen >> 8) & 0xff);
            lenBuf[2] = (byte) ((dataLen >> 16) & 0xff);
            lenBuf[3] = (byte) ((dataLen >> 24) & 0xff);

            ByteBuffer buf = ByteBuffer.wrap(lenBuf).order(ByteOrder.LITTLE_ENDIAN);
            int parsedLen = buf.getInt();
            assertEquals(1024, parsedLen, "长度字段应正确解析为 1024");
        }

        @Test
        @Order(3)
        @DisplayName("完整帧结构: 2字节头 + 4字节长度 + JPEG数据")
        void testCompleteFrameStructure() {
            byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
            int dataLen = fakeJpeg.length;

            byte[] head = new byte[2];
            head[0] = (byte) (BinaryProtocol.Header.SM_JPG & 0xff);
            head[1] = (byte) ((BinaryProtocol.Header.SM_JPG >> 8) & 0xff);

            byte[] lenBuf = new byte[4];
            lenBuf[0] = (byte) (dataLen & 0xff);
            lenBuf[1] = (byte) ((dataLen >> 8) & 0xff);
            lenBuf[2] = (byte) ((dataLen >> 16) & 0xff);
            lenBuf[3] = (byte) ((dataLen >> 24) & 0xff);

            byte[] frame = new byte[2 + 4 + dataLen];
            System.arraycopy(head, 0, frame, 0, 2);
            System.arraycopy(lenBuf, 0, frame, 2, 4);
            System.arraycopy(fakeJpeg, 0, frame, 6, dataLen);

            ByteBuffer headerBuf = ByteBuffer.wrap(frame, 0, 2).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(0x0011, headerBuf.getShort(), "帧头应为 SM_JPG");

            byte[] jpegData = new byte[frame.length - 6];
            System.arraycopy(frame, 6, jpegData, 0, jpegData.length);
            assertArrayEquals(fakeJpeg, jpegData, "JPEG 数据应完整提取");
        }
    }
}
