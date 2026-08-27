package com.aitool.deepseek;

import com.aitool.deepseek.model.ChatLog;
import com.aitool.deepseek.model.DeepSeekMessage;
import com.aitool.deepseek.model.DeepSeekModel;
import com.aitool.deepseek.model.DeepSeekRequest;
import com.aitool.deepseek.model.DeepSeekResponse;
import com.system.ConfigUtils;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;

/**
 * DeepSeek AI 工具类
 * <p>
 * 提供对 DeepSeek AI 模型的高层封装，支持快速对话、多轮会话、
 * 思考辅助等场景。内部持有单例 {@link DeepSeekClient}，线程安全。
 * </p>
 *
 * <h3>使用示例:</h3>
 * <pre>
 * // 1. 初始化 (只需调用一次)
 * DeepSeekTool.init("sk-your-api-key");
 *
 * // 2. 简单对话
 * String answer = DeepSeekTool.ask("用Java写一个冒泡排序");
 * System.out.println(answer);
 *
 * // 3. 带系统提示词的对话
 * DeepSeekTool.init("sk-xxx", "你是一个专业的翻译官，只输出翻译结果");
 * String result = DeepSeekTool.ask("将以下内容翻译成英文: 今天天气很好");
 *
 * // 4. 流式对话 (逐字输出)
 * DeepSeekTool.askStream("讲一个故事", chunk -> System.out.print(chunk));
 *
 * // 5. 多轮对话
 * DeepSeekRequest request = DeepSeekRequest.builder()
 *     .addSystemMessage("你是一个编程导师")
 *     .addUserMessage("什么是递归?")
 *     .build();
 * DeepSeekResponse resp = DeepSeekTool.chatCompletion(request);
 * System.out.println(resp.getContent());
 * </pre>
 */
public class DeepSeekTool {

    private static final Logger log = Logger.getLogger(DeepSeekTool.class);

    private static volatile DeepSeekClient client;
    private static volatile DeepSeekConfig currentConfig;
    /** 是否自动记录对话日志 */
    private static volatile boolean autoLogEnabled = true;

    private DeepSeekTool() {
        // 工具类不允许实例化
    }

    // ========== 初始化方法 ==========

    /**
     * 使用 API Key 初始化 DeepSeek 客户端 (使用默认配置)
     *
     * @param apiKey DeepSeek API Key
     */
    public static synchronized void init(String apiKey) {
        init(DeepSeekConfig.of(apiKey));
    }

    /**
     * 使用 API Key 和系统提示词初始化
     *
     * @param apiKey       DeepSeek API Key
     * @param systemPrompt 默认系统提示词
     */
    public static synchronized void init(String apiKey, String systemPrompt) {
        init(DeepSeekConfig.builder()
                .apiKey(apiKey)
                .systemPrompt(systemPrompt)
                .build());
    }

    /**
     * 使用完整配置初始化 DeepSeek 客户端
     *
     * @param config DeepSeek 配置
     */
    public static synchronized void init(DeepSeekConfig config) {
        if (client != null) {
            client.close();
        }
        currentConfig = config;
        client = new DeepSeekClient(config);
        log.info("[DeepSeekTool] 初始化完成, 模型: " + config.getModel());
    }

    /**
     * 从 ConfigLoader 配置初始化 DeepSeek 客户端
     * <p>
     * 自动读取 global.json 中的 deepseek 配置节点，
     * 支持四种 API Key 读取方式：
     * <ul>
     *   <li>明文: {@code sk-xxx}</li>
     *   <li>AES加密: {@code ENC(Base64密文)}</li>
     *   <li>环境变量: {@code ${ENV:DEEPSEEK_API_KEY}}</li>
     *   <li>Windows凭据: {@code ${WINCRED:DeepSeekApiKey}}</li>
     * </ul>
     * 同时从 deepseek_request.json 加载默认请求参数。
     * </p>
     *
     * @param loader ConfigLoader 实例
     * @throws IllegalStateException 配置缺失或 API Key 为空时抛出
     */
    public static synchronized void initFromConfig(ConfigUtils.ConfigLoader loader) {
        if (!loader.isDeepSeekConfigured()) {
            throw new IllegalStateException(
                    "DeepSeek 配置缺失，请检查 global.json 中是否包含 deepseek 节点及有效的 apiKey");
        }

        DeepSeekConfig config = DeepSeekConfig.builder()
                .apiKey(loader.getDeepSeekApiKey())
                .baseUrl(loader.getDeepSeekBaseUrl())
                .model(loader.getDeepSeekModel())
                .thinkingEnabled(loader.isDeepSeekThinkingEnabled())
                .reasoningEffort(loader.getDeepSeekReasoningEffort())
                .systemPrompt(loader.getDeepSeekSystemPrompt())
                .build();

        init(config);
        log.info("[DeepSeekTool] 从 ConfigLoader 初始化完成");
    }

