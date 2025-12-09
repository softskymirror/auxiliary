package com.webtool.captureutils.bean;

import java.io.Serializable;

/**
 * �ͻ�������
 *
 * @author puhaiyang
 * created on 2019/10/25 22:37
 */
public class ClientRequest implements Serializable {
    private String host;
    private int port;
    private boolean isHttps;


    public ClientRequest(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isHttps() {
        return isHttps;
    }

    public void setHttps(boolean https) {
        isHttps = https;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
