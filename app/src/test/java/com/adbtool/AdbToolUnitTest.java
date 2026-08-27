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
import com.adbtool.adb.AdbForward;
import com.adbtool.console.Console;
import com.adbtool.minicap.Banner;
import com.adbtool.protocol.BinaryProtocol;
import com.adbtool.protocol.TextProtocol;
import com.adbtool.util.AdbUtils;
import com.adbtool.util.Constant;
import com.android.ddmlib.IDevice;
import com.system.ConfigUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * adbtool 包单元测试。
 * <p>
 * 测试覆盖范围（纯单元测试，不依赖网络/ADB环境）：
 * <ul>
 *   <li>TextProtocol 文本协议解析与构建</li>
 *   <li>BinaryProtocol 二进制协议构建</li>
 *   <li>Banner 数据模型 getter/setter/toString</li>
 *   <li>AdbForward 端口转发解析</li>
 *   <li>Constant 路径配置代理</li>
 *   <li>AdbUtils 设备 JSON 序列化</li>
 *   <li>Console 命令注册与执行</li>
 *   <li>WSServer 工具方法（toLH/toHH）</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdbToolUnitTest {

    // ==================== 全局初始化 ====================

    @BeforeAll
    static void initConstantConfig() throws Exception {
        ConfigUtils.ConfigLoader config = new ConfigUtils.ConfigLoader();
        Field f = Constant.class.getDeclaredField("defaultConfig");
        f.setAccessible(true);
        f.set(null, config);
    }

    @AfterAll
    static void resetConstantConfig() throws Exception {
        Field f = Constant.class.getDeclaredField("defaultConfig");
        f.setAccessible(true);
        f.set(null, null);
    }

    // ==================== TextProtocol 文本协议测试 ====================

    @Nested
    @DisplayName("TextProtocol 文本协议测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TextProtocolTest {

        @Test
        @Order(1)
        @DisplayName("Header 常量值完整且正确")
        void testHeaderConstants() {
            // 客户端→服务端 Header
            assertEquals("M_WAIT", TextProtocol.Header.M_WAIT);
            assertEquals("M_START", TextProtocol.Header.M_START);
            assertEquals("M_WAITTING", TextProtocol.Header.M_WAITTING);
            assertEquals("M_TOUCH", TextProtocol.Header.M_TOUCH);
            assertEquals("M_KEYEVENT", TextProtocol.Header.M_KEYEVENT);
            assertEquals("M_INPUT", TextProtocol.Header.M_INPUT);
            assertEquals("M_PUSH", TextProtocol.Header.M_PUSH);
            assertEquals("M_SHOT", TextProtocol.Header.M_SHOT);
            assertEquals("M_DEVICES", TextProtocol.Header.M_DEVICES);
            // 服务端→客户端 Header
            assertEquals("SM_OPENED", TextProtocol.Header.SM_OPENED);
            assertEquals("SM_SERVICE_STATE", TextProtocol.Header.SM_SERVICE_STATE);
            assertEquals("SM_MESSAGE", TextProtocol.Header.SM_MESSAGE);
            assertEquals("SM_DISCONNECT", TextProtocol.Header.SM_DISCONNECT);
            assertEquals("SM_DEVICES", TextProtocol.Header.SM_DEVICES);
            assertEquals("SM_SHOT", TextProtocol.Header.SM_SHOT);
            assertEquals("SM_JPG", TextProtocol.Header.SM_JPG);
        }

        @Test
        @Order(2)
        @DisplayName("newProtocol 构建协议对象")
        void testNewProtocol() {
            TextProtocol p = TextProtocol.newProtocol("M_DEVICES", "body content");
            assertAll("验证构建结果",
                    () -> assertEquals("M_DEVICES", p.getProtocolHeader()),
                    () -> assertEquals("body content", p.getProtocolBody()));
        }

        @Test
        @Order(3)
        @DisplayName("newProtocol 空 body 正常构建")
        void testNewProtocolEmptyBody() {
            TextProtocol p = TextProtocol.newProtocol("M_WAIT", "");
            assertAll("验证空 body",
                    () -> assertEquals("M_WAIT", p.getProtocolHeader()),
                    () -> assertEquals("", p.getProtocolBody()));
        }

        @Test
        @Order(4)
        @DisplayName("ParseWithString 正常解析标准格式")
        void testParseWithStringStandard() {
            TextProtocol p = TextProtocol.ParseWithString("M_TOUCH://d:0:100:200:1");
            assertAll("验证解析结果",
                    () -> assertEquals("M_TOUCH", p.getProtocolHeader()),
                    () -> assertEquals("d:0:100:200:1", p.getProtocolBody()));
        }

        @Test
        @Order(5)
        @DisplayName("ParseWithString 解析含 JSON body 的协议")
        void testParseWithStringJsonBody() {
            String json = "{\"sn\":\"ABC123\",\"type\":\"cap\"}";
            TextProtocol p = TextProtocol.ParseWithString("M_START://" + json);
            assertAll("验证 JSON body 解析",
                    () -> assertEquals("M_START", p.getProtocolHeader()),
                    () -> assertEquals(json, p.getProtocolBody()));
        }

        @Test
        @Order(6)
        @DisplayName("ParseWithString 空 body 解析")
        void testParseWithStringEmptyBody() {
            TextProtocol p = TextProtocol.ParseWithString("M_DEVICES://");
            assertAll("验证空 body 解析",
                    () -> assertEquals("M_DEVICES", p.getProtocolHeader()),
                    () -> assertEquals("", p.getProtocolBody()));
        }

        @Test
        @Order(7)
        @DisplayName("ParseWithString body 含 :// 不截断")
        void testParseWithStringMultipleSeparators() {
            TextProtocol p = TextProtocol.ParseWithString("M_INPUT://http://example.com");
            assertAll("验证多 :// 解析",
                    () -> assertEquals("M_INPUT", p.getProtocolHeader()),
                    () -> assertEquals("http://example.com", p.getProtocolBody()));
        }

        @Test
        @Order(8)
        @DisplayName("ParseWithString 非法格式抛 InvalidParameterException")
        void testParseWithStringInvalidFormat() {
            assertThrows(InvalidParameterException.class,
                    () -> TextProtocol.ParseWithString("no_separator_here"),
                    "缺少 :// 应抛异常");
        }

        @Test
        @Order(9)
        @DisplayName("ParseWithString 空字符串抛异常")
        void testParseWithStringEmpty() {
            assertThrows(InvalidParameterException.class,
                    () -> TextProtocol.ParseWithString(""),
                    "空字符串应抛异常");
        }

        @Test
        @Order(10)
        @DisplayName("newProtocol 构建后格式化为标准字符串")
        void testProtocolToString() {
            TextProtocol p = TextProtocol.newProtocol("SM_DEVICES", "[{\"sn\":\"test\"}]");
            String formatted = String.format("%s://%s", p.getProtocolHeader(), p.getProtocolBody());
            assertEquals("SM_DEVICES://[{\"sn\":\"test\"}]", formatted);
        }
    }

    // ==================== BinaryProtocol 二进制协议测试 ====================

    @Nested
    @DisplayName("BinaryProtocol 二进制协议测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BinaryProtocolTest {

        @Test
        @Order(1)
        @DisplayName("Header 常量值正确")
        void testHeaderConstants() {
            assertEquals(0x0010, BinaryProtocol.Header.SM_SHOT, "SM_SHOT 应为 0x0010");
            assertEquals(0x0011, BinaryProtocol.Header.SM_JPG, "SM_JPG 应为 0x0011");
        }

        @Test
        @Order(2)
        @DisplayName("newProtocol 构建二进制协议对象")
        void testNewProtocol() {
            byte[] body = new byte[]{1, 2, 3, 4, 5};
            BinaryProtocol p = BinaryProtocol.newProtocol(BinaryProtocol.Header.SM_JPG, body);
            assertNotNull(p, "构建的协议对象不应为 null");
        }

        @Test
        @Order(3)
        @DisplayName("newProtocol 空 body 正常构建")
        void testNewProtocolEmptyBody() {
            BinaryProtocol p = BinaryProtocol.newProtocol(BinaryProtocol.Header.SM_SHOT, new byte[0]);
            assertNotNull(p, "空 body 协议对象不应为 null");
        }

        @Test
        @Order(4)
        @DisplayName("SM_SHOT 和 SM_JPG 值不同")
        void testHeaderValuesDistinct() {
            assertNotEquals(BinaryProtocol.Header.SM_SHOT, BinaryProtocol.Header.SM_JPG,
                    "SM_SHOT 和 SM_JPG 应为不同值");
        }
    }

    // ==================== Banner 数据模型测试 ====================

    @Nested
    @DisplayName("Banner 数据模型测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BannerTest {

        @Test
        @Order(1)
        @DisplayName("getter/setter 正确读写所有字段")
        void testGetterSetter() {
            Banner banner = new Banner();
            banner.setVersion(1);
            banner.setLength(24);
            banner.setPid(12345);
            banner.setReadWidth(1080);
            banner.setReadHeight(1920);
            banner.setVirtualWidth(540);
            banner.setVirtualHeight(960);
            banner.setOrientation(0);
            banner.setQuirks(2);

            assertAll("验证所有字段",
                    () -> assertEquals(1, banner.getVersion()),
                    () -> assertEquals(24, banner.getLength()),
                    () -> assertEquals(12345, banner.getPid()),
                    () -> assertEquals(1080, banner.getReadWidth()),
                    () -> assertEquals(1920, banner.getReadHeight()),
                    () -> assertEquals(540, banner.getVirtualWidth()),
                    () -> assertEquals(960, banner.getVirtualHeight()),
                    () -> assertEquals(0, banner.getOrientation()),
                    () -> assertEquals(2, banner.getQuirks()));
        }

        @Test
        @Order(2)
        @DisplayName("默认值为 0")
        void testDefaultValues() {
            Banner banner = new Banner();
            assertAll("验证默认值",
                    () -> assertEquals(0, banner.getVersion()),
                    () -> assertEquals(0, banner.getLength()),
                    () -> assertEquals(0, banner.getPid()),
                    () -> assertEquals(0, banner.getReadWidth()),
                    () -> assertEquals(0, banner.getReadHeight()),
                    () -> assertEquals(0, banner.getVirtualWidth()),
                    () -> assertEquals(0, banner.getVirtualHeight()),
                    () -> assertEquals(0, banner.getOrientation()),
                    () -> assertEquals(0, banner.getQuirks()));
        }

        @Test
        @Order(3)
        @DisplayName("toString 包含所有关键信息")
        void testToString() {
            Banner banner = new Banner();
            banner.setVersion(1);
            banner.setReadWidth(1080);
            banner.setReadHeight(1920);
            banner.setOrientation(90);

            String str = banner.toString();
            assertAll("验证 toString 内容",
                    () -> assertTrue(str.contains("version=1"), "应包含 version"),
                    () -> assertTrue(str.contains("readWidth=1080"), "应包含 readWidth"),
                    () -> assertTrue(str.contains("readHeight=1920"), "应包含 readHeight"),
                    () -> assertTrue(str.contains("orientation=90"), "应包含 orientation"));
        }

        @Test
        @Order(4)
        @DisplayName("多次 set 同一字段取最后值")
        void testOverwriteField() {
            Banner banner = new Banner();
            banner.setReadWidth(100);
            assertEquals(100, banner.getReadWidth());
            banner.setReadWidth(200);
            assertEquals(200, banner.getReadWidth(), "应取最后一次设置的值");
        }
    }

    // ==================== AdbForward 端口转发解析测试 ====================

    @Nested
    @DisplayName("AdbForward 端口转发解析测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AdbForwardTest {

        @Test
        @Order(1)
        @DisplayName("正常解析标准格式")
        void testParseStandardFormat() {
            AdbForward forward = new AdbForward("64b2b4d9 tcp:555 localabstract:minicap");
            assertAll("验证解析结果",
                    () -> assertTrue(forward.isForward(), "应解析成功"),
                    () -> assertEquals("64b2b4d9", forward.getSerialNumber()),
                    () -> assertEquals(555, forward.getPort()),
                    () -> assertEquals("minicap", forward.getLocalabstract()));
        }

        @Test
        @Order(2)
        @DisplayName("解析不同序列号和端口")
        void testParseDifferentValues() {
            AdbForward forward = new AdbForward("ABC123 tcp:8080 localabstract:myapp");
            assertAll("验证不同值",
                    () -> assertTrue(forward.isForward()),
                    () -> assertEquals("ABC123", forward.getSerialNumber()),
                    () -> assertEquals(8080, forward.getPort()),
                    () -> assertEquals("myapp", forward.getLocalabstract()));
        }

        @Test
        @Order(3)
        @DisplayName("null 字符串解析失败")
        void testParseNull() {
            AdbForward forward = new AdbForward((String) null);
            assertFalse(forward.isForward(), "null 输入应解析失败");
        }

        @Test
        @Order(4)
        @DisplayName("空字符串解析失败")
        void testParseEmpty() {
            AdbForward forward = new AdbForward("");
            assertFalse(forward.isForward(), "空字符串应解析失败");
        }

        @Test
        @Order(5)
        @DisplayName("空白字符串解析失败")
        void testParseBlank() {
            AdbForward forward = new AdbForward("   ");
            assertFalse(forward.isForward(), "空白字符串应解析失败");
        }

        @Test
        @Order(6)
        @DisplayName("字段数不足解析失败")
        void testParseInsufficientFields() {
            AdbForward forward = new AdbForward("SN tcp:555");
            assertFalse(forward.isForward(), "字段数不足应解析失败");
        }

        @Test
        @Order(7)
        @DisplayName("端口号非数字解析失败")
        void testParseInvalidPort() {
            AdbForward forward = new AdbForward("SN tcp:abc localabstract:name");
            assertFalse(forward.isForward(), "端口号非数字应解析失败");
        }

        @Test
        @Order(8)
        @DisplayName("三参数构造函数正确设置字段")
        void testThreeArgConstructor() {
            AdbForward forward = new AdbForward("SN001", 9999, "test_abstract");
            assertAll("验证三参数构造",
                    () -> assertTrue(forward.isForward()),
                    () -> assertEquals("SN001", forward.getSerialNumber()),
                    () -> assertEquals(9999, forward.getPort()),
                    () -> assertEquals("test_abstract", forward.getLocalabstract()));
        }

        @Test
        @Order(9)
        @DisplayName("toString 包含关键信息")
        void testToString() {
            AdbForward forward = new AdbForward("SN_TOSTR", 1234, "abs_name");
            String str = forward.toString();
            assertAll("验证 toString",
                    () -> assertTrue(str.contains("SN_TOSTR"), "应包含序列号"),
                    () -> assertTrue(str.contains("1234"), "应包含端口"),
                    () -> assertTrue(str.contains("abs_name"), "应包含 abstract 名"));
        }

        @Test
        @Order(10)
        @DisplayName("多余空格自动 trim")
        void testParseWithExtraSpaces() {
            AdbForward forward = new AdbForward("  SN001 tcp:555 localabstract:test  ");
            assertAll("验证 trim 后解析",
                    () -> assertTrue(forward.isForward(), "多余空格应能解析成功"),
                    () -> assertEquals("SN001", forward.getSerialNumber()),
                    () -> assertEquals(555, forward.getPort()));
        }
    }

    // ==================== Constant 路径配置代理测试 ====================

    @Nested
    @DisplayName("Constant 路径配置代理测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ConstantTest {

        @Test
        @Order(1)
        @DisplayName("PROP_ABI 和 PROP_SDK 常量值正确")
        void testPropertyConstants() {
            assertEquals("ro.product.cpu.abi", Constant.PROP_ABI);
            assertEquals("ro.build.version.sdk", Constant.PROP_SDK);
        }

        @Test
        @Order(2)
        @DisplayName("getResourceDir 返回非 null 目录")
        void testGetResourceDir() {
            File dir = Constant.getResourceDir();
            assertNotNull(dir, "getResourceDir 不应返回 null");
        }

        @Test
        @Order(3)
        @DisplayName("getDataDir 返回非 null 目录")
        void testGetDataDir() {
            File dir = Constant.getDataDir();
            assertNotNull(dir, "getDataDir 不应返回 null");
        }

        @Test
        @Order(4)
        @DisplayName("getDataCache 返回带文件名的路径")
        void testGetDataCache() {
            File f = Constant.getDataCache("test_file.csv");
            assertNotNull(f, "getDataCache 不应返回 null");
            assertTrue(f.getPath().endsWith("test_file.csv"), "路径应以文件名结尾");
        }

        @Test
        @Order(5)
        @DisplayName("getMinicap 返回正确路径结构")
        void testGetMinicapPath() {
            File f = Constant.getMinicap("arm64-v8a");
            // 若资源目录存在则验证路径结构
            if (f != null) {
                String path = f.getPath().replace('\\', '/');
                assertTrue(path.contains("minicap"), "路径应包含 minicap");
                assertTrue(path.contains("arm64-v8a"), "路径应包含 ABI");
            }
        }

        @Test
        @Order(6)
        @DisplayName("getMinicapSo 返回正确路径结构")
        void testGetMinicapSoPath() {
            File f = Constant.getMinicapSo("arm64-v8a", "30");
            if (f != null) {
                String path = f.getPath().replace('\\', '/');
                assertTrue(path.contains("minicap"), "路径应包含 minicap");
                assertTrue(path.contains("android-30"), "路径应包含 SDK 版本");
                assertTrue(path.contains("arm64-v8a"), "路径应包含 ABI");
                assertTrue(path.endsWith("minicap.so"), "路径应以 minicap.so 结尾");
            }
        }

        @Test
        @Order(7)
        @DisplayName("getMinitouchBin 返回正确路径结构")
        void testGetMinitouchBinPath() {
            File f = Constant.getMinitouchBin("armeabi-v7a");
            if (f != null) {
                String path = f.getPath().replace('\\', '/');
                assertTrue(path.contains("minitouch"), "路径应包含 minitouch");
                assertTrue(path.contains("armeabi-v7a"), "路径应包含 ABI");
            }
        }

        @Test
        @Order(8)
        @DisplayName("getTmpFile 返回临时目录下的文件")
        void testGetTmpFile() {
            File f = Constant.getTmpFile("test_tmp.txt");
            assertNotNull(f, "getTmpFile 不应返回 null");
            String path = f.getPath();
            assertTrue(path.contains("AndroidControl"), "路径应包含 AndroidControl");
            assertTrue(path.endsWith("test_tmp.txt"), "路径应以文件名结尾");
        }
    }

    // ==================== AdbUtils JSON 序列化测试 ====================

    @Nested
    @DisplayName("AdbUtils JSON 序列化测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AdbUtilsTest {

        private IDevice createMockIDevice(String serialNumber) {
            IDevice device = mock(IDevice.class);
            when(device.getSerialNumber()).thenReturn(serialNumber);
            when(device.getProperty("ro.build.version.sdk")).thenReturn("30");
            when(device.getProperty("ro.product.cpu.abi")).thenReturn("arm64-v8a");
            when(device.getProperty("ro.product.model")).thenReturn("TestModel");
            return device;
        }

        @Test
        @Order(1)
        @DisplayName("devices2JSON 空列表返回 []")
        void testDevices2JSONEmptyList() {
            String json = AdbUtils.devices2JSON(Collections.emptyList());
            assertEquals("[]", json, "空列表应返回 []");
        }

        @Test
        @Order(2)
        @DisplayName("devices2JSON 单设备返回 JSON 数组")
        void testDevices2JSONSingleDevice() {
            List<AdbDevice> devices = new ArrayList<>();
            devices.add(new AdbDevice(createMockIDevice("SN001")));
            String json = AdbUtils.devices2JSON(devices);
            assertNotNull(json, "JSON 不应为 null");
            assertTrue(json.startsWith("["), "应以 [ 开头");
            assertTrue(json.endsWith("]"), "应以 ] 结尾");
            assertTrue(json.contains("SN001"), "应包含设备序列号");
        }

        @Test
        @Order(3)
        @DisplayName("devices2JSON 多设备各自出现在 JSON 中")
        void testDevices2JSONMultipleDevices() {
            List<AdbDevice> devices = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                devices.add(new AdbDevice(createMockIDevice("DEV_" + i)));
            }
            String json = AdbUtils.devices2JSON(devices);
            for (int i = 0; i < 3; i++) {
                assertTrue(json.contains("DEV_" + i), "JSON 应包含设备 DEV_" + i);
            }
        }

        @Test
        @Order(4)
        @DisplayName("apps2JSON 空列表返回 []")
        void testApps2JSONEmptyList() {
            String json = AdbUtils.apps2JSON(Collections.emptyList());
            assertEquals("[]", json, "空列表应返回 []");
        }
    }

    // ==================== Console 命令测试 ====================

    @Nested
    @DisplayName("Console 命令注册与执行测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ConsoleCommandTest {

        @Test
        @Order(1)
        @DisplayName("HelpCommand 执行返回可用命令列表")
        void testHelpCommand() throws Exception {
            java.lang.reflect.Constructor<Console.HelpCommand> ctor =
                    Console.HelpCommand.class.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            Console.HelpCommand cmd = ctor.newInstance("help");
            String result = cmd.execute();
            assertNotNull(result, "HelpCommand 不应返回 null");
            assertTrue(result.contains("help"), "应包含 help 命令");
            assertTrue(result.contains("hello"), "应包含 hello 命令");
            assertTrue(result.contains("device"), "应包含 device 命令");
        }

        @Test
        @Order(2)
        @DisplayName("HelloCommand 执行返回问候语")
        void testHelloCommand() throws Exception {
            java.lang.reflect.Constructor<Console.HelloCommand> ctor =
                    Console.HelloCommand.class.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            Console.HelloCommand cmd = ctor.newInstance("hello");
            String result = cmd.execute();
            assertNotNull(result, "HelloCommand 不应返回 null");
            assertFalse(result.isEmpty(), "HelloCommand 结果不应为空");
        }

        @Test
        @Order(3)
        @DisplayName("Command.getCommand 返回命令名")
        void testCommandGetCommand() throws Exception {
            java.lang.reflect.Constructor<Console.HelpCommand> helpCtor =
                    Console.HelpCommand.class.getDeclaredConstructor(String.class);
            helpCtor.setAccessible(true);
            Console.HelpCommand help = helpCtor.newInstance("help");
            assertEquals("help", help.getCommand());

            java.lang.reflect.Constructor<Console.HelloCommand> helloCtor =
                    Console.HelloCommand.class.getDeclaredConstructor(String.class);
            helloCtor.setAccessible(true);
            Console.HelloCommand hello = helloCtor.newInstance("hello");
            assertEquals("hello", hello.getCommand());
        }

        @Test
        @Order(4)
        @DisplayName("Console 单例模式")
        void testConsoleSingleton() {
            Console c1 = Console.getInstance();
            Console c2 = Console.getInstance();
            assertSame(c1, c2, "getInstance 应返回同一实例");
        }

        @Test
        @Order(5)
        @DisplayName("registerCommand 注册自定义命令不抛异常")
        void testRegisterCommand() {
            Console console = Console.getInstance();
            assertDoesNotThrow(() ->
                    console.registerCommand("test", s -> new Console.Command(s) {
                        @Override
                        public String execute() {
                            return "test ok";
                        }
                    }), "注册自定义命令不应抛异常");
        }
    }

    // ==================== WSServer 工具方法测试 ====================

    @Nested
    @DisplayName("WSServer 工具方法测试")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WSServerUtilTest {

        @Test
        @Order(1)
        @DisplayName("toLH 小端字节序转换")
        void testToLH() {
            byte[] result = com.adbtool.server.WSServer.toLH(0x04030201);
            assertAll("验证小端序",
                    () -> assertEquals(0x01, result[0] & 0xff, "byte[0] 应为最低位"),
                    () -> assertEquals(0x02, result[1] & 0xff, "byte[1] 应为次低位"),
                    () -> assertEquals(0x03, result[2] & 0xff, "byte[2] 应为次高位"),
                    () -> assertEquals(0x04, result[3] & 0xff, "byte[3] 应为最高位"));
        }

        @Test
        @Order(2)
        @DisplayName("toHH 大端字节序转换")
        void testToHH() {
            byte[] result = com.adbtool.server.WSServer.toHH(0x04030201);
            assertAll("验证大端序",
                    () -> assertEquals(0x04, result[0] & 0xff, "byte[0] 应为最高位"),
                    () -> assertEquals(0x03, result[1] & 0xff, "byte[1] 应为次高位"),
                    () -> assertEquals(0x02, result[2] & 0xff, "byte[2] 应为次低位"),
                    () -> assertEquals(0x01, result[3] & 0xff, "byte[3] 应为最低位"));
        }

        @Test
        @Order(3)
        @DisplayName("toLH(0) 返回全零数组")
        void testToLHZero() {
            byte[] result = com.adbtool.server.WSServer.toLH(0);
            assertEquals(4, result.length);
            for (byte b : result) {
                assertEquals(0, b, "全零输入应返回全零数组");
            }
        }

        @Test
        @Order(4)
        @DisplayName("toHH(0) 返回全零数组")
        void testToHHZero() {
            byte[] result = com.adbtool.server.WSServer.toHH(0);
            assertEquals(4, result.length);
            for (byte b : result) {
                assertEquals(0, b, "全零输入应返回全零数组");
            }
        }

        @Test
        @Order(5)
        @DisplayName("toLH 和 toHH 对同一值结果互为反转")
        void testToLHAndToHHReversed() {
            int value = 0xAABBCCDD;
            byte[] lh = com.adbtool.server.WSServer.toLH(value);
            byte[] hh = com.adbtool.server.WSServer.toHH(value);
            for (int i = 0; i < 4; i++) {
                assertEquals(lh[i], hh[3 - i],
                        "小端 byte[" + i + "] 应等于大端 byte[" + (3 - i) + "]");
            }
        }

        @Test
        @Order(6)
        @DisplayName("ImageData 字段可通过反射验证")
        void testImageDataFields() throws Exception {
            // ImageData 构造函数为包级私有，通过反射创建实例验证字段
            Class<?> clazz = com.adbtool.server.WSServer.ImageData.class;
            java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor(byte[].class);
            ctor.setAccessible(true);
            byte[] data = new byte[]{10, 20, 30};
            long before = System.currentTimeMillis();
            Object img = ctor.newInstance((Object) data);
            long after = System.currentTimeMillis();

            java.lang.reflect.Field dataField = clazz.getDeclaredField("data");
            dataField.setAccessible(true);
            java.lang.reflect.Field tsField = clazz.getDeclaredField("timesp");
            tsField.setAccessible(true);

            assertAll("验证 ImageData",
                    () -> assertArrayEquals(data, (byte[]) dataField.get(img), "data 应一致"),
                    () -> assertTrue((long) tsField.get(img) >= before && (long) tsField.get(img) <= after,
                            "时间戳应在合理范围内"));
        }
    }
}
