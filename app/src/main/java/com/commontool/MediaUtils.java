package com.commontool;

import com.system.CmdUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频处理工具类
 */
public class MediaUtils {

        private static final String FFMPEG_PATH = "ffmpeg"; // 假设 ffmpeg 在系统 PATH 中

        /**
         * 混合视频和音频（替换或添加音轨）
         *
         * @param videoPath  视频文件路径
         * @param audioPath  音频文件路径
         * @param outputPath 输出文件路径
         * @throws IOException          如果输入文件不存在或进程启动失败
         * @throws InterruptedException 如果等待进程中断
         */
        public static void mergeVideoAudio(String videoPath, String audioPath, String outputPath)
                throws IOException, InterruptedException {
            // 1. 检查输入文件是否存在
            Path video = Paths.get(videoPath);
            Path audio = Paths.get(audioPath);
            if (!Files.exists(video)) {
                throw new IOException("视频文件不存在: " + videoPath);
            }
            if (!Files.exists(audio)) {
                throw new IOException("音频文件不存在: " + audioPath);
            }

            // 2. 确保输出目录存在
            Path output = Paths.get(outputPath);
            if (output.getParent() != null && Files.notExists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }

            // 3. 构建 ffmpeg 命令（参考原参数）
            List<String> command = new ArrayList<>();
            command.add(FFMPEG_PATH);
            command.add("-i");
            command.add(videoPath);
            command.add("-i");
            command.add(audioPath);
            command.add("-c:v");      // 视频编码
            command.add("copy");       // 保持原视频编码（不重新编码）
            command.add("-c:a");       // 音频编码
            command.add("aac");
            command.add("-strict");
            command.add("experimental");
            command.add("-map");
            command.add("0:v:0");      // 取第一个输入的视频流
            command.add("-map");
            command.add("1:a:0");      // 取第二个输入的音频流
            command.add("-vcodec");    // 指定视频编码器（与 -c:v 重复，可保留一个）
            command.add("mpeg4");
            command.add("-s");         // 设置输出尺寸
            command.add("1600x720");
            command.add("-f");         // 强制格式
            command.add("mp4");
            command.add("-y");         // 覆盖输出文件
            command.add(outputPath);

            // 4. 启动进程
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // 合并错误流到标准输出
            Process process = pb.start();

            // 5. 异步读取输出（避免阻塞）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 可选：打印或记录日志
                    System.out.println("[ffmpeg] " + line);
                }
            }

            // 6. 等待进程结束
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("ffmpeg 执行失败，退出码: " + exitCode);
            }
            System.out.println("处理成功: " + outputPath);
        }

/**
 * 转换视频为mp4格式
 * @param videoPath  视频文件路径
 * @param outputPath 输出文件路径
 * @throws IOException          如果输入文件不存在或进程启动失败
 * @throws InterruptedException 如果等待进程中断
 */
public static void convertVideo(String videoPath, String outputPath)
        throws IOException, InterruptedException {
    // 构建 ffmpeg 命令（不使用 start）
    List<String> command = new ArrayList<>();
    command.add("ffmpeg");
    command.add("-i");
    command.add(videoPath);
    command.add("-f");
    command.add("mp4");
    command.add("-s");
    command.add("1600x720");
    command.add("-y");
    command.add(outputPath);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true); // 合并错误输出到标准输出
    Process process = pb.start();

    // 读取 ffmpeg 输出（可选）
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[ffmpeg] " + line);
        }
    }

    int exitCode = process.waitFor(); // 等待 ffmpeg 完成
    if (exitCode == 0) {
        System.out.println("转换成功");
    } else {
        System.out.println("转换失败，退出码：" + exitCode);
    }
}

}
