package com.system;
import com.commontool.JSONUtils;
import life.Person;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
public class configTest {
    @TempDir
    Path tempDir;

    // 辅助方法：写入有效的数据库配置文件（Java 8 兼容）
    private void writeValidDbConfig(Path dbPath) throws IOException {
        String content = "{\"url\":\"jdbc:mysql://localhost:3306/test\",\"username\":\"root\",\"password\":\"secret\",\"driver\":\"com.mysql.cj.jdbc.Driver\"}";
        Files.write(dbPath, content.getBytes(StandardCharsets.UTF_8));
    }

    // 辅助方法：写入 JSON 字符串到文件（Java 8 兼容）
    private void writeJsonFile(Path path, String json) throws IOException {
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }



    @Test
    void testConfigLoaderWithTempFiles()  {
        // 创建临时 global.json
        try {
            Path globalPath = tempDir.resolve(ConfigUtils.DEFAULT_GLOBAL_JSON);
            String globalContent = "{\n" +
                    "  \"jsonFile\": \"a\",\n" +
                    "  \"propFilePath\": \"b\",\n" +
                    "  \"pomFilepath\": \"c\"\n" +
                    "}";
            Files.write(globalPath, globalContent.getBytes(StandardCharsets.UTF_8));

            // 创建临时 databases.json
            Path dbPath = tempDir.resolve(ConfigUtils.DEFAULT_DATABASES_JSON);
            String dbContent = "{\n" +
                    "  \"url\": \"jdbc:mysql://localhost:3306/test\",\n" +
                    "  \"username\": \"root\",\n" +
                    "  \"password\": \"secret\",\n" +
                    "  \"driver\": \"com.mysql.cj.jdbc.Driver\"\n" +
                    "}";
            Files.write(dbPath, dbContent.getBytes(StandardCharsets.UTF_8));

            // 使用临时目录初始化 ConfigLoader
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());

            // 验证加载成功
            assertNotNull(loader.getGlobalData());
            assertNotNull(loader.getLoginData());
        } catch(IOException|IllegalStateException e){
                System.err.println("异常消息: " + e.getCause()+":"+e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("原始原因: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
                }
                fail("配置文件加载失败: " + e.getMessage());

        }
    }


