package com.sqltool;

import java.sql.*;
import java.util.*;

/**
 * MySQL数据库操作工具类
 * 提供连接管理、数据库操作、表操作、数据增删改查等功能
 */

public class MySQLUtils {

        // 数据库连接信息
        private String url;
        private String username;
        private String password;
        private Connection connection;
        private boolean autoCommit = true;

        // 默认连接参数
        private static final String DEFAULT_HOST = "localhost";
        private static final int DEFAULT_PORT = 3306;
        private static final String DEFAULT_CHARSET = "UTF-8";
        private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

        /**
         * 构造函数 - 使用完整URL
         * @param url 数据库连接URL
         * @param username 用户名
         * @param password 密码
         */
        public MySQLUtils(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        /**
         * 构造函数 - 使用主机、端口和数据库名
         * @param host 主机地址
         * @param port 端口号
         * @param database 数据库名
         * @param username 用户名
         * @param password 密码
         */
        public MySQLUtils(String host, int port, String database, String username, String password) {
            this.url = String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=%s&useSSL=false&serverTimezone=%s",
                    host, port, database, DEFAULT_CHARSET, DEFAULT_TIMEZONE
            );
            this.username = username;
            this.password = password;
        }

        /**
         * 构造函数 - 使用主机和数据库名（默认端口3306）
         * @param host 主机地址
         * @param database 数据库名
         * @param username 用户名
         * @param password 密码
         */
        public MySQLUtils(String host, String database, String username, String password) {
            this(host, DEFAULT_PORT, database, username, password);
        }

        /**
         * 构造函数 - 使用数据库名（默认本地主机）
         * @param database 数据库名
         * @param username 用户名
         * @param password 密码
         */
//        public MySQLUtils(String database, String username, String password) {
//            this(DEFAULT_HOST, DEFAULT_PORT, database, username, password);
//        }

