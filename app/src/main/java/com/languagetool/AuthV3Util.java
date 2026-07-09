package com.languagetool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

public class AuthV3Util {

    /**
     * 添加鉴权相关参数
     *
     * @param appKey    应用ID
     * @param appSecret 应用密钥
     * @param paramsMap 请求参数表
     * @throws NoSuchAlgorithmException 算法不存在异常
     */
    public static void addAuthParams(String appKey, String appSecret, Map<String, String[]> paramsMap)
            throws NoSuchAlgorithmException {
        String[] qArray = paramsMap.get("q");
        if (qArray == null) {
            qArray = paramsMap.get("img");
        }

        StringBuilder q = new StringBuilder();
        for (String item : qArray) {
            q.append(item);
        }

        String salt = UUID.randomUUID().toString();
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        String sign = calculateSign(appKey, appSecret, q.toString(), salt, curtime);

        paramsMap.put("appKey", new String[]{appKey});
        paramsMap.put("salt", new String[]{salt});
        paramsMap.put("curtime", new String[]{curtime});
        paramsMap.put("signType", new String[]{"v3"});
        paramsMap.put("sign", new String[]{sign});
    }

    /**
     * 计算签名
     *
     * @param appKey    应用ID
     * @param appSecret 应用密钥
     * @param q         待翻译文本
     * @param salt      随机字符串
     * @param curtime   当前时间戳(秒)
     * @return 签名字符串
     * @throws NoSuchAlgorithmException 算法不存在异常
     */
    private static String calculateSign(String appKey, String appSecret, String q, String salt, String curtime)
            throws NoSuchAlgorithmException {
        String input = getInput(q);
        String signSrc = appKey + input + salt + curtime + appSecret;
        return sha256(signSrc);
    }

    /**
     * 计算input字段
     *
     * @param q 待翻译文本
     * @return input字段值
     */
    private static String getInput(String q) {
        if (q == null) {
            return "";
        }

        int len = q.length();
        if (len <= 20) {
            return q;
        }

        String head = q.substring(0, 10);
        String tail = q.substring(len - 10);
        return head + len + tail;
    }

    /**
     * SHA256加密
     *
     * @param src 待加密字符串
     * @return 加密后的十六进制字符串
     * @throws NoSuchAlgorithmException 算法不存在异常
     */
    private static String sha256(String src) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(src.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
