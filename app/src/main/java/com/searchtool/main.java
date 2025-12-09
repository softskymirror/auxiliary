package com.searchtool;

import java.util.*;

import com.commontool.*;

import net.sf.json.JSONObject;

import static com.system.CmdUtils.executive;
import static com.system.CmdUtils.getMsg;

public class main{
ArrayList<String> enemy_list=new ArrayList<>();
public static String str_regexp="\\";
    public static void main(String [] args){
//        System.out.println(ParamUtils.getHexInt("3cf3"));
//        try {
//            testStreamString();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        System.out.println(0+"mytime");
//        try {
//            FileUtils.writeFormattedFile("GitHub:https://github.com/softskymirror/", "UTF-8", "D:\\DOC\\ѧϰ����\\��������\\���������\\������Ӫ\\GitHub\\githubר�òֿ�.txt");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        //        System.out.println(ParamUtils.getHexInt(ParamUtils.getIntHex(463)));
//        List<String> lists=new ArrayList<>();
//        lists.add("commons-beanutils-1.7.0.jar");
//        lists.add("commons-compress-1.14.jar");
//        lists.add("commons-collections-3.2.1.jar");
//        lists.add("commons-lang-2.3.jar");
//        lists.add("commons-logging-1.0.4.jar");
//        lists.add("xbean-reflect-3.4.jar");
//        try {
//            checkFiles("D:\\programfiles\\programworkplace\\maven\\softskymirror\\lib\\",lists);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        List<String> lists=new ArrayList<>();
//        lists.add("commons-beanutils-1.7.0.jar");
//        lists.add("commons-compress-1.14.jar");
//        lists.add("commons-collections-3.2.1.jar");
//        lists.add("commons-lang-2.3.jar");
//        lists.add("commons-logging-1.0.4.jar");
//        lists.add("ezmorph-1.0.3.jar");
//        lists.add("json-lib-2.1-jdk15.jar");
//        try {
//            copyListFiles ("D:\\programfiles\\programworkplace\\maven\\softskymirror\\lib\\",lists,"D:\\programfiles\\programworkplace\\maven\\softskymirror\\WebContent\\WEB-INF\\lib\\");
//        }catch (Exception e){
//            e.printStackTrace();
//        }

//        List<String> lists=new ArrayList<>();
//        lists.add("commons-collections-3.2.jar");
//        try {
//            deleteFiles("D:\\programfiles\\programworkplace\\maven\\softskymirror\\lib\\",lists);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        List<HashMap<String,String>> map_lists=new ArrayList<>();
//        HashMap<String,String> map=new HashMap<>();
//        map.put("old","queryChart.sql.txt");
//        map.put("new","queryChart.sql");
//        map_lists.add(map);
//        try {
//          renameFiles("C:\\Users\\HPHRNSTUDY\\Desktop\\",map_lists);
//        }catch (Exception e){
//          e.printStackTrace();
//        }
        String text="<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <script>\n" +
                "     function autoWakeUp(){\n" +
                "     var info = navigator.userAgent;\n" +
                "         var isPhone = /mobile/i.test(info);\n" +
                "         //���������Mobile�������ֻ��豸���򷵻�true\n" +
                "          if(!isPhone){\n" +
                "              alert('pc')\n" +
                "              window.open('tencent://AddContact/?fromId=50&fromSubId=1&subcmd=all&uin=845321437')\n" +
                "           }else{\n" +
                "              alert('phone')\n" +
                "              window.open('mqqwpa://im/chat?chat_type=wpa&uin=845321437&version=1&src_type=web&web_src=oicqzone.com')\n" +
                "}\n" +
                "}\n" +
                "</script>\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<!-- app�� -->\n" +
                "<a href=\"javascript:void(0)\" onclick=\"autoWakeUp()\" >����˽�����</a>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                "</a>\n" +
                "</body>\n" +
                "</html>";
        try {
            FileUtils.writeFormattedFile(text, "UTF-8", "C:\\Users\\HPHRNSTUDY\\Desktop\\test.html");
            System.out.println("�ļ������ɹ���");
        }catch (Exception e){
            e.printStackTrace();
        }
//        try {
//            executeSQLCommand("C:\\Users\\HPHRNSTUDY\\Desktop\\SQLScript.sql");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        ArrayList<String> paths=new ArrayList<>();
////        paths.add("D:\\programfiles\\test");
//        try {
//            for (String str : getFileList("D:\\programfiles")) {
//                System.out.println(str+"\r\n");
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//      paths.add()
//        try {
//            deleteFileDirs(paths);
//            checkFileDirs(paths);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
        String file_path="D:\\mp3\\For family\\Count.mp3";
// try {
//  FileUtils.writeFormattedFile("GitHub�ֿ��ַ:https://github.com/softskymirror/", "UTF-8", file_path);
// }catch (Exception e){
//    e.printStackTrace();
//  }
//List<String> lists=new ArrayList<>();
//lists.add(file_path);
//openFiles(lists);
//        try{
//            FileUtils.changeMD5("D:\\mp4","gay (1).mp4","gay1.mp4");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
}


