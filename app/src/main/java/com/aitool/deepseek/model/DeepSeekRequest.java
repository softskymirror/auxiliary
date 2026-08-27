package com.aitool.deepseek.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek 聊天补全请求模型
 * <p>
 * 封装发送到 DeepSeek API 的请求参数，
 * 支持模型选择、温度、最大Token、思维链等配置。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<DeepSeekMessage> messages;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("stream")
    private Boolean stream;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("stop")
    private List<String> stop;

    @JsonProperty("user")
    private String user;

    /** 思考模式配置 */
    @JsonProperty("thinking")
    private ThinkingConfig thinking;

    /** 推理力度: low / medium / high */
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;

    private DeepSeekRequest(Builder builder) {
        this.model = builder.model;
        this.messages = builder.messages;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.maxTokens = builder.maxTokens;
        this.stream = builder.stream;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.presencePenalty = builder.presencePenalty;
        this.stop = builder.stop;
        this.user = builder.user;
        this.thinking = builder.thinking;
        this.reasoningEffort = builder.reasoningEffort;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ========== 思考模式配置 ==========

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ThinkingConfig {
        @JsonProperty("type")
        private String type; // "enabled" or "disabled"

        public ThinkingConfig() {
        }

        public ThinkingConfig(String type) {
            this.type = type;
        }

        public static ThinkingConfig enabled() {
            return new ThinkingConfig("enabled");
        }

        public static ThinkingConfig disabled() {
            return new ThinkingConfig("disabled");
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // ========== Builder ==========

    public static final class Builder {
        private String model = DeepSeekModel.DEEPSEEK_V4_PRO.getValue();
        private List<DeepSeekMessage> messages = new ArrayList<>();
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private Boolean stream;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private List<String> stop;
        private String user;
        private ThinkingConfig thinking;
        private String reasoningEffort;

        private Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder model(DeepSeekModel model) {
            this.model = model.getValue();
            return this;
        }

        public Builder messages(List<DeepSeekMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder addMessage(DeepSeekMessage message) {
            this.messages.add(message);
            return this;
        }

        public Builder addSystemMessage(String content) {
            this.messages.add(DeepSeekMessage.system(content));
            return this;
        }

        public Builder addUserMessage(String content) {
            this.messages.add(DeepSeekMessage.user(content));
            return this;
        }

        public Builder addAssistantMessage(String content) {
            this.messages.add(DeepSeekMessage.assistant(content));
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder thinking(ThinkingConfig thinking) {
            this.thinking = thinking;
            return this;
        }

        public Builder thinkingEnabled(boolean enabled) {
            this.thinking = enabled ? ThinkingConfig.enabled() : ThinkingConfig.disabled();
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public DeepSeekRequest build() {
            return new DeepSeekRequest(this);
        }
    }

    // ========== Getter ==========

    public String getModel() {
        return model;
    }

    public List<DeepSeekMessage> getMessages() {
        return messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Boolean getStream() {
        return stream;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public String getUser() {
        return user;
    }

    public ThinkingConfig getThinking() {
        return thinking;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    @Override
    public String toString() {
        return "DeepSeekRequest{model='" + model + "', messages=" + messages + "}";
    }
}
