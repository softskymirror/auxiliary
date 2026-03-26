package com.system;

import com.commontool.JSONUtils;
import com.commontool.XMLParser;
import com.searchtool.FileUtils;
import com.sqltool.FieldInfo;
import com.sqltool.TableInfo;
import com.sqltool.XMLForSqlConvertor;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;



public class ConfigUtils {
public static String defaultConfigPath="./config";
public static String defaultglobalPath="global.json";
public static String defaultdbPath="databases.json";
public static String defaultpomPath="pom.xml";
public Map<String, Object> loginData;
public Map<String, Object> globalData;

    /**
     *First, verify the file, then parse and extract the configuration file information,and encapsulate it as a Map object.
     *
     *
     */
    public ConfigUtils() {
    Path globalpath = FileUtils.getConfigFilePath(defaultConfigPath, defaultglobalPath);
    Path dbpath = FileUtils.getConfigFilePath(defaultConfigPath, defaultdbPath);
    try {
        if (validateJsonFile(globalpath)) globalData = extractGlobalData(JSONUtils.readJsonFromFile(globalpath));
        if (validateJsonFile(dbpath)) loginData = extractLoginData(JSONUtils.readJsonFromFile(dbpath));
    }catch(Exception e){
        e.printStackTrace();
        }
    }



        /**
         * 校验 JSON 配置文件是否正常
         * @param path 文件路径
         * @return true 表示文件存在、非空、内容为合法 JSON；false 表示有问题
         */
        public static boolean validateJsonFile(Path path) {
            // 1. 存在性检查
            if (!Files.exists(path) || Files.isDirectory(path)) {
                return false;
            }
            // 2. 非空检查
            try {
                if (Files.size(path) == 0) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
            // 3. 格式检查
            try {
                String content = new String(Files.readAllBytes(path), "UTF-8");
                new JSONObject(content); // 解析失败会抛异常
                return true;
            } catch (IOException | JSONException e) {
                return false;
            }
        }

        /**
         * 校验 Properties 配置文件是否正常
         * @param path 文件路径
         * @return true 表示文件存在、非空、内容为合法 Properties 格式；false 表示有问题
         */
        public static boolean validatePropertiesFile(Path path) {
            // 1. 存在性检查
            if (!Files.exists(path) || Files.isDirectory(path)) {
                return false;
            }
            // 2. 非空检查
            try {
                if (Files.size(path) == 0) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
            // 3. 格式检查
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                Properties props = new Properties();
                props.load(fis);
                // 可选：检查是否包含必要键（如 url），此处仅判断加载成功
                return true;
            } catch (IOException e) {
                return false;
            }
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

    public static Map<String, Object> extractLoginDataFromProperties(Properties props) {
        Map<String, Object> data = new HashMap<>();
        // 使用 getProperty(key) 如果 key 不存在则返回 null，此处用 "" 代替
        data.put("url", props.getProperty("url", ""));
        data.put("username", props.getProperty("username", ""));
        data.put("password", props.getProperty("password", ""));
        data.put("driver", props.getProperty("driver", ""));
        // 可根据需要添加更多字段
        return data;
    }

    /**
     * 从数据库配置 JSONObject 中提取登录信息（url, username, password, driver 等）。
     * @param dbConfig 包含数据库配置的 JSONObject
     * @return 登录信息映射
     */
    public static Map<String, Object> extractGlobalData(JSONObject dbConfig) {
        Map<String, Object> data = new HashMap<>();
        data.put("jsonFile", dbConfig.optString("jsonFile"));
        data.put("propFilePath", dbConfig.optString("propFilePath"));
        data.put("pomFilepath", dbConfig.optString("pomFilepath"));
        // 可根据需要添加更多字段
        return data;
    }

    public static Map<String, Object> extractGlobalDataFromProperties(Properties props) {
        Map<String, Object> data = new HashMap<>();
        // 使用 getProperty(key) 如果 key 不存在则返回 null，此处用 "" 代替
        data.put("jsonFile", props.getProperty("jsonFile", ""));
        data.put("propFilePath", props.getProperty("propFilePath", ""));
        data.put("pomFilepath", props.getProperty("pomFilepath", ""));
        // 可根据需要添加更多字段
        return data;
    }

    /**
     * 将 JSONObject 转换为 Properties 对象
     * @param jsonObject 包含数据库配置的 JSON 对象
     * @return Properties 对象，可直接用于存储或操作
     */
    public static Properties jsonToProperties(JSONObject jsonObject) {
        Properties props = new Properties();
        // 遍历 JSON 的所有键，放入 Properties
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            // Properties 只接受字符串值，将非字符串转换为字符串
            props.setProperty(key, value == null ? "" : value.toString());
        }
        return props;
    }



    /**
     * 将 Properties 对象保存到文件（UTF-8 编码）
     * @param props Properties 对象
     * @param filePath 输出文件路径
     * @throws IOException 如果写入失败
     */
    public static void savePropertiesToFile(Properties props, String filePath) throws IOException {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            // 第二个参数为注释，可填 null 或描述信息
            props.store(out, "Database Configuration");
        }
    }
    /**
     * 从指定路径的文件加载 Properties 配置
     * @param filePath 配置文件路径
     * @return Properties 对象
     * @throws IOException 文件不存在或读取失败时抛出
     */
    public static Properties loadPropertiesFromFile(String filePath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(filePath)) {
            props.load(in);
        }
        return props;
    }

    /**
     * Enable the conversion between JSON objects and properties objects
     * @param jsonFilePath
     * @param propFilePath
     * @throws Exception
     */
    public static void convertJsonToProperties(Path jsonFilePath, String propFilePath) throws Exception {
        // 1. Read the content of the JSON file

        JSONObject json = new JSONObject(JSONUtils.readJsonFromFile(jsonFilePath));

        // 2. 转换为 Properties
        Properties props = jsonToProperties(json);

        // 3. 保存为 properties 文件
        savePropertiesToFile(props, propFilePath);
        System.out.println("转换成功！生成文件：" + propFilePath);
    }





}


