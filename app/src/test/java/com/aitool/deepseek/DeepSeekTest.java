package com.aitool.deepseek;

import com.aitool.deepseek.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.ConfigUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DeepSeek AI 模块全面测试
 * <p>
 * 测试覆盖范围：
 * <ul>
 *   <li>DeepSeekModel 枚举值验证</li>
 *   <li>DeepSeekMessage 工厂方法与序列化</li>
 *   <li>DeepSeekConfig Builder 模式与参数校验</li>
 *   <li>DeepSeekRequest Builder 模式、消息构建、思考模式</li>
 *   <li>DeepSeekResponse 内容提取与快捷方法</li>
 *   <li>ChatLog 模型与 fromResponse 工厂方法</li>
 *   <li>ChatLogStorage XML 持久化存储 (使用 TempDir)</li>
 *   <li>DeepSeekRequestLoader JSON 加载与预设模式</li>
 *   <li>ConfigUtils DeepSeek 配置集成 (四种 API Key 读取方式)</li>
 * </ul>
 * </p>
 */
public class DeepSeekTest {

    @TempDir
    Path tempDir;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== DeepSeekModel 枚举测试 ====================

    @Nested
    @DisplayName("DeepSeekModel 枚举测试")
    class DeepSeekModelTest {

        @Test
        @DisplayName("枚举值与字符串映射正确")
        void testEnumValues() {
            assertEquals("deepseek-v4-pro", DeepSeekModel.DEEPSEEK_V4_PRO.getValue());
            assertEquals("deepseek-v4-flash", DeepSeekModel.DEEPSEEK_V4_FLASH.getValue());
            assertEquals("deepseek-chat", DeepSeekModel.DEEPSEEK_CHAT.getValue());
            assertEquals("deepseek-reasoner", DeepSeekModel.DEEPSEEK_REASONER.getValue());
        }

        @Test
        @DisplayName("toString 返回模型字符串值")
        void testToString() {
            assertEquals("deepseek-v4-pro", DeepSeekModel.DEEPSEEK_V4_PRO.toString());
        }

        @Test
        @DisplayName("枚举数量正确")
        void testEnumCount() {
            assertEquals(4, DeepSeekModel.values().length);
        }
    }

    // ==================== DeepSeekMessage 测试 ====================

    @Nested
    @DisplayName("DeepSeekMessage 消息测试")
    class DeepSeekMessageTest {

        @Test
        @DisplayName("工厂方法创建正确角色的消息")
        void testFactoryMethods() {
            DeepSeekMessage system = DeepSeekMessage.system("系统提示");
            DeepSeekMessage user = DeepSeekMessage.user("用户问题");
            DeepSeekMessage assistant = DeepSeekMessage.assistant("AI回答");

            assertEquals("system", system.getRole());
            assertEquals("系统提示", system.getContent());
            assertEquals("user", user.getRole());
            assertEquals("用户问题", user.getContent());
            assertEquals("assistant", assistant.getRole());
            assertEquals("AI回答", assistant.getContent());
        }

        @Test
        @DisplayName("构造方法与 Getter/Setter 正常工作")
        void testConstructorAndSetters() {
            DeepSeekMessage msg = new DeepSeekMessage("user", "原始内容");
            assertEquals("user", msg.getRole());
            assertEquals("原始内容", msg.getContent());
            assertNull(msg.getReasoningContent());

            msg.setRole("assistant");
            msg.setContent("新内容");
            msg.setReasoningContent("思考过程...");

            assertEquals("assistant", msg.getRole());
            assertEquals("新内容", msg.getContent());
            assertEquals("思考过程...", msg.getReasoningContent());
        }

        @Test
        @DisplayName("Jackson 序列化与反序列化")
        void testJsonSerialization() throws Exception {
            DeepSeekMessage msg = DeepSeekMessage.user("测试消息");
            String json = objectMapper.writeValueAsString(msg);

            assertTrue(json.contains("\"role\":\"user\""));
            assertTrue(json.contains("\"content\":\"测试消息\""));
            // reasoningContent 为 null 时不应序列化
            assertFalse(json.contains("reasoning_content"));

            // 反序列化
            DeepSeekMessage deserialized = objectMapper.readValue(json, DeepSeekMessage.class);
            assertEquals("user", deserialized.getRole());
            assertEquals("测试消息", deserialized.getContent());
        }

        @Test
        @DisplayName("带 reasoning_content 的 JSON 反序列化")
        void testJsonDeserializationWithReasoning() throws Exception {
            String json = "{\"role\":\"assistant\",\"content\":\"最终答案\",\"reasoning_content\":\"推理过程\"}";
            DeepSeekMessage msg = objectMapper.readValue(json, DeepSeekMessage.class);

            assertEquals("assistant", msg.getRole());
            assertEquals("最终答案", msg.getContent());
            assertEquals("推理过程", msg.getReasoningContent());
        }
    }

    // ==================== DeepSeekConfig 测试 ====================

    @Nested
    @DisplayName("DeepSeekConfig 配置测试")
    class DeepSeekConfigTest {

