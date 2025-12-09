package com.adbtool.util;

//import com.commontool.ArrayListTest;
//import com.commontool.JsonUtils;
//import com.searchtool.ByteUtils;
//import com.system.Cmd;
//import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileUtil {
    public static File searchDirectory(String filepath,String filename) throws Exception{
        File path=new File((filepath.endsWith("\\")?filepath:filepath+"\\"));
        File file=new File((path.getPath().endsWith("\\"))?path.getPath():(path.getPath()+"\\")+filename);
    if(!file.exists())
        file.mkdirs();
    return file;
}


    class FileNotFoundException extends Exception {
        public FileNotFoundException(String message) {
            super(message);
        }
    }

    public static void createFile(String path) throws IOException {
        File file=new File(path);
        file.createNewFile();
    };

    public static void checkImageDir(String sourcePath,String targetPath){
        File imageFileDir=new File(sourcePath);
        if(imageFileDir.exists()){
            File [] files=imageFileDir.listFiles();
            for(File file:files){
                String validnameString=file.getName().toLowerCase().trim().replaceAll("-", "").replaceAll(".jpg", ".png");
                File mvfile=new File((targetPath.endsWith("\\")?targetPath:targetPath+"\\")+validnameString);
                System.out.println((targetPath.endsWith("\\")?targetPath:targetPath+"\\")+validnameString);
                file.renameTo(mvfile);
            }
        }
    }

    /**
     * 批量重命名文件名
     * @param sourcePath
     * @param targetPath
     * @param oldfilenameformatstr
     * @param filenameformatstr
     */
    public static void renameGenerator(String sourcePath,String targetPath,String oldfilenameformatstr,String filenameformatstr){
        File imageFileDir=new File(sourcePath);
        if(imageFileDir.exists()){
            File [] files=imageFileDir.listFiles();
            String [] divideStrings=null;
//查找符合条件的文件名分解字符元素
//for(File filetest:files){
//String [] divide_string_temp=filetest.getName().split(String.valueOf(0));
//if(divide_string_temp.length==1){
//divideStrings=divide_string_temp;
//break;
//}
//}
//for(String s:divideStrings)
//System.out.println(s);
            for(File file:files){
                //file.getName().r
                String target_name_string=file.getName().replace(oldfilenameformatstr,filenameformatstr);
                File mvfile=new File((targetPath.endsWith("\\")?targetPath:targetPath+"\\")+target_name_string);
                System.out.println((targetPath.endsWith("\\")?targetPath:targetPath+"\\")+target_name_string);
                file.renameTo(mvfile);
            }
        }
    }

    /**
     * 移动指定文件
     */
    public static void copyGenerator(String oldfilepath,String newfilepath){
        File file_path=new File(oldfilepath);
        File new_path=new File(newfilepath);
    }

    /**
     * 打印文件相关信息，并检查文件是否存在
     * @param file
     * @throws Exception
     */
    public static File checkFile(File file) throws Exception{
    if(file.exists()){
        if (file.isFile()){
            return file;
        }else{
            return null;
        }
    }
    return null;
}

    /**
     * 监察文件是否可读写
     * @param file
     * @return
     */
    public static HashMap<String,Boolean> getProperties(File file) {
        HashMap<String,Boolean> map=new HashMap<>();
        if(file!=null){
         map.put("canRead",file.canRead());
         map.put("canWrite",file.canWrite());
         map.put("canExecute",file.canExecute());
        }
        return map;
}

    /**
     * 获取文件MD5值
     * @param file
     * @throws Exception
     */
    public static String getMD5(File file) throws Exception {
        if(file!=null) {
            String command = "certutil -hashfile " + file.getPath() + " MD5";
            Cmd.executive( command);
            return  Cmd.getMsg();
        }else
            return null;
}

    /**
     * 获取文件MD5值
     * @param file
     * @throws Exception
     */
    public static long getFileSize(File file) throws Exception {
        if(file!=null) {
          return file.length();
        }else
          return -1;
    }

    /**
     * 获取文件最后修改时间
     * @param file
     * @return
     */
    public static long getLastestDate(File file){
        if(file!=null) return file.lastModified(); else return -1;
    }
    /**
     * 打印文件夹相关信息，并检查文件夹是否存在
     * @param file
     * @throws Exception
     */