    /**
     * ��Ƶת��ʽ
     * @param videopath
     * @param audiopath
     * @param outputpath
     * @throws Exception
     */
    public static void mixVideo(String videopath,String audiopath,String outputpath) throws Exception {
    String command = "start ffmpeg -i " + videopath + " -i " + audiopath + " -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 -vcodec mpeg4 -s 1600x720 -f mp4 -y " + outputpath;
    String command2 = "TASKLIST /FI \"IMAGENAME eq ffmpeg.exe\"";
    String command3 = "pause";
     //executive("cmd", command2);
    executive("cmd", command);
    executive("cmd", command2);
}


    /**
     *��ָ��Ŀ¼��������Ŀ¼��ȡָ���ļ�������(����)·��
     * @return
     */
  public static String getSelectedFileFromDirectory(String dir_path,String file_name) throws Exception{
      String command = "dir /b /d /s " + dir_path +file_name;
//          System.out.println(command);
      executive("cmd", command);
      return getMsg().split("\r\n")[0];
  }

    /**
     * ��ȡָ���ļ��е��������ļ���
     * @param file_dir
     * @return
     */
  public static ArrayList<String> getFileList(String file_dir) throws Exception{
      ArrayList<String> lists=new ArrayList<>();
      String command = "dir /b " + file_dir;
      executive("cmd", command);
      for(String str:getMsg().split("\r\n")){
          lists.add(str);
      }
      return lists;
  }
    /**
     * ��������ָ���ļ�
     * @param dir_path
     * @param files
     * @param output_path
     * @throws Exception
     */
    public static void copyListFiles(String dir_path,List<String> files,String output_path) throws Exception{
      for(String file:files){
          String command_copy="copy "+  getSelectedFileFromDirectory(dir_path,file)+" "+output_path+"\\"+file;
//        System.out.println(command_copy);
          executive("cmd", command_copy);
          System.out.println(file+":"+"�Ѹ��Ƶ�"+ output_path);
      }
}

    /**
     * �Զ�ִ��SQL Command�ű�
     * @param file_path
     */
    public static void executeSQLCommand(String file_path) throws Exception{
        String command = " start mysql -u root -p >" + file_path;
        System.out.println(command);
        executive("cmd", command);
        System.out.println("��ִ��SQL�ű�");
}