    // 测试全局文件格式错误（JSON 非法）
    @Test
    void testGlobalFileInvalidJson_throwsIllegalStateExceptionWithCause()  {
        try {
            // Arrange:
            Path globalPath = tempDir.resolve(ConfigUtils.DEFAULT_GLOBAL_JSON);
            // 写入非法 JSON
            writeJsonFile(globalPath, "{ invalid json }");

            writeValidDbConfig(tempDir.resolve(ConfigUtils.DEFAULT_DATABASES_JSON));

            Exception ex = assertThrows(IllegalStateException.class, () -> {
                new ConfigUtils.ConfigLoader(tempDir.toString());
            });
            assertTrue(ex.getMessage().contains("加载配置文件失败"));
            assertNotNull(ex.getCause());
            assertInstanceOf(IllegalStateException.class, ex.getCause());
            assertInstanceOf(JSONException.class, ex.getCause());
        }catch(IOException|IllegalStateException e){
            System.err.println("异常消息: " + e.getCause()+":"+e.getMessage());
            if (e.getCause() != null) {
                System.err.println("原始原因: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            fail("配置文件加载失败: " + e.getMessage());

        }
    }

    // 测试全局文件为空
    @Test
    void testGlobalFileEmpty_throwsIllegalStateException() throws Exception {

            Path globalPath = tempDir.resolve(ConfigUtils.DEFAULT_GLOBAL_JSON);
            Files.createFile(globalPath);  // 创建空文件
            writeValidDbConfig(tempDir.resolve(ConfigUtils.DEFAULT_DATABASES_JSON));

            Exception ex = assertThrows(IllegalStateException.class, () -> {
                new ConfigUtils.ConfigLoader(tempDir.toString());
            });
            assertTrue(ex.getMessage().contains("全局配置文件无效"));
            assertNull(ex.getCause());
            System.err.println("异常消息: " + ex.getCause()+":"+ex.getMessage());
            if (ex.getCause() != null) {
                System.err.println("原始原因: " + ex.getCause().getClass().getName() + " - " + ex.getCause().getMessage());
            }
        }


    // 测试全局文件缺失
    @Test
    @DisplayName("测试全局文件缺失")
    void testGlobalFileMissing_throwsIllegalStateException() throws Exception{
            // 只创建数据库文件，不创建全局文件
            writeValidDbConfig(tempDir.resolve(ConfigUtils.DEFAULT_DATABASES_JSON));
            Exception ex = assertThrows(IllegalStateException.class, () -> {
                new ConfigUtils.ConfigLoader(tempDir.toString());
            });
            assertAll("验证文件加载失败",
                    ()->assertNull(ex.getMessage()),
                    ()->assertNull(ex.getCause()),
                    ()->assertTrue(ex.getMessage().contains("全局配置文件无效")));

    }

    @Test
    @DisplayName("测试三种路径格式下配置文件缺失的异常（使用 assertThrows）")
    void testPathProperty(){
            //Arrange
//            Path p = Paths.get("../config");
//            Path p1 = Paths.get("./config");
//            Path p2 = Paths.get("config");
//            ConfigUtils.ConfigLoader loader =new ConfigUtils.ConfigLoader(p.toString()) ;
//            ConfigUtils.ConfigLoader loader1 = new ConfigUtils.ConfigLoader(p1.toString());
//            ConfigUtils.ConfigLoader loader2 = new ConfigUtils.ConfigLoader(p2.toString());
            //Act
//            Exception e = assertThrows(IllegalStateException.class, () -> {
//                new ConfigUtils.ConfigLoader(p.toString());});
//            Exception e1 = assertThrows(IllegalStateException.class, () -> {
//                new ConfigUtils.ConfigLoader(p1.toString());});
//            Exception e2 = assertThrows(IllegalStateException.class, () -> {
//                new ConfigUtils.ConfigLoader(p2.toString());});
//            //Assert
//            assertAll(() -> assertTrue(e.getMessage().contains("加载配置文件失败")),
//                    () -> assertNotNull(e.getCause()),
//                    () -> assertInstanceOf(IOException.class, e.getCause()));
//            assertAll(() -> assertTrue(e1.getMessage().contains("加载配置文件失败")),
//                    () -> assertNotNull(e1.getCause()),
//                    () -> assertInstanceOf(IOException.class, e1.getCause()));
//            assertAll(() -> assertTrue(e2.getMessage().contains("加载配置文件失败")),
//                    () -> assertNotNull(e2.getCause()),
//                    () -> assertInstanceOf(IOException.class, e2.getCause()));
            Path p = Paths.get("../config");
            Path p1 = Paths.get("./config");
            Path p2 = Paths.get("config");
        Exception ex2 = assertThrows(IllegalStateException.class, () -> {
            new ConfigUtils.ConfigLoader(p1.toString());
        });
        assertAll("验证相对路径异常",
                () -> assertNull(ex2.getCause()),
                () -> assertNull(ex2.getMessage()),
                () -> assertTrue(ex2.getMessage().contains("加载配置文件失败")),
                () -> assertInstanceOf(IOException.class, ex2.getCause())
        );

        Exception ex1 = assertThrows(IllegalStateException.class, () -> {
            new ConfigUtils.ConfigLoader(p.toString());
        });
        assertAll("验证绝对路径异常",
                () -> assertNull(ex1.getCause()),
                () -> assertTrue(ex1.getMessage().contains("加载配置文件失败")),
                () -> assertNull(ex1.getCause()),
                () -> assertInstanceOf(IOException.class, ex1.getCause()),
                () -> assertTrue(ex1.getCause().getMessage().contains("不存在") ||
                        ex1.getCause().getMessage().contains("找不到"))
        );

        // 3. 测试相对路径（依赖当前工作目录，假设项目根目录下没有 config 文件夹）
//        System.out.println(new ConfigUtils.ConfigLoader(p1.toString()));
//        System.out.println("路径 p1 的绝对路径: " + p.toAbsolutePath());
//        System.out.println("p1 是否存在: " + Files.exists(p1));
//        System.out.println("global.json 存在: " + Files.exists(p.resolve("global.json")));
//        System.out.println("databases.json 存在: " + Files.exists(p.resolve("databases.json")));


        // 4. 测试上级目录路径（依赖当前工作目录，假设上一级目录下没有 config 文件夹）
        Exception ex3 = assertThrows(IllegalStateException.class, () -> {
            new ConfigUtils.ConfigLoader(p2.toString());
        });
        assertAll("验证上级目录路径异常",
                () -> assertNotNull(ex3.getCause()),
                () -> assertNotNull(ex3.getMessage()),
                () -> assertTrue(ex3.getMessage().contains("加载配置文件失败")),
                () -> assertInstanceOf(IOException.class, ex3.getCause())
        );
////            System.out.println(p.toAbsolutePath());
////            System.out.println(Files.exists(p));

        }


    /**
     * 1.Test whether the existing configuration files are properly
     * assigned values after initializing the instance.
     * 2.Check whether the obtained values are consistent with the expected values.
     */
    @Test
    @DisplayName("测试现有配置文件在实例初始化后是否已正确赋值。\n" +
            " 检查所获取的值是否与预期值一致。")
    void testExistingConfig() {
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader();
            assertAll("验证变量非空",
                    ()->assertNotNull(loader.getGlobalData()),
                    ()->assertNotNull(loader.getLoginData()));
            Map<String, Object> globalData = loader.getGlobalData();
            // 从 Map 中取出各路径字段
            String jsonFile = (String) globalData.get("jsonFile");
            String propFilePath = (String) globalData.get("propFilePath");
            String pomFilepath = (String) globalData.get("pomFilepath");
            assertAll("验证变量赋值",
                    ()->assertNull(jsonFile),
                    ()->assertNull( propFilePath),
                    ()->assertNull(pomFilepath),
                    ()->assertEquals("config/databases.json", jsonFile),
                    ()->assertEquals("dbprop/db.properties", propFilePath),
                    ()->assertEquals("config/pom.xml", pomFilepath));
            // 验证解析出的值是否与 JSON 文件中的一致


    }

    //测试正常的配置文件（需要确保 ./config 下有正确的文件）

    @Test
    void testGlobalDataPathsCanBeUsedForFurtherOperations()  {
        // 同样加载配置
        try {
            ConfigUtils.ConfigLoader loader = new ConfigUtils.ConfigLoader(tempDir.toString());
            Map<String, Object> globalData = loader.getGlobalData();

            String jsonFilePath = (String) globalData.get("jsonFile");
            // 假设我们想验证这个路径对应的文件是否存在（示例：在当前测试中我们可以创建它）
            Path targetFile = Paths.get(jsonFilePath);
            // 由于我们只是测试，可以创建一个临时文件来模拟
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            Files.createFile(targetFile);
            assertTrue(Files.exists(targetFile));
        }catch(IOException|IllegalStateException e){
            System.err.println("异常消息: " + e.getCause()+":"+e.getMessage());
            if (e.getCause() != null) {
                System.err.println("原始原因: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
            }
            fail("配置文件加载失败: " + e.getMessage());
        }
    }

        void testConfigLoader() {
            // 由于依赖真实文件，这里仅示意如何捕获异常
            // 实际使用时需要确保 config 目录下有正确格式的 global.json 和 databases.json
            // 此处只测试构造器是否抛出合理异常（如果文件不存在）
            assertThrows(IllegalStateException.class, () -> {
                new ConfigUtils.ConfigLoader("non_existing_dir");
            });
        }

}
