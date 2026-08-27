package com.aitool.deepseek;

import com.aitool.deepseek.model.*;
import com.system.ConfigUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DeepSeek AI 完整链路集成测试
 * <p>
 * 测试覆盖全链路：
 * <ol>
 *   <li>配置读取 - ConfigUtils.ConfigLoader 从 global.json 加载 DeepSeek 配置</li>
 *   <li>请求参数加载 - DeepSeekRequestLoader 从 deepseek_request.json 加载预设</li>
 *   <li>API 调用 - DeepSeekTool 发送真实请求并获取响应</li>
 *   <li>日志缓存 - ChatLogStorage 将对话记录持久化到 XML</li>
 * </ol>
 * </p>
 * <p>
 * 注意：需要设置环境变量 DEEPSEEK_API_KEY 或 global.json 中配置有效 apiKey 才能运行。
 * 未配置 API Key 时所有测试自动跳过 (assumeTrue)。
 * </p>
 */
public class DeepSeekLiveTest {

    /** 真实 API Key (从环境变量或 global.json 获取) */
    private static String resolvedApiKey;

    /** API 是否可用 (连通性预检通过) */
    private static boolean apiAvailable = false;

    /** API 不可用原因 */
    private static String apiSkipReason = "";

    /** 是否已完成初始化 */
    private static boolean initialized = false;

    @TempDir
    Path tempDir;

    // ==================== 全链路前置检查 ====================

    /**
     * 解析 API Key，按优先级依次尝试多种获取途径：
     * <ol>
     *   <li>环境变量 DEEPSEEK_API_KEY</li>
     *   <li>JVM 系统属性 -Ddeepseek.api.key 或 -DDEEPSEEK_API_KEY</li>
     *   <li>global.json 配置 (通过 CryptoUtils.resolve 解析，支持明文/ENC/ENV/WINCRED)</li>
     * </ol>
     * 如果都无法获取有效 Key，则跳过所有测试。
     */
    @BeforeAll
    static void resolveApiKey() {
        // 1. 优先从环境变量获取
        String envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            resolvedApiKey = envKey;
            System.out.println("[DeepSeekLiveTest] 从环境变量 DEEPSEEK_API_KEY 获取 API Key");
            return;
        }

        // 2. 从 JVM 系统属性获取 (-Ddeepseek.api.key=xxx 或 -DDEEPSEEK_API_KEY=xxx)
        String sysPropKey = System.getProperty("deepseek.api.key");
        if (sysPropKey == null || sysPropKey.isEmpty()) {
            sysPropKey = System.getProperty("DEEPSEEK_API_KEY");
        }
        if (sysPropKey != null && !sysPropKey.isEmpty()) {
            resolvedApiKey = sysPropKey;
            System.out.println("[DeepSeekLiveTest] 从系统属性获取 API Key");
            return;
        }

