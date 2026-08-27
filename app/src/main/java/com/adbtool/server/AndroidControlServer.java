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

package com.adbtool.server;


import com.adbtool.server.handler.HTTPHandler;
import com.adbtool.server.handler.TCPHandler;
import com.adbtool.server.handler.WSHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.log4j.Logger;

/**
 * AndroidControlServer - Netty-based server for Android device control.
 * <p>
 * Supports both blocking (production) and non-blocking (test) modes.
 * <ul>
 *   <li>{@link #start(int)} - non-blocking, returns immediately after binding</li>
 *   <li>{@link #listen(int)} - blocking, waits until server channel is closed</li>
 *   <li>{@link #stop()} - graceful shutdown</li>
 * </ul>
 */
public class AndroidControlServer extends BaseServer {

    private static final Logger logger = Logger.getLogger(AndroidControlServer.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private volatile Channel serverChannel;

    /**
     * Start the server in non-blocking mode.
     * Binds to the given port and returns immediately.
     * Call {@link #stop()} to shut down.
     *
     * @param port the port to bind
     * @throws InterruptedException if binding is interrupted
     */
    public void start(int port) throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("tcp", new TCPHandler());
                        ch.pipeline().addLast("http-codec", new HttpServerCodec());
                        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                        ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                        ch.pipeline().addLast("websocket", new WSHandler(new WSServer()));
                        ch.pipeline().addLast("http", new HTTPHandler(new HttpServer()));
                    }
                });

        ChannelFuture f = b.bind(port).sync();
        serverChannel = f.channel();
        logger.info("AndroidControlServer started on port " + port);
    }

    /**
     * Start the server in blocking mode.
     * Binds to the given port and blocks until the server channel is closed.
     *
     * @param port the port to bind
     * @throws InterruptedException if binding or waiting is interrupted
     */
    public void listen(int port) throws InterruptedException {
        start(port);
        try {
            serverChannel.closeFuture().sync();
        } finally {
            stop();
        }
    }

    /**
     * Gracefully shut down the server.
     */
    public void stop() {
        logger.info("Shutting down AndroidControlServer...");
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("AndroidControlServer stopped.");
    }

    /**
     * Check if the server is currently running.
     *
     * @return true if the server channel is active
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}
