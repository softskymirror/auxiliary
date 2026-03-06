package com.searchtool;

import com.system.CmdUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.system.CmdUtils.executive;

public class FileUtils {
    private static final String DEFAULT_CHARSET = "UTF-8";

    // 私有构造器，防止实例化
    private FileUtils() {}

    /* ==================== 目录/文件创建 ==================== */

    /**
     * 确保目录存在，如果不存在则创建（包括父目录）
     *
     * @param dirPath 目录路径
     * @return 目录的 File 对象
     * @throws IOException 如果创建失败
     */
    public static File ensureDirectoryExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        } else if (!Files.isDirectory(path)) {
            throw new IOException("路径已存在但不是目录: " + dirPath);
        }
        return path.toFile();
    }

    /**
     * 在指定目录下创建文件（如果文件不存在）
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @return 文件的 File 对象
     * @throws IOException 如果创建失败
     */
    public static File createFileIfNotExists(String dirPath, String fileName) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        Path filePath = dir.resolve(fileName);
        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
        }
        return filePath.toFile();
    }

    /* ==================== 文件重命名/移动 ==================== */

    /**
     * 重命名或移动文件（原子操作，如果支持）
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否成功
     * @throws IOException 如果移动失败
     */
    public static boolean moveFile(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        // 确保目标父目录存在
        if (target.getParent() != null && Files.notExists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return true;
    }


    /**
     * 重命名文件（同一目录下）
     * @param dirPath   目录路径
     * @param oldName   旧文件名
     * @param newName   新文件名
     * @return 是否成功
     * @throws IOException 如果重命名失败
     */
    public static boolean renameFile(String dirPath, String oldName, String newName) throws IOException {
        Path dir = Paths.get(dirPath);
        Path oldFile = dir.resolve(oldName);
        Path newFile = dir.resolve(newName);
        if (Files.notExists(oldFile)) {
            throw new IOException("源文件不存在: " + oldFile);
        }
        Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @param replaceExisting 是否替换已存在的目标文件
     * @throws IOException 如果复制失败
     */
    public static void copyFile(String sourcePath, String targetPath, boolean replaceExisting) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        if (Files.notExists(source)) {
            throw new IOException("源文件不存在: " + sourcePath);
        }
        if (target.getParent() != null && Files.notExists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        if (replaceExisting) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(source, target);
        }
    }

    /**
     * 批量复制文件到指定目录
     *
     * @param sourceDir   源目录路径
     * @param fileNames   待复制的文件名列表（仅文件名，不包含路径）
     * @param targetDir   目标目录路径
     * @param replace     是否覆盖已存在的目标文件
     * @throws IOException 如果复制过程中发生 I/O 错误
     */
    public static void copyListFiles(String sourceDir, List<String> fileNames, String targetDir, boolean replace)
            throws IOException {
        // 确保目标目录存在
        Path targetPath = Paths.get(targetDir);
        if (Files.notExists(targetPath)) {
            Files.createDirectories(targetPath);
        }

        // 遍历文件列表进行复制
        for (String fileName : fileNames) {
            Path source = Paths.get(sourceDir, fileName);
            Path target = targetPath.resolve(fileName);

            // 检查源文件是否存在
            if (Files.notExists(source)) {
                System.err.println("源文件不存在，跳过: " + source);
                continue;
            }

            // 执行复制
            try {
                if (replace) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.copy(source, target);
                }
                System.out.println("复制成功: " + fileName + " -> " + targetDir);
            } catch (FileAlreadyExistsException e) {
                System.err.println("目标文件已存在且未覆盖，跳过: " + target);
            } catch (IOException e) {
                System.err.println("复制失败: " + fileName + "，原因: " + e.getMessage());
                // 可根据需要决定是否抛出异常或继续复制其他文件
            }
        }
    }

    /**
     * 删除单个文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功（文件不存在时返回 false）
     * @throws IOException 如果删除失败（如权限问题）
     */
    public static boolean deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.isDirectory(path)) {
            throw new IOException("路径是目录，请使用 deleteDirectory 方法: " + filePath);
        }
        return Files.deleteIfExists(path);
    }

    /**
     * 递归删除目录及其所有内容
     *
     * @param dirPath 目录路径
     * @throws IOException 如果删除失败
     */
    public static void deleteDirectory(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.notExists(dir)) {
            return;
        }
        if (!Files.isDirectory(dir)) {
            throw new IOException("路径不是目录: " + dirPath);
        }
        // 使用 walkFileTree 递归删除
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


