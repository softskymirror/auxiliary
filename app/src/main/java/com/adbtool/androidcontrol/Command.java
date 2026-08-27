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

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;

import java.security.InvalidParameterException;

/**
 * Created by harry on 2017/4/26.
 */
public class Command {
    private static final Logger logger = Logger.getLogger(Command.class);

    /**
     * 命令方案枚举 (保留旧名称以兼容)
     */
    public enum Schem {
        WAIT("wait"),
        OPEN("open"),
        START("start"),
        WAITTING("waitting"),
        TOUCH("touch"),
        DEVICES("devices"),
        KEYEVENT("keyevent"),
        INPUT("input"),
        SHOT("shot"),
        MINICAP("minicap"),
        MINITOUCH("minitouch"),
        PUSH("push"),
        MESSAGE("message");

        private String schemStr;

        public String getSchemString() {
            return schemStr;
        }

        Schem(String str) {
            schemStr = str;
        }
    }

    private Schem schem;
    private Object content;
    /** 命令原始字符串，用于日志和调试 */
    private final String rawCommand;

    public Command(String command) throws InvalidParameterException {
        this.rawCommand = command;
        // 截取schem
        int splitIndex = -1;
        if ((splitIndex = command.indexOf("://")) == -1) {
            throw new InvalidParameterException(command + " is not a valid Command");
        }

        String schemStr = command.substring(0, splitIndex);
        switch (schemStr) {
            case "wait":
                schem = Schem.WAIT;
                break;
            case "open":
                schem = Schem.OPEN;
                break;
            case "start":
                schem = Schem.START;
                break;
            case "waitting":
                schem = Schem.WAITTING;
                break;
            case "touch":
                schem = Schem.TOUCH;
                break;
            case "devices":
                schem = Schem.DEVICES;
                break;
            case "keyevent":
                schem = Schem.KEYEVENT;
                break;
            case "shot":
                schem = Schem.SHOT;
                break;
            case "input":
                schem = Schem.INPUT;
                break;
            case "minicap":
                schem = Schem.MINICAP;
                break;
            case "minitouch":
                schem = Schem.MINITOUCH;
                break;
            case "push":
                schem = Schem.PUSH;
                break;
            case "message":
                schem = Schem.MESSAGE;
                break;
            default:
                throw new InvalidParameterException("Unknown scheme: " + schemStr);
        }

        String contentStr = command.substring(splitIndex + 3);

        // 此消息不是json格式。其他都为json键值对
        if (!schem.equals(Schem.TOUCH) && !schem.equals(Schem.KEYEVENT) && !schem.equals(Schem.INPUT) && !schem.equals(Schem.MINICAP) && !schem.equals(Schem.MINITOUCH) && !schem.equals(Schem.MESSAGE)) {
            try {
                this.content = parseContentJson(contentStr);
            } catch (JSONException e) {
                throw new InvalidParameterException(e.getMessage());
            }
        } else {
            this.content = contentStr;
        }
    }

    private Object parseContentJson(String content) throws JSONException {
        if (content == null || content.isEmpty()) {
            return JSONObject.parse("{}");
        }

        Object jsonObj = JSONObject.parse(content);
        if (jsonObj == null) {
            throw new JSONException(content + " 无法解析该json");
        }
        return jsonObj;
    }

    public Schem getSchem() {
        return schem;
    }

    public String getCommandString() {
        return schem.getSchemString() + "://" + getContent();
    }

    public String getContent() {
        if (content != null) {
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof JSONObject){
                return JSONObject.toJSONString(content);
            }
        }
        return "";
    }

    public String getString(String key, String defVal) {
        if (content != null && content instanceof  JSONObject) {
            try {
                String s = ((JSONObject) content).getString(key);
                return s == null ? defVal : s;
            } catch (JSONException e) {
                logger.warn("JSON key lookup failed: " + key, e);
                return defVal;
            }
        }
        return defVal;
    }

    public Object get(String key) {
        if (content != null && content instanceof JSONObject) {
            try {
                return ((JSONObject) content).get(key);
            } catch (JSONException e) {
                logger.warn("JSON key get failed: " + key, e);
                return null;
            }
        }
        return null;
    }

    /**
     * 解析命令字符串，返回Command对象，解析失败返回null
     * @param command 命令字符串
     * @return Command对象或null
     */
    public static Command parseCommand(String command) {
        try {
            return new Command(command);
        } catch (InvalidParameterException ex) {
            logger.warn("Failed to parse command: " + command + " - " + ex.getMessage());
            return null;
        }
    }

    /**
     * @deprecated Use {@link #parseCommand(String)} instead
     */
    @Deprecated
    public static Command ParseCommand(String command) {
        return parseCommand(command);
    }

    /**
     * 获取原始命令字符串
     */
    public String getRawCommand() {
        return rawCommand;
    }

}
