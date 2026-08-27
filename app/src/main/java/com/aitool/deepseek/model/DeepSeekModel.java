package com.aitool.deepseek.model;

/**
 * DeepSeek 支持的模型枚举
 * <p>
 * 参考 DeepSeek 官方文档: https://api-docs.deepseek.com/
 * 以及 deepseek4j SDK: https://github.com/pig-mesh/deepseek4j
 * </p>
 */
public enum DeepSeekModel {

    /** DeepSeek V4 Pro - 旗舰推理模型，适合复杂任务 */
    DEEPSEEK_V4_PRO("deepseek-v4-pro"),

    /** DeepSeek V4 Flash - 轻量快速模型，适合简单任务 */
    DEEPSEEK_V4_FLASH("deepseek-v4-flash"),

    /** DeepSeek Chat (旧版，已弃用) */
    @Deprecated
    DEEPSEEK_CHAT("deepseek-chat"),

    /** DeepSeek Reasoner (旧版，已弃用) */
    @Deprecated
    DEEPSEEK_REASONER("deepseek-reasoner");

    private final String value;

    DeepSeekModel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
