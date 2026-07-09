package com.languagetool;

public enum LanguageEnum {
    AUTO("auto", "自动检测"),
    ZH_CHS("zh-CHS", "中文简体"),
    ZH_CHT("zh-CHT", "中文繁体"),
    EN("en", "英语"),
    JA("ja", "日语"),
    KO("ko", "韩语"),
    FR("fr", "法语"),
    ES("es", "西班牙语"),
    PT("pt", "葡萄牙语"),
    IT("it", "意大利语"),
    RU("ru", "俄语"),
    VI("vi", "越南语"),
    DE("de", "德语"),
    AR("ar", "阿拉伯语"),
    ID("id", "印尼语"),
    TH("th", "泰语");

    private final String code;
    private final String description;

    LanguageEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