    /**
     * 从默认配置目录初始化 DeepSeek 客户端
     * <p>
     * 自动查找 global.json 并加载 deepseek 配置。
     * </p>
     */
    public static synchronized void initFromConfig() {
        ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader();
        initFromConfig(loader);
    }

    /**
     * 确保客户端已初始化
     */
    private static DeepSeekClient ensureClient() {
        if (client == null) {
            throw new IllegalStateException(
                    "DeepSeekTool 未初始化，请先调用 DeepSeekTool.init(apiKey) 进行初始化");
        }
        return client;
    }

    // ========== 核心对话方法 ==========

    /**
     * 快速提问 - 发送一条用户消息并获取回复文本
     * <p>对话内容会自动记录到本地 XML 日志</p>
     *
     * @param question 用户问题
     * @return AI 回复的文本内容
     */
    public static String ask(String question) {
        DeepSeekResponse response = ensureClient().chat(question);
        autoLog(question, response);
        return response.getContent();
    }

    /**
     * 快速提问 - 指定模型
     *
     * @param question 用户问题
     * @param model    使用的模型
     * @return AI 回复的文本内容
     */
    public static String ask(String question, DeepSeekModel model) {
        DeepSeekRequest request = DeepSeekRequest.builder()
                .model(model)
                .addUserMessage(question)
                .build();
        DeepSeekResponse response = ensureClient().chatCompletion(request);
        autoLog(question, response);
        return response.getContent();
    }

    /**
     * 快速提问 - 获取完整响应对象 (包含 Token 用量等信息)
     *
     * @param question 用户问题
     * @return 完整的 DeepSeek 响应
     */
    public static DeepSeekResponse askWithDetails(String question) {
        return ensureClient().chat(question);
    }

    /**
     * 流式对话 - 通过回调逐字接收内容
     *
     * @param question        用户问题
     * @param onPartialContent 每收到一段内容时的回调
     */
    public static void askStream(String question, Consumer<String> onPartialContent) {
        askStream(question, onPartialContent, null);
    }

    /**
     * 流式对话 - 带完成回调
     *
     * @param question         用户问题
     * @param onPartialContent 每收到一段内容时的回调
     * @param onComplete       流结束时的回调
     */
    public static void askStream(String question,
                                 Consumer<String> onPartialContent,
                                 Consumer<DeepSeekResponse> onComplete) {
        DeepSeekRequest request = buildDefaultRequest();
        request.getMessages().add(DeepSeekMessage.user(question));
        ensureClient().chatCompletionStream(request, onPartialContent, onComplete);
    }

    /**
     * 发送自定义聊天请求 (同步)
     * <p>对话内容会自动记录到本地 XML 日志</p>
     *
     * @param request 聊天请求
     * @return DeepSeek 响应
     */
    public static DeepSeekResponse chatCompletion(DeepSeekRequest request) {
        DeepSeekResponse response = ensureClient().chatCompletion(request);
        // 提取最后一条用户消息作为问题
        String lastQuestion = extractLastUserMessage(request);
        autoLog(lastQuestion, response);
        return response;
    }

    /**
     * 发送自定义聊天请求 (流式)
     *
     * @param request          聊天请求
     * @param onPartialContent 内容回调
     * @param onComplete       完成回调
     */
    public static void chatCompletionStream(DeepSeekRequest request,
                                            Consumer<String> onPartialContent,
                                            Consumer<DeepSeekResponse> onComplete) {
        ensureClient().chatCompletionStream(request, onPartialContent, onComplete);
    }

