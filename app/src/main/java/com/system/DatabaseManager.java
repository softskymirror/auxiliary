package com.system;

import com.sqltool.MySQLUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * 数据库管理器，负责初始化和提供 MySQL 连接工具实例。
 * <p>
 * 采用延迟加载模式，首次调用 {@link #getInstance()} 时才真正初始化数据库连接，
 * 支持失败后重试，异常信息明确指向配置问题。
 */
public class DatabaseManager {
    private static final String CONFIG_KEY_HOST = "db.host";
    private static final String CONFIG_KEY_PORT = "db.port";
    private static final String CONFIG_KEY_DATABASE = "db.name";
    private static final String CONFIG_KEY_USER = "db.user";
    private static final String CONFIG_KEY_PASSWORD = "db.password";
    private static final String DEFAULT_CONFIG_FILE = "db.properties";

    private static volatile MySQLUtils dbUtil;
    private static final Object lock = new Object();

    private DatabaseManager() {}

    /**
     * 获取 MySQL 工具类单例实例（延迟加载）。
     * <p>
     * 首次调用时从 classpath 的 {@code db.properties} 读取配置并初始化连接，
     * 后续调用直接返回已创建的实例。
     *
     * @return MySQLUtils 实例
     * @throws IllegalStateException 配置文件缺失或格式错误时抛出
     */
    public static MySQLUtils getInstance() {
        if (dbUtil == null) {
            synchronized (lock) {
                if (dbUtil == null) {
                    dbUtil = initializeFromProperties();
                }
            }
        }
        return dbUtil;
    }

    /**
     * 从 Properties 文件初始化数据库连接。
     *
     * @return 初始化后的 MySQLUtils 实例
     * @throws IllegalStateException 配置缺失或解析失败时抛出
     */
    private static MySQLUtils initializeFromProperties() {
        InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
        if (input == null) {
            throw new IllegalStateException("配置文件不存在: " + DEFAULT_CONFIG_FILE + "（位于 classpath 根目录）");
        }

        Properties prop = new Properties();
        try (InputStream in = input) {
            prop.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("配置文件加载失败: " + DEFAULT_CONFIG_FILE, e);
        }

        String host = prop.getProperty(CONFIG_KEY_HOST);
        String portStr = prop.getProperty(CONFIG_KEY_PORT);
        String database = prop.getProperty(CONFIG_KEY_DATABASE);
        String user = prop.getProperty(CONFIG_KEY_USER);
        String password = prop.getProperty(CONFIG_KEY_PASSWORD);

        if (host == null || portStr == null || database == null || user == null || password == null) {
            throw new IllegalStateException(
                String.format("数据库配置不完整，缺少必需项: %s, %s, %s, %s, %s",
                    CONFIG_KEY_HOST, CONFIG_KEY_PORT, CONFIG_KEY_DATABASE, CONFIG_KEY_USER, CONFIG_KEY_PASSWORD)
            );
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("端口号格式错误: " + portStr, e);
        }

        return new MySQLUtils(host, port, database, user, password);
    }

    /**
     * 手动指定配置初始化数据库连接（用于测试或动态配置场景）。
     *
     * @param host     数据库主机地址
     * @param port     数据库端口
     * @param database 数据库名
     * @param user     用户名
     * @param password 密码
     * @return 新创建的 MySQLUtils 实例
     */
    public static MySQLUtils createInstance(String host, int port, String database, String user, String password) {
        MySQLUtils instance = new MySQLUtils(host, port, database, user, password);
        synchronized (lock) {
            dbUtil = instance;
        }
        return instance;
    }

    /**
     * 重置数据库连接实例，下次调用 {@link #getInstance()} 时将重新初始化。
     * <p>
     * 主要用于测试场景或配置热更新。
     */
    public static void reset() {
        synchronized (lock) {
            dbUtil = null;
        }
    }
}
