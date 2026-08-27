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

package com.adbtool.androidcontrol;

import com.adbtool.androidcontrol.client.LocalClient;
import com.adbtool.minicap.Minicap;
import com.adbtool.minitouch.Minitouch;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;

/**
 * 设备控制会话管理，维护浏览器和客户端之间的双向WebSocket连接
 */
public class Protocol {
    private static final Logger logger = Logger.getLogger(Protocol.class);

    /** 会话状态枚举 */
    public enum State {
        INITIALIZING, ACTIVE, CLOSING, CLOSED
    }

    /** 默认会话超时时间: 30分钟 */
    public static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000;

    public ChannelHandlerContext broswerSocket;
    public ChannelHandlerContext clientSocket;
    public String key;
    public String sn;
    public Minicap minicap;
    public Minitouch minitouch;
    public LocalClient localClient;

    /** 会话状态 */
    private volatile State state = State.INITIALIZING;
    /** 最后活跃时间戳 */
    private volatile long lastActiveTime = System.currentTimeMillis();
    /** 会话创建时间 */
    private final long createTime = System.currentTimeMillis();

    public void broswerDisconnect() {
        if (clientSocket != null) {
            clientSocket.channel().close();
        }
    }

    public void clientDisconnect() {
        if (broswerSocket != null) {
            broswerSocket.channel().close();
        }
    }

    /** 更新最后活跃时间 */
    public void touch() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /** 检查会话是否已超时 */
    public boolean isTimedOut() {
        return isTimedOut(DEFAULT_TIMEOUT_MS);
    }

    /** 检查会话是否已超时 */
    public boolean isTimedOut(long timeoutMs) {
        return (System.currentTimeMillis() - lastActiveTime) > timeoutMs;
    }

    /** 获取会话状态 */
    public State getState() {
        return state;
    }

    /** 设置会话状态 */
    public void setState(State state) {
        this.state = state;
    }

    /** 获取会话存活时长(ms) */
    public long getAliveDurationMs() {
        return System.currentTimeMillis() - createTime;
    }

    public void setBroswerSocket(ChannelHandlerContext broswerSocket) {
        this.broswerSocket = broswerSocket;
    }

    public void setClientSocket(ChannelHandlerContext clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setMinicap(Minicap minicap) {
        this.minicap = minicap;
    }

    public void setMinitouch(Minitouch minitouch) {
        this.minitouch = minitouch;
    }

    public void setLocalClient(LocalClient localClient) {
        this.localClient = localClient;
    }

    public ChannelHandlerContext getBroswerSocket() {
        return broswerSocket;
    }

    public ChannelHandlerContext getClientSocket() {
        return clientSocket;
    }

    public String getKey() {
        return key;
    }


    public String getSn() {
        return sn;
    }

    public Minicap getMinicap() {
        return minicap;
    }

    public Minitouch getMinitouch() {
        return minitouch;
    }

    public LocalClient getLocalClient() {
        return localClient;
    }

    public void close() {
        setState(State.CLOSING);

        if (minicap != null) {
            try {
                minicap.kill();
            } catch (Exception e) {
                logger.warn("Failed to kill minicap for session " + key, e);
            }
            minicap = null;
        }

        if (minitouch != null) {
            try {
                minitouch.kill();
            } catch (Exception e) {
                logger.warn("Failed to kill minitouch for session " + key, e);
            }
            minitouch = null;
        }

        setState(State.CLOSED);
        logger.info("Protocol session closed: sn=" + sn + ", key=" + key
                + ", alive=" + (getAliveDurationMs() / 1000) + "s");
    }
}
