package com.system;

import com.sqltool.MySQLUtils;

import java.io.InputStream;
import java.util.Properties;

public class DatabaseManager {
    private static MySQLUtils dbUtil;

    static {
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            String host = prop.getProperty("db.host");
            int port = Integer.parseInt(prop.getProperty("db.port"));
            String database = prop.getProperty("db.name");
            String user = prop.getProperty("db.user");
            String password = prop.getProperty("db.password");

            dbUtil = new MySQLUtils(host, port, database, user, password);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    public static MySQLUtils getInstance() {
        return dbUtil;
    }
}
