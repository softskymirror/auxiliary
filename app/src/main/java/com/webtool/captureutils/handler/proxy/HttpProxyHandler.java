package com.webtool.captureutils.handler.proxy;

import com.webtool.captureutils.bean.ClientRequest;
import com.webtool.captureutils.bean.Constans;
import com.webtool.captureutils.handler.response.HttpProxyResponseHandler;
import com.webtool.captureutils.utils.ProxyRequestUtil;
import com.captureutils.handler.proxy.IProxyHandler;
import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ChannelBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.webtool.captureutils.bean.Constans.CLIENTREQUEST_ATTRIBUTE_KEY;

/**
 * ?
 * created on 2019/10/25 18:00
 *
 * @author puhaiyang
 */
public class HttpProxyHandler extends ChannelInboundHandlerAdapter implements IProxyHandler {
    private Logger logger = LoggerFactory.getLogger(HttpProxyHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("[HttpProxyHandler]");
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            //????????????
            ClientRequest clientRequest = ProxyRequestUtil.getClientRequest(ctx.channel());
            if (clientRequest == null) {
                //??????????��??
                Attribute<ClientRequest> clientRequestAttribute = ctx.channel().attr(CLIENTREQUEST_ATTRIBUTE_KEY);
                clientRequest = ProxyRequestUtil.getClientReuqest(httpRequest);
                //??clientRequest???��channel??
                clientRequestAttribute.setIfAbsent(clientRequest);
            }
            //?????connect???????????????????????
            if (sendSuccessResponseIfConnectMethod(ctx, httpRequest.getMethod().name())) {
                logger.debug("[HttpProxyHandler][channelRead]sendSuccessResponseConnect");
                ctx.channel().pipeline().remove("httpRequestDecoder");
                ctx.channel().pipeline().remove("httpResponseEncoder");
                ctx.channel().pipeline().remove("httpAggregator");
                ReferenceCountUtil.release(msg);
                return;
            }
            if (clientRequest.isHttps()) {
                //https???????????
                channelRead(ctx, msg);
                return;
            }
            sendToServer(clientRequest, ctx, msg);
            return;
        }
        channelRead(ctx, msg);
    }

    /**
     * ?????connect??????????????????????
     *
     * @param ctx        ChannelHandlerContext
     * @param methodName ??????????
     * @return ????connect????
     */
    private boolean sendSuccessResponseIfConnectMethod(ChannelHandlerContext ctx, String methodName) {
        if (Constans.CONNECT_METHOD_NAME.equalsIgnoreCase(methodName)) {
            //?????????
            //HTTP??????????
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, Constans.CONNECT_SUCCESS);
            ctx.write(response);
            return true;
        }
        return false;
    }


    @Override
    public void sendToServer(ClientRequest clientRequest, ChannelHandlerContext ctx, Object msg) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                // ???????
                .channel(ctx.channel().getClass())
                // ???NioSocketChannel????????????channel??
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        //?????????server??handler
                        ch.pipeline().addLast(new HttpRequestEncoder());
                        ch.pipeline().addLast(new HttpResponseDecoder());
                        ch.pipeline().addLast(new HttpObjectAggregator(6553600));
                        //????handler,????????????????
                        ch.pipeline().addLast(new HttpProxyResponseHandler(ctx.channel()));
                    }
                });

        //???????server
        ChannelFuture cf = bootstrap.connect();
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    //??????
                    future.channel().write(msg);
                    logger.debug("[operationComplete] connect remote server success!");
                } else {
                    //???????
                    logger.error("[operationComplete] ???????server?????");
                    ctx.channel().close();
                }
            }
        });
    }

    @Override
    public void sendToClient(ClientRequest clientRequest, ChannelHandlerContext ctx, Object msg) {

    }




}
