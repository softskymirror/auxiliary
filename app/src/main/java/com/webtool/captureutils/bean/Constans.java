package com.webtool.captureutils.bean;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.netty.util.*;
/**
 * ����
 *
 * @author puhaiyang
 * created on 2019/10/25 22:51
 */
public class Constans {
    /**
     * connect������������
     */
    public static final String CONNECT_METHOD_NAME = "CONNECT";
    /**
     * https����Э����
     */
    public static final String HTTPS_PROTOCOL_NAME = "https";
    /**
     * �������ֳɹ���Ӧstatus
     */
    public final static HttpResponseStatus CONNECT_SUCCESS = new HttpResponseStatus(200, "Connection established");
    /**
     * channel�е�clientReuqest
     */
    public final static AttributeKey<ClientRequest> CLIENTREQUEST_ATTRIBUTE_KEY = new AttributeKey("clien;tRequest");

}
