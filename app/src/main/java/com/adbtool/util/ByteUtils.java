package com.adbtool.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ByteUtils {
  public static final String key="abcde5hgfthgfrg3";
  private static final String ALGORITHMSTR="AES/ECB/PKCS5Padding";
  public static final String filepath="C:\\Program Files\\HtmlWatcher\\temp\\media\\resposeText.bin";
  static byte [] bytes=null;



    public static byte[] filetoBytes(String file_path) throws Exception{
        byte [] bytes=new byte[1024];
        byte [] total_bytes=new byte[0];
        FileInputStream sources=new FileInputStream(file_path);
        while (sources.read(bytes, 0, 1024) != -1) {
            total_bytes=ByteUtils.byteMerger(total_bytes,bytes);
        }
        return total_bytes;
    }


    public static void bytesToFile(byte[] bytes, String filepath){
    try{
    FileOutputStream fos=new FileOutputStream(new File(filepath));
    fos.write(bytes);
}catch(Exception e){
    e.printStackTrace();
}
  }



  /*public static int testZipInputStream() throws IOException, Exception{
    int i=filetoBytes(filepath).read();
     return (filetoBytes(filepath).read() << 8) | i;
  }*/

  public static void md5(String dateString) throws Exception{
    MessageDigest md5=null;
    byte[] digest=MessageDigest.getInstance("md5").digest(dateString.getBytes());
    String md5code=new BigInteger(1,digest).toString(16);

  }
  
  public static String aesDecryptByBytes(byte[] encryptBytes,String decryptKey) throws Exception{
    KeyGenerator kgen=KeyGenerator.getInstance("AES");
    kgen.init(128);
    Cipher cipher=Cipher.getInstance(ALGORITHMSTR);
    cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(decryptKey.getBytes(), "AES"));
    byte[] decryptBytes=cipher.doFinal(encryptBytes);
    return new String(decryptBytes);
  }
 public static byte[] base64Decoder(String base64Code) throws Exception{
  return Base64.decodeBase64(base64Code.getBytes());
 }
 public static byte[] base64Encoder(String base64Code) throws Exception{
  return Base64.encodeBase64(base64Code.getBytes());
 }
  public static String aesDecrypt(String encryptStr,String decryptKey) throws Exception{
    return aesDecryptByBytes(base64Decoder(encryptStr),decryptKey);
  }
 
  public static String string2MD5(String inStr){
    MessageDigest md5 = null;
    try{
    md5 = MessageDigest.getInstance("MD5");
    }catch (Exception e){
    System.out.println(e.toString());
    e.printStackTrace();
    return "";
    }
    char[] charArray = inStr.toCharArray();
    byte[] byteArray = new byte[charArray.length];
 
    for (int i = 0; i < charArray.length; i++)
    byteArray[i] = (byte) charArray[i];
    byte[] md5Bytes = md5.digest(byteArray);
    StringBuffer hexValue = new StringBuffer();
    for (int i = 0; i < md5Bytes.length; i++){
    int val = ((int) md5Bytes[i]) & 0xff;
    if (val < 16)
      hexValue.append("0");
    hexValue.append(Integer.toHexString(val));
    }
    return hexValue.toString();
 
  }
 
  /**
   * ���ܽ����㷨 ִ��һ�μ��ܣ����ν���
   */
  public static String convertMD5(String inStr){
 
    char[] a = inStr.toCharArray();
    for (int i = 0; i < a.length; i++){
    a[i] = (char) (a[i] ^ 't');
    }
    String s = new String(a);
    return s;
 
  }
/*
 �����ַ���ͨ��GZIPѹ������String����
 */
public static String toHexString(String s){
  String str="";
  for(int i=0;i<s.length();i++){
    int ch=(int)s.charAt(i);
    String s4=Integer.toHexString(ch);
    str=str+s4;
  }
  return "0x"+str;
}

public static int byte2Int(byte[] res){
  int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) // | ��ʾ��λ�� 
  | ((res[2] << 24) >>> 8) | (res[3] << 24); 
  return targets;
}