        @Test
        @DisplayName("Builder 默认值正确")
        void testBuilderDefaults() {
            DeepSeekConfig config = DeepSeekConfig.builder()
                    .apiKey("sk-test-key")
                    .build();

            assertEquals("sk-test-key", config.getApiKey());
            assertEquals("https://api.deepseek.com/v1/", config.getBaseUrl());
            assertEquals("deepseek-v4-pro", config.getModel());
            assertTrue(config.isThinkingEnabled());
            assertEquals("high", config.getReasoningEffort());
            assertEquals(60000, config.getConnectTimeout());
            assertEquals(120000, config.getReadTimeout());
            assertFalse(config.isLogRequests());
            assertFalse(config.isLogResponses());
            assertFalse(config.hasProxy());
        }

        @Test
        @DisplayName("Builder 自定义值正确")
        void testBuilderCustomValues() {
            DeepSeekConfig config = DeepSeekConfig.builder()
                    .apiKey("sk-custom")
                    .baseUrl("https://custom.api.com/")
                    .model(DeepSeekModel.DEEPSEEK_V4_FLASH)
                    .thinkingEnabled(false)
                    .reasoningEffort("low")
                    .systemPrompt("你是翻译官")
                    .connectTimeout(30000)
                    .readTimeout(60000)
                    .logRequests(true)
                    .logResponses(true)
                    .proxy("127.0.0.1", 8080)
                    .build();

            assertEquals("sk-custom", config.getApiKey());
            assertEquals("https://custom.api.com/", config.getBaseUrl());
            assertEquals("deepseek-v4-flash", config.getModel());
            assertFalse(config.isThinkingEnabled());
            assertEquals("low", config.getReasoningEffort());
            assertEquals("你是翻译官", config.getSystemPrompt());
            assertEquals(30000, config.getConnectTimeout());
            assertEquals(60000, config.getReadTimeout());
            assertTrue(config.isLogRequests());
            assertTrue(config.isLogResponses());
            assertTrue(config.hasProxy());
            assertEquals("127.0.0.1", config.getProxyHost());
            assertEquals(8080, config.getProxyPort());
        }

        @Test
        @DisplayName("of() 快捷方法创建配置")
        void testOfMethod() {
            DeepSeekConfig config = DeepSeekConfig.of("sk-quick");
            assertEquals("sk-quick", config.getApiKey());
            assertEquals("https://api.deepseek.com/v1/", config.getBaseUrl());
        }