        // 3. 从 global.json 配置获取 (通过 CryptoUtils 解析)
        try {
            String configDir = ConfigUtils.resolveDefaultConfigDir();
            Path globalPath = Paths.get(configDir, ConfigUtils.DEFAULT_GLOBAL_JSON);
            if (Files.exists(globalPath)) {
                String content = new String(Files.readAllBytes(globalPath), StandardCharsets.UTF_8);
                org.json.JSONObject json = new org.json.JSONObject(content);
                org.json.JSONObject deepseek = json.optJSONObject("deepseek");
                if (deepseek != null) {
                    String rawKey = deepseek.optString("apiKey", "");
                    if (!rawKey.isEmpty()) {
                        // 如果 rawKey 是 ${ENV:XXX} 格式，先检查对应环境变量是否存在
                        if (rawKey.startsWith("${ENV:")) {
                            String envVarName = rawKey.substring(6, rawKey.indexOf('}'));
                            String envValue = System.getenv(envVarName);
                            if (envValue != null && !envValue.isEmpty()) {
                                resolvedApiKey = envValue;
                                System.out.println("[DeepSeekLiveTest] 从 global.json 引用环境变量 " + envVarName + " 获取 API Key");
                                return;
                            }
                            System.out.println("[DeepSeekLiveTest] global.json 引用环境变量 " + envVarName + "，但该变量未设置");
                        } else {
                            // 尝试通过 CryptoUtils 解析 (支持 ENC() 等)
                            try {
                                String resolved = com.system.CryptoUtils.resolve(rawKey);
                                if (resolved != null && !resolved.isEmpty()
                                        && !resolved.startsWith("${ENV:")
                                        && !resolved.startsWith("${WINCRED:")
                                        && !resolved.startsWith("ENC(")) {
                                    resolvedApiKey = resolved;
                                    System.out.println("[DeepSeekLiveTest] 从 global.json 配置获取 API Key");
                                    return;
                                }
                            } catch (Exception resolveEx) {
                                System.out.println("[DeepSeekLiveTest] CryptoUtils.resolve 解析失败: " + resolveEx.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DeepSeekLiveTest] 从 global.json 获取 API Key 失败: " + e.getMessage());
        }

        // 4. 无有效 Key，打印诊断信息并跳过所有测试
        resolvedApiKey = null;
        System.out.println("[DeepSeekLiveTest] 未找到有效的 API Key，所有测试将跳过");
        System.out.println("[DeepSeekLiveTest] 诊断信息:");
        System.out.println("  - 环境变量 DEEPSEEK_API_KEY: " + (System.getenv("DEEPSEEK_API_KEY") != null ? "已设置" : "未设置"));
        System.out.println("  - 系统属性 deepseek.api.key: " + (System.getProperty("deepseek.api.key") != null ? "已设置" : "未设置"));
        System.out.println("[DeepSeekLiveTest] 可通过以下方式设置 API Key:");
        System.out.println("  1. PowerShell: $env:DEEPSEEK_API_KEY = \"sk-xxx\" (需先停止 Gradle Daemon: .\\gradlew.bat --stop)");
        System.out.println("  2. Gradle 参数: .\\gradlew.bat test -Ddeepseek.api.key=sk-xxx");
        System.out.println("  3. global.json: 直接写入明文 apiKey 或使用 ENC() 加密");
        return;
    }

    /**
     * API 连通性预检：发送最小请求验证 API Key 有效性和账户余额。
     * 在 @BeforeAll 中执行，确保只对 API 做一次探测请求。
     */
    @BeforeAll
    static void preflightCheck() {
        if (resolvedApiKey == null || resolvedApiKey.isEmpty()) {
            return; // 无 Key，直接跳过
        }

        try {
            DeepSeekTool.init(resolvedApiKey);
            // 发送最小请求验证 API 连通性和账户状态
            String testAnswer = DeepSeekTool.ask("hi");
            if (testAnswer != null && !testAnswer.isEmpty()) {
                apiAvailable = true;
                System.out.println("[DeepSeekLiveTest] API 连通性预检通过，账户状态正常");
            } else {
                apiAvailable = false;
                apiSkipReason = "API 返回空响应";
                System.out.println("[DeepSeekLiveTest] " + apiSkipReason);
            }
        } catch (Exception e) {
            apiAvailable = false;
            String msg = e.getMessage();
            if (msg != null && msg.contains("402")) {
                apiSkipReason = "账户余额不足 (HTTP 402)，请充值后重试";
            } else if (msg != null && msg.contains("401")) {
                apiSkipReason = "API Key 无效 (HTTP 401)，请检查 Key 是否正确";
            } else if (msg != null && (msg.contains("connect") || msg.contains("timeout") || msg.contains("UnknownHost"))) {
                apiSkipReason = "无法连接 DeepSeek API，请检查网络: " + msg;
            } else {
                apiSkipReason = "API 预检失败: " + msg;
            }
            System.out.println("[DeepSeekLiveTest] " + apiSkipReason);
        } finally {
            DeepSeekTool.shutdown();
            initialized = false;
        }
    }

    /**
     * 初始化 DeepSeekTool 并进行 API 连通性预检。
     * 如果 API Key 无效或账户余额不足，设置 apiAvailable=false 并优雅跳过后续测试。
     */
    @BeforeEach
    void initClient() {
        assumeTrue(resolvedApiKey != null && !resolvedApiKey.isEmpty(),
                "未配置有效的 DEEPSEEK_API_KEY，跳过真实 API 测试");
        assumeTrue(apiAvailable, "API 不可用: " + apiSkipReason);

        if (!DeepSeekTool.isInitialized()) {
            DeepSeekTool.init(resolvedApiKey);
            initialized = true;
        }
    }

    @AfterAll
    static void cleanup() {
        if (DeepSeekTool.isInitialized()) {
            DeepSeekTool.shutdown();
        }
        DeepSeekRequestLoader.clearCache();
    }

    // ==================== 1. 配置读取链路测试 ====================

    @Nested
    @DisplayName("1. 配置读取链路测试")
    class ConfigLoadingChainTest {

        @Test
        @DisplayName("ConfigLoader 从生产 global.json 加载 DeepSeek 配置")
        void testConfigLoaderLoadDeepSeek() {
            assumeTrue(resolvedApiKey != null, "无有效 API Key，跳过");

            String configDir = ConfigUtils.resolveDefaultConfigDir();
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);

            assertTrue(loader.isDeepSeekConfigured(), "DeepSeek 配置应完整");
            assertNotNull(loader.getDeepSeekApiKey(), "apiKey 不应为 null");
            assertFalse(loader.getDeepSeekApiKey().isEmpty(), "apiKey 不应为空");
            assertNotNull(loader.getDeepSeekModel(), "model 不应为 null");
            assertNotNull(loader.getDeepSeekBaseUrl(), "baseUrl 不应为 null");
        }

        @Test
        @DisplayName("ConfigLoader 配置字段值合法性验证")
        void testConfigFieldValues() {
            assumeTrue(resolvedApiKey != null, "无有效 API Key，跳过");

            String configDir = ConfigUtils.resolveDefaultConfigDir();
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);

            // 模型名称应为已知枚举之一
            String model = loader.getDeepSeekModel();
            assertNotNull(model);
            assertTrue(model.startsWith("deepseek-"), "模型名应以 deepseek- 开头: " + model);

            // baseUrl 应为合法 URL
            String baseUrl = loader.getDeepSeekBaseUrl();
            assertTrue(baseUrl.startsWith("http"), "baseUrl 应为合法 URL: " + baseUrl);

            // reasoningEffort 应为 low/medium/high 之一
            String effort = loader.getDeepSeekReasoningEffort();
            assertTrue("low".equals(effort) || "medium".equals(effort) || "high".equals(effort),
                    "reasoningEffort 应为 low/medium/high: " + effort);
        }

        @Test
        @DisplayName("DeepSeekTool.initFromConfig() 从 ConfigLoader 初始化")
        void testInitFromConfigLoader() {
            assumeTrue(resolvedApiKey != null, "无有效 API Key，跳过");

            // 先关闭之前的客户端
            DeepSeekTool.shutdown();

            String configDir = ConfigUtils.resolveDefaultConfigDir();
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);

            DeepSeekTool.initFromConfig(loader);
            assertTrue(DeepSeekTool.isInitialized(), "从 ConfigLoader 初始化应成功");
        }
    }

    // ==================== 2. 请求参数加载链路测试 ====================

    @Nested
    @DisplayName("2. 请求参数加载链路测试")
    class RequestLoadingChainTest {

        @BeforeEach
        void clearCache() {
            DeepSeekRequestLoader.clearCache();
        }

        @Test
        @DisplayName("从生产 deepseek_request.json 加载预设参数")
        void testLoadProductionRequestConfig() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            org.json.JSONObject config = DeepSeekRequestLoader.loadRequestConfig(prodFile);
            assertNotNull(config);
            assertTrue(config.has("default"), "应包含 default 预设");
            assertTrue(config.has("presets"), "应包含 presets 节点");
        }

        @Test
        @DisplayName("buildDefaultRequest 使用生产配置参数")
        void testBuildDefaultRequestFromProduction() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildDefaultRequest("测试消息");

            assertNotNull(request);
            assertNotNull(request.getModel(), "模型不应为 null");
            assertNotNull(request.getMessages(), "消息列表不应为 null");
            assertEquals(1, request.getMessages().size());
            assertEquals("测试消息", request.getMessages().get(0).getContent());
        }

