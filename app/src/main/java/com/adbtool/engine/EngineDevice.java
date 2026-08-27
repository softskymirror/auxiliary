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

package com.adbtool.engine;

import com.android.ddmlib.IDevice;
import com.google.common.util.concurrent.SettableFuture;
import com.adbtool.adb.AdbDevice;
import com.adbtool.adb.AdbServer;
import com.adbtool.minitouch.Minitouch;
import com.adbtool.minitouch.MinitouchListener;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * EngineDevice - high-level device abstraction with caching and gesture API.
 */
public class EngineDevice {

    private static final Logger logger = Logger.getLogger(EngineDevice.class);

    /** Cache of active EngineDevice instances by serial number */
    private static final Map<String, EngineDevice> deviceCache = new ConcurrentHashMap<>();

    private final AdbDevice device;
    private final Minitouch minitouch;
    private volatile boolean minitouchOpen = false;

    /**
     * Get or create an EngineDevice for the given serial number.
     * Returns null if the device is not found or minitouch fails to start.
     */
    public static EngineDevice getDevice(String serialNumber) {
        EngineDevice cached = deviceCache.get(serialNumber);
        if (cached != null && cached.isMinitouchOpen()) {
            return cached;
        }

        AdbDevice iDevice = AdbServer.server().getDevice(serialNumber);
        if (iDevice == null) {
            logger.warn("Device not found: " + serialNumber);
            return null;
        }

        EngineDevice ed = new EngineDevice(iDevice);
        if (ed.isMinitouchOpen()) {
            deviceCache.put(serialNumber, ed);
            return ed;
        }
        return null;
    }

    /**
     * Remove a device from the cache.
     */
    public static void removeDevice(String serialNumber) {
        EngineDevice removed = deviceCache.remove(serialNumber);
        if (removed != null) {
            removed.close();
        }
    }

    public EngineDevice(AdbDevice iDevice) {
        this.device = iDevice;
        this.minitouch = new Minitouch(iDevice);
        SettableFuture<Boolean> future = SettableFuture.create();

        minitouch.addEventListener(new MinitouchListener() {
            @Override
            public void onStartup(Minitouch minitouch, boolean success) {
                future.set(success);
            }

            @Override
            public void onClose(Minitouch minitouch) {
                minitouchOpen = false;
            }
        });
        minitouch.start();

        try {
            Boolean success = future.get();
            if (Boolean.TRUE.equals(success)) {
                minitouchOpen = true;
            }
        } catch (InterruptedException e) {
            logger.error("Minitouch startup interrupted for device: " + iDevice.getIDevice().getSerialNumber(), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.error("Minitouch startup failed for device: " + iDevice.getIDevice().getSerialNumber(), e);
        }
    }

    protected boolean isMinitouchOpen() {
        return minitouchOpen;
    }

    // --- Basic touch API ---

    public void touchDown(int x, int y) {
        minitouch.sendEvent("d 0 " + x + " " + y + " 50\nc\n");
    }

    public void touchMove(int x, int y) {
        minitouch.sendEvent("m 0 " + x + " " + y + " 50\nc\n");
    }

    public void touchUp() {
        minitouch.sendEvent("u 0\nc\n");
    }

    // --- Advanced gesture API ---

    /**
     * Perform a tap at the given coordinates.
     */
    public void tap(int x, int y) {
        touchDown(x, y);
        touchUp();
    }

    /**
     * Perform a swipe from (x1,y1) to (x2,y2) over the given duration in ms.
     */
    public void swipe(int x1, int y1, int x2, int y2, long durationMs) {
        touchDown(x1, y1);
        int steps = Math.max(1, (int) (durationMs / 16)); // ~60fps
        for (int i = 1; i <= steps; i++) {
            int cx = x1 + (x2 - x1) * i / steps;
            int cy = y1 + (y2 - y1) * i / steps;
            touchMove(cx, cy);
            try {
                Thread.sleep(durationMs / steps);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        touchUp();
    }

    /**
     * Perform a long press at the given coordinates for the specified duration.
     */
    public void longPress(int x, int y, long durationMs) {
        touchDown(x, y);
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        touchUp();
    }

    // --- Shell commands ---

    public String executeShellAndGetString(String command) {
        return AdbServer.executeShellCommand(device.getIDevice(), command);
    }

    public void startApp(String str) {
        AdbServer.executeShellCommand(device.getIDevice(), "am start " + str);
    }

    /**
     * Close minitouch and release resources.
     */
    public void close() {
        if (minitouch != null) {
            try {
                minitouch.kill();
            } catch (Exception e) {
                logger.warn("Error closing minitouch", e);
            }
        }
        minitouchOpen = false;
    }
}
