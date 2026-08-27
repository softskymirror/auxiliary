package com.aitool.deepseek.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DeepSeek 聊天补全响应模型
 * <p>
 * 封装 DeepSeek API 返回的响应数据，
 * 包含响应ID、模型、选择列表、Token用量等信息。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepSeekResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private Long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private Usage usage;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    // ========== 快捷方法 ==========

    /**
     * 获取第一条响应的文本内容
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }

    /**
     * 获取第一条响应的思维链内容
     */
    public String getReasoningContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getReasoningContent();
            }
        }
        return null;
    }

    // ========== 内部类 ==========

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        @JsonProperty("index")
        private Integer index;

        @JsonProperty("message")
        private DeepSeekMessage message;

        @JsonProperty("delta")
        private DeepSeekMessage delta;

        @JsonProperty("finish_reason")
        private String finishReason;

        public Integer getIndex() {
            return index;
        }

        public DeepSeekMessage getMessage() {
            return message;
        }

        public DeepSeekMessage getDelta() {
            return delta;
        }

        public String getFinishReason() {
            return finishReason;
        }

        @Override
        public String toString() {
            return "Choice{index=" + index + ", message=" + message + ", finishReason='" + finishReason + "'}";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        /** 思维链Token数 (思考模式下返回) */
        @JsonProperty("prompt_tokens_details")
        private TokenDetails promptTokensDetails;

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public TokenDetails getPromptTokensDetails() {
            return promptTokensDetails;
        }

        @Override
        public String toString() {
            return "Usage{promptTokens=" + promptTokens + ", completionTokens=" + completionTokens
                    + ", totalTokens=" + totalTokens + "}";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenDetails {
        @JsonProperty("cached_tokens")
        private Integer cachedTokens;

        public Integer getCachedTokens() {
            return cachedTokens;
        }

        @Override
        public String toString() {
            return "TokenDetails{cachedTokens=" + cachedTokens + "}";
        }
    }

    // ========== Getter / Setter ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getSystemFingerprint() {
        return systemFingerprint;
    }

    public void setSystemFingerprint(String systemFingerprint) {
        this.systemFingerprint = systemFingerprint;
    }

    @Override
    public String toString() {
        return "DeepSeekResponse{id='" + id + "', model='" + model + "', choices=" + choices + ", usage=" + usage + "}";
    }
}
