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

package com.adbtool.androidcontrol.client;

import com.alibaba.fastjson.JSONObject;
import com.adbtool.adb.AdbDevice;
import com.adbtool.adb.AdbServer;
import com.adbtool.androidcontrol.Command;
import com.adbtool.androidcontrol.Protocol;
import com.adbtool.minicap.Banner;
import com.adbtool.minicap.Minicap;
import com.adbtool.minicap.MinicapListener;
import com.adbtool.minitouch.Minitouch;
import com.adbtool.minitouch.MinitouchListener;
import com.adbtool.util.Constant;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.log4j.Logger;

/**
 * LocalClient - executes commands locally via Netty WebSocket channel.
 */
public class LocalClient extends BaseClient implements MinicapListener, MinitouchListener {

    private static final Logger logger = Logger.getLogger(LocalClient.class);

    private Protocol protocol;

    public LocalClient(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected void sendImage(byte[] data) {
        if (protocol != null && protocol.getBroswerSocket() != null) {
            logger.debug("Sending image, thread:" + Thread.currentThread().getId());
            protocol.getBroswerSocket().channel().writeAndFlush(
                    new BinaryWebSocketFrame(Unpooled.copiedBuffer(data)));
        }
    }

    public void executeCommand(ChannelHandlerContext ctx, Command command) {
        switch (command.getSchem()) {
            case START:
                startCommand(ctx, command);
                break;
            case TOUCH:
                touchCommand(ctx, command);
                break; // fixed: was missing break
            case WAITTING:
                waittingCommand(ctx, command);
                break;
            case KEYEVENT:
                keyeventCommand(command);
                break;
            case INPUT:
                inputCommand(command);
                break;
            case PUSH:
                pushCommand(command);
                break;
            default:
                logger.warn("Unhandled command schem: " + command.getSchem());
                break;
        }
    }

    // minicap callbacks
    @Override
    public void onStartup(Minicap minicap, boolean success) {
        if (protocol != null && protocol.getBroswerSocket() != null && success) {
            protocol.getBroswerSocket().channel().writeAndFlush(new TextWebSocketFrame("minicap://open"));
        }
    }

    @Override
    public void onClose(Minicap minicap) {
        if (protocol != null && protocol.getBroswerSocket() != null) {
            protocol.getBroswerSocket().channel().writeAndFlush(new TextWebSocketFrame("minicap://close"));
        }
    }

    @Override
    public void onBanner(Minicap minicap, Banner banner) {}

    @Override
    public void onJPG(Minicap minicap, byte[] data) {
        onNewJPG(data);
    }

    // minitouch callbacks
    @Override
    public void onStartup(Minitouch minitouch, boolean success) {
        if (protocol != null && protocol.getBroswerSocket() != null && success) {
            protocol.getBroswerSocket().channel().writeAndFlush(new TextWebSocketFrame("minitouch://open"));
        }
    }

    @Override
    public void onClose(Minitouch minitouch) {
        if (protocol != null && protocol.getBroswerSocket() != null) {
            protocol.getBroswerSocket().channel().writeAndFlush(new TextWebSocketFrame("minitouch://close"));
        }
    }

    public void setWaitting(boolean waitting) {
        setWaiting(waitting);
    }

    // --- command handlers ---

    private void startCommand(ChannelHandlerContext ctx, Command command) {
        String str = command.getString("type", null);
        if (str != null) {
            if (str.equals("minicap")) {
                startMinicap(command);
            } else if (str.equals("minitouch")) {
                startMinitouch(command);
            }
        }
    }

    private void waittingCommand(ChannelHandlerContext ctx, Command command) {
        setWaitting(true);
    }

    private void keyeventCommand(Command command) {
        try {
            int k = Integer.parseInt(command.getContent());
            if (protocol.getMinitouch() != null) {
                protocol.getMinitouch().sendKeyEvent(k);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid keyevent: " + command.getContent());
        }
    }

    private void touchCommand(ChannelHandlerContext ctx, Command command) {
        String str = (String) command.getContent();
        if (protocol.getMinitouch() != null) {
            protocol.getMinitouch().sendEvent(str);
        }
    }

    private void inputCommand(Command command) {
        String str = (String) command.getContent();
        if (protocol.getMinitouch() != null) {
            protocol.getMinitouch().inputText(str);
        }
    }

    private void pushCommand(Command command) {
        String name = command.getString("name", null);
        String path = command.getString("path", null);

        AdbDevice device = AdbServer.server().getDevice(protocol.getSn());
        try {
            if (device != null) {
                device.getIDevice().pushFile(Constant.getTmpFile(name).getAbsolutePath(), path + "/" + name);
            }
        } catch (Exception e) {
            logger.error("Failed to push file: " + name, e);
        }
        if (protocol != null && protocol.getBroswerSocket() != null) {
            protocol.getBroswerSocket().channel().writeAndFlush(new TextWebSocketFrame("message://pushfile success"));
        }
    }

    private void startMinicap(Command command) {
        if (protocol.getMinicap() != null) {
            protocol.getMinicap().kill();
        }

        JSONObject obj = (JSONObject) command.get("config");
        Float scale = obj.getFloat("scale");
        Float rotate = obj.getFloat("rotate");
        if (scale == null) { scale = 0.3f; }
        if (scale < 0.01) { scale = 0.01f; }
        if (scale > 1.0) { scale = 1.0f; }
        if (rotate == null) { rotate = 0.0f; }
        Minicap minicap = new Minicap(protocol.getSn());
        minicap.addEventListener(this);
        minicap.start(scale, rotate.intValue());
        protocol.setMinicap(minicap);
    }

    private void startMinitouch(Command command) {
        if (protocol.getMinitouch() != null) {
            protocol.getMinitouch().kill();
        }

        Minitouch minitouch = new Minitouch(protocol.getSn());
        minitouch.addEventListener(this);
        minitouch.start();
        protocol.setMinitouch(minitouch);
    }

}
