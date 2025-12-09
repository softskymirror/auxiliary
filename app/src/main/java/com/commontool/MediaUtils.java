package com.commontool;

import com.system.CmdUtils;

/**
 * 多媒体操作工具类
 */
public class MediaUtils {
    public static void mixVideo(String videopath,String audiopath,String outputpath) throws Exception{
        String command="start ffmpeg -i "+videopath+" -i "+audiopath+" -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 -vcodec mpeg4 -s 1200x720 -f mp4 -y "+outputpath;
        String command1="start ffmpeg -i "+videopath+" -f mp4 -s 1600x720 -y "+outputpath;
        String command2="TASKLIST /FI \"IMAGENAME eq ffmpeg.exe\"";
        String command3="pause";
        //executive("cmd", command2);
        //executive("cmd", command);
        //executive("cmd", command2);
        CmdUtils.executive("cmd", command);


    }

    public static void convert(String videopath,String audiopath, String outputpath){
        /*File files=new File("E:\\视频\\");
        File []listfiles=files.listFiles();
        for(File file:listfiles){
        String videopath=file.getPath();*/
        // String videopath="D:\\programfiles\\bilibili\\70595571\\c_122307801\\64\\video.m4s";
        // String audiopath="D:\\programfiles\\bilibili\\70595571\\c_122307801\\64\\audio.m4s";
        // String outputpath="D:\\mp4\\asmr-whisper-Three.mp4";

        try {
            mixVideo(videopath,audiopath,outputpath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
