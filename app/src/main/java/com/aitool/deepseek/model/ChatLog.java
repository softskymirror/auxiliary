package com.aitool.deepseek.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 对话日志记录模型
 * <p>
 * 封装一次问答的完整信息，包括问题、回答、模型、Token用量、时间戳等。
 * 用于本地 XML 持久化存储。
 * </p>
 */
public class ChatLog {

    /** 唯一标识 */
    private String id;

    /** 时间戳 */
    private String timestamp;

    /** 使用的模型 */
    private String model;

    /** 用户问题 */
    private String question;

    /** AI 回答 */
    private String answer;

    /** 思维链推理内容 (思考模式下) */
    private String reasoningContent;

    /** 提示词 Token 数 */
    private int promptTokens;

    /** 补全 Token 数 */
    private int completionTokens;

    /** 总 Token 数 */
    private int totalTokens;

    /** 完成原因 (stop/length等) */
    private String finishReason;

    public ChatLog() {
        this.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * 从 DeepSeek 响应构建 ChatLog
     *
     * @param question 用户问题
     * @param response DeepSeek 响应
     * @param model    使用的模型名称
     * @return ChatLog 实例
     */
    public static ChatLog fromResponse(String question, DeepSeekResponse response, String model) {
        ChatLog chatLog = new ChatLog();
        chatLog.setModel(model);
        chatLog.setQuestion(question);
        chatLog.setAnswer(response.getContent());
        chatLog.setReasoningContent(response.getReasoningContent());

        if (response.getUsage() != null) {
            chatLog.setPromptTokens(response.getUsage().getPromptTokens() != null ? response.getUsage().getPromptTokens() : 0);
            chatLog.setCompletionTokens(response.getUsage().getCompletionTokens() != null ? response.getUsage().getCompletionTokens() : 0);
            chatLog.setTotalTokens(response.getUsage().getTotalTokens() != null ? response.getUsage().getTotalTokens() : 0);
        }

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            chatLog.setFinishReason(response.getChoices().get(0).getFinishReason());
        }

        return chatLog;
    }

    // ========== Getter / Setter ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    @Override
    public String toString() {
        return "ChatLog{id='" + id + "', timestamp='" + timestamp + "', model='" + model
                + "', question='" + (question != null && question.length() > 50 ? question.substring(0, 50) + "..." : question) + "'}";
    }
}
