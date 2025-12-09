package com.captureutils.handler.proxy;


import com.webtool.captureutils.bean.ClientRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * ����handler
 *
 * @author puhaiyang
 * created on 2019/10/25 23:07
 */
public interface IProxyHandler {
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    /**
     * server中间代理接口
     *
     * @param clientRequest 处理客户端数据包信息并发送至
     * @param ctx           ChannelHandlerContext
     * @param msg
     */
    void sendToServer(ClientRequest clientRequest, final ChannelHandlerContext ctx, final Object msg);
    /**
     *client中间代理接口
     *处理
     */
    void sendToClient(ClientRequest clientRequest, final ChannelHandlerContext ctx, final Object msg);
}
