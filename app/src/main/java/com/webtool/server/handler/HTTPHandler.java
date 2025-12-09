/*
 *
 * MIT License
 *
 * Copyright (c) 2017 朱辉 https://blog.yeetor.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.webtool.server.handler;

import com.webtool.server.HttpServer;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

public class HTTPHandler extends SimpleChannelInboundHandler<Object> {
    
    HttpServer server;
    
    public HTTPHandler(HttpServer server) {
        this.server = server;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) {
            throw new IllegalArgumentException("Not a http request!");
        }
        HttpRequest request = (HttpRequest) msg;
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        boolean isKeepAlive = HttpUtil.isKeepAlive(request);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Server", "Yeetor");
        
        server.onRequest(ctx, request, response);
        
//        HttpResponse response = server.onRequest(ctx, request);
//        if (response == null) {
//            return;
//        }
        
//        if (isKeepAlive) {
//            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        } else {
//            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//        }
//        ctx.writeAndFlush(response);
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext channelHandlerContext) throws Exception {

    }

    @Override
    public ChannelBuf newInboundBuffer(ChannelHandlerContext channelHandlerContext) throws Exception {
        return null;
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext channelHandlerContext, ChannelBuf channelBuf) throws Exception {

    }
}
 
