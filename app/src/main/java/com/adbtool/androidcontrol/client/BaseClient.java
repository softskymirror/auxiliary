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

import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * BaseClient - common image buffering logic shared by LocalClient and RemoteClient.
 * Subclasses must implement {@link #sendImage(byte[])} to define the actual transport.
 */
public abstract class BaseClient {

    private static final Logger logger = Logger.getLogger(BaseClient.class);

    /** Image data timeout in milliseconds */
    public static final int DATA_TIMEOUT = 100;

    private volatile boolean isWaiting = false;
    private final BlockingQueue<ImageData> dataQueue = new LinkedBlockingQueue<>();

    /**
     * Image data wrapper with timestamp.
     */
    public static class ImageData {
        public final long timestamp;
        public final byte[] data;

        public ImageData(byte[] data) {
            this.timestamp = System.currentTimeMillis();
            this.data = data;
        }
    }

    /**
     * Subclasses implement this to send image bytes through their specific transport.
     */
    protected abstract void sendImage(byte[] data);

    /**
     * Called when a new JPG frame is available from Minicap.
     * If waiting for a frame, sends immediately (picking non-expired from queue if available).
     * Otherwise, clears expired frames and enqueues.
     */
    public void onNewJPG(byte[] data) {
        if (isWaiting) {
            if (!dataQueue.isEmpty()) {
                dataQueue.add(new ImageData(data));
                ImageData d = getUsefulImage();
                if (d != null) {
                    sendImage(d.data);
                }
            } else {
                sendImage(data);
            }
            isWaiting = false;
        } else {
            clearObsoleteImage();
            dataQueue.add(new ImageData(data));
        }
    }

    /**
     * Set waiting state and attempt to send a buffered image.
     */
    public void setWaiting(boolean waiting) {
        this.isWaiting = waiting;
        trySendImage();
    }

    public boolean isWaiting() {
        return isWaiting;
    }

    private void trySendImage() {
        ImageData d = getUsefulImage();
        if (d != null) {
            isWaiting = false;
            sendImage(d.data);
        }
    }

    private void clearObsoleteImage() {
        ImageData d = dataQueue.peek();
        long curTS = System.currentTimeMillis();
        while (d != null) {
            if (curTS - d.timestamp < DATA_TIMEOUT) {
                dataQueue.poll();
                d = dataQueue.peek();
            } else {
                break;
            }
        }
    }

    private ImageData getUsefulImage() {
        long curTS = System.currentTimeMillis();
        ImageData d = null;
        while (true) {
            d = dataQueue.poll();
            if (d == null || curTS - d.timestamp < DATA_TIMEOUT || dataQueue.isEmpty()) {
                break;
            }
        }
        return d;
    }

    /**
     * Clear all buffered image data. Call on cleanup.
     */
    public void clearImageBuffer() {
        dataQueue.clear();
        isWaiting = false;
    }
}
