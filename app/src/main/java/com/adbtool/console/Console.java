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

package com.adbtool.console;

import com.adbtool.androidcontrol.server.BaseServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Console - TCP-based command console with registry-based command dispatch.
 */
public class Console {

    private static final Logger logger = Logger.getLogger(Console.class);

    private static final String PROMPT = "> ";
    private static final String BANNER = "help - show help\r\nhello - greeting\r\n";

    private static Console instance;

    private final Map<String, Function<String, Command>> commandRegistry = new HashMap<>();

    public static Console getInstance() {
        if (instance == null) {
            instance = new Console();
            instance.commandRegistry.put("help", HelpCommand::new);
            instance.commandRegistry.put("hello", HelloCommand::new);
            instance.commandRegistry.put("device", DeviceCommand::new);
        }
        return instance;
    }

    /**
     * Register a new console command.
     * @param name command name
     * @param factory function that creates a Command from the input string
     */
    public void registerCommand(String name, Function<String, Command> factory) {
        commandRegistry.put(name, factory);
    }

    /**
     * Start listening on the given TCP port.
     */
    public void listenOnTCP(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new Adapter());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port);
            logger.info("Console listening on port: " + port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Console interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    class Adapter extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byte[] res = new byte[buf.readableBytes()];
            buf.readBytes(res);
            buf.release();

            String str = new String(res).trim();
            if (str.isEmpty()) return;

            Function<String, Command> factory = commandRegistry.get(str);
            if (factory != null) {
                try {
                    Command command = factory.apply(str);
                    sendStringL(ctx, command.execute());
                } catch (Exception e) {
                    logger.error("Command execution failed: " + str, e);
                    sendString(ctx, "error: " + e.getMessage() + "\n");
                }
            } else {
                sendString(ctx, "command not found\n");
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            sendString(ctx, BANNER);
            sendPrompt(ctx);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            sendPrompt(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("Console handler error", cause);
            ctx.close();
        }

        private void sendStringL(ChannelHandlerContext ctx, String text) {
            if (!text.endsWith("\n")) {
                text += "\n";
            }
            sendString(ctx, text);
        }

        private void sendString(ChannelHandlerContext ctx, String text) {
            ByteBuf encoded = ctx.alloc().buffer(4 * text.length());
            encoded.writeBytes(text.getBytes());
            ctx.write(encoded);
            ctx.flush();
        }

        private void sendPrompt(ChannelHandlerContext ctx) {
            sendString(ctx, PROMPT);
        }
    }

    // --- Command base and implementations ---

    public abstract static class Command {
        private final String command;

        public Command(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }

        public abstract String execute();
    }

    public static class HelpCommand extends Command {
        HelpCommand(String command) {
            super(command);
        }

        @Override
        public String execute() {
            return "Available commands: help, hello, device";
        }
    }

    public static class HelloCommand extends Command {
        static final String[] STRINGS = {
                "Talking is cheap, show me the code!",
                "Visit <http://yeetor.com>"
        };

        HelloCommand(String command) {
            super(command);
        }

        @Override
        public String execute() {
            return STRINGS[RandomUtils.nextInt(0, STRINGS.length)];
        }
    }

    public static class DeviceCommand extends Command {
        public DeviceCommand(String command) {
            super(command);
        }

        @Override
        public String execute() {
            return BaseServer.getDevicesJSON();
        }
    }
}
