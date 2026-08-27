package com.aitool.deepseek.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DeepSeek 聊天消息模型
 * <p>
 * 每条消息包含角色 (role) 和内容 (content)，
 * 角色可以是 system / user / assistant。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekMessage {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;

    /** 思维链内容 (仅 assistant 消息在思考模式下返回) */
    @JsonProperty("reasoning_content")
    private String reasoningContent;

    public DeepSeekMessage() {
    }

    public DeepSeekMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // ========== 快捷工厂方法 ==========

    public static DeepSeekMessage system(String content) {
        return new DeepSeekMessage("system", content);
    }

    public static DeepSeekMessage user(String content) {
        return new DeepSeekMessage("user", content);
    }

    public static DeepSeekMessage assistant(String content) {
        return new DeepSeekMessage("assistant", content);
    }

    // ========== Getter / Setter ==========

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    @Override
    public String toString() {
        return "DeepSeekMessage{role='" + role + "', content='" + content + "'}";
    }
}
