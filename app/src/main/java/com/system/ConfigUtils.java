package com.system;

import com.commontool.JSONUtils;
import com.commontool.XMLParser;
import com.sqltool.FieldInfo;
import com.sqltool.TableInfo;
import org.json.JSONObject;
import org.w3c.dom.Document;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.commontool.JSONUtils.CHA_SET_DATA;
import static com.searchtool.FileUtils.savePropertiesToFile;

public class ConfigUtils {
    /**
     * 从 XML 文件加载并解析表结构信息。
     * @param xmlFilePath XML 文件路径
     * @return TableInfo 列表
     * @throws Exception 如果文件不存在或解析失败
     */
    public static List<TableInfo> loadTableInfoFromXml(String xmlFilePath) throws Exception {
        File file = new File(xmlFilePath);
        if (!file.exists()) {
            throw new IOException("XML 文件不存在: " + xmlFilePath);
        }
        Document doc = XMLParser.loadFromFile(xmlFilePath); // 假设 XMLParser 提供该方法
        return XMLParser.xmlParseToTables(doc);            // 假设 XMLParser 提供该方法
    }


    /**
     * 从 JSON 文件加载并解析为 JSONObject。
     * @param jsonFilePath JSON 文件路径
     * @return JSONObject 对象
     * @throws Exception 如果文件不存在或解析失败
     */
    public static JSONObject loadJsonFromFile(String jsonFilePath) throws Exception {
        File file = new File(jsonFilePath);
        if (!file.exists()) {
            throw new IOException("JSON 文件不存在: " + jsonFilePath);
        }
        String content = new String(Files.readAllBytes(file.toPath()));
        return new JSONObject(content);
    }

    /**
     * 从数据库配置 JSONObject 中提取登录信息（url, username, password, driver 等）。
     * @param dbConfig 包含数据库配置的 JSONObject
     * @return 登录信息映射
     */
    public static Map<String, Object> extractLoginData(JSONObject dbConfig) {
        Map<String, Object> data = new HashMap<>();
        data.put("url", dbConfig.optString("url"));
        data.put("username", dbConfig.optString("username"));
        data.put("password", dbConfig.optString("password"));
        data.put("driver", dbConfig.optString("driver"));
        // 可根据需要添加更多字段
        return data;
    }

    public static void convertJsonToProperties(String jsonFilePath, String propFilePath) throws Exception {
        // 1. 读取 JSON 文件内容
        String content = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
        JSONObject json = new JSONObject(content);

        // 2. 转换为 Properties
        Properties props = JSONUtils.jsonToProperties(json);

        // 3. 保存为 properties 文件
        savePropertiesToFile(props, propFilePath);
        System.out.println("转换成功！生成文件：" + propFilePath);
    }

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


