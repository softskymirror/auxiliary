package com.languagetool;

import java.util.List;

public class TranslateResult {
    private String errorCode;
    private String query;
    private List<String> translation;
    private BasicInfo basic;
    private List<WebMeaning> web;
    private String l;

    public static class BasicInfo {
        private String phonetic;
        private String ukPhonetic;
        private String usPhonetic;
        private List<String> explains;
        private List<String> ukExplains;
        private List<String> usExplains;

        public String getPhonetic() {
            return phonetic;
        }

        public void setPhonetic(String phonetic) {
            this.phonetic = phonetic;
        }

        public String getUkPhonetic() {
            return ukPhonetic;
        }

        public void setUkPhonetic(String ukPhonetic) {
            this.ukPhonetic = ukPhonetic;
        }

        public String getUsPhonetic() {
            return usPhonetic;
        }

        public void setUsPhonetic(String usPhonetic) {
            this.usPhonetic = usPhonetic;
        }

        public List<String> getExplains() {
            return explains;
        }

        public void setExplains(List<String> explains) {
            this.explains = explains;
        }

        public List<String> getUkExplains() {
            return ukExplains;
        }

        public void setUkExplains(List<String> ukExplains) {
            this.ukExplains = ukExplains;
        }

        public List<String> getUsExplains() {
            return usExplains;
        }

        public void setUsExplains(List<String> usExplains) {
            this.usExplains = usExplains;
        }
    }

    public static class WebMeaning {
        private String key;
        private List<String> value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public List<String> getValue() {
            return value;
        }

        public void setValue(List<String> value) {
            this.value = value;
        }
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getTranslation() {
        return translation;
    }

    public void setTranslation(List<String> translation) {
        this.translation = translation;
    }

    public BasicInfo getBasic() {
        return basic;
    }

    public void setBasic(BasicInfo basic) {
        this.basic = basic;
    }

    public List<WebMeaning> getWeb() {
        return web;
    }

    public void setWeb(List<WebMeaning> web) {
        this.web = web;
    }

    public String getL() {
        return l;
    }

    public void setL(String l) {
        this.l = l;
    }

    public boolean isSuccess() {
        return "0".equals(errorCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (translation != null && !translation.isEmpty()) {
            sb.append("翻译结果: ").append(String.join("; ", translation));
        }
        if (basic != null) {
            if (basic.getPhonetic() != null) {
                sb.append("\n音标: ").append(basic.getPhonetic());
            }
            if (basic.getExplains() != null) {
                sb.append("\n释义: ").append(String.join("; ", basic.getExplains()));
            }
        }
        return sb.toString();
    }
}
