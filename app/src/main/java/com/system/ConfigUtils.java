package com.system;

import org.json.JSONException;
import org.json.JSONObject;


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

    /**
     * 默认配置目录候选路径（按优先级排列）。
     * <p>
     * 覆盖两种运行环境：
     * <ul>
     *   <li>IDE 直接运行 / Gradle app:run（CWD=项目根目录）</li>
     *   <li>生产部署（CWD=部署根目录，config/ 独立存在）</li>
     * </ul>
     * <p>
     * 注意：projectRoot 统一使用 CWD（user.dir），
     * 因此所有候选路径必须是从 CWD 出发的相对路径。
     */
    private static final String[] DEFAULT_CONFIG_CANDIDATES = {
            "app/src/main/resources/config",   // Gradle app:run CWD=项目根目录时
            "src/main/resources/config",       // IDE CWD=app/ 时
            "config"                           // 生产部署 CWD=部署根目录时
    };

    // JSON 字段常量
    private static final String JSON_KEY_URL = "url";
    private static final String JSON_KEY_USERNAME = "username";
    private static final String JSON_KEY_PASSWORD = "password";
    private static final String JSON_KEY_DRIVER = "driver";
    private static final String JSON_KEY_JSON_FILE = "jsonFile";
    private static final String JSON_KEY_PROP_FILE = "propFilePath";
    private static final String JSON_KEY_POM_FILE = "pomFilepath";
    private static final String JSON_KEY_SERVER_PORT = "serverPort";
    private static final String JSON_KEY_TEST_PORT = "testPort";
    private static final String JSON_KEY_RESOURCE_ROOT = "resource.root";
    private static final String JSON_KEY_PROJECT_ROOT = "project.root";
    private static final String JSON_KEY_DATA_ROOT = "data.root";

    // DeepSeek 配置字段常量
    private static final String JSON_KEY_DEEPSEEK = "deepseek";
    private static final String JSON_KEY_DEEPSEEK_API_KEY = "apiKey";
    private static final String JSON_KEY_DEEPSEEK_MODEL = "model";
    private static final String JSON_KEY_DEEPSEEK_BASE_URL = "baseUrl";
    private static final String JSON_KEY_DEEPSEEK_THINKING = "thinkingEnabled";
    private static final String JSON_KEY_DEEPSEEK_REASONING = "reasoningEffort";
    private static final String JSON_KEY_DEEPSEEK_SYSTEM_PROMPT = "systemPrompt";
    private static final String JSON_KEY_DEEPSEEK_REQUEST_FILE = "requestParamsFile";

    private ConfigUtils() {}

    /**
     * 按优先级查找默认配置目录。
     * <p>
     * 依次检查 {@link #DEFAULT_CONFIG_CANDIDATES} 中的路径，
     * 返回第一个同时包含 global.json 和 databases.json 的目录。
     * 全部未找到时尝试从 classpath（fat JAR）中提取配置到临时目录。
     *
     * @return 找到的配置目录路径
     * @throws IllegalStateException 所有方式均未找到配置时抛出
     */
    public static String resolveDefaultConfigDir() {
        // 1. 优先从文件系统查找（开发/IDE/外部配置部署）
        for (String candidate : DEFAULT_CONFIG_CANDIDATES) {
            File dir = new File(candidate);
            if (dir.isDirectory()) {
                File globalFile = new File(dir, DEFAULT_GLOBAL_JSON);
                File dbFile = new File(dir, DEFAULT_DATABASES_JSON);
                if (globalFile.isFile() && dbFile.isFile()) {
                    return candidate;
                }
            }
        }
        // 2. 从 classpath 提取（fat JAR 生产部署）
        String extracted = extractConfigFromClasspath();
        if (extracted != null) {
            return extracted;
        }
        return DEFAULT_CONFIG_DIR;
    }

    /**
     * 从 classpath（fat JAR 内部）提取配置文件到临时目录。
     * <p>
     * 当以 {@code java -jar} 方式运行时，配置文件打包在 JAR 内部的
     * {@code config/} 目录中。此方法将其提取到系统临时目录下的
     * {@code auxiliary-config/} 子目录，供 {@link ConfigLoader} 以文件方式读取。
     *
     * @return 提取后的配置目录路径，classpath 中无配置文件时返回 null
     */
    private static String extractConfigFromClasspath() {
        try {
            ClassLoader cl = ConfigUtils.class.getClassLoader();
            java.net.URL globalUrl = cl.getResource("config/" + DEFAULT_GLOBAL_JSON);
            java.net.URL dbUrl = cl.getResource("config/" + DEFAULT_DATABASES_JSON);
            if (globalUrl == null || dbUrl == null) {
                return null;
            }
            // 提取到临时目录
            File tmpDir = new File(System.getProperty("java.io.tmpdir"), "auxiliary-config");
            if (!tmpDir.exists()) tmpDir.mkdirs();
            copyResourceToFile(cl, "config/" + DEFAULT_GLOBAL_JSON, new File(tmpDir, DEFAULT_GLOBAL_JSON));
            copyResourceToFile(cl, "config/" + DEFAULT_DATABASES_JSON, new File(tmpDir, DEFAULT_DATABASES_JSON));
            System.out.println("[ConfigUtils] 从 classpath 提取配置到: " + tmpDir.getAbsolutePath());
            return tmpDir.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("[ConfigUtils] classpath 配置提取失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 classpath 资源复制为文件系统文件。
     */
    private static void copyResourceToFile(ClassLoader cl, String resource, File dest) throws IOException {
        try (java.io.InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("资源不存在: " + resource);
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

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
     * <p>
     * password 字段会自动通过 {@link CryptoUtils#resolve(String)} 解析，
     * 支持 {@code ENC(...)} 加密格式和 {@code ${ENV:VAR}} 环境变量引用。
     *
     * @param dbConfig 包含数据库连接信息的 JSON 对象
     * @return 包含 url、username、password（已解密）、driver 键值对的 Map
     */
    public static Map<String, Object> extractLoginData(JSONObject dbConfig) {
        Map<String, Object> data = extractConfigFields(dbConfig, JSON_KEY_URL, JSON_KEY_USERNAME, JSON_KEY_PASSWORD, JSON_KEY_DRIVER);
        // 自动解析敏感字段（支持 ENC() 加密 和 ${ENV:} 环境变量引用）
        String rawPassword = (String) data.get(JSON_KEY_PASSWORD);
        data.put(JSON_KEY_PASSWORD, CryptoUtils.resolve(rawPassword));
        return data;
    }

    /**
     * 从 Properties 对象中提取数据库登录连接信息。
     * <p>
     * password 字段会自动通过 {@link CryptoUtils#resolve(String)} 解析，
     * 支持 {@code ENC(...)} 加密格式和 {@code ${ENV:VAR}} 环境变量引用。
     *
     * @param props 包含数据库连接配置的 Properties 对象
     * @return 包含 url、username、password（已解密）、driver 键值对的 Map
     */
    public static Map<String, Object> extractLoginDataFromProperties(Properties props) {
        Map<String, Object> data = extractConfigFieldsFromProperties(props, JSON_KEY_URL, JSON_KEY_USERNAME, JSON_KEY_PASSWORD, JSON_KEY_DRIVER);
        // 自动解析敏感字段
        String rawPassword = (String) data.get(JSON_KEY_PASSWORD);
        data.put(JSON_KEY_PASSWORD, CryptoUtils.resolve(rawPassword));
        return data;
    }

    /**
     * 从全局配置的 JSONObject 中提取核心文件路径信息。
     *
     * @param globalConfig 包含全局配置的 JSON 对象
     * @return 包含 jsonFile、propFilePath、pomFilepath 键值对的 Map
     */
    public static Map<String, Object> extractGlobalData(JSONObject globalConfig) {
        return extractConfigFields(globalConfig,
                JSON_KEY_JSON_FILE, JSON_KEY_PROP_FILE, JSON_KEY_POM_FILE,
                JSON_KEY_SERVER_PORT, JSON_KEY_TEST_PORT,
                JSON_KEY_RESOURCE_ROOT, JSON_KEY_PROJECT_ROOT, JSON_KEY_DATA_ROOT);
    }

    /**
     * 从全局配置的 JSONObject 中提取 DeepSeek 配置信息。
     * <p>
     * apiKey 字段会自动通过 {@link CryptoUtils#resolve(String)} 解析，
     * 支持四种读取方式：
     * <ul>
     *   <li>明文: {@code sk-xxx}</li>
     *   <li>AES加密: {@code ENC(Base64密文)}</li>
     *   <li>环境变量: {@code ${ENV:DEEPSEEK_API_KEY}}</li>
     *   <li>Windows凭据: {@code ${WINCRED:DeepSeekApiKey}}</li>
     * </ul>
     *
     * @param globalConfig 包含全局配置的 JSON 对象
     * @return 包含 DeepSeek 配置键值对的 Map，若不存在 deepseek 节点则返回空 Map
     */
    public static Map<String, Object> extractDeepSeekData(JSONObject globalConfig) {
        Map<String, Object> data = new HashMap<>();
        JSONObject deepseekJson = globalConfig.optJSONObject(JSON_KEY_DEEPSEEK);
        if (deepseekJson == null) {
            return data;
        }
        data.put(JSON_KEY_DEEPSEEK_API_KEY, deepseekJson.optString(JSON_KEY_DEEPSEEK_API_KEY, ""));
        data.put(JSON_KEY_DEEPSEEK_MODEL, deepseekJson.optString(JSON_KEY_DEEPSEEK_MODEL, "deepseek-v4-pro"));
        data.put(JSON_KEY_DEEPSEEK_BASE_URL, deepseekJson.optString(JSON_KEY_DEEPSEEK_BASE_URL, "https://api.deepseek.com/v1/"));
        data.put(JSON_KEY_DEEPSEEK_THINKING, deepseekJson.optBoolean(JSON_KEY_DEEPSEEK_THINKING, true));
        data.put(JSON_KEY_DEEPSEEK_REASONING, deepseekJson.optString(JSON_KEY_DEEPSEEK_REASONING, "high"));
        data.put(JSON_KEY_DEEPSEEK_SYSTEM_PROMPT, deepseekJson.optString(JSON_KEY_DEEPSEEK_SYSTEM_PROMPT, ""));
        data.put(JSON_KEY_DEEPSEEK_REQUEST_FILE, deepseekJson.optString(JSON_KEY_DEEPSEEK_REQUEST_FILE, "data/deepseek_request.json"));
        // apiKey 是敏感字段，自动通过 CryptoUtils.resolve() 解析
        // 环境变量不存在时优雅降级为空字符串，不阻止服务启动
        String rawApiKey = (String) data.get(JSON_KEY_DEEPSEEK_API_KEY);
        try {
            data.put(JSON_KEY_DEEPSEEK_API_KEY, CryptoUtils.resolve(rawApiKey));
        } catch (RuntimeException e) {
            System.err.println("[ConfigUtils] DeepSeek API Key 解析失败，已置空: " + e.getMessage());
            data.put(JSON_KEY_DEEPSEEK_API_KEY, "");
        }
        return data;
    }

    /**
     * 从 Properties 对象中提取全局核心文件路径信息。
     *
     * @param props 包含全局配置的 Properties 对象
     * @return 包含 jsonFile、propFilePath、pomFilepath 键值对的 Map
     */
    public static Map<String, Object> extractGlobalDataFromProperties(Properties props) {
        return extractConfigFieldsFromProperties(props,
                JSON_KEY_JSON_FILE, JSON_KEY_PROP_FILE, JSON_KEY_POM_FILE,
                JSON_KEY_SERVER_PORT, JSON_KEY_TEST_PORT,
                JSON_KEY_RESOURCE_ROOT, JSON_KEY_PROJECT_ROOT, JSON_KEY_DATA_ROOT);
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
     * <p>
     * <b>路径解析规范</b>：
     * <ul>
     *   <li>global.json 中所有路径统一使用<b>相对路径</b>（相对于项目根目录）</li>
     *   <li>项目根目录 = 配置目录的父目录（如 configDir=../config，则 projectRoot=..）</li>
     *   <li>绝对路径原样使用，不做解析</li>
     *   <li>通过 {@link #resolveFile(String)} 统一解析，保证跨平台一致性</li>
     * </ul>
     */
    public static class ConfigLoader {
        private final Map<String, Object> loginData;
        private final Map<String, Object> globalData;
        private final Map<String, Object> deepseekData;
        private final String configDir;
        private final File projectRoot;

        /**
         * 使用默认配置目录创建加载器。
         * <p>
         * 按优先级自动查找配置目录：
         * <ol>
         *   <li>{@code app/src/main/resources/config} — Gradle 测试/开发</li>
         *   <li>{@code src/main/resources/config} — IDE 直接运行</li>
         *   <li>{@code config} — 生产部署</li>
         * </ol>
         * 全部未找到时回退到 {@link #DEFAULT_CONFIG_DIR}。
         */
        public ConfigLoader() {
            this(resolveDefaultConfigDir());
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
            // projectRoot 统一使用 CWD，覆盖所有运行场景：
            //   - gradle app:run: workingDir = rootProject.projectDir ✓
            //   - IDE 直接运行: CWD = 项目根目录 ✓
            //   - java -jar: CWD = 部署根目录 ✓
            this.projectRoot = new File(System.getProperty("user.dir"));
            Path globalPath = Paths.get(configDir, DEFAULT_GLOBAL_JSON);
            Path dbPath = Paths.get(configDir, DEFAULT_DATABASES_JSON);
            try {
                validateJsonFile(globalPath);
                JSONObject globalJson = new JSONObject(new String(Files.readAllBytes(globalPath), StandardCharsets.UTF_8));
                globalData = extractGlobalData(globalJson);
                // 加载 DeepSeek 配置 (可选，不存在时不抛异常)
                deepseekData = extractDeepSeekData(globalJson);
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

        /**
         * 获取服务端端口（主包名外部调用时使用）。
         *
         * @return serverPort 值，默认 8080
         */
        public int getServerPort() {
            Object val = globalData.get(JSON_KEY_SERVER_PORT);
            if (val instanceof Number) return ((Number) val).intValue();
            try { return Integer.parseInt(String.valueOf(val)); }
            catch (NumberFormatException e) { return 8080; }
        }

        /**
         * 获取测试端口（JUnit 测试时使用）。
         *
         * @return testPort 值，默认 6655
         */
        public int getTestPort() {
            Object val = globalData.get(JSON_KEY_TEST_PORT);
            if (val instanceof Number) return ((Number) val).intValue();
            try { return Integer.parseInt(String.valueOf(val)); }
            catch (NumberFormatException e) { return 6655; }
        }

        // ---------- 路径配置方法（统一自 Constant.java） ----------

        /**
         * 从 globalData 获取路径值，空字符串时回退到默认值。
         */
        private String getPathValue(String key, String defaultValue) {
            String val = String.valueOf(globalData.get(key));
            return (val == null || val.isEmpty()) ? defaultValue : val;
        }

        /**
         * 统一路径解析方法。
         * <p>
         * 解析规则：
         * <ul>
         *   <li>绝对路径 → 原样返回</li>
         *   <li>相对路径 → 相对于项目根目录（configDir 的父目录）解析</li>
         * </ul>
         *
         * @param path 配置中的路径值
         * @return 解析后的 File 对象
         */
        public File resolveFile(String path) {
            File f = new File(path);
            if (f.isAbsolute()) {
                return f;
            }
            // 相对路径相对于项目根目录解析
            return (projectRoot != null) ? new File(projectRoot, path) : f;
        }

        /**
         * 获取项目根目录（配置目录的父目录）。
         *
         * @return 项目根目录 File
         */
        public File getProjectRoot() {
            return projectRoot;
        }

        /**
         * 获取资源目录（minicap/minitouch/web 等静态资源）。
         * <p>配置项：{@code resource.root}，默认 {@code ./resources}
         *
         * @return 解析后的绝对路径 File
         */
        public File getResourceDir() {
            return resolveFile(getPathValue(JSON_KEY_RESOURCE_ROOT, "resources"));
        }

        /**
         * 获取项目目录（即项目根目录，configDir 的父目录）。
         *
         * @return 项目根目录 File
         */
        public File getProjectDir() {
            return projectRoot;
        }

        /**
         * 获取数据缓存根目录。
         * <p>配置项：{@code data.root}，默认 {@code ./data}
         *
         * @return 解析后的绝对路径 File
         */
        public File getDataDir() {
            return resolveFile(getPathValue(JSON_KEY_DATA_ROOT, "data"));
        }

        /**
         * 获取数据缓存指定文件。
         *
         * @param name 缓存文件名
         * @return {data.root}/{name} 对应的 File
         */
        public File getDataCache(String name) {
            return new File(getDataDir(), name);
        }

        /**
         * 获取资源目录指定文件。
         *
         * @param name 资源文件名
         * @return {resource.root}/{name} 对应的 File
         */
        public File getResourceFile(String name) {
            return new File(getResourceDir(), name);
        }

        /**
         * 获取临时文件（使用系统临时目录）。
         *
         * @param fileName 临时文件名
         * @return {java.io.tmpdir}/AndroidControl/{fileName}
         */
        public File getTmpFile(String fileName) {
            String tmpdir = System.getProperty("java.io.tmpdir");
            File tmp = new File(tmpdir, "AndroidControl");
            if (!tmp.exists()) tmp.mkdirs();
            return new File(tmp, fileName);
        }

        // ========== DeepSeek 配置方法 ==========

        /**
         * 获取 DeepSeek 配置信息。
         *
         * @return 包含 apiKey、model、baseUrl 等键值对的 Map
         */
        public Map<String, Object> getDeepSeekData() {
            return deepseekData;
        }

        /**
         * 获取 DeepSeek API Key (已解密)。
         * <p>
         * 支持四种读取方式：
         * <ul>
         *   <li>明文: {@code sk-xxx}</li>
         *   <li>AES加密: {@code ENC(Base64密文)}</li>
         *   <li>环境变量: {@code ${ENV:DEEPSEEK_API_KEY}}</li>
         *   <li>Windows凭据: {@code ${WINCRED:DeepSeekApiKey}}</li>
         * </ul>
         *
         * @return 解密后的 API Key，未配置时返回空字符串
         */
        public String getDeepSeekApiKey() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_API_KEY);
            return val != null ? val.toString() : "";
        }

        /**
         * 获取 DeepSeek 模型名称。
         *
         * @return 模型名称，默认 "deepseek-v4-pro"
         */
        public String getDeepSeekModel() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_MODEL);
            return val != null ? val.toString() : "deepseek-v4-pro";
        }

        /**
         * 获取 DeepSeek API Base URL。
         *
         * @return Base URL，默认 "https://api.deepseek.com/v1/"
         */
        public String getDeepSeekBaseUrl() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_BASE_URL);
            return val != null ? val.toString() : "https://api.deepseek.com/v1/";
        }

        /**
         * 获取 DeepSeek 是否启用思考模式。
         *
         * @return true 启用，false 禁用
         */
        public boolean isDeepSeekThinkingEnabled() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_THINKING);
            if (val instanceof Boolean) return (Boolean) val;
            return Boolean.parseBoolean(String.valueOf(val));
        }

        /**
         * 获取 DeepSeek 推理力度。
         *
         * @return 推理力度 (low/medium/high)，默认 "high"
         */
        public String getDeepSeekReasoningEffort() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_REASONING);
            return val != null ? val.toString() : "high";
        }

        /**
         * 获取 DeepSeek 默认系统提示词。
         *
         * @return 系统提示词
         */
        public String getDeepSeekSystemPrompt() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_SYSTEM_PROMPT);
            return val != null ? val.toString() : "";
        }

        /**
         * 获取 DeepSeek 请求参数文件路径。
         *
         * @return 请求参数 JSON 文件路径
         */
        public String getDeepSeekRequestParamsFile() {
            Object val = deepseekData.get(JSON_KEY_DEEPSEEK_REQUEST_FILE);
            return val != null ? val.toString() : "data/deepseek_request.json";
        }

        /**
         * 获取 DeepSeek 请求参数文件的 File 对象。
         *
         * @return 解析后的 File 对象
         */
        public File getDeepSeekRequestParamsFileObj() {
            return resolveFile(getDeepSeekRequestParamsFile());
        }

        /**
         * 检查 DeepSeek 配置是否完整 (至少包含 apiKey)。
         *
         * @return true 配置完整，false 配置缺失
         */
        public boolean isDeepSeekConfigured() {
            return deepseekData != null && !deepseekData.isEmpty()
                    && !getDeepSeekApiKey().isEmpty();
        }
    }
}




