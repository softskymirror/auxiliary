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
import com.neovisionaries.ws.client.*;
import com.adbtool.adb.AdbDevice;
import com.adbtool.adb.AdbServer;
import com.adbtool.androidcontrol.Command;
import com.adbtool.androidcontrol.message.BinaryMessage;
import com.adbtool.androidcontrol.message.FileMessage;
import com.adbtool.minicap.Banner;
import com.adbtool.minicap.Minicap;
import com.adbtool.minicap.MinicapListener;
import com.adbtool.minitouch.Minitouch;
import com.adbtool.minitouch.MinitouchListener;
import com.adbtool.util.Constant;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * RemoteClient - connects to a remote server via WebSocket, proxies commands locally.
 */
public class RemoteClient extends BaseClient implements MinicapListener, MinitouchListener {

    private static final Logger logger = Logger.getLogger(RemoteClient.class);

    private String ip;
    private int port;
    private String key;
    private String serialNumber;
    private WebSocket ws;

    Minicap minicap = null;
    Minitouch minitouch = null;

    public RemoteClient(String ip, int port, String key, String serialNumber) throws IOException, WebSocketException {
        this.ip = ip;
        this.port = port;
        this.key = key;
        this.serialNumber = serialNumber;
        if (serialNumber == null || serialNumber.isEmpty()) {
            AdbDevice device = AdbServer.server().getFirstDevice();
            if (device == null)
                throw new RuntimeException("No device found!");
            this.serialNumber = device.getIDevice().getSerialNumber();
        }

        ws = new WebSocketFactory().createSocket("ws://" + ip + ":" + port);
        ws.addListener(new MyWebsocketEvent());
        ws.connect();
    }

    @Override
    protected void sendImage(byte[] data) {
        if (ws != null) {
            ws.sendBinary(data);
        }
    }

    // --- Minicap callbacks ---
    @Override
    public void onStartup(Minicap minicap, boolean success) {
        if (ws != null) {
            ws.sendText("minicap://open");
        }
    }

    @Override
    public void onClose(Minicap minicap) {
        if (ws != null) {
            ws.sendText("minicap://close");
        }
    }

    @Override
    public void onBanner(Minicap minicap, Banner banner) {}

    @Override
    public void onJPG(Minicap minicap, byte[] data) {
        onNewJPG(data);
    }

    // --- Minitouch callbacks ---
    @Override
    public void onStartup(Minitouch minitouch, boolean success) {
        if (ws != null) {
            ws.sendText("minitouch://open");
        }
    }

    @Override
    public void onClose(Minitouch minitouch) {
        if (ws != null) {
            ws.sendText("minitouch://close");
        }
    }

    public void setWaitting(boolean waitting) {
        setWaiting(waitting);
    }

    void executeCommand(Command command) {
        switch (command.getSchem()) {
            case START:
                startCommand(command);
                break;
            case TOUCH:
                touchCommand(command);
                break; // fixed: was missing break
            case WAITTING:
                waittingCommand(command);
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

    private void startCommand(Command command) {
        String str = command.getString("type", null);
        if (str != null) {
            if (str.equals("minicap")) {
                startMinicap(command);
            } else if (str.equals("minitouch")) {
                startMinitouch(command);
            }
        }
    }

    private void waittingCommand(Command command) {
        setWaitting(true);
    }

    private void keyeventCommand(Command command) {
        try {
            int k = Integer.parseInt(command.getContent());
            if (minitouch != null) minitouch.sendKeyEvent(k);
        } catch (NumberFormatException e) {
            logger.warn("Invalid keyevent: " + command.getContent());
        }
    }

    private void touchCommand(Command command) {
        String str = (String) command.getContent();
        if (minitouch != null) minitouch.sendEvent(str);
    }

    private void inputCommand(Command command) {
        String str = (String) command.getContent();
        if (minitouch != null) minitouch.inputText(str);
    }

    private void pushCommand(Command command) {
        String name = command.getString("name", null);
        String path = command.getString("path", null);

        AdbDevice device = AdbServer.server().getDevice(serialNumber);
        try {
            if (device != null) {
                device.getIDevice().pushFile(Constant.getTmpFile(name).getAbsolutePath(), path + "/" + name);
            }
        } catch (Exception e) {
            logger.error("Failed to push file: " + name, e);
        }
        if (ws != null) {
            ws.sendText("message://pushfile success");
        }
    }

    private void startMinicap(Command command) {
        if (minicap != null) {
            minicap.kill();
        }
        JSONObject obj = (JSONObject) command.get("config");
        Float scale = obj.getFloat("scale");
        Float rotate = obj.getFloat("rotate");
        if (scale == null) { scale = 0.3f; }
        if (scale < 0.01) { scale = 0.01f; }
        if (scale > 1.0) { scale = 1.0f; }
        if (rotate == null) { rotate = 0.0f; }
        Minicap minicap = new Minicap(serialNumber);
        minicap.addEventListener(this);
        minicap.start(scale, rotate.intValue());
        this.minicap = minicap;
    }

    private void startMinitouch(Command command) {
        if (minitouch != null) {
            minitouch.kill(); // fixed: was incorrectly killing minicap
        }

        Minitouch minitouch = new Minitouch(serialNumber);
        minitouch.addEventListener(this);
        minitouch.start();
        this.minitouch = minitouch;
    }

    class MyWebsocketEvent extends WebSocketAdapter {
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
            logger.info("Connected to server " + ip + ":" + port);
            JSONObject obj = new JSONObject();
            obj.put("sn", serialNumber);
            obj.put("key", key);
            websocket.sendText("open://" + obj.toJSONString());
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) {
            Command command = Command.parseCommand(text);
            if (command != null) {
                switch (command.getSchem()) {
                    case START:
                    case WAITTING:
                    case TOUCH:
                    case KEYEVENT:
                    case INPUT:
                    case PUSH:
                        executeCommand(command);
                        break;
                    default:
                        logger.warn("Unhandled remote command: " + command.getSchem());
                        break;
                }
            }
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] data) {
            int headlen = (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
            String infoJSON = new String(data, 2, headlen);
            BinaryMessage message = BinaryMessage.parse(infoJSON);
            logger.debug("Binary message received: " + infoJSON);
            if (message != null && "file".equals(message.getType())) {
                FileMessage fileMessage = (FileMessage) message;
                File file = Constant.getTmpFile(fileMessage.name);
                if (fileMessage.offset == 0 && file.exists()) {
                    file.delete();
                }
                try {
                    FileOutputStream os = new FileOutputStream(file, true);
                    byte[] bs = Arrays.copyOfRange(data, 2 + headlen, data.length);
                    os.write(bs);
                    os.close();
                } catch (FileNotFoundException e) {
                    logger.error("File not found: " + fileMessage.name, e);
                } catch (IOException e) {
                    logger.error("IO error writing file: " + fileMessage.name, e);
                }
                if (fileMessage.offset + fileMessage.packagesize == fileMessage.filesize) {
                    ws.sendText("message://upload file success");
                }
            }
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
            logger.warn("Server disconnected: " + ip + ":" + port);
            System.exit(0);
        }
    }

}
