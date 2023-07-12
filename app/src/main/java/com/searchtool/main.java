package com.searchtool;

import java.lang.reflect.Parameter;
import java.util.*;

import com.commontool.*;

import net.sf.json.JSONObject;

import static com.searchtool.CmdUtils.executive;
import static com.searchtool.CmdUtils.getMsg;
import static com.searchtool.ParamUtils.testStreamString;

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
//            FileUtils.writeFormattedFile("GitHub仓库地址:https://github.com/softskymirror/", "UTF-8", "D:\\DOC\\学习资料\\个人资料\\计算机代码\\开发运营\\GitHub\\github专用仓库.txt");
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
                "         //如果包含“Mobile”（是手机设备）则返回true\n" +
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
                "<!-- app端 -->\n" +
                "<a href=\"javascript:void(0)\" onclick=\"autoWakeUp()\" >点击了解详情</a>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                "</a>\n" +
                "</body>\n" +
                "</html>";
        try {
            FileUtils.writeFormattedFile(text, "UTF-8", "C:\\Users\\HPHRNSTUDY\\Desktop\\test.html");
            System.out.println("文件创建成功！");
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
//  FileUtils.writeFormattedFile("GitHub仓库地址:https://github.com/softskymirror/", "UTF-8", file_path);
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
     * 视频转格式
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
     *从指定目录查找其子目录获取指定文件的完整(绝对)路径
     * @return
     */
  public static String getSelectedFileFromDirectory(String dir_path,String file_name) throws Exception{
      String command = "dir /b /d /s " + dir_path +file_name;
//          System.out.println(command);
      executive("cmd", command);
      return getMsg().split("\r\n")[0];
  }

    /**
     * 获取指定文件夹的所有子文件夹
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
     * 批量复制指定文件
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
          System.out.println(file+":"+"已复制到"+ output_path);
      }
}

    /**
     * 自动执行SQL Command脚本
     * @param file_path
     */
    public static void executeSQLCommand(String file_path) throws Exception{
        String command = " start mysql -u root -p >" + file_path;
        System.out.println(command);
        executive("cmd", command);
        System.out.println("已执行SQL脚本");
}

    /**
     * 批量检查文件是否存在
     */
    public static void checkFiles(String file_path,List<String> lists) {
        for (String list:lists) {
            try {
                if(FileUtils.checkFile((file_path.endsWith("\\") ? file_path : file_path + "\\") +list)){
                    System.out.println("文件存在");
                }else{
                    System.out.println("文件不存在");
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
     * 检查文件夹目录是否存在
     * @param file_dirs
     */
    public static void checkFileDirs(ArrayList<String> file_dirs){
       for(String file_dir:file_dirs){
           if(FileUtils.checkFileDir((file_dir.endsWith("\\") ? file_dir : file_dir + "\\"))) {
               System.out.println(file_dir+"文件夹存在");
           }else{
               System.out.println(file_dir+"文件夹不存在");
           }
       }
}
    /**
     * 批量删除文件
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
     * 批量删除文件夹
     */
    public static void deleteFileDirs(ArrayList<String> file_path) throws Exception{
        for(String path:file_path) {
            //将后部分"\\"注释为正式目录
            path=(path.endsWith("\\")?path.substring(0,path.length() -str_regexp.length()): path);
            if(FileUtils.checkFileDir(path)){
                FileUtils.deleteFileDir(path);
                System.out.println("删除"+path+"文件夹成功");
            }
        }
    }
//        try {
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }

    /**
     * 批量重命名文件是否存在
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
     * drawable文件夹文件名合法化命名
     */
    public void renameImgName() {
    String sourcePath = "C:\\Users\\HPHRNSTUDY\\AndroidStudioProjects\\FamilyManager\\app\\src\\main\\res\\drawable\\loading\\椭圆 1 拷贝-FMAssistant";
    String targetPath = "C:\\Users\\HPHRNSTUDY\\AndroidStudioProjects\\FamilyManager\\app\\src\\main\\res\\drawable\\loading\\椭圆 1 拷贝-FMAssistant";
    //System.out.println(sourcePath.endsWith("\\")?sourcePath:sourcePath+"\\");
    FileUtils.checkImageDir("椭圆 1 拷贝-FMAssistant_", "loaddingstate");
}


    /**
     * 批量打开指定目录的文件浏览器
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
     * Json与HashMap数据类型相互转换
     */
public void testHashMap(){
    new ParamResearcher();
        JsonUtils jsonUtils=new JsonUtils();
        ArrayList<HashMap<String,Object>> map_list=new ArrayList<>();
        HashMap<String,Object> info=new HashMap<>();
        info.put("仇恨发生地","揭阳职业技术学院");
        info.put("被得罪人","黄锐楠");
        for(int i=0;i<3;i++){
            HashMap <String,Object> map=new HashMap<>();
            switch(i){
       case 0:
        map.put("name", "李永海");
        map.put("BeRevenged_reason","无缘无故见人就追");
        map.put("Revenge_way","每日反追加仇视");
        map_list.add(map);
        break;
        case 1:
        map.put("name", "郑博伟");
        map.put("BeRevenged_reason","强制反复测核酸、打卡、接种疫苗及信息登记，影响正常学习生活");
        map.put("Revenge_way","不缴交学费、不测核酸、断绝联系、发精神病并同归于尽");
        map_list.add(map);
        break;
        case 2:
        map.put("name", "钟荣标");
        map.put("BeRevenged_reason","形式主义的走狗，误导他人");
        map.put("Revenge_way","不轻易相信，当面指责其过错、为上级要求干扰学习生活");
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