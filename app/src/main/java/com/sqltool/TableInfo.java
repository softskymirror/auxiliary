package com.sqltool;

import java.util.ArrayList;
import java.util.List;

/**
 * Package all the data in the table, according to the table structure and field types.
 */
public class TableInfo {
    private String tableName;
    private List<FieldInfo> fields = new ArrayList<>();
    private String characterSet = "utf8mb4";
    private String collate = "utf8mb4_general_ci";
    private String engine = "InnoDB";
    private List<String> primaryKeys = new ArrayList<>(); // 支持复合主键

    // 构造器、setter/getter 省略
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public void setCollate(String collate) {
        this.collate = collate;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public String getTableName() {
        return tableName;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public String getCollate() {
        return collate;
    }

    public String getEngine() {
        return engine;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    /**
     * 生成完整的 CREATE TABLE 语句
     */
    public String toCreateTableSQL() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");

        // 添加列定义
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo field = fields.get(i);
            sql.append("  ").append(field.toDefinition());
            if (i < fields.size() - 1 || !primaryKeys.isEmpty()) {
                sql.append(",\n");
            } else {
                sql.append("\n");
            }
        }

        // 添加主键约束
        if (!primaryKeys.isEmpty()) {
            sql.append("  PRIMARY KEY (");
            for (int i = 0; i < primaryKeys.size(); i++) {
                sql.append("`").append(primaryKeys.get(i)).append("`");
                if (i < primaryKeys.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")\n");
        } else {
            // 如果没有主键，去掉最后一个逗号（需要回退处理，这里简化）
            // 实际可单独处理，但为了简洁，假定主键存在
        }

        sql.append(") ENGINE=").append(engine)
                .append(" DEFAULT CHARSET=").append(characterSet)
                .append(" COLLATE=").append(collate);

        return sql.toString();
    }

    // 其他方法：addField, setPrimaryKeys 等
}