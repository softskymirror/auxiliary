package com.languagetool;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

public class HttpUtil {

    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;

    /**
     * POST请求
     *
     * @param url     请求URL
     * @param headers 请求头参数
     * @param params  表单参数
     * @param charset 字符编码
     * @return 响应内容字节数组
     * @throws IOException IO异常
     */
    public static byte[] doPost(String url, Map<String, String> headers, Map<String, String[]> params, Charset charset)
            throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();

        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder paramBuilder = new StringBuilder();
        if (params != null) {
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    for (String value : values) {
                        if (paramBuilder.length() > 0) {
                            paramBuilder.append("&");
                        }
                        paramBuilder.append(key).append("=").append(value);
                    }
                }
            }
        }

        String paramString = paramBuilder.toString();
        httpPost.setEntity(new org.apache.http.entity.StringEntity(paramString, charset));
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        byte[] result = EntityUtils.toByteArray(entity);

        EntityUtils.consume(entity);
        response.close();
        httpClient.close();

        return result;
    }
}