    /**
     * ��������ļ��Ƿ����
     */
    public static void checkFiles(String file_path,List<String> lists) {
        for (String list:lists) {
            try {
                if(FileUtils.checkFile((file_path.endsWith("\\") ? file_path : file_path + "\\") +list)){
                    System.out.println("�ļ�����");
                }else{
                    System.out.println("�ļ�������");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
//        try {
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }

    /**
     * ����ļ���Ŀ¼�Ƿ����
     * @param file_dirs
     */
    public static void checkFileDirs(ArrayList<String> file_dirs){
       for(String file_dir:file_dirs){
           if(FileUtils.checkFileDir((file_dir.endsWith("\\") ? file_dir : file_dir + "\\"))) {
               System.out.println(file_dir+"�ļ��д���");
           }else{
               System.out.println(file_dir+"�ļ��в�����");
           }
       }
}
    /**
     * ����ɾ���ļ�
     */
    public static void deleteFiles(String file_path,List<String> lists) throws Exception{
        for(String list:lists) {
            try {
                FileUtils.deleteFile((file_path.endsWith("\\")?file_path:file_path+"\\")+list);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
//        try {
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    /**
     * ����ɾ���ļ���
     */
    public static void deleteFileDirs(ArrayList<String> file_path) throws Exception{
        for(String path:file_path) {
            //���󲿷�"\\"ע��Ϊ��ʽĿ¼
            path=(path.endsWith("\\")?path.substring(0,path.length() -str_regexp.length()): path);
            if(FileUtils.checkFileDir(path)){
                FileUtils.deleteFileDir(path);
                System.out.println("ɾ��"+path+"�ļ��гɹ�");
            }
        }
    }
//        try {
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }

    /**
     * �����������ļ��Ƿ����
     */
    public static void renameFiles(String file_path,List<HashMap<String,String>>lists){
        for(HashMap<String,String> map:lists) {
            try {
                FileUtils.renameFile((file_path.endsWith("\\")?file_path:file_path+"\\"),map.get("old"),map.get("new"));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
//        try {
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    /**
     * drawable�ļ����ļ����Ϸ�������
     */
    public void renameImgName() {
    String sourcePath = "C:\\Users\\HPHRNSTUDY\\AndroidStudioProjects\\FamilyManager\\app\\src\\main\\res\\drawable\\loading\\��Բ 1 ����-FMAssistant";
    String targetPath = "C:\\Users\\HPHRNSTUDY\\AndroidStudioProjects\\FamilyManager\\app\\src\\main\\res\\drawable\\loading\\��Բ 1 ����-FMAssistant";
    //System.out.println(sourcePath.endsWith("\\")?sourcePath:sourcePath+"\\");
    FileUtils.checkImageDir("��Բ 1 ����-FMAssistant_", "loaddingstate");
}


    /**
     * ������ָ��Ŀ¼���ļ������
     * @param
     */
    public static void openFiles(List<String> lists){
        String command = null;
    for(String path:lists) {
        try {
            if(path.endsWith(".txt")){
                command="notepad "+path;
            }else if(path.endsWith(".doc")||path.endsWith(".docx")){
                command=getSelectedFileFromDirectory("C:\\Program Files\\","ksolaunch.exe ")+path;
            }else if(path.endsWith(".mp3")||path.endsWith(".wav")){
                command=path;
            }else{
                command="explorer "+path;
            }
            executive("cmd", command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }


    /**
     * Json��HashMap���������໥ת��
     */
public void testHashMap(){
    new ParamResearcher();
        JsonUtils jsonUtils=new JsonUtils();
        ArrayList<HashMap<String,Object>> map_list=new ArrayList<>();
        HashMap<String,Object> info=new HashMap<>();
        info.put("��޷�����","����ְҵ����ѧԺ");
        info.put("��������","�����");
        for(int i=0;i<3;i++){
            HashMap <String,Object> map=new HashMap<>();
            switch(i){
       case 0:
        map.put("name", "������");
        map.put("BeRevenged_reason","��Ե�޹ʼ��˾�׷");
        map.put("Revenge_way","ÿ�շ�׷�ӳ���");
        map_list.add(map);
        break;
        case 1:
        map.put("name", "֣��ΰ");
        map.put("BeRevenged_reason","ǿ�Ʒ�������ᡢ�򿨡��������缰��Ϣ�Ǽǣ�Ӱ������ѧϰ����");
        map.put("Revenge_way","���ɽ�ѧ�ѡ�������ᡢ�Ͼ���ϵ�������񲡲�ͬ���ھ�");
        map_list.add(map);
        break;
        case 2:
        map.put("name", "���ٱ�");
        map.put("BeRevenged_reason","��ʽ������߹���������");
        map.put("Revenge_way","���������ţ�����ָ�������Ϊ�ϼ�Ҫ�����ѧϰ����");
        map_list.add(map);
        break;
    }
    }

    JSONObject jsonObject=JsonUtils.generateJson(info,"enemy",map_list);
    HashMap <String,Object> map=JsonUtils.parseJSONOject(jsonObject.toString().getBytes());
    Set<String> key=map.keySet();
    for(Iterator iterator =key.iterator(); iterator.hasNext();) {
        String k=(String)iterator.next();
       System.out.println(k+":"+map.get(k));
   }
     ArrayListTest m=new ArrayListTest();
     m.HashMapTestIterator();
       /*  String path="D:\\xml\\";
        String filename="test.xml";
        String rootElement="animation-list";
        String nodeElement="item";
        String [] attributes={"android:drawable","android:duration"};
        int totalcount=56;
        String [][] infos=new String[totalcount][attributes.length];
        for(int i=0;i<totalcount;i++){
      for(int j=0;j<attributes.length;j++){
 switch (j) {
    case 0:
    infos[i][j]="@drawable/loaddingstate"+i;
     break;
    case 1:
    infos[i][j]="60";
    default:
        break;
 }
}
        }
        try {
            XMLBuilder.generateXml(FileUtils.searchDirectory(path,filename), rootElement, nodeElement, attributes, infos);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }*/
}
}