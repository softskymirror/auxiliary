
package com.adbtool.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class Cmd {
    static String inStr;
    static ArrayList<String> runList = new ArrayList<String>();
    public static void executive(String cmd) throws Exception {
        Runtime runtime = Runtime.getRuntime(); //获取Runtime实例
        //执行命令
        try {
            String[] command = {"cmd","/c",cmd};
            Process process = runtime.exec(command);
            inStr = consumeInputStream(process.getInputStream());
            // 标准错误流（必须写在 waitFor 之前）
            String errStr = consumeInputStream(process.getErrorStream());
            //new ProcessClearStream(process.getInputStream(), "INFO").start();
            //new ProcessClearStream(process.getErrorStream(), "ERROR").start();
            int proc = process.waitFor();
            InputStream errorStream = process.getErrorStream(); //若有错误信息则输出
            if (proc == 0) {
                System.out.println("执行成功" + "\r\n" + inStr);
            } else {
                System.out.println("执行失败"+ errStr);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
}




  