    // ========== 预设模式对话方法 ==========

    /**
     * 使用预设模式进行对话
     * <p>
     * 预设模式从 {@code resources/data/deepseek_request.json} 加载参数，
     * 包含特定的模型、温度、系统提示词等配置。
     * </p>
     *
     * @param presetName  预设名称 (coding/translation/creative/analysis/daily)
     * @param userMessage 用户消息
     * @return AI 回复的文本内容
     */
    public static String askWithPreset(String presetName, String userMessage) {
        DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest(presetName, userMessage);
        DeepSeekResponse response = ensureClient().chatCompletion(request);
        autoLog(userMessage, response);
        return response.getContent();
    }

    /**
     * 使用预设模式进行对话 - 获取完整响应
     *
     * @param presetName  预设名称
     * @param userMessage 用户消息
     * @return 完整的 DeepSeek 响应
     */
    public static DeepSeekResponse askWithPresetFull(String presetName, String userMessage) {
        DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest(presetName, userMessage);
        DeepSeekResponse response = ensureClient().chatCompletion(request);
        autoLog(userMessage, response);
        return response;
    }

    /**
     * 获取所有可用的预设模式名称
     *
     * @return 预设名称集合
     */
    public static java.util.Set<String> getAvailablePresets() {
        return DeepSeekRequestLoader.getAvailablePresets();
    }

    // ========== 思考辅助方法 ==========

    /**
     * 获取 AI 的思考过程 (思维链)
     * <p>
     * 需要模型支持思考模式 (默认开启)，返回的内容包含推理过程和最终答案。
     * </p>
     *
     * @param question 用户问题
     * @return 包含思维链和最终答案的结果
     */
    public static ThinkingResult think(String question) {
        DeepSeekRequest request = DeepSeekRequest.builder()
                .model(currentConfig != null ? currentConfig.getModel() : DeepSeekModel.DEEPSEEK_V4_PRO.getValue())
                .addUserMessage(question)
                .thinkingEnabled(true)
                .build();

        DeepSeekResponse response = ensureClient().chatCompletion(request);
        return new ThinkingResult(response.getReasoningContent(), response.getContent(), response);
    }

    /**
     * 思考结果封装类
     */
    public static class ThinkingResult {
        /** 思维链推理过程 */
        private final String reasoningContent;
        /** 最终回答 */
        private final String answer;
        /** 原始响应 */
        private final DeepSeekResponse response;

        public ThinkingResult(String reasoningContent, String answer, DeepSeekResponse response) {
            this.reasoningContent = reasoningContent;
            this.answer = answer;
            this.response = response;
        }

        public String getReasoningContent() {
            return reasoningContent;
        }

        public String getAnswer() {
            return answer;
        }

        public DeepSeekResponse getResponse() {
            return response;
        }

