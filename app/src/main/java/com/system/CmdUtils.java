
package com.system;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class CmdUtils {
  static String inStr;
  static ArrayList <String> runList=new ArrayList<String>();
public static void executive(String commandTool,String stmt) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime(); //
        //ִ������
        try {
          String []command={commandTool,"/c",stmt};
          Process process = runtime.exec(command);
          // ��׼������������д�� waitFor ֮ǰ��
          inStr = consumeInputStream(process.getInputStream());
          // ��׼������������д�� waitFor ֮ǰ��
          String errStr = consumeInputStream(process.getErrorStream());
          //new ProcessClearStream(process.getInputStream(), "INFO").start();
          //new ProcessClearStream(process.getErrorStream(), "ERROR").start();
          int proc = process.waitFor();
          //InputStream errorStream = process.getErrorStream(); //���д�����Ϣ�����
          if (proc == 0) {
            System.out.println("ִ"+"\r\n"+inStr);
          } else {
            System.out.println("" + errStr);
          }
        } catch (IOException | InterruptedException e) {
         e.printStackTrace();
        }finally{
            System.out.println(inStr);
        }
      }

    /**
     * getMsg
     * @return
     */
    public static String getMsg(){
          return inStr;
      }

      public static String consumeInputStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is,"GBK"));
        String s;

        StringBuilder sb = new StringBuilder();
        while ((s = br.readLine()) != null) {
         System.out.println(s);
          runList.add(s);
          sb.append(s);
        }
        return sb.toString();
      }
  



 

    public static void mixVideo(String videopath,String audiopath,String outputpath) throws Exception{
      String command="start ffmpeg -i "+videopath+" -i "+audiopath+" -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 -vcodec mpeg4 -s 1200x720 -f mp4 -y "+outputpath;
      String command1="start ffmpeg -i "+videopath+" -f mp4 -s 1600x720 -y "+outputpath;
      String command2="TASKLIST /FI \"IMAGENAME eq ffmpeg.exe\"";
      String command3="pause";
      //executive("cmd", command2);
      //executive("cmd", command);
      //executive("cmd", command2);
      executive("cmd", command);
  
     
    }

    public static void convert(String [] args){
        /*File files=new File("E:\\��Ƶ\\");
        File []listfiles=files.listFiles();
        for(File file:listfiles){
        String videopath=file.getPath();*/
        String videopath="D:\\programfiles\\bilibili\\70595571\\c_122307801\\64\\video.m4s";
        String audiopath="D:\\programfiles\\bilibili\\70595571\\c_122307801\\64\\audio.m4s";
        String outputpath="D:\\mp4\\asmr-whisper-Three.mp4";
   
        try {
          mixVideo(videopath,audiopath,outputpath);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    
}

  
