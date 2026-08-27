package com.aitool.deepseek;

import com.aitool.deepseek.model.DeepSeekModel;
import com.aitool.deepseek.model.DeepSeekRequest;
import com.system.ConfigUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * DeepSeek 请求参数加载器
 * <p>
 * 从 {@code resources/data/deepseek_request.json} 加载请求参数配置，
 * 支持预设模式 (presets) 和默认配置。通过 {@link ConfigUtils.ConfigLoader}
 * 获取配置文件路径，保证跨环境兼容性。
 * </p>
 *
 * <h3>预设模式:</h3>
 * <ul>
 *   <li><b>default</b> - 默认模式 (思考开启, temperature=0.7)</li>
 *   <li><b>coding</b> - 编程助手 (低温度, 高Token限制)</li>
 *   <li><b>translation</b> - 翻译模式 (Flash模型, 快速响应)</li>
 *   <li><b>creative</b> - 创意写作 (高温度, 高创造性)</li>
 *   <li><b>analysis</b> - 深度分析 (低温度, 高Token, 思考模式)</li>
 *   <li><b>daily</b> - 日常对话 (Flash模型, 快速问答)</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>
 * // 使用默认参数构建请求
 * DeepSeekRequest request = DeepSeekRequestLoader.buildDefaultRequest("你好");
 *
 * // 使用预设模式
 * DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("coding", "写一个排序算法");
 *
 * // 获取所有可用预设
 * Set<String> presets = DeepSeekRequestLoader.getAvailablePresets();
 * </pre>
 */
public class DeepSeekRequestLoader {

    private static final Logger log = Logger.getLogger(DeepSeekRequestLoader.class);

    /** 默认预设名称 */
    public static final String PRESET_DEFAULT = "default";

    /** 请求参数配置文件名 */
    private static final String REQUEST_PARAMS_FILE = "deepseek_request.json";

    /** 缓存的配置数据 */
    private static volatile JSONObject cachedConfig;

    private DeepSeekRequestLoader() {
        // 工具类不允许实例化
    }

    // ========== 核心加载方法 ==========

    /**
     * 从 JSON 文件加载请求参数配置
     *
     * @param configDir 配置目录 (通过 ConfigLoader 获取)
     * @return 解析后的 JSONObject
     */
    public static JSONObject loadRequestConfig(String configDir) {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        try {
            // 通过 ConfigLoader 解析数据目录
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(configDir);
            File dataDir = loader.getDataDir();
            File requestFile = new File(dataDir, REQUEST_PARAMS_FILE);

            if (!requestFile.exists()) {
                log.warn("[DeepSeekRequestLoader] 请求参数文件不存在: " + requestFile.getAbsolutePath());
                return createDefaultConfig();
            }

            String content = new String(Files.readAllBytes(requestFile.toPath()), StandardCharsets.UTF_8);
            cachedConfig = new JSONObject(content);
            log.info("[DeepSeekRequestLoader] 请求参数配置加载成功: " + requestFile.getAbsolutePath());
            return cachedConfig;

        } catch (Exception e) {
            log.error("[DeepSeekRequestLoader] 加载请求参数配置失败: " + e.getMessage(), e);
            return createDefaultConfig();
        }
    }

    /**
     * 使用默认配置目录加载请求参数
     */
    public static JSONObject loadRequestConfig() {
        return loadRequestConfig(ConfigUtils.resolveDefaultConfigDir());
    }