        @Override
        public String toString() {
            return "ThinkingResult{answer='" + answer + "', hasReasoning=" + (reasoningContent != null) + "}";
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 查询可用模型列表
     *
     * @return 模型列表 JSON 字符串
     */
    public static String listModels() {
        return ensureClient().listModels();
    }

    /**
     * 关闭客户端，释放资源
     */
    public static synchronized void shutdown() {
        if (client != null) {
            client.close();
            client = null;
            currentConfig = null;
            log.info("[DeepSeekTool] 已关闭");
        }
    }

    /**
     * 检查客户端是否已初始化
     */
    public static boolean isInitialized() {
        return client != null;
    }

    // ========== 日志记录控制 ==========

    /**
     * 启用/禁用自动对话日志记录
     *
     * @param enabled true 启用, false 禁用
     */
    public static void setAutoLog(boolean enabled) {
        autoLogEnabled = enabled;
        log.info("[DeepSeekTool] 自动日志记录: " + (enabled ? "已启用" : "已禁用"));
    }

    /**
     * 检查自动日志记录是否启用
     */
    public static boolean isAutoLogEnabled() {
        return autoLogEnabled;
    }

    /**
     * 手动保存一条对话记录
     *
     * @param question 用户问题
     * @param response DeepSeek 响应
     */
    public static void saveChatLog(String question, DeepSeekResponse response) {
        String model = currentConfig != null ? currentConfig.getModel() : DeepSeekModel.DEEPSEEK_V4_PRO.getValue();
        ChatLog chatLog = ChatLog.fromResponse(question, response, model);
        ChatLogStorage.saveLog(chatLog);
    }

    /**
     * 获取今天的对话记录
     *
     * @return 对话记录列表
     */
    public static List<ChatLog> getTodayLogs() {
        return ChatLogStorage.loadTodayLogs();
    }

    /**
     * 获取指定日期的对话记录
     *
     * @param dateStr 日期字符串 (格式: yyyy-MM-dd)
     * @return 对话记录列表
     */
    public static List<ChatLog> getLogs(String dateStr) {
        return ChatLogStorage.loadLogs(dateStr);
    }

    // ========== 内部辅助方法 ==========

    /**
     * 自动记录对话日志
     */
    private static void autoLog(String question, DeepSeekResponse response) {
        if (autoLogEnabled && response != null) {
            try {
                String model = response.getModel();
                if (model == null && currentConfig != null) {
                    model = currentConfig.getModel();
                }
                ChatLog chatLog = ChatLog.fromResponse(question, response, model);
                ChatLogStorage.saveLog(chatLog);
            } catch (Exception e) {
                log.warn("[DeepSeekTool] 自动记录对话日志失败: " + e.getMessage());
            }
        }
    }

    /**
     * 从请求中提取最后一条用户消息
     */
    private static String extractLastUserMessage(DeepSeekRequest request) {
        if (request.getMessages() != null) {
            for (int i = request.getMessages().size() - 1; i >= 0; i--) {
                DeepSeekMessage msg = request.getMessages().get(i);
                if ("user".equals(msg.getRole())) {
                    return msg.getContent();
                }
            }
        }
        return "unknown";
    }

    private static DeepSeekRequest buildDefaultRequest() {
        DeepSeekRequest.Builder builder = DeepSeekRequest.builder()
                .model(currentConfig != null ? currentConfig.getModel() : DeepSeekModel.DEEPSEEK_V4_PRO.getValue());

        if (currentConfig != null && currentConfig.getSystemPrompt() != null) {
            builder.addSystemMessage(currentConfig.getSystemPrompt());
        }
        if (currentConfig != null && currentConfig.isThinkingEnabled()) {
            builder.thinkingEnabled(true);
        }
        return builder.build();
    }

    // ========== Demo 方法 ==========

    /**
     * 演示 DeepSeek AI 工具的基本用法
     */
    public static void testDemo() {
        // 注意: 运行前请替换为真实的 API Key
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[DeepSeekTool] 请设置环境变量 DEEPSEEK_API_KEY 或在代码中直接指定 API Key");
            return;
        }

        // 初始化
        DeepSeekTool.init(apiKey);

        try {
            // 1. 简单对话
            System.out.println("=== 简单对话 ===");
            String answer = DeepSeekTool.ask("用一句话介绍Java语言");
            System.out.println("回答: " + answer);

            // 2. 指定模型
            System.out.println("\n=== 指定模型 (Flash) ===");
            String fastAnswer = DeepSeekTool.ask("1+1等于几?", DeepSeekModel.DEEPSEEK_V4_FLASH);
            System.out.println("回答: " + fastAnswer);

            // 3. 带详情的响应
            System.out.println("\n=== 带详情的响应 ===");
            DeepSeekResponse resp = DeepSeekTool.askWithDetails("什么是AI?");
            System.out.println("回答: " + resp.getContent());
            System.out.println("模型: " + resp.getModel());
            System.out.println("Token用量: " + resp.getUsage());

            // 4. 思考模式
            System.out.println("\n=== 思考模式 ===");
            ThinkingResult result = DeepSeekTool.think("证明根号2是无理数");
            System.out.println("推理过程: " + (result.getReasoningContent() != null ?
                    result.getReasoningContent().substring(0, Math.min(200, result.getReasoningContent().length())) + "..."
                    : "无"));
            System.out.println("最终答案: " + result.getAnswer());

        } finally {
            DeepSeekTool.shutdown();
        }
    }
}
