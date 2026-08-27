package com.aitool.deepseek;

import com.aitool.deepseek.model.ChatLog;
import com.commontool.XMLUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DeepSeek 对话日志 XML 存储工具类
 * <p>
 * 将对话记录以 XML 格式持久化存储到 {@code resources/log/} 目录下。
 * 日志文件按日期分割，格式为 {@code chat_log_yyyy-MM-dd.xml}。
 * </p>
 * <p>
 * XML 结构示例:
 * <pre>
 * {@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <chatLogs date="2024-01-01">
 *   <chatLog id="a1b2c3d4e5f6g7h8" timestamp="2024-01-01 12:00:00" model="deepseek-v4-pro">
 *     <question>用户问题</question>
 *     <answer>AI回答</answer>
 *     <reasoning>思维链内容(可选)</reasoning>
 *     <usage promptTokens="100" completionTokens="200" totalTokens="300" finishReason="stop"/>
 *   </chatLog>
 * </chatLogs>
 * }
 * </pre>
 * </p>
 *
 * <h3>使用示例:</h3>
 * <pre>
 * // 保存一条对话记录
 * ChatLog log = ChatLog.fromResponse("问题", response, "deepseek-v4-pro");
 * ChatLogStorage.saveLog(log);
 *
 * // 加载今天的所有记录
 * List<ChatLog> logs = ChatLogStorage.loadTodayLogs();
 *
 * // 加载指定日期的记录
 * List<ChatLog> logs = ChatLogStorage.loadLogs("2024-01-01");
 *
 * // 查询所有日志文件
 * List<String> files = ChatLogStorage.listLogFiles();
 * </pre>
 */
public class ChatLogStorage {

    private static final Logger log = Logger.getLogger(ChatLogStorage.class);

    /** 日志存储根目录 (resources/log/) */
    private static final String LOG_DIR = resolveLogDir();

    /** 日志文件前缀 */
    private static final String LOG_FILE_PREFIX = "chat_log_";

    /** 日志文件后缀 */
    private static final String LOG_FILE_SUFFIX = ".xml";

    /** 根元素名称 */
    private static final String ROOT_ELEMENT = "chatLogs";

    /** 记录元素名称 */
    private static final String RECORD_ELEMENT = "chatLog";

    private ChatLogStorage() {
        // 工具类不允许实例化
    }

    // ========== 核心存储方法 ==========

