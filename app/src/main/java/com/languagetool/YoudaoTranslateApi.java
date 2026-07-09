package com.languagetool;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class YoudaoTranslateApi {

    private static final String TRANSLATE_API_URL = "https://openapi.youdao.com/api";

    private final String appKey;
    private final String appSecret;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param appKey    应用ID
     * @param appSecret 应用密钥
     */
    public YoudaoTranslateApi(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行文本翻译（自动检测源语言）
     *
     * @param text       待翻译文本
     * @param targetLang 目标语言
     * @return 翻译结果对象
     * @throws Exception 翻译异常
     */
    public TranslateResult translate(String text, LanguageEnum targetLang) throws Exception {
        return translate(text, LanguageEnum.AUTO, targetLang);
    }

    /**
     * 执行文本翻译
     *
     * @param text       待翻译文本
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 翻译结果对象
     * @throws Exception 翻译异常
     */
    public TranslateResult translate(String text, LanguageEnum sourceLang, LanguageEnum targetLang) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("待翻译文本不能为空");
        }

        Map<String, String[]> params = createRequestParams(text, sourceLang, targetLang);
        AuthV3Util.addAuthParams(appKey, appSecret, params);

        byte[] result = HttpUtil.doPost(TRANSLATE_API_URL, null, params, StandardCharsets.UTF_8);
        String responseStr = new String(result, StandardCharsets.UTF_8);

        return objectMapper.readValue(responseStr, TranslateResult.class);
    }

    /**
     * 批量翻译文本
     *
     * @param texts      待翻译文本数组
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 翻译结果数组
     * @throws Exception 翻译异常
     */
    public TranslateResult[] batchTranslate(String[] texts, LanguageEnum sourceLang, LanguageEnum targetLang)
            throws Exception {
        if (texts == null || texts.length == 0) {
            throw new IllegalArgumentException("待翻译文本数组不能为空");
        }

        TranslateResult[] results = new TranslateResult[texts.length];
        for (int i = 0; i < texts.length; i++) {
            results[i] = translate(texts[i], sourceLang, targetLang);
        }

        return results;
    }

    /**
     * 快速翻译（使用默认配置）
     *
     * @param text 待翻译文本
     * @return 翻译结果字符串
     * @throws Exception 翻译异常
     */
    public String quickTranslate(String text) throws Exception {
        TranslateResult result = translate(text, LanguageEnum.EN);
        if (result.isSuccess() && result.getTranslation() != null) {
            return String.join("\n", result.getTranslation());
        }
        return "翻译失败，错误码: " + result.getErrorCode();
    }

    /**
     * 创建请求参数
     *
     * @param text       待翻译文本
     * @param sourceLang 源语言
     * @param targetLang 目标语言
     * @return 请求参数Map
     */
    private Map<String, String[]> createRequestParams(String text, LanguageEnum sourceLang, LanguageEnum targetLang) {
        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{text});
        params.put("from", new String[]{sourceLang.getCode()});
        params.put("to", new String[]{targetLang.getCode()});
        return params;
    }
}