        /**
         * 加载MySQL驱动
         */
        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("MySQL驱动加载成功");
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL驱动加载失败: " + e.getMessage());
                throw new RuntimeException("无法加载MySQL驱动，请检查驱动jar包", e);
            }
        }

        /**
         * 建立数据库连接
         * @return 连接是否成功
         */
        public boolean connect() {
            try {
                if (connection == null || connection.isClosed()) {
                    connection = DriverManager.getConnection(url, username, password);
                    connection.setAutoCommit(autoCommit);
                    System.out.println("数据库连接成功: " + url);
                    return true;
                }
                return true;
            } catch (SQLException e) {
                System.err.println("数据库连接失败: " + e.getMessage());
                return false;
            }
        }

        /**
         * 测试数据库连接
         * @return 连接是否成功
         */
        public boolean testConnection() {
            try {
                connect();
                if (connection != null && !connection.isClosed()) {
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("数据库连接测试失败: " + e.getMessage());
            }
            return false;
        }

        /**
         * 关闭数据库连接
         */
        public void close() {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("数据库连接已关闭");
                }
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }

        /**
         * 设置是否自动提交事务
         * @param autoCommit 是否自动提交
         */
        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (SQLException e) {
                System.err.println("设置自动提交失败: " + e.getMessage());
            }
        }

        /**
         * 提交事务
         */
        public void commit() {
            try {
                if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
                    connection.commit();
                    System.out.println("事务已提交");
                }
            } catch (SQLException e) {
                System.err.println("提交事务失败: " + e.getMessage());
            }
        }

        /**
         * 回滚事务
         */
        public void rollback() {
            try {
                if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
                    connection.rollback();
                    System.out.println("事务已回滚");
                }
            } catch (SQLException e) {
                System.err.println("回滚事务失败: " + e.getMessage());
            }
        }

        /**
         * 创建数据库
         * @param databaseName 数据库名
         * @param charset 字符集
         * @param collation 排序规则
         * @return 是否创建成功
         */
        public boolean createDatabase(String databaseName, String charset, String collation) {
            String sql = String.format(
                    "CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET %s COLLATE %s",
                    databaseName, charset, collation
            );

            try {
                connect();
                Statement stmt = connection.createStatement();
                int result = stmt.executeUpdate(sql);
                stmt.close();
                System.out.println("数据库创建成功: " + databaseName);
                return true;
            } catch (SQLException e) {
                System.err.println("创建数据库失败: " + e.getMessage());
                return false;
            }
        }

        /**
         * 创建数据库（使用默认字符集和排序规则）
         * @param databaseName 数据库名
         * @return 是否创建成功
         */
        public boolean createDatabase(String databaseName) {
            return createDatabase(databaseName, "utf8mb4", "utf8mb4_general_ci");
        }

        /**
         * 删除数据库
         * @param databaseName 数据库名
         * @return 是否删除成功
         */
        public boolean dropDatabase(String databaseName) {
            String sql = "DROP DATABASE IF EXISTS `" + databaseName + "`";

            try {
                connect();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
                System.out.println("数据库删除成功: " + databaseName);
                return true;
            } catch (SQLException e) {
                System.err.println("删除数据库失败: " + e.getMessage());
                return false;
            }
        }

        /**
         * 切换当前使用的数据库
         * @param databaseName 数据库名
         * @return 是否切换成功
         */
        public boolean useDatabase(String databaseName) {
            String sql = "USE `" + databaseName + "`";

            try {
                connect();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
                System.out.println("切换到数据库: " + databaseName);
                return true;
            } catch (SQLException e) {
                System.err.println("切换数据库失败: " + e.getMessage());
                return false;
            }
        }

        /**
         * 创建表
         * @param tableName 表名
         * @param columns 列定义，格式：列名 数据类型 [约束]
         * @param primaryKey 主键列
         * @param engine 存储引擎
         * @return 是否创建成功
         */
        public boolean createTable(String tableName, Map<String, String> columns,
                                   String primaryKey, String engine) {
            StringBuilder sql = new StringBuilder();
            sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");

            // 添加列定义
            for (Map.Entry<String, String> entry : columns.entrySet()) {
                sql.append("  `").append(entry.getKey()).append("` ")
                        .append(entry.getValue()).append(",\n");
            }

            // 添加主键
            if (primaryKey != null && !primaryKey.isEmpty()) {
                sql.append("  PRIMARY KEY (`").append(primaryKey).append("`)");
            } else {
                // 移除最后一个逗号
                sql.delete(sql.length() - 2, sql.length());
            }

            sql.append("\n) ENGINE=").append(engine).append(" DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            try {
                connect();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(sql.toString());
                stmt.close();
                System.out.println("表创建成功: " + tableName);
                return true;
            } catch (SQLException e) {
                System.err.println("创建表失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return false;
            }
        }

        /**
         * 创建表（使用默认存储引擎InnoDB）
         * @param tableName 表名
         * @param columns 列定义
         * @param primaryKey 主键列
         * @return 是否创建成功
         */
        public boolean createTable(String tableName, Map<String, String> columns, String primaryKey) {
            return createTable(tableName, columns, primaryKey, "InnoDB");
        }

        /**
         * 删除表
         * @param tableName 表名
         * @return 是否删除成功
         */
        public boolean dropTable(String tableName) {
            String sql = "DROP TABLE IF EXISTS `" + tableName + "`";

            try {
                connect();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
                System.out.println("表删除成功: " + tableName);
                return true;
            } catch (SQLException e) {
                System.err.println("删除表失败: " + e.getMessage());
                return false;
            }
        }

        /**
         * 插入数据
         * @param tableName 表名
         * @param data 数据映射（列名 -> 值）
         * @return 插入的行数
         */
        public int insert(String tableName, Map<String, Object> data) {
            if (data == null || data.isEmpty()) {
                System.err.println("插入数据为空");
                return 0;
            }

            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            List<Object> values = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                columns.append("`").append(entry.getKey()).append("`, ");
                placeholders.append("?, ");
                values.add(entry.getValue());
            }

            // 移除最后的逗号和空格
            columns.delete(columns.length() - 2, columns.length());
            placeholders.delete(placeholders.length() - 2, placeholders.length());

            String sql = "INSERT INTO `" + tableName + "` (" + columns + ") VALUES (" + placeholders + ")";

            try {
                connect();
                PreparedStatement pstmt = connection.prepareStatement(sql);

                // 设置参数
                for (int i = 0; i < values.size(); i++) {
                    pstmt.setObject(i + 1, values.get(i));
                }

                int result = pstmt.executeUpdate();
                pstmt.close();
                System.out.println("插入成功，影响行数: " + result);
                return result;
            } catch (SQLException e) {
                System.err.println("插入数据失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return 0;
            }
        }

        /**
         * 批量插入数据
         * @param tableName 表名
         * @param columnNames 列名数组
         * @param dataList 数据列表（每行数据是一个Object数组）
         * @return 插入的行数
         */
        public int batchInsert(String tableName, String[] columnNames, List<Object[]> dataList) {
            if (columnNames == null || columnNames.length == 0 || dataList == null || dataList.isEmpty()) {
                System.err.println("批量插入数据为空");
                return 0;
            }

            // 构建列名部分
            StringBuilder columns = new StringBuilder();
            for (String column : columnNames) {
                columns.append("`").append(column).append("`, ");
            }
            columns.delete(columns.length() - 2, columns.length());

            // 构建占位符部分
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < columnNames.length; i++) {
                placeholders.append("?, ");
            }
            placeholders.delete(placeholders.length() - 2, placeholders.length());

            String sql = "INSERT INTO `" + tableName + "` (" + columns + ") VALUES (" + placeholders + ")";

            try {
                connect();
                connection.setAutoCommit(false); // 关闭自动提交以进行批量操作
                PreparedStatement pstmt = connection.prepareStatement(sql);

                // 添加批处理
                for (Object[] rowData : dataList) {
                    for (int i = 0; i < columnNames.length; i++) {
                        pstmt.setObject(i + 1, rowData[i]);
                    }
                    pstmt.addBatch();
                }

                // 执行批处理
                int[] results = pstmt.executeBatch();
                connection.commit();
                pstmt.close();

                int total = 0;
                for (int result : results) {
                    total += result;
                }

                System.out.println("批量插入成功，影响行数: " + total);
                return total;
            } catch (SQLException e) {
                rollback();
                System.err.println("批量插入数据失败: " + e.getMessage());
                return 0;
            } finally {
                setAutoCommit(true); // 恢复自动提交
            }
        }

        /**
         * 更新数据
         * @param tableName 表名
         * @param data 要更新的数据映射（列名 -> 值）
         * @param condition WHERE条件（不包含WHERE关键字）
         * @param params 条件参数值
         * @return 更新的行数
         */
        public int update(String tableName, Map<String, Object> data, String condition, Object... params) {
            if (data == null || data.isEmpty()) {
                System.err.println("更新数据为空");
                return 0;
            }

            StringBuilder setClause = new StringBuilder();
            List<Object> values = new ArrayList<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                setClause.append("`").append(entry.getKey()).append("` = ?, ");
                values.add(entry.getValue());
            }

            // 移除最后的逗号和空格
            setClause.delete(setClause.length() - 2, setClause.length());

            String sql = "UPDATE `" + tableName + "` SET " + setClause;
            if (condition != null && !condition.trim().isEmpty()) {
                sql += " WHERE " + condition;
            }

            // 添加条件参数值
            if (params != null) {
                for (Object param : params) {
                    values.add(param);
                }
            }

            try {
                connect();
                PreparedStatement pstmt = connection.prepareStatement(sql);

                // 设置参数
                for (int i = 0; i < values.size(); i++) {
                    pstmt.setObject(i + 1, values.get(i));
                }

                int result = pstmt.executeUpdate();
                pstmt.close();
                System.out.println("更新成功，影响行数: " + result);
                return result;
            } catch (SQLException e) {
                System.err.println("更新数据失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return 0;
            }
        }

        /**
         * 删除数据
         * @param tableName 表名
         * @param condition WHERE条件（不包含WHERE关键字）
         * @param params 条件参数值
         * @return 删除的行数
         */
        public int delete(String tableName, String condition, Object... params) {
            String sql = "DELETE FROM `" + tableName + "`";
            if (condition != null && !condition.trim().isEmpty()) {
                sql += " WHERE " + condition;
            }

            try {
                connect();
                PreparedStatement pstmt;

                if (params != null && params.length > 0) {
                    pstmt = connection.prepareStatement(sql);
                    for (int i = 0; i < params.length; i++) {
                        pstmt.setObject(i + 1, params[i]);
                    }
                } else {
                    pstmt = connection.prepareStatement(sql);
                }

                int result = pstmt.executeUpdate();
                pstmt.close();
                System.out.println("删除成功，影响行数: " + result);
                return result;
            } catch (SQLException e) {
                System.err.println("删除数据失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return 0;
            }
        }

        /**
         * 查询数据
         * @param tableName 表名
         * @param columns 要查询的列名数组（null表示所有列）
         * @param condition WHERE条件（不包含WHERE关键字）
         * @param params 条件参数值
         * @return 查询结果集
         */
        public List<Map<String, Object>> select(String tableName, String[] columns,
                                                String condition, Object... params) {
            StringBuilder columnClause = new StringBuilder();

            if (columns == null || columns.length == 0) {
                columnClause.append("*");
            } else {
                for (String column : columns) {
                    columnClause.append("`").append(column).append("`, ");
                }
                columnClause.delete(columnClause.length() - 2, columnClause.length());
            }

            String sql = "SELECT " + columnClause + " FROM `" + tableName + "`";
            if (condition != null && !condition.trim().isEmpty()) {
                sql += " WHERE " + condition;
            }

            List<Map<String, Object>> resultList = new ArrayList<>();

            try {
                connect();
                PreparedStatement pstmt = connection.prepareStatement(sql);

                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        pstmt.setObject(i + 1, params[i]);
                    }
                }

                ResultSet rs = pstmt.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    resultList.add(row);
                }

                rs.close();
                pstmt.close();
                System.out.println("查询成功，返回行数: " + resultList.size());
                return resultList;
            } catch (SQLException e) {
                System.err.println("查询数据失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return resultList;
            }
        }

        /**
         * 查询单条数据
         * @param tableName 表名
         * @param columns 要查询的列名数组
         * @param condition WHERE条件
         * @param params 条件参数值
         * @return 单行数据
         */
        public Map<String, Object> selectOne(String tableName, String[] columns,
                                             String condition, Object... params) {
            List<Map<String, Object>> result = select(tableName, columns, condition, params);
            return result.isEmpty() ? null : result.get(0);
        }

        /**
         * 执行自定义SQL查询
         * @param sql SQL语句
         * @param params 参数值
         * @return 查询结果集
         */
        public List<Map<String, Object>> executeQuery(String sql, Object... params) {
            List<Map<String, Object>> resultList = new ArrayList<>();

            try {
                connect();
                PreparedStatement pstmt = connection.prepareStatement(sql);

                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        pstmt.setObject(i + 1, params[i]);
                    }
                }

                ResultSet rs = pstmt.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    resultList.add(row);
                }

                rs.close();
                pstmt.close();
                System.out.println("SQL查询执行成功，返回行数: " + resultList.size());
                return resultList;
            } catch (SQLException e) {
                System.err.println("执行SQL查询失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return resultList;
            }
        }

        /**
         * 执行自定义SQL更新（INSERT/UPDATE/DELETE等）
         * @param sql SQL语句
         * @param params 参数值
         * @return 影响的行数
         */
        public int executeUpdate(String sql, Object... params) {
            try {
                connect();
                PreparedStatement pstmt = connection.prepareStatement(sql);

                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        pstmt.setObject(i + 1, params[i]);
                    }
                }

                int result = pstmt.executeUpdate();
                pstmt.close();
                System.out.println("SQL更新执行成功，影响行数: " + result);
                return result;
            } catch (SQLException e) {
                System.err.println("执行SQL更新失败: " + e.getMessage());
                System.err.println("SQL: " + sql);
                return 0;
            }
        }

        /**
         * 获取数据库的所有表名
         * @return 表名列表
         */
        public List<String> getTables() {
            List<String> tables = new ArrayList<>();
            String sql = "SHOW TABLES";

            try {
                connect();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    tables.add(rs.getString(1));
                }

                rs.close();
                stmt.close();
                return tables;
            } catch (SQLException e) {
                System.err.println("获取表列表失败: " + e.getMessage());
                return tables;
            }
        }

        /**
         * 获取表结构信息
         * @param tableName 表名
         * @return 表结构信息列表
         */
        public List<Map<String, Object>> getTableStructure(String tableName) {
            List<Map<String, Object>> structure = new ArrayList<>();
            String sql = "DESCRIBE `" + tableName + "`";

            try {
                connect();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> columnInfo = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        columnInfo.put(columnName, value);
                    }
                    structure.add(columnInfo);
                }

                rs.close();
                stmt.close();
                return structure;
            } catch (SQLException e) {
                System.err.println("获取表结构失败: " + e.getMessage());
                return structure;
            }
        }

        /**
         * 检查表是否存在
         * @param tableName 表名
         * @return 是否存在
         */
        public boolean tableExists(String tableName) {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = ?";

            try {
                connect();
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, tableName);
                ResultSet rs = pstmt.executeQuery();

                boolean exists = false;
                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }

                rs.close();
                pstmt.close();
                return exists;
            } catch (SQLException e) {
                System.err.println("检查表是否存在失败: " + e.getMessage());
                return false;
            }
        }

        /**
         * 获取数据库连接URL
         * @return 连接URL
         */
        public String getUrl() {
            return url;
        }

        /**
         * 获取当前连接的数据库名
         * @return 数据库名
         */
        public String getCurrentDatabase() {
            try {
                connect();
                return connection.getCatalog();
            } catch (SQLException e) {
                System.err.println("获取当前数据库失败: " + e.getMessage());
                return null;
            }
        }

        /**
         * 测试用例和示例
         */
        public static void main(String[] args) {
            // 示例1: 创建数据库和表
            System.out.println("=== 示例1: 创建数据库和表 ===");

            // 连接到MySQL服务器（不指定数据库）
            MySQLUtils dbUtil = new MySQLUtils("localhost", 3306, "mysql", "root", "password");

            // 测试连接
            if (!dbUtil.testConnection()) {
                System.err.println("数据库连接失败，请检查连接信息");
                return;
            }

            // 创建测试数据库
            dbUtil.createDatabase("test_db");

            // 切换到新数据库
            dbUtil.useDatabase("test_db");

            // 创建表结构定义
            Map<String, String> columns = new LinkedHashMap<>();
            columns.put("id", "INT PRIMARY KEY AUTO_INCREMENT");
            columns.put("name", "VARCHAR(50) NOT NULL");
            columns.put("age", "INT");
            columns.put("email", "VARCHAR(100)");
            columns.put("created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            // 创建用户表
            dbUtil.createTable("users", columns, "id", "InnoDB");

            // 示例2: 插入数据
            System.out.println("\n=== 示例2: 插入数据 ===");

            // 插入单条数据
            Map<String, Object> user1 = new HashMap<>();
            user1.put("name", "张三");
            user1.put("age", 25);
            user1.put("email", "zhangsan@example.com");
            dbUtil.insert("users", user1);

            // 插入多条数据
            Map<String, Object> user2 = new HashMap<>();
            user2.put("name", "李四");
            user2.put("age", 30);
            user2.put("email", "lisi@example.com");
            dbUtil.insert("users", user2);

            // 批量插入数据
            String[] userColumns = {"name", "age", "email"};
            List<Object[]> userData = new ArrayList<>();
            userData.add(new Object[]{"王五", 28, "wangwu@example.com"});
            userData.add(new Object[]{"赵六", 35, "zhaoliu@example.com"});
            dbUtil.batchInsert("users", userColumns, userData);

            // 示例3: 查询数据
            System.out.println("\n=== 示例3: 查询数据 ===");

            // 查询所有数据
            List<Map<String, Object>> allUsers = dbUtil.select("users", null, null);
            System.out.println("所有用户:");
            for (Map<String, Object> user : allUsers) {
                System.out.println(user);
            }

            // 条件查询
            List<Map<String, Object>> youngUsers = dbUtil.select(
                    "users",
                    new String[]{"id", "name", "age"},
                    "age < ?",
                    30
            );
            System.out.println("\n年龄小于30的用户:");
            for (Map<String, Object> user : youngUsers) {
                System.out.println(user);
            }

            // 示例4: 更新数据
            System.out.println("\n=== 示例4: 更新数据 ===");

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("age", 31);
            updateData.put("email", "lisi_updated@example.com");

            int updatedRows = dbUtil.update(
                    "users",
                    updateData,
                    "name = ?",
                    "李四"
            );
            System.out.println("更新了 " + updatedRows + " 行数据");

            // 示例5: 删除数据
            System.out.println("\n=== 示例5: 删除数据 ===");

            int deletedRows = dbUtil.delete(
                    "users",
                    "name = ?",
                    "赵六"
            );
            System.out.println("删除了 " + deletedRows + " 行数据");

            // 示例6: 表操作
            System.out.println("\n=== 示例6: 表操作 ===");

            // 获取所有表
            List<String> tables = dbUtil.getTables();
            System.out.println("当前数据库中的表: " + tables);

            // 获取表结构
            List<Map<String, Object>> tableStructure = dbUtil.getTableStructure("users");
            System.out.println("\nusers表结构:");
            for (Map<String, Object> column : tableStructure) {
                System.out.println(column);
            }

            // 示例7: 事务处理
            System.out.println("\n=== 示例7: 事务处理 ===");

            dbUtil.setAutoCommit(false); // 关闭自动提交

            try {
                // 事务操作1: 插入数据
                Map<String, Object> transactionUser = new HashMap<>();
                transactionUser.put("name", "事务用户");
                transactionUser.put("age", 40);
                transactionUser.put("email", "transaction@example.com");
                dbUtil.insert("users", transactionUser);

                // 事务操作2: 更新数据
                Map<String, Object> updateAge = new HashMap<>();
                updateAge.put("age", 26);
                dbUtil.update("users", updateAge, "name = ?", "张三");

                // 提交事务
                dbUtil.commit();
                System.out.println("事务提交成功");
            } catch (Exception e) {
                // 回滚事务
                dbUtil.rollback();
                System.err.println("事务回滚: " + e.getMessage());
            } finally {
                dbUtil.setAutoCommit(true); // 恢复自动提交
            }

            // 示例8: 执行自定义SQL
            System.out.println("\n=== 示例8: 执行自定义SQL ===");

            // 自定义查询
            List<Map<String, Object>> customResult = dbUtil.executeQuery(
                    "SELECT name, age FROM users WHERE age BETWEEN ? AND ? ORDER BY age DESC",
                    25, 35
            );
            System.out.println("自定义查询结果:");
            for (Map<String, Object> row : customResult) {
                System.out.println(row);
            }

            // 关闭连接
            dbUtil.close();

            System.out.println("\n=== 所有示例执行完成 ===");
        }
    }