//    /**
//     * ɾ��ָ���ļ�
//     * @param file_path
//     */
//    public static void deleteFile(String file_path) throws Exception{
//        File file=new File(file_path);
//        if(checkFile(file_path)){
//            file.delete();
//            System.out.println(file_path+"ɾ���ɹ�");
//        }
//    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return true 如果存在且是文件（不是目录）
     */
    public static boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && !Files.isDirectory(path);
    }

    /**
     * 检查目录是否存在
     * @param dirPath 目录路径
     * @return true 如果存在且是目录
     */
    public static boolean directoryExists(String dirPath) {
        Path path = Paths.get(dirPath);
        return Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * 获取文件基本信息（大小、最后修改时间、是否可执行等）
     *
     * @param filePath 文件路径
     * @return 包含信息的 Map
     * @throws IOException 如果读取属性失败
     */
    public static Map<String, Object> getFileInfo(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.notExists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("path", path.toAbsolutePath().toString());
        info.put("isFile", Files.isRegularFile(path));
        info.put("isDirectory", Files.isDirectory(path));
        info.put("size", Files.size(path));
        info.put("lastModified", Files.getLastModifiedTime(path).toMillis());
        info.put("isReadable", Files.isReadable(path));
        info.put("isWritable", Files.isWritable(path));
        info.put("isExecutable", Files.isExecutable(path));
        return info;
    }

    /**
     * 将字符串写入文件（指定编码）
     *
     * @param content  要写入的内容
     * @param charset  字符编码（如 "UTF-8"）
     * @param filePath 文件路径
     * @throws IOException 如果写入失败
     */
    public static void writeStringToFile(String content, String charset, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null && Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        //Files.writeString(path, content, StandardCharsets.UTF_8); // 实际使用 charset 需要转换
        // 注：Files.writeString 只支持标准字符集，如果 charset 不是 UTF-8，需手动处理
        // 这里简化，直接使用 Files.write 支持 Charset
        Files.write(path, content.getBytes(Charset.forName(charset)));
    }

    /**
     * 计算文件的 MD5 哈希值
     * @param filePath 文件路径
     * @return 32 位小写 MD5 字符串
     * @throws IOException              如果文件读取失败
     * @throws NoSuchAlgorithmException 如果 MD5 算法不可用（通常不会）
     */
    public static String calculateMD5(String filePath) throws IOException, NoSuchAlgorithmException {
        Path path = Paths.get(filePath);
        if (Files.notExists(path) || Files.isDirectory(path)) {
            throw new IOException("文件不存在或是一个目录: " + filePath);
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        }
        byte[] digest = md.digest();
        // 转换为 16 进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }


    /**
     * 列出目录下所有文件（不递归）
     *
     * @param dirPath 目录路径
     * @return 文件名列表
     * @throws IOException 如果读取目录失败
     */
    public static List<String> listFiles(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("目录不存在或不是一个目录: " + dirPath);
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        }
    }

    /**
     * 递归遍历目录下所有文件（返回相对路径列表）
     *
     * @param dirPath 目录路径
     * @return 文件相对路径列表（相对于 dirPath）
     * @throws IOException 如果遍历失败
     */
    public static List<String> listAllFilesRecursively(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("目录不存在或不是一个目录: " + dirPath);
        }
        List<String> result = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relative = dir.relativize(file).toString().replace('\\', '/');
                result.add(relative);
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    /**
     * 在目录中查找匹配特定扩展名的文件（递归）
     *
     * @param dirPath     目录路径
     * @param extension   扩展名（如 ".txt"），不区分大小写
     * @return 匹配文件的路径列表
     * @throws IOException 如果遍历失败
     */
    public static List<String> findFilesByExtension(String dirPath, String extension) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("目录不存在或不是一个目录: " + dirPath);
        }
        String ext = extension.toLowerCase();
        List<String> result = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().toLowerCase().endsWith(ext)) {
                    result.add(dir.relativize(file).toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    /**
     * 批量重命名图片文件：将目录下所有 .jpg 文件（不区分大小写）重命名为 .png，同时移除文件名中的 "-"
     * 例如 "abc-123.jpg" -> "abc123.png"
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录（可与源目录相同）
     * @throws IOException 如果处理失败
     */
    public static void convertJpgToPngAndRemoveDash(String sourceDir, String targetDir) throws IOException {
        Path source = Paths.get(sourceDir);
        Path target = Paths.get(targetDir);
        if (Files.notExists(source) || !Files.isDirectory(source)) {
            throw new IOException("源目录不存在或不是目录: " + sourceDir);
        }
        if (!target.equals(source) && Files.notExists(target)) {
            Files.createDirectories(target);
        }
        try (Stream<Path> stream = Files.list(source)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jpg"))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        // 移除所有 "-"
                        String base = name.substring(0, name.length() - 4).replace("-", "");
                        String newName = base + ".png";
                        Path targetPath = target.resolve(newName);
                        try {
                            Files.move(p, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("已重命名: " + name + " -> " + newName);
                        } catch (IOException e) {
                            System.err.println("重命名失败: " + name + " -> " + newName + " : " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * 获取文件扩展名（包含点，如 ".txt"）
     *
     * @param fileName 文件名
     * @return 扩展名，如果没有则返回空字符串
     */
    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    /**
     * 获取不带扩展名的文件名
     *
     * @param fileName 文件名
     * @return 主文件名
     */
    public static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    /**
     * 生成一个临时文件名（不创建文件）
     *
     * @param prefix 前缀
     * @param suffix 后缀（如 ".tmp"）
     * @return 临时文件路径字符串
     */
    public static String generateTempFileName(String prefix, String suffix) {
        return prefix + UUID.randomUUID() + suffix;
    }


    public static void openFiles(List<String> paths) {
        // 检查当前平台是否支持 Desktop API
        if (!Desktop.isDesktopSupported()) {
            System.err.println("当前系统不支持 Desktop 操作，无法打开文件");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        for (String path : paths) {
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("文件不存在，跳过: " + path);
                continue;
            }

            try {
                String lowerPath = path.toLowerCase();
                if (lowerPath.endsWith(".txt")) {
                    // 使用关联编辑器打开文本文件（通常为记事本）
                    desktop.edit(file);
                    System.out.println("已打开编辑器: " + path);
                } else if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx") ||
                        lowerPath.endsWith(".mp3") || lowerPath.endsWith(".wav")) {
                    // 使用系统默认程序打开文件
                    desktop.open(file);
                    System.out.println("已打开文件: " + path);
                } else {
                    // 其他类型：如果是目录则打开文件夹，如果是文件则用默认程序打开
                    if (file.isDirectory()) {
                        desktop.open(file); // 打开文件夹
                    } else {
                        desktop.open(file); // 用默认程序打开文件
                    }
                    System.out.println("已打开: " + path);
                }
            } catch (IOException e) {
                System.err.println("打开失败: " + path + "，原因: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}