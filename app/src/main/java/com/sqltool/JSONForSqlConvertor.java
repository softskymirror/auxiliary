package com.sqltool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commontool.JSONUtils.CHA_SET_DATA;

public class JSONForSqlConvertor {
    /**
     * 将 TableInfo 列表转换为 generateJson 所需的 maps 结构。
     * 使用 CHA_SET_DATA 类型包装，key="tables"，items 为表信息列表。
     * @param tables 表信息列表
     * @return 符合 generateJson 要求的 maps 结构
     */
    public static ArrayList<HashMap<Integer, HashMap<String, Object>>> buildMapsFromTableInfo(List<TableInfo> tables) {
        // 将表结构转换为列表，每个表为一个 Map
        List<Map<String, Object>> tablesList = new ArrayList<>();
        for (TableInfo table : tables) {
            Map<String, Object> tableMap = new HashMap<>();
            tableMap.put("tableName", table.getTableName());
            tableMap.put("characterSet", table.getCharacterSet());
            tableMap.put("collate", table.getCollate());
            tableMap.put("engine", table.getEngine());
            tableMap.put("primaryKeys", table.getPrimaryKeys()); // 假设返回 List<String>

            // 构建字段列表
            List<Map<String, Object>> fieldsList = new ArrayList<>();
            for (FieldInfo field : table.getFields()) {
                Map<String, Object> fieldMap = new HashMap<>();
                fieldMap.put("columnName", field.getColumnName());
                fieldMap.put("dataType", field.getDataType());
                fieldMap.put("length", field.getLength());
                fieldMap.put("precision", field.getPrecision());
                fieldMap.put("scale", field.getScale());
                fieldMap.put("unsigned", field.isUnsigned());
                fieldMap.put("notNull", field.isNotNull());
                fieldMap.put("defaultValue", field.getDefaultValue());
                fieldMap.put("autoIncrement", field.isAutoIncrement());
                fieldsList.add(fieldMap);
            }
            tableMap.put("fields", fieldsList);
            tablesList.add(tableMap);
        }

        // 创建 maps 外层 ArrayList
        ArrayList<HashMap<Integer, HashMap<String, Object>>> maps = new ArrayList<>();
        HashMap<Integer, HashMap<String, Object>> mapInfo = new HashMap<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("key", "tables");
        dataMap.put("items", tablesList);
        mapInfo.put(CHA_SET_DATA, dataMap);
        maps.add(mapInfo);

        return maps;
    }
}
