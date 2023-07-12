package com.searchtool;

import java.io.*;
import java.util.Scanner;

public class ParamUtils {
    /**
     * 十六进制转为十进制(int)
     */
    public static int getHexInt(String hex){
        //判断hex单字节符长度不能小于1
        String new_hex=hex.length()<2?0+hex:hex;
        //转换为十六进制格式
        String s="0x"+new_hex;
        if(s.length()<4){
            s = 0 + s;
            System.out.println(s);
        }
        //判断字符串是否为偶数或是为奇数
//        if(s.length()%2==0)
//            System.out.println("字符串为偶数");
//        else {
//            System.out.println("字符串为奇数");
//            s="0x"+s;
//        }
//        if (s.length() < 4) {
//            System.out.println("字符串长度小于4");
//            s = "0x" + s;
//        }
        int i=Integer.decode(s);
        return i;
    }

    /**
     * 十进制（int)转为十六进制
     */
    public static String getIntHex(int integar){
      return Integer.toHexString(integar);
    }


    /**
     * 获取字节流换行后的字符串
     */
    public static void testStreamString() throws Exception{
        int n=0;
        String m=null;
        BufferedReader reader =new BufferedReader(new InputStreamReader(System.in)) ;	// 将字节流变为字符流// 声明对象
        BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(System.out));
        System.out.print("请输入内容：") ;
//      Scanner scanner=new Scanner(in);
        String str =null;	// 接收输入内容
        int a = 65;
        char b = '2';
        String c = "this is a good time for me to invent,Don't you think it?";
//        writer.write(a);
//        writer.write("\n");
//        writer.write(a + "\n");  // 使用 + 号拼接个字符串 使参数整体为一个字符串
//        writer.write(Integer.toString(a)); // 输出a的字符串形式
//        writer.write("\n");
        writer.write(c);
        writer.write("\n");
        try{
//            n = reader.read() ;	// 读取一行数据
            m = reader.readLine() ;	// 读取一行数据
        }catch(IOException e){
            e.printStackTrace() ;	// 输出信息
        }
//        System.out.println("输入的内容为：" +n) ;
        System.out.println("输入的内容为：" +m) ;
        writer.flush();
    }
}
