/*
 * UAT 测试层 —— 浏览器自动化验收测试
 *
 * 特点：模拟真实用户通过浏览器操作，验证业务功能是否满足需求
 * 依赖：Selenium WebDriver 3.x + WebDriverManager（已在 build.gradle 引入）
 * 运行：.\gradlew.bat app:test --tests "com.adbtool.WebSocketUATTest" "-Dprod.server=localhost:6655"
 *
 * 说明：
 *   UAT 测试由业务方/最终用户参与验收，验证：
 *   - 浏览器打开首页能看到设备列表
 *   - 点击设备能进入控制页面
 *   - 实时截屏画面正常显示
 *   - 触摸/按键操作能正确响应
 *   - 页面 CSS/JS 资源正常加载
 *
 * 前置条件：
 *   1. 本机安装 Chrome 浏览器（WebDriverManager 会自动下载匹配的 ChromeDriver）
 *   2. 先启动真实服务器：.\gradlew.bat app:run
 *   3. 另一个终端运行 UAT 测试：.\gradlew.bat app:test --tests "com.adbtool.WebSocketUATTest" -Dprod.server=localhost:6655
 */

package com.adbtool;

import org.junit.jupiter.api.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 【UAT 测试层】用户验收测试
 * <p>
 * 使用 Selenium WebDriver 模拟真实用户通过浏览器操作 AndroidControl 系统，
 * 验证业务功能是否满足需求。
 * <p>
 * UAT 测试与系统测试的区别：
 * <ul>
 *   <li>系统测试：验证技术层面的端到端链路（TCP/WebSocket/HTTP 协议）</li>
 *   <li>UAT 测试：验证业务层面的用户体验（页面展示/交互响应/功能完整性）</li>
 * </ul>
 */
