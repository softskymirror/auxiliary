package com.webtool.captureutils.handler.proxy;


import com.webtool.captureutils.bean.ClientRequest;
import com.webtool.captureutils.handler.response.SocksResponseHandler;
import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ChannelBuf;
import io.netty.channel.*;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.captureutils.handler.proxy.IProxyHandler;
import static com.webtool.captureutils.bean.Constans.CLIENTREQUEST_ATTRIBUTE_KEY;


/**
 * socks?????handler
 *
 * @author puhaiyang
 * created on 2019/10/25 20:56
 */
public class SocksProxyHandler extends ChannelInboundHandlerAdapter implements IProxyHandler {
    private Logger logger = LoggerFactory.getLogger(HttpsProxyHandler.class);

    private ChannelFuture notHttpReuqstCf;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("[SocksProxyHandler]");
        Attribute<ClientRequest> clientRequestAttribute = ctx.channel().attr(CLIENTREQUEST_ATTRIBUTE_KEY);
        ClientRequest clientRequest = clientRequestAttribute.get();
        sendToServer(clientRequest, ctx, msg);
    }

    @Override
    public void sendToServer(ClientRequest clientRequest, ChannelHandlerContext ctx, Object msg) {
        //????http????????????????
        if (notHttpReuqstCf == null) {
            //??????????????
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop())
                    // ????????????????
                    .channel(ctx.channel().getClass())
                    // ???NioSocketChannel????????????channel??
                    .handler(new ChannelInitializer() {
                        @Override
                        public void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new SocksResponseHandler(ctx.channel()));
                        }
                    });
            notHttpReuqstCf = bootstrap.connect();
            notHttpReuqstCf.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        future.channel().write(msg);
                    } else {
                        ctx.channel().close();
                    }
                }
            });
        } else {
            notHttpReuqstCf.channel().write(msg);
        }
    }

    @Override
    public void sendToClient(ClientRequest clientRequest, ChannelHandlerContext ctx, Object msg) {

    }

}