public static byte[] int2byte(int res) {  
    byte[] targets = new byte[4];  
      
    targets[0] = (byte) (res & 0xff);
    targets[1] = (byte) ((res >> 8) & 0xff);// �ε�λ   
    targets[2] = (byte) ((res >> 16) & 0xff);// �θ�λ   
    targets[3] = (byte) (res >>> 24);
    return targets;   
    }
    
 
    private static String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        String hv;
        for (int i = 0; i < src.length; i++) {
            // ��ʮ�����ƣ����� 16���޷���������ʽ����һ�������������ַ�����ʾ��ʽ����ת��Ϊ��д
            hv = Integer.toHexString(src[i] & 0xFF).toUpperCase();
            if (hv.length() < 2) {
                builder.append(0);
            }
            builder.append(hv);
        }

        return builder.toString();
    }

    /**
     * ��Hex Stringת��ΪByte����
     *
     * @param hexString the hex string
     * @return the byte [ ]
     */
    public static byte[] hexStringToBytes(String hexString) {
      
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() >> 1];
        int index = 0;
        for (int i = 0; i < hexString.length(); i++) {
            if (index  > hexString.length() - 1) {
                return byteArray;
            }
            byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
            byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
            byteArray[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        return byteArray;
    }
public String toStringHex(String s){
  if("0x".equals(s.substring(0,2))){
    s=s.substring(2);
  }
  byte[] baKeyword=new byte[s.length()/2];
  for(int i=0;i<baKeyword.length;i++){
    try{
    baKeyword[i]=(byte)(0xff&Integer.parseInt(s.substring(i*2,i*2+2),16));
    }catch(Exception e){
    e.printStackTrace();
    }
  }
  try{
    s=new String(baKeyword,"utf-8");
  }catch(Exception e){
    e.printStackTrace();
  }
  return s;
}

//int tag=strstr(Base64.decodeBase64(str.getBytes()),b,0,base64.getBytes().length);
public static String str2HexStr(String str){
  char [] chars="0123456789ABCDEF".toCharArray();
  StringBuilder sb=new StringBuilder("");
  byte [] bs=str.getBytes();
  int bit;
  for(int i=0;i<bs.length;i++){
  bit=(bs[i]&0x0f0)>>4;
  sb.append(""+chars[bit]);
  bit=bs[i]&0x0f;
  sb.append(chars[bit]);
}
return sb.toString().trim();
}
public static void printHexString(byte [] b){
  for(int i=0;i<b.length;i++){
    String hex=Integer.toHexString(b[i]&0xFF);
    if(hex.length()==1){
    hex='0'+hex;
    }
    System.out.println(hex.toUpperCase());
  }
}

  public String gZip(String input) throws UnsupportedEncodingException {
    byte[] bytes = null;
    GZIPOutputStream gzip = null;
    ByteArrayOutputStream bos = null;
    try {
    bos = new ByteArrayOutputStream();
    gzip = new GZIPOutputStream(bos);
    gzip.write(input.getBytes("ISO-8859-1"));
    gzip.finish();
    gzip.close();
    bytes = bos.toByteArray();
    bos.close();
    } catch (Exception e) {
    e.printStackTrace();
    } finally {
    try {
      if (gzip != null)
        gzip.close();
      if (bos != null)
        bos.close();
    } catch (final IOException ioe) {
      ioe.printStackTrace();
      //log.error("ѹ������", ioe);
    }
    }
    return new String(Base64.encodeBase64(bytes),"ISO-8859-1");//��ֱ�ӷ���ֱ��δ��Base64���ܵ��ı��ֽڽ���������
  }

/*
 �����ַ���ͨ��GZIPѹ������String����
 */
  public String unGZip(String base64unZipInput) {
    byte[] bytes;
    String out="";
    GZIPInputStream gzip = null;
    ByteArrayInputStream bis;
    ByteArrayOutputStream bos = null;
    try {
    bis = new ByteArrayInputStream(base64unZipInput.getBytes());
    gzip = new GZIPInputStream(bis);
    byte[] buf = new byte[1024];
    int num;
    bos = new ByteArrayOutputStream();
    while ((num = gzip.read(buf, 0, buf.length)) != -1) {
      bos.write(buf, 0, num);
    }
    bytes = bos.toByteArray();
    out = new String(bytes, "UTF-8");
    gzip.close();
    bis.close();
    bos.flush();
    bos.close();
    } catch (Exception e){
    e.printStackTrace();
    JOptionPane.showMessageDialog(null, "��ѹ����");
    } finally {
    try {
      if (gzip != null)
        gzip.close();
      if (bos != null)
        bos.close();
        
    } catch (final IOException ioe) {
      ioe.printStackTrace();
    }
    }
    return out;
  }

  
  public static int strstr(byte[] str1, byte[] str2, int start, int end)
  {
      int index1 = start;
      int index2 = 0;
      if(str2!=null)
      {
          while(index1<str1.length && index1<end)
          {
              int dsite = 0;
              while(str1[index1+dsite]==str2[index2+dsite]) {
                  if(index2+dsite+1>=str2.length)
                      return index1;
                  dsite++;
                  if(index1+dsite>=str1.length || index2+dsite>=str2.length)
                      break;
              }
              index1++;
          }
          return -1;
      }
      else
          return index1;
  }

  public static byte[] subByte(byte[] b,int off,int length){
    byte[] b1 = new byte[length];
    System.arraycopy(b, off, b1, 0, length);
    return b1;
}

 public static byte[] getGZipAreaBytes(byte []responseText){
  int begintag=strstr(responseText,new byte[]{0x1F,(byte) 0x8B},0,responseText.length);
  return subByte(responseText,begintag,responseText.length-begintag);
 } 

public static String getHexColumns(int columnCount){
  StringBuffer sb=new StringBuffer();
  int initize=0x0000;
  for(int j=0;j<columnCount;j++){
   String hexString=Integer.toHexString(initize+18*j);
  if(hexString.length()==1){
   hexString="000"+hexString;
  }else if(hexString.length()==2){
    hexString="00"+hexString;
  }else if(hexString.length()==3){
    hexString="0"+hexString;
  }
  hexString="0x"+hexString+"\r\n";
  sb.append(hexString.toUpperCase());
 }
 return sb.toString();
 }

  public static byte[] getStringBytes(byte[] byteArray, String charsetName) throws IOException{
      return toByteArray(new String(byteArray, charsetName));
  }

 public static byte[] toByteArray(Object obj) {
  byte[] bytes = null;
  ByteArrayOutputStream bos = new ByteArrayOutputStream();
  try {
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(obj);
      oos.flush();
      bytes = bos.toByteArray ();
      oos.close();
      bos.close();
  } catch (IOException ex) {
      ex.printStackTrace();
  }
  return bytes;
}   


 


/**  


* ����ת����  


* @param bytes  


* @return  


*/  
 public static String getByteHex(byte [] bytes) throws Exception{
  StringBuffer sb=new StringBuffer();
  String hex=null;
  for(int i=0;i<bytes.length;i++){
    hex=Integer.toHexString(bytes[i]);
    if(hex.length()<=1){
      hex="0"+hex;
     }
     hex=hex+" ";
     sb.append(hex.toUpperCase());
}
return sb.toString();
 }

public byte [] getHexBytes(String hexString) throws Exception{
byte[] result=new byte[]{};
String [] hexs=hexString.split(" ");
for(int i=0;i<hexs.length;i++){
  result=byteMerger(result,hexStringToBytes(hexs[i]));
  };
  //for(int i=0;i<result.toArray().length;i++)
  return result;
}

public static byte[] byteMerger(byte[] bt1, byte[] bt2){ 
    byte[] bt3 = new byte[bt1.length+bt2.length]; 
  int i=0;
    for(byte bt: bt1){
     bt3[i]=bt;
   i++;
  }
  for(byte bt: bt2){
   bt3[i]=bt;
   i++;
  }
    return bt3; 
  }


 public static String getByteString(byte [] bytes) throws Exception{
    return new String(bytes);
 }

}
