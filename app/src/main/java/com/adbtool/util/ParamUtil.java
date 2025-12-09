package com.adbtool.util;

import java.io.*;

public class ParamUtil {
    /**
     *
     */
    public static int getHexInt(String hex){
        //�ж�hex���ֽڷ����Ȳ���С��1
        String new_hex=hex.length()<2?0+hex:hex;
        //ת��Ϊʮ�����Ƹ�ʽ
        String s="0x"+new_hex;
        if(s.length()<4){
            s = 0 + s;
            System.out.println(s);
        }
        //�ж��ַ����Ƿ�Ϊż������Ϊ����
//        if(s.length()%2==0)
//            System.out.println("�ַ���Ϊż��");
//        else {
//            System.out.println("�ַ���Ϊ����");
//            s="0x"+s;
//        }
//        if (s.length() < 4) {
//            System.out.println("�ַ�������С��4");
//            s = "0x" + s;
//        }
        int i=Integer.decode(s);
        return i;
    }

    /**
     * ʮ���ƣ�int)תΪʮ������
     */
    public static String getIntHex(int integar){
      return Integer.toHexString(integar);
    }


    /**
     * ��ȡ�ֽ������к���ַ���
     */
    public static void testStreamString() throws Exception{
        int n=0;
        String m=null;
        BufferedReader reader =new BufferedReader(new InputStreamReader(System.in)) ;	// ���ֽ�����Ϊ�ַ���// ��������
        BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(System.out));
        System.out.print("���������ݣ�") ;
//      Scanner scanner=new Scanner(in);
        String str =null;	// ������������
        int a = 65;
        char b = '2';
        String c = "this is a good time for me to invent,Don't you think it?";
//        writer.write(a);
//        writer.write("\n");
//        writer.write(a + "\n");  // ʹ�� + ��ƴ�Ӹ��ַ��� ʹ��������Ϊһ���ַ���
//        writer.write(Integer.toString(a)); // ���a���ַ�����ʽ
//        writer.write("\n");
        writer.write(c);
        writer.write("\n");
        try{
//            n = reader.read() ;	// ��ȡһ������
            m = reader.readLine() ;	// ��ȡһ������
        }catch(IOException e){
            e.printStackTrace() ;	// �����Ϣ
        }
//        System.out.println("���������Ϊ��" +n) ;
        System.out.println("���������Ϊ��" +m) ;
        writer.flush();
    }
}
