package com.system;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * ConfigUtils、CryptoUtils 与 WindowsCredentialUtils 的集成测试。
 *
 * 测试覆盖范围：
 * <ul>
 *   <li>配置文件的生成与读取（临时目录与真实 ../config 目录）</li>
 *   <li>配置文件格式异常（非法 JSON、空文件、缺失文件）</li>
 *   <li>路径格式与不存在目录的异常处理</li>
 *   <li>CryptoUtils 加解密、ENC(...) 配置格式、resolve 多种解析策略</li>
 *   <li>ConfigUtils 从 JSON / Properties 提取登录信息并自动解析密码</li>
 *   <li>ConfigLoader 对明文、ENC、ENV、WINCRED 密码的兼容性</li>
 * </ul>
 */
public class configTest {

    @TempDir
    Path tempDir;

    // ==================== 通用辅助方法 ====================

    /**
     * 在指定目录写入标准 global.json。
     *
     * @param dir           目标目录
     * @param jsonFile      jsonFile 字段值
     * @param propFilePath  propFilePath 字段值
     * @param pomFilepath   pomFilepath 字段值
     * @throws IOException 写入失败
     */
    private void writeGlobalConfig(Path dir, String jsonFile, String propFilePath, String pomFilepath) throws IOException {
        String content = "{\n" +
                "  \"jsonFile\": \"" + jsonFile + "\",\n" +
                "  \"propFilePath\": \"" + propFilePath + "\",\n" +
                "  \"pomFilepath\": \"" + pomFilepath + "\"\n" +
                "}";
        Files.write(dir.resolve(ConfigUtils.DEFAULT_GLOBAL_JSON), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 在指定目录写入 databases.json，支持自定义密码。
     * <p>
     * password 可以是明文、ENC(...)、${ENV:VAR} 或 ${WINCRED:Target}。
     *
     * @param dir      目标目录
     * @param username 数据库用户名
     * @param password 密码（支持多种解析格式）
     * @throws IOException 写入失败
     */
    private void writeDbConfig(Path dir, String username, String password) throws IOException {
        String content = "{\n" +
                "  \"url\": \"jdbc:mysql://localhost:3306/test\",\n" +
                "  \"username\": \"" + username + "\",\n" +
                "  \"password\": \"" + password + "\",\n" +
                "  \"driver\": \"com.mysql.cj.jdbc.Driver\"\n" +
                "}";
        Files.write(dir.resolve(ConfigUtils.DEFAULT_DATABASES_JSON), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 通用断言：验证 ConfigLoader 成功加载且 global 字段符合预期。
     *
     * @param loader        已初始化的 ConfigLoader
     * @param jsonFile      期望的 jsonFile 值
     * @param propFilePath  期望的 propFilePath 值
     * @param pomFilepath   期望的 pomFilepath 值
     */
    private void assertGlobalDataMatches(ConfigUtils.ConfigLoader loader,
                                         String jsonFile, String propFilePath, String pomFilepath) {
        assertAll("验证全局配置",
                () -> assertNotNull(loader.getGlobalData(), "globalData 不应为 null"),
                () -> assertNotNull(loader.getLoginData(), "loginData 不应为 null"),
                () -> assertEquals(jsonFile, loader.getGlobalData().get("jsonFile")),
                () -> assertEquals(propFilePath, loader.getGlobalData().get("propFilePath")),
                () -> assertEquals(pomFilepath, loader.getGlobalData().get("pomFilepath")));
    }

    /**
     * 现有配置文件加载核心逻辑。
     *
     * @param configDir       配置目录
     * @param expectedPassword 期望解析后的密码；null 表示不校验密码
     * @param label           测试场景描述（用于断言消息）
     */
    private void runExistingConfigTest(String configDir, String expectedPassword, String label) {
        ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);
        assertGlobalDataMatches(loader, "config/databases.json", "dbprop/db.properties", "config/pom.xml");
        if (expectedPassword != null) {
            assertEquals(expectedPassword, loader.getLoginData().get("password"),
                    label + "：password 解析值应与预期一致");
        }
    }

    // ==================== ConfigLoader 基础测试 ====================

    @Test
    @DisplayName("ConfigLoader 使用临时 global.json 与 databases.json 成功加载")
    void testConfigLoaderWithTempFiles() throws IOException {
        writeGlobalConfig(tempDir, "a", "b", "c");
        writeDbConfig(tempDir, "root", "secret");

        ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());

        assertAll("验证加载结果",
                () -> assertNotNull(loader.getGlobalData(), "globalData 不应为 null"),
                () -> assertNotNull(loader.getLoginData(), "loginData 不应为 null"));
    }

    @Test
    @DisplayName("global.json 非法 JSON 时抛出 IllegalStateException 并保留 JSONException 原因")
    void testGlobalFileInvalidJson_throwsIllegalStateExceptionWithCause() throws IOException {
        Path globalPath = tempDir.resolve(ConfigUtils.DEFAULT_GLOBAL_JSON);
        Files.write(globalPath, "{ invalid json }".getBytes(StandardCharsets.UTF_8));
        writeDbConfig(tempDir, "root", "secret");

        Exception ex = assertThrows(IllegalStateException.class, () ->
                new ConfigUtils.ConfigLoader(tempDir.toString()));

        assertAll("验证异常信息",
                () -> assertTrue(ex.getMessage().contains("全局配置文件加载失败")),
                () -> assertNotNull(ex.getCause()),
                () -> assertInstanceOf(JSONException.class, ex.getCause()));
    }

    @Test
    @DisplayName("global.json 为空文件时抛出 IllegalStateException")
    void testGlobalFileEmpty_throwsIllegalStateException() throws IOException {
        Files.createFile(tempDir.resolve(ConfigUtils.DEFAULT_GLOBAL_JSON));
        writeDbConfig(tempDir, "root", "secret");

        Exception ex = assertThrows(IllegalStateException.class, () ->
                new ConfigUtils.ConfigLoader(tempDir.toString()));

        assertAll("验证异常信息",
                () -> assertTrue(ex.getMessage().contains("全局配置文件加载失败")),
                () -> assertNotNull(ex.getCause()),
                () -> assertInstanceOf(IllegalArgumentException.class, ex.getCause()));
    }

    @Test
    @DisplayName("global.json 缺失时抛出 IllegalStateException")
    void testGlobalFileMissing_throwsIllegalStateException() throws IOException {
        writeDbConfig(tempDir, "root", "secret");

        Exception ex = assertThrows(IllegalStateException.class, () ->
                new ConfigUtils.ConfigLoader(tempDir.toString()));

        assertAll("验证异常信息",
                () -> assertNotNull(ex.getMessage()),
                () -> assertTrue(ex.getMessage().contains("全局配置文件加载失败")),
                () -> assertNotNull(ex.getCause()),
                () -> assertInstanceOf(IOException.class, ex.getCause()));
    }

    @Test
    @DisplayName("三种路径格式下配置目录缺失均抛出 IllegalStateException")
    void testPathProperty() {
        Path relativeParent = Paths.get("../__nonexistent_config_test__");
        Path relativeCurrent = Paths.get("./__nonexistent_config_test__");
        Path noPrefix = Paths.get("__nonexistent_config_test__");

        for (Path p : java.util.Arrays.asList(relativeParent, relativeCurrent, noPrefix)) {
            Exception ex = assertThrows(IllegalStateException.class, () ->
                    new ConfigUtils.ConfigLoader(p.toString()));
            assertAll("验证路径 " + p,
                    () -> assertNotNull(ex.getCause()),
                    () -> assertNotNull(ex.getMessage()),
                    () -> assertTrue(ex.getMessage().contains("配置文件")),
                    () -> assertInstanceOf(IOException.class, ex.getCause()));
        }
    }

    @Test
    @DisplayName("现有配置文件加载：真实路径失败时自动回退到临时路径")
    void testExistingConfig() throws IOException {
        boolean realPathSucceeded = false;
        try {
            // 优先使用真实 ../config 目录，验证生产配置格式
            runExistingConfigTest(ConfigUtils.DEFAULT_CONFIG_DIR, null, "真实路径");
            realPathSucceeded = true;
        } catch (Exception | AssertionError e) {
            System.err.println("真实路径测试失败，自动切换到临时路径继续验证。原因："
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        if (!realPathSucceeded) {
            // 回退到临时配置，保证 CI/新环境也能通过核心流程验证
            writeGlobalConfig(tempDir, "config/databases.json", "dbprop/db.properties", "config/pom.xml");
            writeDbConfig(tempDir, "root", "test_password");
            runExistingConfigTest(tempDir.toString(), "test_password", "临时路径");
        }
    }

    @Test
    @DisplayName("globalData 中的 jsonFile 路径可用于后续文件操作")
    void testGlobalDataPathsCanBeUsedForFurtherOperations() throws IOException {
        writeGlobalConfig(tempDir, "test_output.json", "b", "c");
        writeDbConfig(tempDir, "root", "secret");

        ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
        String jsonFilePath = (String) loader.getGlobalData().get("jsonFile");
        assertNotNull(jsonFilePath, "jsonFile 路径不应为 null");

        Path targetFile = tempDir.resolve(jsonFilePath);
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        Files.createFile(targetFile);
        assertTrue(Files.exists(targetFile));
    }

    @Test
    @DisplayName("不存在的目录应抛出 IllegalStateException")
    void testConfigLoaderWithNonExistingDir() {
        assertThrows(IllegalStateException.class, () ->
                new ConfigUtils.ConfigLoader("non_existing_dir"));
    }

    // ==================== CryptoUtils 测试 ====================

    @Nested
    @DisplayName("CryptoUtils 加解密与 resolve 测试")
    class CryptoUtilsTest {

        @Test
        @DisplayName("AES 加密后解密应还原原文")
        void testEncryptDecryptRoundTrip() {
            String plaintext = "mySecretPassword1234";
            String encrypted = CryptoUtils.encrypt(plaintext);
            assertAll("验证加解密一致性",
                    () -> assertNotNull(encrypted, "加密结果不应为 null"),
                    () -> assertNotEquals(plaintext, encrypted, "密文不应与明文相同"),
                    () -> assertEquals(plaintext, CryptoUtils.decrypt(encrypted), "解密后应与原文一致"));
        }

        @Test
        @DisplayName("不同明文加密后密文应不同")
        void testDifferentPlaintextProducesDifferentCiphertext() {
            String enc1 = CryptoUtils.encrypt("password1");
            String enc2 = CryptoUtils.encrypt("password2");
            assertNotEquals(enc1, enc2, "不同明文的密文应不同");
        }

        @Test
        @DisplayName("相同明文两次加密结果应不同（IV 随机）")
        void testSamePlaintextProducesDifferentCiphertext() {
            String enc1 = CryptoUtils.encrypt("same_password");
            String enc2 = CryptoUtils.encrypt("same_password");
            assertAll("验证随机性",
                    () -> assertNotEquals(enc1, enc2, "由于 IV 随机，相同明文加密结果应不同"),
                    () -> assertEquals("same_password", CryptoUtils.decrypt(enc1)),
                    () -> assertEquals("same_password", CryptoUtils.decrypt(enc2)));
        }

        @Test
        @DisplayName("中文及特殊字符加解密应正确还原")
        void testEncryptDecryptChineseAndSpecialChars() {
            String plaintext = "密码测试!@#$%^&*()";
            String encrypted = CryptoUtils.encrypt(plaintext);
            assertEquals(plaintext, CryptoUtils.decrypt(encrypted), "中文和特殊字符加解密应正确还原");
        }

        @Test
        @DisplayName("空字符串加解密应还原为空字符串")
        void testEncryptDecryptEmptyString() {
            String encrypted = CryptoUtils.encrypt("");
            assertEquals("", CryptoUtils.decrypt(encrypted), "空字符串加解密应还原为空字符串");
        }

        @Test
        @DisplayName("encryptForConfig 生成 ENC(...) 格式")
        void testEncryptForConfigFormat() {
            String result = CryptoUtils.encryptForConfig("test123");
            assertAll("验证 ENC 格式",
                    () -> assertTrue(result.startsWith("ENC("), "应以 ENC( 开头"),
                    () -> assertTrue(result.endsWith(")"), "应以 ) 结尾"),
                    () -> assertFalse(result.substring(4, result.length() - 1).isEmpty(), "ENC() 内部密文不应为空"));
        }

        @Test
        @DisplayName("resolve 明文直接返回")
        void testResolvePlainText() {
            assertEquals("123456", CryptoUtils.resolve("123456"));
            assertEquals("plainPassword", CryptoUtils.resolve("plainPassword"));
        }

        @Test
        @DisplayName("resolve null 返回 null")
        void testResolveNull() {
            assertNull(CryptoUtils.resolve(null));
        }

        @Test
        @DisplayName("resolve 空字符串返回空字符串")
        void testResolveEmpty() {
            assertEquals("", CryptoUtils.resolve(""));
        }

        @Test
        @DisplayName("resolve ENC(...) 格式自动解密")
        void testResolveEncryptedFormat() {
            String plaintext = "myDbPassword";
            String encrypted = CryptoUtils.encryptForConfig(plaintext);
            assertEquals(plaintext, CryptoUtils.resolve(encrypted), "resolve 应自动解密 ENC() 格式的值");
        }

        @Test
        @DisplayName("resolve ENC(...) 带前后空格也能正确解密")
        void testResolveEncryptedWithWhitespace() {
            String plaintext = "trimmedPassword";
            String encrypted = "  " + CryptoUtils.encryptForConfig(plaintext) + "  ";
            assertEquals(plaintext, CryptoUtils.resolve(encrypted), "带空格的 ENC() 值也应正确解密");
        }

        @Test
        @DisplayName("resolve ${ENV:PATH} 解析已有环境变量")
        void testResolveEnvVarExisting() {
            String pathValue = System.getenv("PATH");
            assumeTrue(pathValue != null, "PATH 环境变量应存在");
            assertEquals(pathValue, CryptoUtils.resolve("${ENV:PATH}"), "resolve 应返回 PATH 环境变量的值");
        }

        @Test
        @DisplayName("resolve ${ENV:不存在的变量} 抛出异常")
        void testResolveEnvVarNonExistent() {
            String fakeVar = "NOT_EXIST_VAR_" + System.currentTimeMillis();
            Exception ex = assertThrows(RuntimeException.class, () ->
                    CryptoUtils.resolve("${ENV:" + fakeVar + "}"));
            assertTrue(ex.getMessage().contains(fakeVar), "异常信息应包含变量名");
        }

        @Test
        @DisplayName("resolve 损坏的 Base64 密文抛出异常")
        void testResolveCorruptedCiphertext() {
            assertThrows(RuntimeException.class, () ->
                    CryptoUtils.resolve("ENC(!!!invalid_base64!!!)"));
        }

        @Test
        @DisplayName("resolve 过短的密文抛出异常")
        void testResolveTooShortCiphertext() {
            String shortBase64 = java.util.Base64.getEncoder().encodeToString(new byte[5]);
            assertThrows(RuntimeException.class, () ->
                    CryptoUtils.resolve("ENC(" + shortBase64 + ")"));
        }

        @Test
        @DisplayName("resolve ${WINCRED:不存在的凭据} 抛出异常")
        void testResolveWinCredNonExistent() {
            assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"),
                    "仅在 Windows 系统测试 Windows 凭据管理器");
            String fakeTarget = "NOT_EXIST_WINCRED_" + System.currentTimeMillis();
            Exception ex = assertThrows(RuntimeException.class, () ->
                    CryptoUtils.resolve("${WINCRED:" + fakeTarget + "}"));
            assertTrue(ex.getMessage().contains("读取 Windows 凭据失败"), "异常信息应说明凭据读取失败");
        }

        @Test
        @DisplayName("WindowsCredentialUtils 写入、读取并删除凭据")
        void testWinCredWriteReadDelete() {
            assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"),
                    "仅在 Windows 系统测试 Windows 凭据管理器");
            String target = "AUXILIARY_TEST_CRED_" + System.currentTimeMillis();
            try {
                WindowsCredentialUtils.writePassword(target, "testUser", "testPassword123");
                String password = WindowsCredentialUtils.readPassword(target);
                assertEquals("testPassword123", password, "读取的密码应与写入的一致");

                String resolved = CryptoUtils.resolve("${WINCRED:" + target + "}");
                assertEquals("testPassword123", resolved, "CryptoUtils.resolve 应能解析 WINCRED 凭据");
            } catch (Exception e) {
                fail("WINCRED 测试失败: " + e.getMessage(), e);
            } finally {
                try {
                    WindowsCredentialUtils.deletePassword(target);
                } catch (Exception ignored) {
                    // 清理失败不影响测试结果
                }
            }
        }
    }

    // ==================== ConfigUtils 密码解析集成测试 ====================

    @Nested
    @DisplayName("ConfigUtils 密码安全解析集成测试")
    class ConfigUtilsPasswordTest {

        @Test
        @DisplayName("extractLoginData 明文密码向后兼容")
        void testExtractLoginDataPlainTextPassword() {
            JSONObject dbConfig = new JSONObject();
            dbConfig.put("url", "jdbc:mysql://localhost:3306/test");
            dbConfig.put("username", "root");
            dbConfig.put("password", "plainPwd");
            dbConfig.put("driver", "com.mysql.cj.jdbc.Driver");

            Map<String, Object> data = ConfigUtils.extractLoginData(dbConfig);
            assertAll("验证解析结果",
                    () -> assertEquals("plainPwd", data.get("password"), "明文密码应原样返回"),
                    () -> assertEquals("root", data.get("username")),
                    () -> assertEquals("jdbc:mysql://localhost:3306/test", data.get("url")),
                    () -> assertEquals("com.mysql.cj.jdbc.Driver", data.get("driver")));
        }

        @Test
        @DisplayName("extractLoginData ENC() 加密密码自动解密")
        void testExtractLoginDataEncryptedPassword() {
            String originalPassword = "secretDbPwd_2024";
            String encrypted = CryptoUtils.encryptForConfig(originalPassword);

            JSONObject dbConfig = new JSONObject();
            dbConfig.put("url", "jdbc:mysql://localhost:3306/prod");
            dbConfig.put("username", "admin");
            dbConfig.put("password", encrypted);
            dbConfig.put("driver", "com.mysql.cj.jdbc.Driver");

            Map<String, Object> data = ConfigUtils.extractLoginData(dbConfig);
            assertAll("验证解密结果",
                    () -> assertEquals(originalPassword, data.get("password"), "ENC() 密码应自动解密为原文"),
                    () -> assertEquals("admin", data.get("username")));
        }

        @Test
        @DisplayName("extractLoginData 环境变量引用自动解析")
        void testExtractLoginDataEnvPassword() {
            JSONObject dbConfig = new JSONObject();
            dbConfig.put("url", "jdbc:mysql://localhost:3306/test");
            dbConfig.put("username", "root");
            dbConfig.put("password", "${ENV:PATH}");
            dbConfig.put("driver", "com.mysql.cj.jdbc.Driver");

            Map<String, Object> data = ConfigUtils.extractLoginData(dbConfig);
            assertEquals(System.getenv("PATH"), data.get("password"), "${ENV:PATH} 应被解析为实际值");
        }

        @Test
        @DisplayName("extractLoginData 缺少 password 字段返回空字符串")
        void testExtractLoginDataMissingPassword() {
            JSONObject dbConfig = new JSONObject();
            dbConfig.put("url", "jdbc:mysql://localhost:3306/test");
            dbConfig.put("username", "root");
            dbConfig.put("driver", "com.mysql.cj.jdbc.Driver");

            Map<String, Object> data = ConfigUtils.extractLoginData(dbConfig);
            assertEquals("", data.get("password"), "缺少 password 字段应返回空字符串");
        }

        @Test
        @DisplayName("extractLoginDataFromProperties 明文密码向后兼容")
        void testExtractLoginDataFromPropertiesPlainText() {
            Properties props = new Properties();
            props.setProperty("url", "jdbc:mysql://localhost:3306/test");
            props.setProperty("username", "root");
            props.setProperty("password", "propPlainPwd");
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");

            Map<String, Object> data = ConfigUtils.extractLoginDataFromProperties(props);
            assertEquals("propPlainPwd", data.get("password"), "Properties 明文密码应原样返回");
        }

        @Test
        @DisplayName("extractLoginDataFromProperties ENC() 加密密码自动解密")
        void testExtractLoginDataFromPropertiesEncrypted() {
            String originalPassword = "propSecretPwd";
            String encrypted = CryptoUtils.encryptForConfig(originalPassword);

            Properties props = new Properties();
            props.setProperty("url", "jdbc:mysql://localhost:3306/prod");
            props.setProperty("username", "admin");
            props.setProperty("password", encrypted);
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");

            Map<String, Object> data = ConfigUtils.extractLoginDataFromProperties(props);
            assertEquals(originalPassword, data.get("password"), "Properties 中 ENC() 密码应自动解密");
        }

        @Test
        @DisplayName("ConfigLoader 加载 ENC() 加密密码的 databases.json")
        void testConfigLoaderWithEncryptedPassword() throws IOException {
            writeGlobalConfig(tempDir, "a", "b", "c");
            String originalPassword = "loaderSecretPwd";
            String encryptedPassword = CryptoUtils.encryptForConfig(originalPassword);
            writeDbConfig(tempDir, "encUser", encryptedPassword);

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            Map<String, Object> loginData = loader.getLoginData();

            assertAll("验证解密加载结果",
                    () -> assertNotNull(loginData, "loginData 不应为 null"),
                    () -> assertEquals(originalPassword, loginData.get("password"),
                            "ConfigLoader 应自动解密 ENC() 格式的密码"),
                    () -> assertEquals("encUser", loginData.get("username")));
        }

        @Test
        @DisplayName("ConfigLoader 加载明文密码的 databases.json（向后兼容）")
        void testConfigLoaderWithPlainTextPassword() throws IOException {
            writeGlobalConfig(tempDir, "a", "b", "c");
            writeDbConfig(tempDir, "root", "plainText123");

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            Map<String, Object> loginData = loader.getLoginData();

            assertEquals("plainText123", loginData.get("password"),
                    "ConfigLoader 对明文密码应原样返回（向后兼容）");
        }

        @Test
        @DisplayName("ConfigLoader 加载 ${ENV:...} 环境变量密码")
        void testConfigLoaderWithEnvPassword() throws IOException {
            assumeTrue(System.getenv("PATH") != null, "PATH 环境变量应存在");
            writeGlobalConfig(tempDir, "a", "b", "c");
            writeDbConfig(tempDir, "root", "${ENV:PATH}");

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            Map<String, Object> loginData = loader.getLoginData();

            assertEquals(System.getenv("PATH"), loginData.get("password"),
                    "ConfigLoader 应解析 ${ENV:PATH} 环境变量密码");
        }

        @Test
        @DisplayName("ConfigLoader 加载 ${WINCRED:...} Windows 凭据密码")
        void testConfigLoaderWithWinCredPassword() throws IOException {
            assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"),
                    "仅在 Windows 系统测试 Windows 凭据管理器");
            String target = "AUXILIARY_TEST_CRED_" + System.currentTimeMillis();
            String password = "winCredLoaderPwd";
            try {
                WindowsCredentialUtils.writePassword(target, "root", password);
                writeGlobalConfig(tempDir, "a", "b", "c");
                writeDbConfig(tempDir, "root", "${WINCRED:" + target + "}");

                ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
                Map<String, Object> loginData = loader.getLoginData();

                assertEquals(password, loginData.get("password"),
                        "ConfigLoader 应解析 ${WINCRED:...} 凭据密码");
            } finally {
                try {
                    WindowsCredentialUtils.deletePassword(target);
                } catch (Exception ignored) {
                    // 清理失败不影响测试结果
                }
            }
        }

        @Test
        @DisplayName("jsonToProperties 保留加密格式不提前解密")
        void testJsonToPropertiesPreservesEncryptedFormat() {
            String encrypted = CryptoUtils.encryptForConfig("jsonPropPwd");
            JSONObject json = new JSONObject();
            json.put("password", encrypted);
            json.put("username", "testUser");

            Properties props = ConfigUtils.jsonToProperties(json);
            assertEquals(encrypted, props.getProperty("password"),
                    "jsonToProperties 应保留加密格式原样，不提前解密");

            props.setProperty("url", "jdbc:mysql://localhost/test");
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");
            Map<String, Object> data = ConfigUtils.extractLoginDataFromProperties(props);
            assertEquals("jsonPropPwd", data.get("password"),
                    "通过 extractLoginDataFromProperties 应解密 ENC() 密码");
        }
    }
}
