package com.webtool.captureutils.handler.response;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;


/**
 * https����responseHandler
 * created on 2019/10/28 15:00
 *
 * @author puhaiyang
 */
public class HttpProxyResponseHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(HttpProxyResponseHandler.class);
    private Channel clientChannel;

    public HttpProxyResponseHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            logger.debug("[channelRead][FullHttpResponse] ���յ�Զ�̵�����1 content:{}", response.content().toString(Charset.defaultCharset()));
        } else if (msg instanceof DefaultHttpResponse) {
            DefaultHttpResponse response = (DefaultHttpResponse) msg;
            logger.debug("[channelRead][FullHttpResponse] ���յ�Զ�̵����� content:{}", response.toString());
        } else if (msg instanceof DefaultHttpContent) {
            DefaultHttpContent httpContent = (DefaultHttpContent) msg;
            logger.debug("[channelRead][DefaultHttpContent] ���յ�Զ�̵����� content:{}", httpContent.content().toString(Charset.defaultCharset()));
        } else {
            logger.debug("[channelRead] ���յ�Զ�̵����� " + msg.toString());
        }
        //���͸��ͻ���
        clientChannel.write(msg);
    }

}
