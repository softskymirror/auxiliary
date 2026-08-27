package com;

import com.adbtool.adb.AdbServer;
import com.adbtool.server.AndroidControlServer;
import com.system.ConfigUtils;
import org.apache.log4j.Logger;

/**
 * 应用主入口 —— 启动 AndroidControl 设备群控服务
 * <p>
 * 运行方式：.\gradlew.bat app:run
 * 默认端口：读取 config/global.json 中的 serverPort（默认 8080）
 * 自定义端口：.\gradlew.bat app:run -PmainClass=com.MainEntry -Dserver.port=6655
 */
public class MainEntry {

    private static final Logger logger = Logger.getLogger(MainEntry.class);

    public static void main(String[] args) {
        // 读取端口：优先使用 -Dserver.port 系统属性，否则从配置文件读取
        int port;
        String portProp = System.getProperty("server.port");
        if (portProp != null && !portProp.isEmpty()) {
            port = Integer.parseInt(portProp);
        } else {
            try {
                port = new ConfigUtils.ConfigLoader().getServerPort();
            } catch (Exception e) {
                logger.warn("配置加载失败，使用默认端口 6655: " + e.getMessage());
                port = 6655;
            }
        }

        // 尝试启动 ADB 设备监控（非必需，无 ADB 环境时仅启动 Web 服务）
        try {
            AdbServer.server().listenUSB();
            AdbServer.server().listenADB();
            logger.info("ADB 设备监控已启动");
        } catch (Exception | UnsatisfiedLinkError e) {
            logger.warn("ADB 环境不可用，跳过设备监控: " + e.getMessage());
        }

        // 启动 Web Server（阻塞模式，进程保持运行直到手动停止）
        try {
            AndroidControlServer server = new AndroidControlServer();
            server.listen(port);
        } catch (InterruptedException e) {
            logger.error("服务器被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("服务器启动失败: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