public static File checkFileDir(File file) {
    System.out.println("文件夹是否存在" + file + ":" + file.exists());
    //若文件存在则返回true,若不存在则返回false
    if (file.exists()) {
        if (file.isDirectory()) {
            return file;
        } else {
            return null;
        }
    }
    return null;
}

    /**
     *从指定目录查找其子目录获取指定文件的完整(绝对)路径
     * @return
     */
    public static List<File> getSelectedFileFromDirectory(String gen_dir_path,String file_name) throws Exception{
       List<File> files=new ArrayList<>();
        String command = "dir /b /d /s " + gen_dir_path +file_name;
        //System.out.println(command);
        Cmd.executive(command);
         for(String path:Cmd.getMsg().split("\r\n")) {
             files.add(new File(path));
         }
             return files;
    }



    /**
     * 修改文件MD5数值
     * @param file_path
     * @param original_file
     * @param target_file
     * @throws Exception
     */
    public static void changeMD5(String file_path,String original_file,String target_file) throws Exception{
        String valid_file_path=file_path.endsWith("\\")?file_path:file_path+"\\";
        String input_text="hello";
        String additional_file_name=valid_file_path+"a.temp";
        writeFormattedFile(input_text.getBytes(),"UTF-8",additional_file_name);
        System.out.println("修改MD5值成功。");
        String command="copy /b \""+valid_file_path+original_file+"\"+\""+additional_file_name+"\" "+valid_file_path+target_file;
        System.out.println(command);
        Cmd.executive(command);
        System.out.println("修改MD5值成功。");
        deleteFile(new File(additional_file_name));
//
}

    /**
     * 删除指定文件
     * @param file
     */
    public static void deleteFile(File file) throws Exception{
    if(file!=null){
        file.delete();
        System.out.println(file.getName()+"删除成功");
    }
}

    /**
     *删除指定文件夹目录
     */
    public static boolean deleteFileDir(File file) throws  Exception{
        String command="RMDIR /S /Q "+file.getPath();
        if(file!=null){
            Cmd.executive(command);
            return true;
        }else{
            return false;
        }
    }


    /**
     *写入指定的内容并以指定的编码保存
     * @param input_text
     * @param format_type
     * @param file_path
     * @throws Exception
     */
    public static void writeFormattedFile(byte[] input_text,String format_type,String file_path) throws Exception{
        ByteUtils.bytesToFile(ByteUtils.getStringBytes(input_text,format_type),file_path);
    }

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
        //executive( command2);
        Cmd.executive( command);
        Cmd.executive( command2);
    }





    /**
     * 获取指定文件夹的所有子文件夹
     * @param file_dir
     * @return
     */
    public static ArrayList<String> getFileList(String file_dir) throws Exception{
        ArrayList<String> lists=new ArrayList<>();
        String command = "dir /b " + file_dir;
        Cmd.executive( command);
        for(String str:Cmd.getMsg().split("\r\n")){
            lists.add(str);
        }
        return lists;
    }
    /**
     * 批量复制指定文件
     * @param output_path
     * @throws Exception
     */
    public static void copyListFiles(List<File> files, String output_path) throws Exception{

        for(File file:files){
            String command_copy="copy "+file.getPath()+" "+output_path+"\\"+file.getPath();
//        System.out.println(command_copy);
            Cmd.executive( command_copy);
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
        Cmd.executive( command);
        System.out.println("已执行SQL脚本");
    }



    /**
     * 批量检查文件是否存在
     */
    public static boolean checkFiles(String file_path,List<String> lists) throws Exception{
        for (String list : lists) {
                File file = checkFile(new File((file_path.endsWith("\\") ? file_path : file_path + "\\") + list));
                if (!file.exists()) return false;
        }
        return true;
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
            File dir=checkFileDir(new File(file_dir.endsWith("\\") ?file_dir : file_dir + "\\"));
            if(dir!=null) {
                System.out.println(file_dir+"文件夹存在");
            }else{
                System.out.println(file_dir+"文件夹不存在");
            }
        }
    }
    /**
     * 批量删除文件
     */
    public static void deleteFiles(List<File> file_lists) throws Exception{
        for(File file:file_lists) {
                FileUtil.deleteFile(file);
        }
//        try {s
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    /**
     * 批量删除文件夹
     */
    public static void deleteFileDirs(ArrayList<File> files) throws Exception{
        for(File file:files) {
            //将后部分"\\"注释为正式目录
//            path=(path.endsWith("\\")?path.substring(0,path.length() -str_regexp.length()): path);
//                FileUtils.deleteFileDir(path);
            }
        }
//        try {
//            System.out.println(ByteUtils.getByteString(ByteUtils.filetoBytes("C:\\Program Files\\HtmlWatcher\\temp\\html\\20230509095528html.temp")));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    /**
     * 重命名文件
     * @param old_file
     * @param new_file_name
     */
    public static boolean renameFile(File old_file,String new_file_name){
        File new_file=new File(old_file.getPath()+"\\"+new_file_name);
        if(old_file.exists()){
            old_file.renameTo(new_file);
            return true;
        }else{
            return false;
        }
    }
    /**
     * 批量重命名文件是否存在
     */
    public static void renameFiles(List<File> old_files,List<String>new_files_name){
        for(int i=0;i<old_files.size();i++) {
            try {
                FileUtil.renameFile(old_files.get(i),new_files_name.get(i));
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
        FileUtil.checkImageDir("椭圆 1 拷贝-FMAssistant_", "loaddingstate");
    }


    /**
     * 批量打开指定目录的文件浏览器
     * @param
     */
    public static void openFiles(List<File> files){
        String command = null;
        for(File file:files) {
            String file_path=file.getPath();
            try {
                if(file_path.endsWith(".txt")){
                    command="notepad "+file_path;
                }else if(file_path.endsWith(".doc")||file_path.endsWith(".docx")){
                    command=getSelectedFileFromDirectory("C:\\Program Files\\","ksolaunch.exe ").get(0)+file_path;
                }else if(file_path.endsWith(".mp3")||file_path.endsWith(".wav")){
                    command=file_path;
                }else{
                    command="explorer "+file_path;
                }
                Cmd.executive( command);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Json与HashMap数据类型相互转换
     */
    public void testHashMap(){
        JsonUtil jsonUtil =new JsonUtil();
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

//        JSONObject jsonObject=JsonUtils.generateJson(info,"enemy",map_list);
//        HashMap <String,Object> map=JsonUtils.parseJSONOject(jsonObject.toString().getBytes());
//        Set<String> key=map.keySet();
//        for(Iterator iterator = key.iterator(); iterator.hasNext();) {
//            String k=(String)iterator.next();
//            System.out.println(k+":"+map.get(k));
//        }
//        ArrayListTest m=new ArrayListTest();
//        m.HashMapTestIterator();
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

