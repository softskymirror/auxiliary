package com.webtool.captureutils.handler.proxy;

//import com.github.puhiayang.handler.response.HttpProxyResponseHandler;


import com.webtool.captureutils.bean.ClientRequest;
import com.webtool.captureutils.handler.response.HttpProxyResponseHandler;
import com.captureutils.utils.HttpsSupport;
import com.captureutils.handler.proxy.IProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ChannelBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.webtool.captureutils.bean.Constans.CLIENTREQUEST_ATTRIBUTE_KEY;


/**
 * created on 2019/10/25 18:00
 *
 * @author puhaiyang
 */
public class HttpsProxyHandler extends ChannelInboundHandlerAdapter implements IProxyHandler {
    private Logger logger = LoggerFactory.getLogger(HttpsProxyHandler.class);
    private ChannelFuture httpsRequestCf;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("[HttpsProxyHandler]");
        Attribute<ClientRequest> clientRequestAttribute = ctx.channel().attr(CLIENTREQUEST_ATTRIBUTE_KEY);
        ClientRequest clientRequest = clientRequestAttribute.get();
        if (msg instanceof HttpRequest) {
            sendToServer(clientRequest, ctx, msg);
        } else if (msg instanceof HttpContent) {
            logger.debug("[HttpsProxyHandler][HttpContent]????????");
            //content????????
//            ReferenceCountUtil.release(msg);
        } else {
            ByteBuf byteBuf = (ByteBuf) msg;
            // ssl????
            if (byteBuf.getByte(0) == 22) {
                logger.debug("[HttpsProxyHandler][do hands]");
                sendToClient(clientRequest, ctx, msg);
            }
        }
    }

    /**
     * ?��????????????????????????
     * @param clientRequest ?????????
     * @param ctx           ChannelHandlerContext
     * @param msg           ????
     */
    @Override
    public void sendToServer(ClientRequest clientRequest, ChannelHandlerContext ctx, Object msg) {
        logger.debug("[HttpsProxyHandler][sendToServer] ????https????server");
        Channel clientChannel = ctx.channel();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(1))
                // ???????
                .channel(NioSocketChannel.class)
                // ???NioSocketChannel????????????channel??
                .handler(new ChannelInitializer() {

                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        //??????ssl?????????��???
                        ch.pipeline().addLast(
                        HttpsSupport.getInstance().getClientSslCtx().newHandler(ch.alloc(),
                        clientRequest.getHost(), clientRequest.getPort()));
                        ch.pipeline().addLast("httpCodec", new HttpClientCodec());
                        //????????????
                        ch.pipeline().addLast("proxyClientHandle", new HttpProxyResponseHandler(clientChannel));
                    }
                });
        httpsRequestCf = bootstrap.connect();
        //????????
        httpsRequestCf.addListener((ChannelFutureListener)future -> {
            if (future.isSuccess()) {
                future.channel().write(msg);
                logger.debug("[HttpsProxyHandler][sendToServer]?????????????????????????????");
            } else {
                logger.error("[HttpsProxyHandler][sendToServer]???????server???");
            }
        });
    }

    /**
     * ??????????????????????
     * @param clientRequest
     * @param ctx
     * @param msg
     */
    @Override
    public void sendToClient(ClientRequest clientRequest, ChannelHandlerContext ctx, Object msg) {
        try {
            logger.debug("[HttpsProxyHandler][sendToClient] ?????????https????");
            SslContext sslCtx = SslContextBuilder
                    .forServer(HttpsSupport.getInstance().getServerPriKey(), HttpsSupport.getInstance().getCert(clientRequest.getHost())).build();
            //??????????????????????????????
            ctx.pipeline().addFirst("httpRequestDecoder", new HttpRequestDecoder());
            //??????????????????????????????
            ctx.pipeline().addFirst("httpResponseEncoder", new HttpResponseEncoder());
            //http???
            ctx.pipeline().addLast("httpAggregator", new HttpObjectAggregator(65536));
            //ssl????
            ctx.pipeline().addFirst("sslHandle", sslCtx.newHandler(ctx.alloc()));
            // ????????pipeline????????????http????
            ctx.pipeline().fireUserEventTriggered(msg);
            Attribute<ClientRequest> clientRequestAttribute = ctx.channel().attr(CLIENTREQUEST_ATTRIBUTE_KEY);
            clientRequest.setHttps(true);
            clientRequestAttribute.set(clientRequest);
        } catch (Exception e) {
            logger.error("[sendToServer] err:{}", e.getMessage());
        }
    }


}