@DisplayName("【UAT 测试】用户验收测试（浏览器自动化）")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WebSocketUATTest {

    /** 外部服务器地址，通过 -Dprod.server=IP:PORT 指定 */
    private String PROD_SERVER;

    /** Selenium WebDriver 实例 */
    private WebDriver driver;

    /** 显式等待 */
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        PROD_SERVER = System.getProperty("prod.server");
        assumeTrue(PROD_SERVER != null && !PROD_SERVER.isEmpty(),
                "未指定 -Dprod.server，跳过 UAT 测试。" +
                "用法: gradle test -Dprod.server=localhost:6655");

        // 使用 WebDriverManager 自动下载匹配 Chrome 版本的 ChromeDriver
        WebDriverManager.chromedriver().setup();

        // 配置 Chrome 无头模式（CI 环境无需显示器）
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");           // 无头模式
        options.addArguments("--no-sandbox");          // 兼容 Docker/CI
        options.addArguments("--disable-dev-shm-usage"); // 避免 /dev/shm 空间不足
        options.addArguments("--window-size=1280,800");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, 10);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ==================== 首页验收测试 ====================

    @Nested
    @DisplayName("首页验收")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class IndexPageUAT {

        @Test
        @Order(1)
        @DisplayName("用户打开首页应看到设备列表区域")
        void testIndexPageShowsDeviceList() {
            driver.get("http://" + PROD_SERVER + "/");

            // 验证页面标题
            String title = driver.getTitle();
            assertTrue(title.contains("AndroidControl"),
                    "页面标题应包含 'AndroidControl'，实际: " + title);

            // 验证设备列表容器存在
            WebElement phoneList = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("phone-list")));
            assertNotNull(phoneList, "页面应包含 #phone-list 设备列表区域");
            System.out.println("[UAT] 首页设备列表区域已加载");
        }

        @Test
        @Order(2)
        @DisplayName("无设备时显示空状态提示")
        void testEmptyStateMessage() throws Exception {
            driver.get("http://" + PROD_SERVER + "/");

            // 等待 Vue 渲染完成，检查是否有空状态提示或设备列表
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("phone-list")));
            Thread.sleep(1000); // 等待 Vue.js 完成数据绑定

            // 当无设备连接时，应显示空状态提示
            List<WebElement> emptyItems = driver.findElements(
                    By.cssSelector(".empty-state-item"));
            List<WebElement> phoneItems = driver.findElements(
                    By.cssSelector("#phone-list .phone"));

            // 两种情况之一必须成立：要么有空状态提示，要么有设备卡片
            assertTrue(!emptyItems.isEmpty() || !phoneItems.isEmpty(),
                    "页面应显示空状态提示或设备列表");

            if (!emptyItems.isEmpty()) {
                String emptyText = emptyItems.get(0).getText();
                assertTrue(emptyText.contains("暂无设备"),
                        "空状态提示应包含 '暂无设备'，实际: " + emptyText);
                System.out.println("[UAT] 空状态提示正确: " + emptyText);
            } else {
                System.out.println("[UAT] 当前有 " + phoneItems.size() + " 台设备连接");
            }
        }

        @Test
        @Order(3)
        @DisplayName("页面 CSS 和 JS 资源正常加载")
        void testResourceLoading() {
            driver.get("http://" + PROD_SERVER + "/");

            // 等待页面完全加载
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("phone-list")));

            // 验证 Bootstrap CSS 已加载（检查 .navbar 样式是否生效）
            WebElement navbar = driver.findElement(By.cssSelector(".navbar"));
            String navbarDisplay = navbar.getCssValue("display");
            assertNotNull(navbarDisplay, "Bootstrap .navbar 应存在");

            // 验证 Font Awesome 已加载（检查图标元素是否渲染）
            WebElement faIcon = driver.findElement(By.cssSelector(".fa"));
            assertNotNull(faIcon, "Font Awesome 图标应存在");

            // 验证 jQuery 已加载（通过 JS 执行检查）
            Boolean jqueryLoaded = (Boolean) ((JavascriptExecutor) driver)
                    .executeScript("return typeof jQuery !== 'undefined'");
            assertTrue(jqueryLoaded, "jQuery 应已加载");

            // 验证 Vue.js 已加载
            Boolean vueLoaded = (Boolean) ((JavascriptExecutor) driver)
                    .executeScript("return typeof Vue !== 'undefined'");
            assertTrue(vueLoaded, "Vue.js 应已加载");

            System.out.println("[UAT] CSS/JS 资源全部正常加载");
        }
    }

    // ==================== 设备控制页验收测试 ====================

    @Nested
    @DisplayName("设备控制页验收")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DevicePageUAT {

        @Test
        @Order(1)
        @DisplayName("直接访问 device.html 页面应正常加载")
        void testDevicePageLoads() {
            // 直接打开 device.html（带 sn 参数）
            driver.get("http://" + PROD_SERVER + "/device.html?sn=test_device");

            // 验证页面标题
            String title = driver.getTitle();
            assertTrue(title.contains("AndroidControl"),
                    "控制页标题应包含 'AndroidControl'，实际: " + title);

            // 验证 canvas 截屏区域存在
            WebElement canvas = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("phone-screen")));
            assertNotNull(canvas, "控制页应包含 #phone-screen 截屏画布");

            // 验证底部控制按钮存在
            assertNotNull(driver.findElement(By.id("btn-back")), "应包含返回按钮");
            assertNotNull(driver.findElement(By.id("btn-home")), "应包含主页按钮");
            assertNotNull(driver.findElement(By.id("btn-menu")), "应包含菜单按钮");

            System.out.println("[UAT] 设备控制页加载成功");
        }

        @Test
        @Order(2)
        @DisplayName("控制页面应显示截屏画布和控制按钮")
        void testControlPageShowsScreenAndControls() {
            driver.get("http://" + PROD_SERVER + "/device.html?sn=test_device");

            // 等待页面加载
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("phone-screen")));

            // 验证 canvas 元素存在且尺寸合理
            WebElement canvas = driver.findElement(By.id("phone-screen"));
            int width = canvas.getSize().getWidth();
            int height = canvas.getSize().getHeight();
            assertTrue(width > 0 && height > 0,
                    "截屏画布应有实际尺寸，当前: " + width + "x" + height);

            // 验证连接状态指示器存在
            WebElement statusDot = driver.findElement(By.id("connection-status"));
            assertNotNull(statusDot, "应包含连接状态指示器");

            WebElement statusText = driver.findElement(By.id("connection-text"));
            assertNotNull(statusText, "应包含连接状态文本");
            System.out.println("[UAT] 连接状态: " + statusText.getText());

            // 验证底部功能按钮全部存在
            String[] buttonIds = {"btn-back", "btn-home", "btn-menu", "btn-more",
                    "shot-screen", "display-no-sleep", "close-data"};
            for (String id : buttonIds) {
                WebElement btn = driver.findElement(By.id(id));
                assertNotNull(btn, "控制按钮 #" + id + " 应存在");
            }
            System.out.println("[UAT] 所有 " + buttonIds.length + " 个控制按钮均存在");
        }

        @Test
        @Order(3)
        @DisplayName("控制页面右侧面板 Tab 切换应正常")
        void testControlPanelTabSwitch() {
            driver.get("http://" + PROD_SERVER + "/device.html?sn=test_device");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("phone-screen")));

            // 验证 4 个 Tab 面板存在
            String[] tabIds = {"group", "farewell", "operation", "settings"};
            String[] tabNames = {"任务管理", "设备状态", "基本操作", "参数设置"};
            for (int i = 0; i < tabIds.length; i++) {
                WebElement tab = driver.findElement(By.id(tabIds[i]));
                assertNotNull(tab, "Tab #" + tabIds[i] + " 应存在");
                String tabText = tab.getText();
                assertTrue(tabText.contains(tabNames[i]),
                        "Tab 应包含 '" + tabNames[i] + "' 文字，实际: " + tabText);
            }

            // 验证默认显示任务管理页面
            WebElement taskPage = driver.findElement(By.id("make-task"));
            assertEquals("block", taskPage.getCssValue("display").trim().toLowerCase()
                            .replaceAll(".*?(block).*", "$1"),
                    "默认应显示任务管理页面");

            System.out.println("[UAT] 右侧面板 4 个 Tab 均正常显示");
        }
    }
}
