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


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class ConfigUtils {
    // 默认配置目录和文件名
    public static final String DEFAULT_CONFIG_DIR = "../config";
    public static final String DEFAULT_GLOBAL_JSON = "global.json";
    public static final String DEFAULT_DATABASES_JSON = "databases.json";
    public static final String DEFAULT_POM_XML = "pom.xml";

    // JSON 字段常量
    private static final String JSON_KEY_URL = "url";
    private static final String JSON_KEY_USERNAME = "username";
    private static final String JSON_KEY_PASSWORD = "password";
    private static final String JSON_KEY_DRIVER = "driver";
    private static final String JSON_KEY_JSON_FILE = "jsonFile";
    private static final String JSON_KEY_PROP_FILE = "propFilePath";
    private static final String JSON_KEY_POM_FILE = "pomFilepath";

    private ConfigUtils() {}

    /**
     *First, verify the file, then parse and extract the configuration file information,and encapsulate it as a Map object.
     *
     *
     */

    /**
     * 校验 JSON 配置文件是否正常，异常时抛出明确异常。
     * <p>
     * 依次检查文件是否存在、是否为空、内容是否为合法 JSON 格式。
     *
     * @param path JSON 配置文件的路径
     * @throws IOException              文件不存在或读取失败时抛出
     * @throws IllegalArgumentException 文件为空时抛出
     * @throws JSONException            文件内容不是合法 JSON 时抛出
     */
    public static void validateJsonFile(Path path) throws IOException, JSONException {

        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new IOException("文件不存在或为目录: " + path);
        }
        if (Files.size(path) == 0) {
            throw new IllegalArgumentException("文件为空: " + path);
        }
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        try {
            new JSONObject(content);
        }catch(JSONException e){
            throw new JSONException("JSON 解析失败，文件内容: " + content, e);
        }
    }

    /**
     * 校验 Properties 配置文件是否正常，异常时抛出明确异常。
     * @param path 文件路径
     * @throws IOException 文件不存在、读取失败或格式错误时抛出
     * @throws IllegalArgumentException 文件为空时抛出
     */
    public static void validatePropertiesFile(Path path) throws IOException, IllegalArgumentException {
        // 1. 存在性检查
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new IOException("Properties 文件不存在或为目录: " + path);
        }
        // 2. 非空检查
        try {
            if (Files.size(path) == 0) {
                throw new IllegalArgumentException("Properties 文件为空: " + path);
            }
        } catch (IOException e) {
            throw new IOException("无法获取文件大小: " + path, e);
        }
        // 3. 格式检查
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            Properties props = new Properties();
            props.load(fis);
            // 可根据需要增加键值对合法性检查
        } catch (IOException e) {
            throw new IOException("Properties 文件加载失败: " + path, e);
        }
    }

    private static boolean isRegularFileNonEmpty(Path path) throws IOException {
        return Files.exists(path) && !Files.isDirectory(path) && Files.size(path) > 0;
    }

    /**
     * 从数据库配置的 JSONObject 中提取登录连接信息。
     * <p>
     * 依次提取 url、username、password、driver 四个字段，
     * 若某个字段不存在则返回空字符串（由 optString 保证安全）。
     *
     * @param dbConfig 包含数据库连接信息的 JSON 对象
     * @return 包含 url、username、password、driver 键值对的 Map
     */
    public static Map<String, Object> extractLoginData(JSONObject dbConfig) {
        return extractConfigFields(dbConfig, JSON_KEY_URL, JSON_KEY_USERNAME, JSON_KEY_PASSWORD, JSON_KEY_DRIVER);
    }

    /**
     * 从 Properties 对象中提取数据库登录连接信息。
     *
     * @param props 包含数据库连接配置的 Properties 对象
     * @return 包含 url、username、password、driver 键值对的 Map
     */
    public static Map<String, Object> extractLoginDataFromProperties(Properties props) {
        return extractConfigFieldsFromProperties(props, JSON_KEY_URL, JSON_KEY_USERNAME, JSON_KEY_PASSWORD, JSON_KEY_DRIVER);
    }

    /**
     * 从全局配置的 JSONObject 中提取核心文件路径信息。
     *
     * @param globalConfig 包含全局配置的 JSON 对象
     * @return 包含 jsonFile、propFilePath、pomFilepath 键值对的 Map
     */
    public static Map<String, Object> extractGlobalData(JSONObject globalConfig) {
        return extractConfigFields(globalConfig, JSON_KEY_JSON_FILE, JSON_KEY_PROP_FILE, JSON_KEY_POM_FILE);
    }

    /**
     * 从 Properties 对象中提取全局核心文件路径信息。
     *
     * @param props 包含全局配置的 Properties 对象
     * @return 包含 jsonFile、propFilePath、pomFilepath 键值对的 Map
     */
    public static Map<String, Object> extractGlobalDataFromProperties(Properties props) {
        return extractConfigFieldsFromProperties(props, JSON_KEY_JSON_FILE, JSON_KEY_PROP_FILE, JSON_KEY_POM_FILE);
    }

    /**
     * 通用方法：从 JSONObject 中按指定键提取字段值。
     *
     * @param json 源 JSON 对象
     * @param keys 需要提取的键名列表
     * @return 包含对应键值对的 Map，缺失的键值为空字符串
     */
    private static Map<String, Object> extractConfigFields(JSONObject json, String... keys) {
        Map<String, Object> data = new HashMap<>();
        for (String key : keys) {
            data.put(key, json.optString(key));
        }
        return data;
    }

    /**
     * 通用方法：从 Properties 中按指定键提取字段值。
     *
     * @param props 源 Properties 对象
     * @param keys  需要提取的键名列表
     * @return 包含对应键值对的 Map，缺失的键值为空字符串
     */
    private static Map<String, Object> extractConfigFieldsFromProperties(Properties props, String... keys) {
        Map<String, Object> data = new HashMap<>();
        for (String key : keys) {
            data.put(key, props.getProperty(key, ""));
        }
        return data;
    }

    /**
     * 将 JSONObject 转换为 Properties 对象。
     * <p>
     * 遍历 JSON 所有键值对，值统一转为字符串存储；若值为 null 则存空字符串。
     *
     * @param jsonObject 待转换的 JSON 对象
     * @return 转换后的 Properties 对象
     */
    public static Properties jsonToProperties(JSONObject jsonObject) {
        Properties props = new Properties();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            props.setProperty(key, value == null ? "" : value.toString());
        }
        return props;
    }

    /**
     * 将 Properties 对象保存为文件（UTF-8 编码），避免中文转义。
     *
     * @param props    待保存的 Properties 对象
     * @param filePath 目标文件路径
     * @throws IOException 文件写入失败时抛出
     */
    public static void savePropertiesToFile(Properties props, String filePath) throws IOException {
        // 使用 Writer 并指定 UTF-8 编码，避免中文转义
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filePath), "UTF-8")) {
            props.store(writer, "Database Configuration");
        }
    }

    /**
     * 从指定 Path 路径加载 Properties 配置文件。
     *
     * @param filePath 配置文件路径
     * @return 加载后的 Properties 对象
     * @throws IOException 文件不存在或读取失败时抛出
     */
    public static Properties loadPropertiesFromFile(Path filePath) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(filePath)) {  // 使用 NIO
            props.load(in);
        }
        return props;
    }

    /**
     * 从指定字符串路径加载 Properties 配置文件。
     *
     * @param filePath 配置文件路径字符串
     * @return 加载后的 Properties 对象
     * @throws IOException 文件不存在或读取失败时抛出
     */
    public static Properties loadPropertiesFromFile(String filePath) throws IOException {
        return loadPropertiesFromFile(Paths.get(filePath));
    }


    // ---------- 转换并保存 JSON 为 Properties 文件 ----------
    /**
     * 读取 JSON 配置文件并转换保存为 Properties 文件。
     * <p>
     * 流程：读取 JSON → 转换为 Properties → 写入目标文件。
     *
     * @param jsonFilePath JSON 源文件路径
     * @param propFilePath Properties 目标文件路径
     * @throws Exception 文件读取、JSON 解析或写入失败时抛出
     */
    public static void convertJsonToProperties(Path jsonFilePath, String propFilePath) throws Exception {
        // 读取 JSON
        String content = new String(Files.readAllBytes(jsonFilePath), "UTF-8");
        JSONObject json = new JSONObject(content);
        // 转换
        Properties props = jsonToProperties(json);
        // 保存
        savePropertiesToFile(props, propFilePath);
        System.out.println("转换成功！生成文件：" + propFilePath);
    }

    /**
     * 配置加载器，负责从指定目录读取全局配置和数据库配置文件，
     * 并将解析后的数据封装为 Map 供外部使用。
     */
    public static class ConfigLoader {
        private final Map<String, Object> loginData;
        private final Map<String, Object> globalData;
        private final String configDir;

        /**
         * 使用默认配置目录（{@code ../config}）创建加载器。
         */
        public ConfigLoader() {
            this(DEFAULT_CONFIG_DIR);
        }

        /**
         * 使用指定配置目录创建加载器。
         * <p>
         * 构造时自动加载 global.json 和 databases.json，
         * 依次执行校验、解析和数据提取。
         *
         * @param configDir 配置文件所在目录路径
         * @throws IllegalStateException 配置文件无效或加载失败时抛出
         */
        public ConfigLoader(String configDir) {
            this.configDir = configDir;
            Path globalPath = Paths.get(configDir, DEFAULT_GLOBAL_JSON);
            Path dbPath = Paths.get(configDir, DEFAULT_DATABASES_JSON);
            try {
                validateJsonFile(globalPath);
                JSONObject globalJson = new JSONObject(new String(Files.readAllBytes(globalPath), StandardCharsets.UTF_8));
                globalData = extractGlobalData(globalJson);
            } catch (IOException | JSONException | IllegalArgumentException e) {
                throw new IllegalStateException("全局配置文件加载失败: " + globalPath, e);
            }
            try {
                validateJsonFile(dbPath);
                JSONObject dbJson = new JSONObject(new String(Files.readAllBytes(dbPath), StandardCharsets.UTF_8));
                loginData = extractLoginData(dbJson);
            } catch (IOException | JSONException | IllegalArgumentException e) {
                throw new IllegalStateException("数据库配置文件加载失败: " + dbPath, e);
            }
        }

        /**
         * 获取数据库登录连接信息。
         *
         * @return 包含 url、username、password、driver 键值对的 Map
         */
        public Map<String, Object> getLoginData() {
            return loginData;
        }

        /**
         * 获取全局核心文件路径配置信息。
         *
         * @return 包含 jsonFile、propFilePath、pomFilepath 键值对的 Map
         */
        public Map<String, Object> getGlobalData() {
            return globalData;
        }

        /**
         * 获取当前配置目录的绝对路径。
         *
         * @return 配置目录路径字符串
         */
        public String getAbsolutePath(){
            return configDir;
        }
    }
}