    /**
     * 从指定路径加载请求参数
     *
     * @param filePath JSON 文件路径
     * @return 解析后的 JSONObject
     */
    public static JSONObject loadRequestConfig(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            cachedConfig = new JSONObject(content);
            return cachedConfig;
        } catch (Exception e) {
            log.error("[DeepSeekRequestLoader] 加载请求参数失败: " + e.getMessage(), e);
            return createDefaultConfig();
        }
    }

    // ========== 请求构建方法 ==========

    /**
     * 使用默认参数构建聊天请求
     *
     * @param userMessage 用户消息
     * @return DeepSeekRequest 实例
     */
    public static DeepSeekRequest buildDefaultRequest(String userMessage) {
        return buildPresetRequest(PRESET_DEFAULT, userMessage);
    }

    /**
     * 使用预设模式构建聊天请求
     *
     * @param presetName  预设名称 (coding/translation/creative/analysis/daily)
     * @param userMessage 用户消息
     * @return DeepSeekRequest 实例
     */
    public static DeepSeekRequest buildPresetRequest(String presetName, String userMessage) {
        JSONObject config = loadRequestConfig();
        JSONObject presetConfig = getPresetConfig(config, presetName);

        DeepSeekRequest.Builder builder = DeepSeekRequest.builder()
                .addUserMessage(userMessage);

        // 应用预设参数
        if (presetConfig.has("model")) {
            builder.model(presetConfig.getString("model"));
        }
        if (presetConfig.has("temperature")) {
            builder.temperature(presetConfig.getDouble("temperature"));
        }
        if (presetConfig.has("topP")) {
            builder.topP(presetConfig.getDouble("topP"));
        }
        if (presetConfig.has("maxTokens")) {
            builder.maxTokens(presetConfig.getInt("maxTokens"));
        }
        if (presetConfig.has("frequencyPenalty")) {
            builder.frequencyPenalty(presetConfig.getDouble("frequencyPenalty"));
        }
        if (presetConfig.has("presencePenalty")) {
            builder.presencePenalty(presetConfig.getDouble("presencePenalty"));
        }
        if (presetConfig.has("reasoningEffort")) {
            builder.reasoningEffort(presetConfig.getString("reasoningEffort"));
        }

        // 处理思考模式
        if (presetConfig.has("thinking")) {
            JSONObject thinkingJson = presetConfig.getJSONObject("thinking");
            String thinkingType = thinkingJson.optString("type", "disabled");
            builder.thinkingEnabled("enabled".equals(thinkingType));
        }

        // 处理系统提示词
        if (presetConfig.has("systemPrompt")) {
            builder.addSystemMessage(presetConfig.getString("systemPrompt"));
        }

        return builder.build();
    }

    /**
     * 使用 ConfigLoader 配置构建请求
     * <p>
     * 优先使用 global.json 中的 deepseek 配置，
     * 再叠加 deepseek_request.json 中的参数。
     * </p>
     *
     * @param loader      ConfigLoader 实例
     * @param userMessage 用户消息
     * @return DeepSeekRequest 实例
     */
    public static DeepSeekRequest buildRequestFromConfig(ConfigUtils.ConfigLoader loader, String userMessage) {
        // 先加载 JSON 预设参数
        DeepSeekRequest request = buildDefaultRequest(userMessage);

        // 用 global.json 的配置覆盖
        DeepSeekRequest.Builder builder = DeepSeekRequest.builder()
                .model(loader.getDeepSeekModel())
                .messages(request.getMessages());

        if (loader.isDeepSeekThinkingEnabled()) {
            builder.thinkingEnabled(true);
            builder.reasoningEffort(loader.getDeepSeekReasoningEffort());
        } else {
            builder.thinkingEnabled(false);
        }

        String systemPrompt = loader.getDeepSeekSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            builder.addSystemMessage(systemPrompt);
        }

        return builder.build();
    }

    /**
     * 获取指定预设配置
     *
     * @param config     完整配置 JSON
     * @param presetName 预设名称
     * @return 预设配置 JSONObject，不存在时返回默认配置
     */
    private static JSONObject getPresetConfig(JSONObject config, String presetName) {
        if (config.has("presets")) {
            JSONObject presets = config.getJSONObject("presets");
            if (presets.has(presetName)) {
                return presets.getJSONObject(presetName);
            }
        }
        // 回退到默认配置
        if (config.has("default")) {
            return config.getJSONObject("default");
        }
        return createDefaultPresetConfig();
    }

    /**
     * 获取所有可用的预设名称
     *
     * @return 预设名称集合
     */
    public static Set<String> getAvailablePresets() {
        JSONObject config = loadRequestConfig();
        Set<String> presets = new LinkedHashSet<>();

        if (config.has("presets")) {
            JSONObject presetsJson = config.getJSONObject("presets");
            presets.addAll(presetsJson.keySet());
        }

        return presets;
    }

    /**
     * 获取预设的描述信息
     *
     * @param presetName 预设名称
     * @return 描述信息，不存在时返回空字符串
     */
    public static String getPresetDescription(String presetName) {
        JSONObject config = loadRequestConfig();
        JSONObject presetConfig = getPresetConfig(config, presetName);
        return presetConfig.optString("description", "");
    }

    /**
     * 清除缓存的配置
     */
    public static void clearCache() {
        cachedConfig = null;
    }

    // ========== 内部辅助方法 ==========

    /**
     * 创建默认配置 (当文件不存在或加载失败时)
     */
    private static JSONObject createDefaultConfig() {
        JSONObject config = new JSONObject();
        config.put("default", createDefaultPresetConfig());
        return config;
    }

    /**
     * 创建默认预设配置
     */
    private static JSONObject createDefaultPresetConfig() {
        JSONObject preset = new JSONObject();
        preset.put("model", DeepSeekModel.DEEPSEEK_V4_PRO.getValue());
        preset.put("temperature", 0.7);
        preset.put("topP", 0.9);
        preset.put("maxTokens", 4096);
        preset.put("frequencyPenalty", 0.0);
        preset.put("presencePenalty", 0.0);

        JSONObject thinking = new JSONObject();
        thinking.put("type", "enabled");
        preset.put("thinking", thinking);

        preset.put("reasoningEffort", "high");
        return preset;
    }

    // ========== Demo 方法 ==========

    /**
     * 演示请求参数加载功能
     */
    public static void testDemo() {
        System.out.println("=== DeepSeek 请求参数加载演示 ===\n");

        // 显示可用预设
        Set<String> presets = getAvailablePresets();
        System.out.println("可用预设模式: " + presets);

        // 显示每个预设的描述
        for (String preset : presets) {
            System.out.println("  - " + preset + ": " + getPresetDescription(preset));
        }

        // 构建默认请求
        System.out.println("\n--- 默认模式请求 ---");
        DeepSeekRequest defaultRequest = buildDefaultRequest("你好，请介绍一下自己");
        System.out.println("模型: " + defaultRequest.getModel());
        System.out.println("温度: " + defaultRequest.getTemperature());
        System.out.println("消息数: " + defaultRequest.getMessages().size());

        // 构建编程模式请求
        System.out.println("\n--- 编程模式请求 ---");
        DeepSeekRequest codingRequest = buildPresetRequest("coding", "用Java写一个快速排序");
        System.out.println("模型: " + codingRequest.getModel());
        System.out.println("温度: " + codingRequest.getTemperature());
        System.out.println("最大Token: " + codingRequest.getMaxTokens());

        // 构建翻译模式请求
        System.out.println("\n--- 翻译模式请求 ---");
        DeepSeekRequest transRequest = buildPresetRequest("translation", "将以下内容翻译成英文: 今天天气很好");
        System.out.println("模型: " + transRequest.getModel());
        System.out.println("温度: " + transRequest.getTemperature());
    }
}