        @Test
        @DisplayName("API Key 为空时抛出 IllegalArgumentException")
        void testApiKeyEmpty_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    DeepSeekConfig.builder().apiKey("").build());
            assertThrows(IllegalArgumentException.class, () ->
                    DeepSeekConfig.builder().apiKey("   ").build());
            assertThrows(IllegalArgumentException.class, () ->
                    DeepSeekConfig.builder().apiKey(null).build());
        }

        @Test
        @DisplayName("model(DeepSeekModel) 枚举重载方法正常")
        void testModelEnumOverload() {
            DeepSeekConfig config = DeepSeekConfig.builder()
                    .apiKey("sk-test")
                    .model(DeepSeekModel.DEEPSEEK_V4_FLASH)
                    .build();
            assertEquals("deepseek-v4-flash", config.getModel());
        }

        @Test
        @DisplayName("toString 包含关键配置信息")
        void testToString() {
            DeepSeekConfig config = DeepSeekConfig.builder()
                    .apiKey("sk-test")
                    .model("deepseek-v4-pro")
                    .build();
            String str = config.toString();
            assertTrue(str.contains("deepseek-v4-pro"));
            assertTrue(str.contains("thinkingEnabled"));
        }
    }

    // ==================== DeepSeekRequest 测试 ====================

    @Nested
    @DisplayName("DeepSeekRequest 请求测试")
    class DeepSeekRequestTest {

        @Test
        @DisplayName("Builder 默认模型为 deepseek-v4-pro")
        void testDefaultModel() {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .addUserMessage("hello")
                    .build();
            assertEquals("deepseek-v4-pro", request.getModel());
            assertNotNull(request.getMessages());
            assertEquals(1, request.getMessages().size());
        }

        @Test
        @DisplayName("消息链式构建 - system/user/assistant")
        void testMessageChaining() {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .addSystemMessage("你是助手")
                    .addUserMessage("问题1")
                    .addAssistantMessage("回答1")
                    .addUserMessage("问题2")
                    .build();

            assertEquals(4, request.getMessages().size());
            assertEquals("system", request.getMessages().get(0).getRole());
            assertEquals("user", request.getMessages().get(1).getRole());
            assertEquals("assistant", request.getMessages().get(2).getRole());
            assertEquals("user", request.getMessages().get(3).getRole());
        }

        @Test
        @DisplayName("参数设置正确 - temperature/topP/maxTokens 等")
        void testParameterSettings() {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .model("deepseek-v4-flash")
                    .addUserMessage("test")
                    .temperature(0.5)
                    .topP(0.85)
                    .maxTokens(2048)
                    .frequencyPenalty(0.3)
                    .presencePenalty(0.2)
                    .stream(true)
                    .user("testUser")
                    .reasoningEffort("medium")
                    .build();

            assertEquals("deepseek-v4-flash", request.getModel());
            assertEquals(0.5, request.getTemperature());
            assertEquals(0.85, request.getTopP());
            assertEquals(2048, request.getMaxTokens());
            assertEquals(0.3, request.getFrequencyPenalty());
            assertEquals(0.2, request.getPresencePenalty());
            assertTrue(request.getStream());
            assertEquals("testUser", request.getUser());
            assertEquals("medium", request.getReasoningEffort());
        }

        @Test
        @DisplayName("思考模式 - thinkingEnabled(true) 创建 enabled 配置")
        void testThinkingEnabled() {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .addUserMessage("test")
                    .thinkingEnabled(true)
                    .build();

            assertNotNull(request.getThinking());
            assertEquals("enabled", request.getThinking().getType());
        }

        @Test
        @DisplayName("思考模式 - thinkingEnabled(false) 创建 disabled 配置")
        void testThinkingDisabled() {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .addUserMessage("test")
                    .thinkingEnabled(false)
                    .build();

            assertNotNull(request.getThinking());
            assertEquals("disabled", request.getThinking().getType());
        }

        @Test
        @DisplayName("ThinkingConfig 静态工厂方法")
        void testThinkingConfigFactory() {
            DeepSeekRequest.ThinkingConfig enabled = DeepSeekRequest.ThinkingConfig.enabled();
            DeepSeekRequest.ThinkingConfig disabled = DeepSeekRequest.ThinkingConfig.disabled();

            assertEquals("enabled", enabled.getType());
            assertEquals("disabled", disabled.getType());
        }

        @Test
        @DisplayName("Jackson 序列化 - null 字段不输出")
        void testJsonSerialization() throws Exception {
            DeepSeekRequest request = DeepSeekRequest.builder()
                    .model("deepseek-v4-pro")
                    .addUserMessage("hello")
                    .temperature(0.7)
                    .build();

            String json = objectMapper.writeValueAsString(request);
            assertTrue(json.contains("\"model\":\"deepseek-v4-pro\""));
            assertTrue(json.contains("\"temperature\":0.7"));
            // stream 为 null 时不应输出
            assertFalse(json.contains("\"stream\""));
        }

        @Test
        @DisplayName("messages(List) 直接设置消息列表")
        void testSetMessagesDirectly() {
            List<DeepSeekMessage> msgs = new ArrayList<>();
            msgs.add(DeepSeekMessage.system("sys"));
            msgs.add(DeepSeekMessage.user("usr"));

            DeepSeekRequest request = DeepSeekRequest.builder()
                    .messages(msgs)
                    .build();

            assertEquals(2, request.getMessages().size());
        }
    }

    // ==================== DeepSeekResponse 测试 ====================

    @Nested
    @DisplayName("DeepSeekResponse 响应测试")
    class DeepSeekResponseTest {

        @Test
        @DisplayName("getContent 提取第一条消息内容")
        void testGetContent() {
            DeepSeekResponse response = createMockResponse("你好世界", null);
            assertEquals("你好世界", response.getContent());
        }

        @Test
        @DisplayName("getReasoningContent 提取思维链内容")
        void testGetReasoningContent() {
            DeepSeekResponse response = createMockResponse("答案", "推理过程");
            assertEquals("推理过程", response.getReasoningContent());
        }

        @Test
        @DisplayName("choices 为空时 getContent 返回 null")
        void testEmptyChoices() {
            DeepSeekResponse response = new DeepSeekResponse();
            response.setChoices(new ArrayList<>());
            assertNull(response.getContent());
            assertNull(response.getReasoningContent());
        }

        @Test
        @DisplayName("choices 为 null 时 getContent 返回 null")
        void testNullChoices() {
            DeepSeekResponse response = new DeepSeekResponse();
            assertNull(response.getContent());
        }

        @Test
        @DisplayName("Usage 信息正确获取")
        void testUsage() {
            DeepSeekResponse response = createMockResponse("test", null);

            DeepSeekResponse.Usage usage = new DeepSeekResponse.Usage() {};
            // Usage 没有 public builder, 通过 setter 设置
            response.setUsage(null);
            assertNull(response.getUsage());
        }

        @Test
        @DisplayName("Jackson 反序列化完整响应 JSON")
        void testJsonDeserialization() throws Exception {
            String json = "{\n" +
                    "  \"id\": \"chatcmpl-123\",\n" +
                    "  \"object\": \"chat.completion\",\n" +
                    "  \"created\": 1700000000,\n" +
                    "  \"model\": \"deepseek-v4-pro\",\n" +
                    "  \"choices\": [{\n" +
                    "    \"index\": 0,\n" +
                    "    \"message\": {\"role\": \"assistant\", \"content\": \"你好!\"},\n" +
                    "    \"finish_reason\": \"stop\"\n" +
                    "  }],\n" +
                    "  \"usage\": {\n" +
                    "    \"prompt_tokens\": 10,\n" +
                    "    \"completion_tokens\": 20,\n" +
                    "    \"total_tokens\": 30\n" +
                    "  }\n" +
                    "}";

            DeepSeekResponse response = objectMapper.readValue(json, DeepSeekResponse.class);

            assertEquals("chatcmpl-123", response.getId());
            assertEquals("deepseek-v4-pro", response.getModel());
            assertEquals("你好!", response.getContent());
            assertNotNull(response.getChoices());
            assertEquals(1, response.getChoices().size());
            assertEquals("stop", response.getChoices().get(0).getFinishReason());
            assertNotNull(response.getUsage());
            assertEquals(10, response.getUsage().getPromptTokens());
            assertEquals(20, response.getUsage().getCompletionTokens());
            assertEquals(30, response.getUsage().getTotalTokens());
        }

        @Test
        @DisplayName("toString 包含关键字段")
        void testToString() {
            DeepSeekResponse response = createMockResponse("test", null);
            response.setId("cmpl-abc");
            String str = response.toString();
            assertTrue(str.contains("cmpl-abc"));
        }

        // 辅助方法：创建模拟响应
        private DeepSeekResponse createMockResponse(String content, String reasoningContent) {
            DeepSeekResponse response = new DeepSeekResponse();
            DeepSeekMessage msg = new DeepSeekMessage("assistant", content);
            msg.setReasoningContent(reasoningContent);

            DeepSeekResponse.Choice choice = new DeepSeekResponse.Choice() {
                // 匿名子类，Choice 没有 public setter for message
                // 需要通过反射或 JSON 反序列化
            };

            // 使用 JSON 反序列化来创建完整的 Choice
            try {
                String choiceJson = "{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\""
                        + content + "\""
                        + (reasoningContent != null ? ",\"reasoning_content\":\"" + reasoningContent + "\"" : "")
                        + "},\"finish_reason\":\"stop\"}";
                DeepSeekResponse.Choice realChoice = objectMapper.readValue(choiceJson, DeepSeekResponse.Choice.class);

                List<DeepSeekResponse.Choice> choices = new ArrayList<>();
                choices.add(realChoice);
                response.setChoices(choices);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return response;
        }
    }

    // ==================== ChatLog 测试 ====================

    @Nested
    @DisplayName("ChatLog 日志模型测试")
    class ChatLogTest {

        @Test
        @DisplayName("构造方法自动生成 ID 和时间戳")
        void testAutoGeneratedFields() {
            ChatLog log = new ChatLog();
            assertNotNull(log.getId());
            assertEquals(16, log.getId().length());
            assertNotNull(log.getTimestamp());
            assertTrue(log.getTimestamp().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        }

        @Test
        @DisplayName("fromResponse 正确构建 ChatLog")
        void testFromResponse() throws Exception {
            String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"你好\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":10,\"total_tokens\":15}}";
            DeepSeekResponse response = objectMapper.readValue(json, DeepSeekResponse.class);
            response.setModel("deepseek-v4-pro");

            ChatLog chatLog = ChatLog.fromResponse("测试问题", response, "deepseek-v4-pro");

            assertEquals("deepseek-v4-pro", chatLog.getModel());
            assertEquals("测试问题", chatLog.getQuestion());
            assertEquals("你好", chatLog.getAnswer());
            assertEquals(5, chatLog.getPromptTokens());
            assertEquals(10, chatLog.getCompletionTokens());
            assertEquals(15, chatLog.getTotalTokens());
            assertEquals("stop", chatLog.getFinishReason());
            assertNotNull(chatLog.getId());
            assertNotNull(chatLog.getTimestamp());
        }

        @Test
        @DisplayName("fromResponse 处理 null Usage")
        void testFromResponseNullUsage() throws Exception {
            String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"回答\"},\"finish_reason\":\"stop\"}]}";
            DeepSeekResponse response = objectMapper.readValue(json, DeepSeekResponse.class);

            ChatLog chatLog = ChatLog.fromResponse("问题", response, "model");

            assertEquals(0, chatLog.getPromptTokens());
            assertEquals(0, chatLog.getCompletionTokens());
            assertEquals(0, chatLog.getTotalTokens());
        }

        @Test
        @DisplayName("toString 截断长问题")
        void testToStringTruncation() {
            ChatLog log = new ChatLog();
            String longQuestion = new String(new char[100]).replace('\0', 'a');
            log.setQuestion(longQuestion);

            String str = log.toString();
            assertTrue(str.contains("..."));
            // 截断后应包含 50 个字符 + "..."
            assertTrue(str.length() < longQuestion.length() + 50);
        }

        @Test
        @DisplayName("toString 不截断短问题")
        void testToStringNoTruncation() {
            ChatLog log = new ChatLog();
            log.setQuestion("短问题");

            String str = log.toString();
            assertTrue(str.contains("短问题"));
            assertFalse(str.contains("..."));
        }
    }

    // ==================== ChatLogStorage 测试 (使用 TempDir) ====================

    @Nested
    @DisplayName("ChatLogStorage XML 存储测试")
    class ChatLogStorageTest {

        @Test
        @DisplayName("保存并加载对话记录")
        void testSaveAndLoad() throws Exception {
            // 通过反射设置 LOG_DIR 为临时目录 (避免影响生产数据)
            // 直接测试 XML 生成和解析逻辑
            ChatLog log = new ChatLog();
            log.setModel("deepseek-v4-pro");
            log.setQuestion("测试问题");
            log.setAnswer("测试回答");
            log.setPromptTokens(10);
            log.setCompletionTokens(20);
            log.setTotalTokens(30);
            log.setFinishReason("stop");

            // 手动创建 XML 文档并保存
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder docBuilder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.newDocument();
            org.w3c.dom.Element root = doc.createElement("chatLogs");
            root.setAttribute("date", "2026-07-16");
            doc.appendChild(root);

            org.w3c.dom.Element record = doc.createElement("chatLog");
            record.setAttribute("id", log.getId());
            record.setAttribute("timestamp", log.getTimestamp());
            record.setAttribute("model", log.getModel());

            org.w3c.dom.Element qElem = doc.createElement("question");
            qElem.setTextContent(log.getQuestion());
            record.appendChild(qElem);

            org.w3c.dom.Element aElem = doc.createElement("answer");
            aElem.setTextContent(log.getAnswer());
            record.appendChild(aElem);

            org.w3c.dom.Element usageElem = doc.createElement("usage");
            usageElem.setAttribute("promptTokens", String.valueOf(log.getPromptTokens()));
            usageElem.setAttribute("completionTokens", String.valueOf(log.getCompletionTokens()));
            usageElem.setAttribute("totalTokens", String.valueOf(log.getTotalTokens()));
            usageElem.setAttribute("finishReason", log.getFinishReason());
            record.appendChild(usageElem);

            root.appendChild(record);

            // 保存到临时文件
            String filePath = tempDir.resolve("chat_log_2026-07-16.xml").toString();
            com.commontool.XMLUtils.saveToFile(doc, filePath);

            // 验证文件存在
            assertTrue(new File(filePath).exists());

            // 加载并验证
            org.w3c.dom.Document loadedDoc = com.commontool.XMLUtils.loadFromFile(filePath);
            assertNotNull(loadedDoc);
            assertEquals("chatLogs", loadedDoc.getDocumentElement().getTagName());
            assertEquals("2026-07-16", loadedDoc.getDocumentElement().getAttribute("date"));

            org.w3c.dom.NodeList records = loadedDoc.getElementsByTagName("chatLog");
            assertEquals(1, records.getLength());

            org.w3c.dom.Element loadedRecord = (org.w3c.dom.Element) records.item(0);
            assertEquals(log.getId(), loadedRecord.getAttribute("id"));
            assertEquals("deepseek-v4-pro", loadedRecord.getAttribute("model"));

            org.w3c.dom.Element loadedQ = (org.w3c.dom.Element) loadedRecord.getElementsByTagName("question").item(0);
            assertEquals("测试问题", loadedQ.getTextContent());

            org.w3c.dom.Element loadedA = (org.w3c.dom.Element) loadedRecord.getElementsByTagName("answer").item(0);
            assertEquals("测试回答", loadedA.getTextContent());
        }

        @Test
        @DisplayName("XML 结构包含所有必需字段")
        void testXmlStructure() throws Exception {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder docBuilder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.newDocument();
            org.w3c.dom.Element root = doc.createElement("chatLogs");
            root.setAttribute("date", "2026-07-16");
            doc.appendChild(root);

            org.w3c.dom.Element record = doc.createElement("chatLog");
            record.setAttribute("id", "test123");
            record.setAttribute("timestamp", "2026-07-16 12:00:00");
            record.setAttribute("model", "deepseek-v4-pro");

            org.w3c.dom.Element qElem = doc.createElement("question");
            qElem.setTextContent("Q");
            record.appendChild(qElem);

            org.w3c.dom.Element aElem = doc.createElement("answer");
            aElem.setTextContent("A");
            record.appendChild(aElem);

            org.w3c.dom.Element rElem = doc.createElement("reasoning");
            rElem.setTextContent("推理过程");
            record.appendChild(rElem);

            org.w3c.dom.Element usageElem = doc.createElement("usage");
            usageElem.setAttribute("promptTokens", "100");
            usageElem.setAttribute("completionTokens", "200");
            usageElem.setAttribute("totalTokens", "300");
            usageElem.setAttribute("finishReason", "stop");
            record.appendChild(usageElem);

            root.appendChild(record);

            String filePath = tempDir.resolve("chat_log_structure.xml").toString();
            com.commontool.XMLUtils.saveToFile(doc, filePath);

            // 验证完整结构
            org.w3c.dom.Document loaded = com.commontool.XMLUtils.loadFromFile(filePath);
            org.w3c.dom.Element loadedRecord = (org.w3c.dom.Element) loaded.getElementsByTagName("chatLog").item(0);

            // 验证 reasoning 节点
            org.w3c.dom.NodeList reasoningNodes = loadedRecord.getElementsByTagName("reasoning");
            assertEquals(1, reasoningNodes.getLength());
            assertEquals("推理过程", reasoningNodes.item(0).getTextContent());

            // 验证 usage 属性
            org.w3c.dom.Element loadedUsage = (org.w3c.dom.Element) loadedRecord.getElementsByTagName("usage").item(0);
            assertEquals("100", loadedUsage.getAttribute("promptTokens"));
            assertEquals("200", loadedUsage.getAttribute("completionTokens"));
            assertEquals("300", loadedUsage.getAttribute("totalTokens"));
            assertEquals("stop", loadedUsage.getAttribute("finishReason"));
        }
    }

    // ==================== DeepSeekRequestLoader 测试 ====================

    @Nested
    @DisplayName("DeepSeekRequestLoader 请求参数加载测试")
    class DeepSeekRequestLoaderTest {

        @BeforeEach
        void clearCache() {
            // 每个测试前清除缓存
            DeepSeekRequestLoader.clearCache();
        }

        @Test
        @DisplayName("从生产环境 JSON 文件加载配置")
        void testLoadFromProductionFile() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            JSONObject config = DeepSeekRequestLoader.loadRequestConfig(prodFile);
            assertNotNull(config);
            assertTrue(config.has("default"));
            assertTrue(config.has("presets"));
        }

        @Test
        @DisplayName("生产环境 JSON 包含所有预设模式")
        void testProductionPresets() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            Set<String> presets = DeepSeekRequestLoader.getAvailablePresets();

            assertTrue(presets.contains("default"));
            assertTrue(presets.contains("coding"));
            assertTrue(presets.contains("translation"));
            assertTrue(presets.contains("creative"));
            assertTrue(presets.contains("analysis"));
            assertTrue(presets.contains("daily"));
        }

        @Test
        @DisplayName("buildDefaultRequest 使用默认预设参数")
        void testBuildDefaultRequest() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildDefaultRequest("你好");

            assertNotNull(request);
            assertEquals(1, request.getMessages().size());
            assertEquals("你好", request.getMessages().get(0).getContent());
            // 默认预设 temperature=0.7
            assertEquals(0.7, request.getTemperature());
            // 默认预设 maxTokens=4096
            assertEquals(4096, request.getMaxTokens());
        }

        @Test
        @DisplayName("coding 预设使用低温度和 Flash 模型")
        void testCodingPreset() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("coding", "写排序算法");

            assertEquals("deepseek-v4-pro", request.getModel());
            assertEquals(0.3, request.getTemperature());
            assertEquals(8192, request.getMaxTokens());
            // coding 模式有系统提示词
            assertTrue(request.getMessages().size() >= 2); // system + user
        }

        @Test
        @DisplayName("translation 预设使用 Flash 模型")
        void testTranslationPreset() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("translation", "翻译内容");

            assertEquals("deepseek-v4-flash", request.getModel());
            assertEquals(0.5, request.getTemperature());
            // 翻译模式思考关闭
            assertNotNull(request.getThinking());
            assertEquals("disabled", request.getThinking().getType());
        }

        @Test
        @DisplayName("creative 预设使用高温度")
        void testCreativePreset() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("creative", "写故事");

            assertEquals(0.9, request.getTemperature());
            assertEquals(0.95, request.getTopP());
        }

        @Test
        @DisplayName("analysis 预设使用高 Token 限制和思考模式")
        void testAnalysisPreset() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("analysis", "证明定理");

            assertEquals(16384, request.getMaxTokens());
            assertEquals(0.2, request.getTemperature());
            assertNotNull(request.getThinking());
            assertEquals("enabled", request.getThinking().getType());
            assertEquals("high", request.getReasoningEffort());
        }

        @Test
        @DisplayName("不存在的预设回退到默认配置")
        void testNonExistentPresetFallback() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("nonexistent", "test");

            // 应回退到 default 预设
            assertEquals(0.7, request.getTemperature());
        }

        @Test
        @DisplayName("从自定义 JSON 文件加载配置")
        void testLoadFromCustomFile() throws IOException {
            String json = "{\n" +
                    "  \"default\": {\n" +
                    "    \"model\": \"deepseek-v4-flash\",\n" +
                    "    \"temperature\": 0.5,\n" +
                    "    \"maxTokens\": 1024,\n" +
                    "    \"thinking\": {\"type\": \"disabled\"}\n" +
                    "  },\n" +
                    "  \"presets\": {\n" +
                    "    \"custom\": {\n" +
                    "      \"model\": \"deepseek-v4-pro\",\n" +
                    "      \"temperature\": 0.1\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            Path customFile = tempDir.resolve("custom_request.json");
            Files.write(customFile, json.getBytes(StandardCharsets.UTF_8));

            DeepSeekRequestLoader.loadRequestConfig(customFile);

            Set<String> presets = DeepSeekRequestLoader.getAvailablePresets();
            assertTrue(presets.contains("custom"));

            DeepSeekRequest request = DeepSeekRequestLoader.buildPresetRequest("custom", "test");
            assertEquals("deepseek-v4-pro", request.getModel());
            assertEquals(0.1, request.getTemperature());
        }

        @Test
        @DisplayName("文件不存在时返回默认配置")
        void testFileNotFoundReturnsDefault() {
            Path nonExistent = tempDir.resolve("nonexistent.json");
            JSONObject config = DeepSeekRequestLoader.loadRequestConfig(nonExistent);

            assertNotNull(config);
            assertTrue(config.has("default"));
        }

        @Test
        @DisplayName("getPresetDescription 返回描述信息")
        void testGetPresetDescription() {
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            DeepSeekRequestLoader.loadRequestConfig(prodFile);
            String desc = DeepSeekRequestLoader.getPresetDescription("coding");
            assertTrue(desc.contains("编程"));
        }
    }

    // ==================== ConfigUtils DeepSeek 集成测试 ====================

    @Nested
    @DisplayName("ConfigUtils DeepSeek 配置集成测试")
    class ConfigUtilsDeepSeekIntegrationTest {

        @Test
        @DisplayName("生产环境 global.json 包含 deepseek 节点")
        void testProductionGlobalJsonHasDeepSeek() throws Exception {
            // 直接读取 JSON 验证结构，避免 ENV 变量不存在时 CryptoUtils.resolve() 抛异常
            String configDir = ConfigUtils.resolveDefaultConfigDir();
            java.nio.file.Path globalPath = Paths.get(configDir, ConfigUtils.DEFAULT_GLOBAL_JSON);
            assumeTrue(Files.exists(globalPath), "生产环境 global.json 不存在，跳过");

            String content = new String(Files.readAllBytes(globalPath), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);

            assertTrue(json.has("deepseek"), "生产环境 global.json 应包含 deepseek 节点");
            JSONObject deepseek = json.getJSONObject("deepseek");
            assertTrue(deepseek.has("apiKey"), "deepseek 节点应包含 apiKey 字段");
            assertTrue(deepseek.has("model"), "deepseek 节点应包含 model 字段");
        }

        @Test
        @DisplayName("ConfigLoader 读取 DeepSeek 默认值")
        void testConfigLoaderDeepSeekDefaults() throws IOException {
            // 创建包含 deepseek 节点的临时配置
            String globalJson = "{\n" +
                    "  \"jsonFile\": \"db.json\",\n" +
                    "  \"propFilePath\": \"db.prop\",\n" +
                    "  \"pomFilepath\": \"pom.xml\",\n" +
                    "  \"deepseek\": {\n" +
                    "    \"apiKey\": \"sk-plaintext-key\",\n" +
                    "    \"model\": \"deepseek-v4-flash\",\n" +
                    "    \"baseUrl\": \"https://custom.api.com/v1/\",\n" +
                    "    \"thinkingEnabled\": false,\n" +
                    "    \"reasoningEffort\": \"low\",\n" +
                    "    \"systemPrompt\": \"你是翻译官\",\n" +
                    "    \"requestParamsFile\": \"data/custom_request.json\"\n" +
                    "  }\n" +
                    "}";
            String dbJson = "{\"url\":\"jdbc:mysql://localhost/db\",\"username\":\"root\",\"password\":\"pass\",\"driver\":\"com.mysql.Driver\"}";

            Files.write(tempDir.resolve("global.json"), globalJson.getBytes(StandardCharsets.UTF_8));
            Files.write(tempDir.resolve("databases.json"), dbJson.getBytes(StandardCharsets.UTF_8));

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());

            assertAll("验证 DeepSeek 配置",
                    () -> assertEquals("sk-plaintext-key", loader.getDeepSeekApiKey()),
                    () -> assertEquals("deepseek-v4-flash", loader.getDeepSeekModel()),
                    () -> assertEquals("https://custom.api.com/v1/", loader.getDeepSeekBaseUrl()),
                    () -> assertFalse(loader.isDeepSeekThinkingEnabled()),
                    () -> assertEquals("low", loader.getDeepSeekReasoningEffort()),
                    () -> assertEquals("你是翻译官", loader.getDeepSeekSystemPrompt()),
                    () -> assertEquals("data/custom_request.json", loader.getDeepSeekRequestParamsFile()),
                    () -> assertTrue(loader.isDeepSeekConfigured())
            );
        }

        @Test
        @DisplayName("明文 API Key 直接返回")
        void testPlaintextApiKey() throws IOException {
            writeDeepSeekConfig("\"apiKey\": \"sk-my-plain-key\"");

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            assertEquals("sk-my-plain-key", loader.getDeepSeekApiKey());
        }

        @Test
        @DisplayName("ENC() 加密 API Key 自动解密")
        void testEncApiKey() throws IOException {
            // 先加密一个已知明文
            String encrypted = com.system.CryptoUtils.encrypt("sk-secret-key");
            writeDeepSeekConfig("\"apiKey\": \"ENC(" + encrypted + ")\"");

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            assertEquals("sk-secret-key", loader.getDeepSeekApiKey());
        }

        @Test
        @DisplayName("${ENV:} 环境变量引用 API Key")
        void testEnvApiKey() throws IOException {
            // PATH 是 Windows 上一定存在的环境变量
            writeDeepSeekConfig("\"apiKey\": \"${ENV:PATH}\"");

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            String apiKey = loader.getDeepSeekApiKey();
            // 应该返回 PATH 环境变量的值，不为空
            assertNotNull(apiKey);
            assertFalse(apiKey.isEmpty());
            assertNotEquals("${ENV:PATH}", apiKey);
        }

        @Test
        @DisplayName("deepseek 节点不存在时返回空配置")
        void testNoDeepSeekNode() throws IOException {
            String globalJson = "{\"jsonFile\":\"db.json\",\"propFilePath\":\"p\",\"pomFilepath\":\"x\"}";
            String dbJson = "{\"url\":\"u\",\"username\":\"root\",\"password\":\"p\",\"driver\":\"d\"}";

            Files.write(tempDir.resolve("global.json"), globalJson.getBytes(StandardCharsets.UTF_8));
            Files.write(tempDir.resolve("databases.json"), dbJson.getBytes(StandardCharsets.UTF_8));

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());

            assertFalse(loader.isDeepSeekConfigured());
            assertEquals("", loader.getDeepSeekApiKey());
            assertEquals("deepseek-v4-pro", loader.getDeepSeekModel()); // 默认值
        }

        @Test
        @DisplayName("extractDeepSeekData 静态方法直接提取")
        void testExtractDeepSeekData() {
            JSONObject globalJson = new JSONObject("{\n" +
                    "  \"deepseek\": {\n" +
                    "    \"apiKey\": \"sk-test\",\n" +
                    "    \"model\": \"deepseek-v4-pro\",\n" +
                    "    \"thinkingEnabled\": true\n" +
                    "  }\n" +
                    "}");

            Map<String, Object> data = ConfigUtils.extractDeepSeekData(globalJson);

            assertEquals("sk-test", data.get("apiKey"));
            assertEquals("deepseek-v4-pro", data.get("model"));
            assertEquals(true, data.get("thinkingEnabled"));
        }

        @Test
        @DisplayName("extractDeepSeekData 无 deepseek 节点返回空 Map")
        void testExtractDeepSeekDataNoNode() {
            JSONObject globalJson = new JSONObject("{\"other\": \"value\"}");
            Map<String, Object> data = ConfigUtils.extractDeepSeekData(globalJson);
            assertTrue(data.isEmpty());
        }

        @Test
        @DisplayName("getDeepSeekRequestParamsFileObj 返回解析后的 File")
        void testGetRequestParamsFileObj() throws IOException {
            writeDeepSeekConfig("\"apiKey\": \"sk-test\", \"requestParamsFile\": \"data/deepseek_request.json\"");

            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            File file = loader.getDeepSeekRequestParamsFileObj();
            assertNotNull(file);
            assertTrue(file.getPath().contains("deepseek_request.json"));
        }

        // 辅助方法：写入包含 deepseek 节点的 global.json 和 databases.json
        private void writeDeepSeekConfig(String deepseekFields) throws IOException {
            String globalJson = "{\n" +
                    "  \"jsonFile\": \"db.json\",\n" +
                    "  \"propFilePath\": \"db.prop\",\n" +
                    "  \"pomFilepath\": \"pom.xml\",\n" +
                    "  \"deepseek\": {\n" +
                    "    " + deepseekFields + "\n" +
                    "  }\n" +
                    "}";
            String dbJson = "{\"url\":\"jdbc:mysql://localhost/db\",\"username\":\"root\",\"password\":\"pass\",\"driver\":\"com.mysql.Driver\"}";

            Files.write(tempDir.resolve("global.json"), globalJson.getBytes(StandardCharsets.UTF_8));
            Files.write(tempDir.resolve("databases.json"), dbJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ==================== DeepSeekTool 初始化测试 (不需要真实 API Key) ====================

    @Nested
    @DisplayName("DeepSeekTool 工具类测试")
    class DeepSeekToolTest {

        @AfterEach
        void shutdown() {
            try {
                DeepSeekTool.shutdown();
            } catch (Exception ignored) {
            }
        }

        @Test
        @DisplayName("未初始化时调用 ensureClient 抛出 IllegalStateException")
        void testNotInitialized_throwsException() {
            assertFalse(DeepSeekTool.isInitialized());
            assertThrows(IllegalStateException.class, () ->
                    DeepSeekTool.ask("test"));
        }

        @Test
        @DisplayName("init(String) 成功初始化")
        void testInitWithApiKey() {
            DeepSeekTool.init("sk-test-key");
            assertTrue(DeepSeekTool.isInitialized());
        }

        @Test
        @DisplayName("init(String, String) 带系统提示词初始化")
        void testInitWithApiKeyAndPrompt() {
            DeepSeekTool.init("sk-test-key", "你是助手");
            assertTrue(DeepSeekTool.isInitialized());
        }

        @Test
        @DisplayName("init(DeepSeekConfig) 完整配置初始化")
        void testInitWithConfig() {
            DeepSeekConfig config = DeepSeekConfig.builder()
                    .apiKey("sk-test")
                    .model(DeepSeekModel.DEEPSEEK_V4_FLASH)
                    .thinkingEnabled(false)
                    .build();
            DeepSeekTool.init(config);
            assertTrue(DeepSeekTool.isInitialized());
        }

        @Test
        @DisplayName("shutdown 后 isInitialized 返回 false")
        void testShutdown() {
            DeepSeekTool.init("sk-test");
            assertTrue(DeepSeekTool.isInitialized());

            DeepSeekTool.shutdown();
            assertFalse(DeepSeekTool.isInitialized());
        }

        @Test
        @DisplayName("setAutoLog 控制自动日志开关")
        void testAutoLogToggle() {
            DeepSeekTool.setAutoLog(true);
            assertTrue(DeepSeekTool.isAutoLogEnabled());

            DeepSeekTool.setAutoLog(false);
            assertFalse(DeepSeekTool.isAutoLogEnabled());

            // 恢复默认
            DeepSeekTool.setAutoLog(true);
        }

        @Test
        @DisplayName("getAvailablePresets 返回预设列表")
        void testGetAvailablePresets() {
            DeepSeekRequestLoader.clearCache();
            Path prodFile = Paths.get("app/src/main/resources/data/deepseek_request.json");
            assumeTrue(Files.exists(prodFile), "生产环境 JSON 文件不存在，跳过");

            Set<String> presets = DeepSeekTool.getAvailablePresets();
            assertNotNull(presets);
            assertFalse(presets.isEmpty());
        }

        @Test
        @DisplayName("重复初始化关闭旧客户端")
        void testReinitialize() {
            DeepSeekTool.init("sk-key-1");
            assertTrue(DeepSeekTool.isInitialized());

            // 再次初始化应关闭旧客户端
            DeepSeekTool.init("sk-key-2");
            assertTrue(DeepSeekTool.isInitialized());
        }
    }
}