    /**
     * 保存一条对话记录到当天的日志文件
     * <p>
     * 如果文件已存在，则追加到现有记录之后；
     * 如果文件不存在，则创建新的 XML 文件。
     * </p>
     *
     * @param chatLog 对话记录
     */
    public static synchronized void saveLog(ChatLog chatLog) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        saveLog(chatLog, dateStr);
    }

    /**
     * 保存一条对话记录到指定日期的日志文件
     *
     * @param chatLog 对话记录
     * @param dateStr 日期字符串 (格式: yyyy-MM-dd)
     */
    public static synchronized void saveLog(ChatLog chatLog, String dateStr) {
        String filePath = getLogFilePath(dateStr);
        ensureLogDir();

        try {
            Document doc;
            Element rootElem;

            File file = new File(filePath);
            if (file.exists()) {
                // 加载现有文件并追加
                doc = XMLUtils.loadFromFile(filePath);
                rootElem = doc.getDocumentElement();
            } else {
                // 创建新文档
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
                rootElem = doc.createElement(ROOT_ELEMENT);
                rootElem.setAttribute("date", dateStr);
                doc.appendChild(rootElem);
            }

            // 构建记录节点
            Element recordElem = chatLogToXml(doc, chatLog);
            rootElem.appendChild(recordElem);

            // 保存文件
            XMLUtils.saveToFile(doc, filePath);
            log.info("[ChatLogStorage] 对话记录已保存: " + filePath + " (ID: " + chatLog.getId() + ")");

        } catch (Exception e) {
            log.error("[ChatLogStorage] 保存对话记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量保存对话记录
     *
     * @param chatLogs 对话记录列表
     */
    public static synchronized void saveLogs(List<ChatLog> chatLogs) {
        for (ChatLog chatLog : chatLogs) {
            saveLog(chatLog);
        }
    }

    /**
     * 加载今天的对话记录
     *
     * @return 对话记录列表
     */
    public static List<ChatLog> loadTodayLogs() {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return loadLogs(dateStr);
    }

    /**
     * 加载指定日期的对话记录
     *
     * @param dateStr 日期字符串 (格式: yyyy-MM-dd)
     * @return 对话记录列表
     */
    public static List<ChatLog> loadLogs(String dateStr) {
        String filePath = getLogFilePath(dateStr);
        File file = new File(filePath);

        if (!file.exists()) {
            log.info("[ChatLogStorage] 日志文件不存在: " + filePath);
            return new ArrayList<>();
        }

        try {
            Document doc = XMLUtils.loadFromFile(filePath);
            return xmlToChatLogs(doc);
        } catch (Exception e) {
            log.error("[ChatLogStorage] 加载对话记录失败: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 加载所有日志文件中的对话记录
     *
     * @return 所有对话记录列表
     */
    public static List<ChatLog> loadAllLogs() {
        List<ChatLog> allLogs = new ArrayList<>();
        File logDir = new File(LOG_DIR);

        if (!logDir.exists()) {
            return allLogs;
        }

        File[] files = logDir.listFiles((dir, name) ->
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));

        if (files != null) {
            for (File file : files) {
                try {
                    Document doc = XMLUtils.loadFromFile(file.getAbsolutePath());
                    allLogs.addAll(xmlToChatLogs(doc));
                } catch (Exception e) {
                    log.error("[ChatLogStorage] 加载日志文件失败: " + file.getName(), e);
                }
            }
        }

        return allLogs;
    }

    /**
     * 列出所有日志文件
     *
     * @return 日志文件名列表
     */
    public static List<String> listLogFiles() {
        List<String> files = new ArrayList<>();
        File logDir = new File(LOG_DIR);

        if (!logDir.exists()) {
            return files;
        }

        File[] logFiles = logDir.listFiles((dir, name) ->
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));

        if (logFiles != null) {
            for (File file : logFiles) {
                files.add(file.getName());
            }
        }

        return files;
    }

    /**
     * 删除指定日期的日志文件
     *
     * @param dateStr 日期字符串 (格式: yyyy-MM-dd)
     * @return 是否删除成功
     */
    public static boolean deleteLogs(String dateStr) {
        String filePath = getLogFilePath(dateStr);
        File file = new File(filePath);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("[ChatLogStorage] 日志文件已删除: " + filePath);
            }
            return deleted;
        }
        return false;
    }

    /**
     * 获取日志目录路径
     */
    public static String getLogDir() {
        return LOG_DIR;
    }

    /**
     * 统计今天对话的次数
     */
    public static int getTodayLogCount() {
        return loadTodayLogs().size();
    }

    /**
     * 统计今天的总 Token 消耗
     */
    public static int getTodayTokenUsage() {
        List<ChatLog> logs = loadTodayLogs();
        int total = 0;
        for (ChatLog chatLog : logs) {
            total += chatLog.getTotalTokens();
        }
        return total;
    }

    // ========== XML 转换方法 ==========

    /**
     * 将 ChatLog 转换为 XML Element
     */
    private static Element chatLogToXml(Document doc, ChatLog chatLog) {
        Element recordElem = doc.createElement(RECORD_ELEMENT);
        recordElem.setAttribute("id", chatLog.getId());
        recordElem.setAttribute("timestamp", chatLog.getTimestamp());

        if (chatLog.getModel() != null) {
            recordElem.setAttribute("model", chatLog.getModel());
        }

        // 问题
        Element questionElem = doc.createElement("question");
        questionElem.setTextContent(chatLog.getQuestion());
        recordElem.appendChild(questionElem);

        // 回答
        Element answerElem = doc.createElement("answer");
        answerElem.setTextContent(chatLog.getAnswer());
        recordElem.appendChild(answerElem);

        // 思维链 (可选)
        if (chatLog.getReasoningContent() != null && !chatLog.getReasoningContent().isEmpty()) {
            Element reasoningElem = doc.createElement("reasoning");
            reasoningElem.setTextContent(chatLog.getReasoningContent());
            recordElem.appendChild(reasoningElem);
        }

        // Token 用量
        Element usageElem = doc.createElement("usage");
        usageElem.setAttribute("promptTokens", String.valueOf(chatLog.getPromptTokens()));
        usageElem.setAttribute("completionTokens", String.valueOf(chatLog.getCompletionTokens()));
        usageElem.setAttribute("totalTokens", String.valueOf(chatLog.getTotalTokens()));
        if (chatLog.getFinishReason() != null) {
            usageElem.setAttribute("finishReason", chatLog.getFinishReason());
        }
        recordElem.appendChild(usageElem);

        return recordElem;
    }

    /**
     * 从 XML Document 解析 ChatLog 列表
     */
    private static List<ChatLog> xmlToChatLogs(Document doc) {
        List<ChatLog> logs = new ArrayList<>();
        Element rootElem = doc.getDocumentElement();
        NodeList recordNodes = rootElem.getElementsByTagName(RECORD_ELEMENT);

        for (int i = 0; i < recordNodes.getLength(); i++) {
            Element recordElem = (Element) recordNodes.item(i);
            ChatLog chatLog = new ChatLog();

            // 解析属性
            if (recordElem.hasAttribute("id")) {
                chatLog.setId(recordElem.getAttribute("id"));
            }
            if (recordElem.hasAttribute("timestamp")) {
                chatLog.setTimestamp(recordElem.getAttribute("timestamp"));
            }
            if (recordElem.hasAttribute("model")) {
                chatLog.setModel(recordElem.getAttribute("model"));
            }

            // 解析子元素
            NodeList questionNodes = recordElem.getElementsByTagName("question");
            if (questionNodes.getLength() > 0) {
                chatLog.setQuestion(questionNodes.item(0).getTextContent());
            }

            NodeList answerNodes = recordElem.getElementsByTagName("answer");
            if (answerNodes.getLength() > 0) {
                chatLog.setAnswer(answerNodes.item(0).getTextContent());
            }

            NodeList reasoningNodes = recordElem.getElementsByTagName("reasoning");
            if (reasoningNodes.getLength() > 0) {
                chatLog.setReasoningContent(reasoningNodes.item(0).getTextContent());
            }

            // 解析 Token 用量
            NodeList usageNodes = recordElem.getElementsByTagName("usage");
            if (usageNodes.getLength() > 0) {
                Element usageElem = (Element) usageNodes.item(0);
                if (usageElem.hasAttribute("promptTokens")) {
                    chatLog.setPromptTokens(Integer.parseInt(usageElem.getAttribute("promptTokens")));
                }
                if (usageElem.hasAttribute("completionTokens")) {
                    chatLog.setCompletionTokens(Integer.parseInt(usageElem.getAttribute("completionTokens")));
                }
                if (usageElem.hasAttribute("totalTokens")) {
                    chatLog.setTotalTokens(Integer.parseInt(usageElem.getAttribute("totalTokens")));
                }
                if (usageElem.hasAttribute("finishReason")) {
                    chatLog.setFinishReason(usageElem.getAttribute("finishReason"));
                }
            }

            logs.add(chatLog);
        }

        return logs;
    }

    // ========== 辅助方法 ==========

    /**
     * 获取日志文件路径
     */
    private static String getLogFilePath(String dateStr) {
        return LOG_DIR + File.separator + LOG_FILE_PREFIX + dateStr + LOG_FILE_SUFFIX;
    }

    /**
     * 确保日志目录存在
     */
    private static void ensureLogDir() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (created) {
                log.info("[ChatLogStorage] 日志目录已创建: " + LOG_DIR);
            }
        }
    }

    /**
     * 解析日志目录路径
     * <p>
     * 优先使用项目根目录下的 resources/log，
     * 如果不存在则尝试 classpath 下的路径。
     * </p>
     */
    private static String resolveLogDir() {
        // 尝试项目根目录
        String projectRoot = System.getProperty("user.dir");
        File projectLogDir = new File(projectRoot, "resources" + File.separator + "log");

        if (projectLogDir.exists() || projectLogDir.mkdirs()) {
            return projectLogDir.getAbsolutePath();
        }

        // 尝试 app/src/main/resources/log (Gradle 项目结构)
        File appLogDir = new File(projectRoot, "app" + File.separator + "src" + File.separator
                + "main" + File.separator + "resources" + File.separator + "log");

        if (appLogDir.exists() || appLogDir.mkdirs()) {
            return appLogDir.getAbsolutePath();
        }

        // 回退到用户目录
        String userHome = System.getProperty("user.home");
        File homeLogDir = new File(userHome, ".deepseek" + File.separator + "log");
        homeLogDir.mkdirs();
        return homeLogDir.getAbsolutePath();
    }

    // ========== Demo 方法 ==========

    /**
     * 演示对话日志存储功能
     */
    public static void testDemo() {
        System.out.println("=== 对话日志存储演示 ===");

        // 创建模拟记录
        ChatLog log1 = new ChatLog();
        log1.setModel("deepseek-v4-pro");
        log1.setQuestion("什么是人工智能?");
        log1.setAnswer("人工智能是计算机科学的一个分支...");
        log1.setPromptTokens(50);
        log1.setCompletionTokens(200);
        log1.setTotalTokens(250);
        log1.setFinishReason("stop");

        ChatLog log2 = new ChatLog();
        log2.setModel("deepseek-v4-pro");
        log2.setQuestion("用Java写一个Hello World");
        log2.setAnswer("public class HelloWorld { ... }");
        log2.setReasoningContent("用户需要一个简单的Java示例...");
        log2.setPromptTokens(30);
        log2.setCompletionTokens(150);
        log2.setTotalTokens(180);
        log2.setFinishReason("stop");

        // 保存记录
        saveLog(log1);
        saveLog(log2);

        // 加载并显示
        List<ChatLog> logs = loadTodayLogs();
        System.out.println("今天共有 " + logs.size() + " 条对话记录:");
        for (ChatLog chatLog : logs) {
            System.out.println("  [" + chatLog.getTimestamp() + "] Q: " + chatLog.getQuestion());
            System.out.println("  A: " + chatLog.getAnswer());
            System.out.println("  Token: " + chatLog.getTotalTokens());
            System.out.println("  ---");
        }

        // 显示日志文件位置
        System.out.println("日志存储目录: " + getLogDir());
        System.out.println("日志文件列表: " + listLogFiles());
        System.out.println("今日Token总消耗: " + getTodayTokenUsage());
    }
}