        @Test
        @DisplayName("buildRequestFromConfig 合并 global.json 与 deepseek_request.json")
        void testBuildRequestFromConfig() {
            assumeTrue(resolvedApiKey != null, "无有效 API Key，跳过");

            String configDir = ConfigUtils.resolveDefaultConfigDir();
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);

            DeepSeekRequest request = DeepSeekRequestLoader.buildRequestFromConfig(loader, "合并配置测试");

            assertNotNull(request);
            // 模型应来自 global.json
            assertEquals(loader.getDeepSeekModel(), request.getModel());
            // 消息应包含用户消息
            assertTrue(request.getMessages().size() >= 1);
        }

        @Test
        @DisplayName("6种预设模式参数完整性验证")
        void testAllPresetsParameterIntegrity() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);

            String[] presets = {"default", "coding", "translation", "creative", "analysis", "daily"};
            for (String preset : presets) {
                DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest(preset, "测试: " + preset);
                assertNotNull(request, preset + " 预设构建不应返回 null");
                assertNotNull(request.getModel(), preset + " 预设应指定模型");
                assertFalse(request.getMessages().isEmpty(), preset + " 预设应包含消息");
            }
        }
    }

    // ==================== 3. 真实 API 调用测试 ====================

    @Nested
    @DisplayName("3. 真实 API 调用测试")
    class LiveApiCallTest {

        @Test
        @DisplayName("简单对话 - ask() 获取文本回复")
        void testSimpleAsk() {
            String answer = DeepSeekTool.ask("用一句话介绍Java语言");
            assertNotNull(answer, "回答不应为 null");
            assertFalse(answer.isEmpty(), "回答不应为空");
            System.out.println("[简单对话] " + answer);
        }

        @Test
        @DisplayName("指定模型对话 - Flash 模型")
        void testAskWithFlashModel() {
            String answer = DeepSeekTool.ask("1+1等于几?", DeepSeekModel.DEEPSEEK_V4_FLASH);
            assertNotNull(answer, "Flash 模型回答不应为 null");
            assertFalse(answer.isEmpty(), "Flash 模型回答不应为空");
            System.out.println("[Flash模型] " + answer);
        }

        @Test
        @DisplayName("带详情响应 - 验证 Token 用量")
        void testAskWithDetails() {
            DeepSeekResponse response = DeepSeekTool.askWithDetails("什么是人工智能?");
            assertNotNull(response, "响应不应为 null");
            assertNotNull(response.getContent(), "响应内容不应为 null");
            assertFalse(response.getContent().isEmpty(), "响应内容不应为空");

            // 验证 Usage 信息
            if (response.getUsage() != null) {
                assertTrue(response.getUsage().getPromptTokens() > 0, "promptTokens 应大于 0");
                assertTrue(response.getUsage().getCompletionTokens() > 0, "completionTokens 应大于 0");
                assertTrue(response.getUsage().getTotalTokens() > 0, "totalTokens 应大于 0");
                System.out.println("[Token用量] prompt=" + response.getUsage().getPromptTokens()
                        + ", completion=" + response.getUsage().getCompletionTokens()
                        + ", total=" + response.getUsage().getTotalTokens());
            }
            System.out.println("[带详情响应] " + response.getContent());
        }

        @Test
        @DisplayName("思考模式 - 获取思维链和最终答案")
        void testThinkMode() {
            DeepSeekTool.ThinkingResult result = DeepSeekTool.think("证明根号2是无理数");
            assertNotNull(result, "思考结果不应为 null");
            assertNotNull(result.getAnswer(), "最终答案不应为 null");
            assertFalse(result.getAnswer().isEmpty(), "最终答案不应为空");

            // 思维链内容可能为 null (取决于模型)
            if (result.getReasoningContent() != null) {
                assertFalse(result.getReasoningContent().isEmpty(), "思维链内容不应为空");
                System.out.println("[思维链] " + result.getReasoningContent()
                        .substring(0, Math.min(200, result.getReasoningContent().length())) + "...");
            }
            System.out.println("[思考模式答案] " + result.getAnswer());
        }

        @Test
        @DisplayName("自定义多轮对话 - chatCompletion")
        void testMultiTurnChat() {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .addSystemMessage("你是一个专业的编程导师，回答要简洁")
                    .addUserMessage("什么是递归?")
                    .build();

            DeepSeekResponse response = DeepSeekTool.chatCompletion(request);
            assertNotNull(response);
            assertNotNull(response.getContent());
            assertFalse(response.getContent().isEmpty());
            System.out.println("[多轮对话] " + response.getContent());
        }

        @Test
        @DisplayName("流式对话 - 逐字接收内容")
        void testStreamChat() {
            StringBuilder fullContent = new StringBuilder();
            final boolean[] completed = {false};

            DeepSeekTool.askStream("用三句话描述春天",
                    chunk -> fullContent.append(chunk),
                    response -> completed[0] = true
            );

            assertTrue(fullContent.length() > 0, "流式内容不应为空");
            System.out.println("[流式对话] " + fullContent.toString());
        }
    }

    // ==================== 4. 预设模式真实调用测试 ====================

    @Nested
    @DisplayName("4. 预设模式真实调用测试")
    class PresetModeLiveTest {

        @BeforeEach
        void clearCache() {
            DeepSeekRequestLoader.clearCache();
        }

        @Test
        @DisplayName("coding 预设 - 编程问答")
        void testCodingPresetLive() {
            String answer = DeepSeekTool.askWithPreset("coding", "用Java写一个冒泡排序");
            assertNotNull(answer, "coding 预设回答不应为 null");
            assertFalse(answer.isEmpty(), "coding 预设回答不应为空");
            // 编程回答应包含代码片段
            System.out.println("[coding预设] " + answer);
        }

        @Test
        @DisplayName("translation 预设 - 翻译")
        void testTranslationPresetLive() {
            String answer = DeepSeekTool.askWithPreset("translation",
                    "将以下内容翻译成英文: 今天天气很好，适合出去散步");
            assertNotNull(answer);
            assertFalse(answer.isEmpty());
            System.out.println("[translation预设] " + answer);
        }

        @Test
        @DisplayName("creative 预设 - 创意写作")
        void testCreativePresetLive() {
            String answer = DeepSeekTool.askWithPreset("creative", "写一首关于春天的五言绝句");
            assertNotNull(answer);
            assertFalse(answer.isEmpty());
            System.out.println("[creative预设] " + answer);
        }

        @Test
        @DisplayName("analysis 预设 - 深度分析")
        void testAnalysisPresetLive() {
            String answer = DeepSeekTool.askWithPreset("analysis", "分析快速排序算法的时间复杂度");
            assertNotNull(answer);
            assertFalse(answer.isEmpty());
            System.out.println("[analysis预设] " + answer);
        }

        @Test
        @DisplayName("daily 预设 - 日常对话")
        void testDailyPresetLive() {
            String answer = DeepSeekTool.askWithPreset("daily", "今天星期几?");
            assertNotNull(answer);
            assertFalse(answer.isEmpty());
            System.out.println("[daily预设] " + answer);
        }

        @Test
        @DisplayName("预设模式完整响应 - 验证 Token 和模型")
        void testPresetFullResponse() {
            DeepSeekResponse response = DeepSeekTool.askWithPresetFull("coding", "什么是时间复杂度?");
            assertNotNull(response);
            assertNotNull(response.getContent());

            // 验证模型
            if (response.getModel() != null) {
                System.out.println("[预设模型] " + response.getModel());
            }
            // 验证 Token
            if (response.getUsage() != null) {
                assertTrue(response.getUsage().getTotalTokens() > 0);
                System.out.println("[预设Token] total=" + response.getUsage().getTotalTokens());
            }
        }
    }

    // ==================== 5. 对话日志缓存测试 ====================

    @Nested
    @DisplayName("5. 对话日志缓存测试")
    class ChatLogCacheTest {

        @Test
        @DisplayName("自动日志记录 - ask() 后自动保存")
        void testAutoLogAfterAsk() {
            // 确保自动日志开启
            DeepSeekTool.setAutoLog(true);

            String answer = DeepSeekTool.ask("日志测试问题: 1+1=?");
            assertNotNull(answer);

            // 等待日志写入
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            // 验证日志已保存
            List<ChatLog> todayLogs = DeepSeekTool.getTodayLogs();
            assertNotNull(todayLogs);
            assertFalse(todayLogs.isEmpty(), "今天应有对话记录");

            // 验证最新记录包含我们的问题
            ChatLog latest = todayLogs.get(todayLogs.size() - 1);
            assertNotNull(latest.getQuestion());
            assertNotNull(latest.getAnswer());
            System.out.println("[自动日志] 问题: " + latest.getQuestion());
            System.out.println("[自动日志] Token: " + latest.getTotalTokens());
        }

        @Test
        @DisplayName("手动保存日志 - saveChatLog")
        void testManualSaveChatLog() {
            DeepSeekResponse response = DeepSeekTool.askWithDetails("手动日志测试");
            assertNotNull(response);

            DeepSeekTool.saveChatLog("手动日志测试", response);

            // 等待日志写入
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            List<ChatLog> logs = DeepSeekTool.getTodayLogs();
            assertFalse(logs.isEmpty(), "应有对话记录");
        }

        @Test
        @DisplayName("日志 XML 持久化验证 - 保存后重新加载")
        void testXmlPersistence() {
            // 发送一个请求产生日志
            DeepSeekTool.ask("XML持久化验证问题");

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            // 验证日志目录存在
            String logDir = ChatLogStorage.getLogDir();
            assertNotNull(logDir);
            File logDirFile = new File(logDir);
            assertTrue(logDirFile.exists(), "日志目录应存在");

            // 验证日志文件列表
            List<String> files = ChatLogStorage.listLogFiles();
            assertNotNull(files);
            System.out.println("[日志文件] " + files);

            // 验证 Token 统计
            int tokenUsage = ChatLogStorage.getTodayTokenUsage();
            assertTrue(tokenUsage >= 0, "Token 用量应非负");
            System.out.println("[今日Token用量] " + tokenUsage);
        }

        @Test
        @DisplayName("日志记录完整性 - 包含所有必需字段")
        void testLogRecordCompleteness() {
            DeepSeekTool.setAutoLog(true);
            DeepSeekTool.ask("完整性测试");

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            List<ChatLog> logs = DeepSeekTool.getTodayLogs();
            assertFalse(logs.isEmpty());

            ChatLog log = logs.get(logs.size() - 1);
            assertNotNull(log.getId(), "ID 不应为 null");
            assertNotNull(log.getTimestamp(), "时间戳不应为 null");
            assertNotNull(log.getModel(), "模型不应为 null");
            assertNotNull(log.getQuestion(), "问题不应为 null");
            assertNotNull(log.getAnswer(), "回答不应为 null");
            // Token 可能为 0 (如果响应中没有 Usage)
            assertTrue(log.getTotalTokens() >= 0, "Token 总数应非负");

            System.out.println("[日志完整性] id=" + log.getId()
                    + ", model=" + log.getModel()
                    + ", tokens=" + log.getTotalTokens()
                    + ", finish=" + log.getFinishReason());
        }

        @Test
        @DisplayName("禁用自动日志后不记录")
        void testDisableAutoLog() {
            // 先记录当前日志数
            int beforeCount = DeepSeekTool.getTodayLogs().size();

            // 禁用自动日志
            DeepSeekTool.setAutoLog(false);
            DeepSeekTool.ask("不应被记录的问题");

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            int afterCount = DeepSeekTool.getTodayLogs().size();
            // 日志数不应增加
            assertEquals(beforeCount, afterCount, "禁用自动日志后不应新增记录");

            // 恢复自动日志
            DeepSeekTool.setAutoLog(true);
        }
    }

    // ==================== 6. 全链路端到端测试 ====================

    @Nested
    @DisplayName("6. 全链路端到端测试")
    class EndToEndChainTest {

        @Test
        @DisplayName("完整链路: 配置读取 → 请求构建 → API调用 → 日志缓存")
        void testFullChain() {
            assumeTrue(resolvedApiKey != null, "无有效 API Key，跳过");

            // Step 1: 从 ConfigLoader 读取配置
            String configDir = ConfigUtils.resolveDefaultConfigDir();
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);
            assertTrue(loader.isDeepSeekConfigured(), "DeepSeek 配置应完整");
            System.out.println("[Step1-配置] model=" + loader.getDeepSeekModel()
                    + ", baseUrl=" + loader.getDeepSeekBaseUrl()
                    + ", thinking=" + loader.isDeepSeekThinkingEnabled());

            // Step 2: 从 deepseek_request.json 加载预设参数
            DeepSeekRequestLoader.clearCache();
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            Set<String> presets = DeepSeekRequestLoader.getAvailablePresets();
            assertFalse(presets.isEmpty(), "应有可用预设");
            System.out.println("[Step2-预设] 可用预设: " + presets);

            // Step 3: 使用 ConfigLoader 初始化 DeepSeekTool
            DeepSeekTool.shutdown();
            DeepSeekTool.initFromConfig(loader);
            assertTrue(DeepSeekTool.isInitialized(), "初始化应成功");

            // Step 4: 发送真实 API 请求
            DeepSeekTool.setAutoLog(true);
            String answer = DeepSeekTool.ask("用一句话解释什么是机器学习");
            assertNotNull(answer, "API 回答不应为 null");
            assertFalse(answer.isEmpty(), "API 回答不应为空");
            System.out.println("[Step3-API回答] " + answer);

            // Step 5: 验证日志已缓存
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            List<ChatLog> logs = DeepSeekTool.getTodayLogs();
            assertFalse(logs.isEmpty(), "应有对话日志");

            ChatLog latestLog = logs.get(logs.size() - 1);
            assertNotNull(latestLog.getQuestion(), "日志问题不应为 null");
            assertNotNull(latestLog.getAnswer(), "日志回答不应为 null");
            assertNotNull(latestLog.getModel(), "日志模型不应为 null");
            System.out.println("[Step4-日志] id=" + latestLog.getId()
                    + ", model=" + latestLog.getModel()
                    + ", tokens=" + latestLog.getTotalTokens());

            // Step 6: 验证 XML 文件持久化
            String logDir = ChatLogStorage.getLogDir();
            assertTrue(new File(logDir).exists(), "日志目录应存在");
            List<String> logFiles = ChatLogStorage.listLogFiles();
            assertFalse(logFiles.isEmpty(), "应有日志文件");
            System.out.println("[Step5-持久化] 日志文件: " + logFiles);
            System.out.println("[Step5-持久化] 今日Token用量: " + ChatLogStorage.getTodayTokenUsage());
        }

        @Test
        @DisplayName("多预设模式端到端验证")
        void testMultiPresetEndToEnd() {
            assumeTrue(resolvedApiKey != null, "无有效 API Key，跳过");

            DeepSeekTool.setAutoLog(true);
            DeepSeekRequestLoader.clearCache();

            // 依次使用 3 种预设模式进行真实调用
            String[][] testCases = {
                    {"coding", "用Java写一个Hello World"},
                    {"translation", "翻译成英文: 你好世界"},
                    {"daily", "今天天气怎么样"}
            };

            for (String[] testCase : testCases) {
                String preset = testCase[0];
                String question = testCase[1];

                String answer = DeepSeekTool.askWithPreset(preset, question);
                assertNotNull(answer, preset + " 预设回答不应为 null");
                assertFalse(answer.isEmpty(), preset + " 预设回答不应为空");

                System.out.println("[" + preset + "] Q: " + question);
                System.out.println("[" + preset + "] A: " + answer);
            }

            // 验证所有调用都已记录日志
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            List<ChatLog> logs = DeepSeekTool.getTodayLogs();
            assertTrue(logs.size() >= 3, "应至少有 3 条日志记录");
            System.out.println("[多预设日志] 今日共 " + logs.size() + " 条记录");
        }
    }
}
