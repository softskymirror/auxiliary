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

package com.adbtool.androidcontrol.message;

import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * BinaryMessage - registry-based factory for parsing binary message headers.
 * Subclasses register their type via {@link #registerType(String, Class)}.
 */
public class BinaryMessage {

    private static final Logger logger = Logger.getLogger(BinaryMessage.class);

    private String type;
    private static final Map<String, Class<? extends BinaryMessage>> map = new HashMap<>();

    static {
        map.put("file", FileMessage.class);
    }

    /**
     * Register a new BinaryMessage subtype.
     * @param type the type identifier (must match the "type" field in JSON)
     * @param clazz the subclass to instantiate for this type
     */
    public static void registerType(String type, Class<? extends BinaryMessage> clazz) {
        if (type == null || clazz == null) {
            throw new IllegalArgumentException("type and clazz must not be null");
        }
        map.put(type, clazz);
    }

    /**
     * Parse a JSON header into the appropriate BinaryMessage subclass.
     * @param json the JSON string from the binary message header
     * @return parsed BinaryMessage, or null if parsing fails
     */
    public static BinaryMessage parse(String json) {
        if (json == null || json.isEmpty()) {
            logger.warn("BinaryMessage.parse received null or empty JSON");
            return null;
        }
        try {
            JSONObject jsonObject = (JSONObject) JSONObject.parse(json);
            String type = jsonObject.getString("type");
            Class<? extends BinaryMessage> c = map.get(type);
            if (c == null) {
                logger.warn("Unknown binary message type: " + type);
                return null;
            }
            return (BinaryMessage) JSONObject.toJavaObject(jsonObject, c);
        } catch (Exception e) {
            logger.error("Failed to parse binary message: " + json, e);
            return null;
        }
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "BinaryMessage{type='" + type + "'}";
    }
}
