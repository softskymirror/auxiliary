package com.aitool.deepseek;

import com.aitool.deepseek.model.DeepSeekMessage;
import com.aitool.deepseek.model.DeepSeekRequest;
import com.aitool.deepseek.model.DeepSeekResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * DeepSeek API 客户端
 * <p>
 * 基于 Apache HttpClient 实现，兼容 DeepSeek 官方 API 协议 (OpenAI 兼容格式)。
 * 支持同步聊天补全、流式聊天补全、模型查询等功能。
 * </p>
 *
 * <pre>
 * DeepSeekConfig config = DeepSeekConfig.of("sk-xxx");
 * DeepSeekClient client = new DeepSeekClient(config);
 *
 * // 简单对话
 * DeepSeekResponse resp = client.chat("你好，请介绍一下自己");
 * System.out.println(resp.getContent());
 *
 * // 自定义请求
 * DeepSeekRequest request = DeepSeekRequest.builder()
 *     .addSystemMessage("你是一个专业的编程助手")
 *     .addUserMessage("用Java写一个快速排序")
 *     .maxTokens(2000)
 *     .build();
 * DeepSeekResponse resp = client.chatCompletion(request);
 * </pre>
 */
public class DeepSeekClient implements Closeable {

    private static final Logger log = Logger.getLogger(DeepSeekClient.class);

    private static final String CHAT_COMPLETIONS_PATH = "chat/completions";
    private static final String MODELS_PATH = "models";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_SIGNAL = "[DONE]";

    private final DeepSeekConfig config;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(DeepSeekConfig config) {
        this.config = config;
        this.objectMapper = createObjectMapper();
        this.httpClient = createHttpClient();
    }

    // ========== 核心 API 方法 ==========

    /**
     * 快速发送一条用户消息并获取回复
     *
     * @param userMessage 用户消息内容
     * @return DeepSeek 响应
     */
    public DeepSeekResponse chat(String userMessage) {
        DeepSeekRequest request = buildDefaultRequest();
        request.getMessages().add(DeepSeekMessage.user(userMessage));
        return chatCompletion(request);
    }

    /**
     * 发送聊天补全请求 (同步)
     *
     * @param request 聊天请求
     * @return DeepSeek 响应
     */
    public DeepSeekResponse chatCompletion(DeepSeekRequest request) {
        try {
            String url = config.getBaseUrl() + CHAT_COMPLETIONS_PATH;
            String requestBody = objectMapper.writeValueAsString(request);

            if (config.isLogRequests()) {
                log.info("[DeepSeek] 请求URL: " + url);
                log.info("[DeepSeek] 请求体: " + requestBody);
            }

            HttpPost httpPost = createHttpPost(url, requestBody);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int statusCode = response.getStatusLine().getStatusCode();

                if (config.isLogResponses()) {
                    log.info("[DeepSeek] 响应状态: " + statusCode);
                    log.info("[DeepSeek] 响应体: " + responseBody);
                }

                if (statusCode != 200) {
                    throw new DeepSeekApiException("DeepSeek API 请求失败, HTTP状态码: " + statusCode + ", 响应: " + responseBody);
                }

                return objectMapper.readValue(responseBody, DeepSeekResponse.class);
            }
        } catch (DeepSeekApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DeepSeekApiException("DeepSeek API 调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 流式聊天补全 - 通过回调逐块接收响应
     *
     * @param request         聊天请求
     * @param onPartialContent 每收到一段内容时的回调 (delta content)
     * @param onComplete      流结束时的回调 (完整内容)，可为 null
     */
    public void chatCompletionStream(DeepSeekRequest request,
                                     Consumer<String> onPartialContent,
                                     Consumer<DeepSeekResponse> onComplete) {
        request = DeepSeekRequest.builder()
                .model(request.getModel())
                .messages(request.getMessages())
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .maxTokens(request.getMaxTokens())
                .stream(true)
                .thinking(request.getThinking())
                .reasoningEffort(request.getReasoningEffort())
                .build();

        try {
            String url = config.getBaseUrl() + CHAT_COMPLETIONS_PATH;
            String requestBody = objectMapper.writeValueAsString(request);

            if (config.isLogRequests()) {
                log.info("[DeepSeek] 流式请求URL: " + url);
            }

            HttpPost httpPost = createHttpPost(url, requestBody);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    throw new DeepSeekApiException("DeepSeek API 流式请求失败, HTTP状态码: " + statusCode + ", 响应: " + errorBody);
                }

                HttpEntity entity = response.getEntity();
                StringBuilder fullContent = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        if (!line.startsWith(SSE_DATA_PREFIX)) continue;

                        String data = line.substring(SSE_DATA_PREFIX.length()).trim();
                        if (SSE_DONE_SIGNAL.equals(data)) break;

                        try {
                            DeepSeekResponse chunk = objectMapper.readValue(data, DeepSeekResponse.class);
                            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                                DeepSeekResponse.Choice choice = chunk.getChoices().get(0);
                                // 优先取 delta (流式), 否则取 message
                                DeepSeekMessage delta = choice.getDelta() != null ? choice.getDelta() : choice.getMessage();
                                if (delta != null && delta.getContent() != null) {
                                    fullContent.append(delta.getContent());
                                    if (onPartialContent != null) {
                                        onPartialContent.accept(delta.getContent());
                                    }
                                }
                            }
                        } catch (Exception parseEx) {
                            log.warn("[DeepSeek] 解析流式数据块失败: " + data, parseEx);
                        }
                    }
                }

                if (onComplete != null) {
                    // 构建最终响应
                    DeepSeekResponse finalResponse = new DeepSeekResponse();
                    finalResponse.setModel(request.getModel());
                    onComplete.accept(finalResponse);
                }
            }
        } catch (DeepSeekApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DeepSeekApiException("DeepSeek API 流式调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 查询可用模型列表 (简化版，返回原始JSON字符串)
     */
    public String listModels() {
        try {
            String url = config.getBaseUrl() + MODELS_PATH;
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + config.getApiKey());
            httpPost.setHeader("Content-Type", CONTENT_TYPE_JSON);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new DeepSeekApiException("查询模型列表失败: " + e.getMessage(), e);
        }
    }

    // ========== 内部方法 ==========

    private DeepSeekRequest buildDefaultRequest() {
        DeepSeekRequest.Builder builder = DeepSeekRequest.builder()
                .model(config.getModel())
                .stream(false);

        if (config.isThinkingEnabled()) {
            builder.thinkingEnabled(true);
        }
        if (config.getReasoningEffort() != null) {
            builder.reasoningEffort(config.getReasoningEffort());
        }
        if (config.getSystemPrompt() != null) {
            builder.addSystemMessage(config.getSystemPrompt());
        }
        return builder.build();
    }

    private HttpPost createHttpPost(String url, String jsonBody) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", "Bearer " + config.getApiKey());
        httpPost.setHeader("Content-Type", CONTENT_TYPE_JSON);
        httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        return httpPost;
    }

    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getConnectTimeout())
                .setSocketTimeout(config.getReadTimeout())
                .setConnectionRequestTimeout(config.getConnectTimeout())
                .build();

        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        if (config.hasProxy()) {
            builder.setProxy(new HttpHost(config.getProxyHost(), config.getProxyPort()));
        }

        return builder.build();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.error("[DeepSeek] 关闭客户端失败", e);
        }
    }

    // ========== 异常类 ==========

    /**
     * DeepSeek API 异常
     */
    public static class DeepSeekApiException extends RuntimeException {
        public DeepSeekApiException(String message) {
            super(message);
        }

        public DeepSeekApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
