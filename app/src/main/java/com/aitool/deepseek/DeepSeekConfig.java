package com.aitool.deepseek;

import com.aitool.deepseek.model.DeepSeekModel;

/**
 * DeepSeek API 配置类
 * <p>
 * 管理 DeepSeek API 的连接参数，包括 API Key、Base URL、模型选择、
 * 超时设置、代理配置等。支持 Builder 模式构建。
 * </p>
 *
 * <pre>
 * DeepSeekConfig config = DeepSeekConfig.builder()
 *     .apiKey("sk-xxx")
 *     .model(DeepSeekModel.DEEPSEEK_V4_PRO)
 *     .thinkingEnabled(true)
 *     .build();
 * </pre>
 */
public class DeepSeekConfig {

    /** DeepSeek API 基础 URL */
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1/";

    /** API 密钥 (必填) */
    private final String apiKey;

    /** API 基础 URL */
    private final String baseUrl;

    /** 默认模型 */
    private final String model;

    /** 是否启用思考模式 */
    private final boolean thinkingEnabled;

    /** 推理力度: low / medium / high */
    private final String reasoningEffort;

    /** 默认系统提示词 */
    private final String systemPrompt;

    /** 连接超时 (毫秒) */
    private final int connectTimeout;

    /** 读取超时 (毫秒) */
    private final int readTimeout;

    /** 是否开启请求日志 */
    private final boolean logRequests;

    /** 是否开启响应日志 */
    private final boolean logResponses;

    /** 代理主机 (可选) */
    private final String proxyHost;

    /** 代理端口 (可选) */
    private final int proxyPort;

    private DeepSeekConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.model = builder.model;
        this.thinkingEnabled = builder.thinkingEnabled;
        this.reasoningEffort = builder.reasoningEffort;
        this.systemPrompt = builder.systemPrompt;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 快速创建仅包含 API Key 的配置
     */
    public static DeepSeekConfig of(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    // ========== Getter ==========

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public boolean isThinkingEnabled() {
        return thinkingEnabled;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public boolean isLogRequests() {
        return logRequests;
    }

    public boolean isLogResponses() {
        return logResponses;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean hasProxy() {
        return proxyHost != null && !proxyHost.isEmpty();
    }

    // ========== Builder ==========

    public static final class Builder {
        private String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private String model = DeepSeekModel.DEEPSEEK_V4_PRO.getValue();
        private boolean thinkingEnabled = true;
        private String reasoningEffort = "high";
        private String systemPrompt;
        private int connectTimeout = 60000;
        private int readTimeout = 120000;
        private boolean logRequests = false;
        private boolean logResponses = false;
        private String proxyHost;
        private int proxyPort;

        private Builder() {
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder model(DeepSeekModel model) {
            this.model = model.getValue();
            return this;
        }

        public Builder thinkingEnabled(boolean thinkingEnabled) {
            this.thinkingEnabled = thinkingEnabled;
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public Builder proxy(String host, int port) {
            this.proxyHost = host;
            this.proxyPort = port;
            return this;
        }

        public DeepSeekConfig build() {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "DeepSeek API Key 不能为空，请访问 https://platform.deepseek.com/api_keys 获取");
            }
            return new DeepSeekConfig(this);
        }
    }

    @Override
    public String toString() {
        return "DeepSeekConfig{baseUrl='" + baseUrl + "', model='" + model
                + "', thinkingEnabled=" + thinkingEnabled + ", reasoningEffort='" + reasoningEffort + "'}";
    }
}
