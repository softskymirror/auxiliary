package com.searchtool;

import com.system.CmdUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.system.CmdUtils.executive;

public class FileUtils {
    private static final String DEFAULT_CHARSET = "UTF-8";

    // 私有构造器，防止实例化
    private FileUtils() {}

    /* ==================== 目录/文件创建 ==================== */

    /**
     * 确保目录存在，如果不存在则创建（包括父目录）
     *
     * @param dirPath 目录路径
     * @return 目录的 File 对象
     * @throws IOException 如果创建失败
     */
    public static File ensureDirectoryExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        } else if (!Files.isDirectory(path)) {
            throw new IOException("路径已存在但不是目录: " + dirPath);
        }
        return path.toFile();
    }

    /**
     * 在指定目录下创建文件（如果文件不存在）
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @return 文件的 File 对象
     * @throws IOException 如果创建失败
     */
    public static File createFileIfNotExists(String dirPath, String fileName) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        Path filePath = dir.resolve(fileName);
        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
        }
        return filePath.toFile();
    }

    /* ==================== 文件重命名/移动 ==================== */

    /**
     * 重命名或移动文件（原子操作，如果支持）
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否成功
     * @throws IOException 如果移动失败
     */
    public static boolean moveFile(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);
        // 确保目标父目录存在
        if (target.getParent() != null && Files.notExists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return true;
    }


    /**
     * 重命名文件（同一目录下）
     * @param dirPath   目录路径
     * @param oldName   旧文件名
     * @param newName   新文件名
     * @return 是否成功
     * @throws IOException 如果重命名失败
     */
    public static boolean renameFile(String dirPath, String oldName, String newName) throws IOException {
        Path dir = Paths.get(dirPath);
        Path oldFile = dir.resolve(oldName);
        Path newFile = dir.resolve(newName);
        if (Files.notExists(oldFile)) {
            throw new IOException("源文件不存在: " + oldFile);
        }
        Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }



    public static File searchDirectory(String filepath,String filename) throws Exception{
        File path=new File((filepath.endsWith("\\")?filepath:filepath+"\\"));
        File file=new File((path.getPath().endsWith("\\"))?path.getPath():(path.getPath()+"\\")+filename);
    if(!file.exists())
        file.mkdirs();
    return file;
}

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
     *
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
//���ҷ����������ļ����ֽ��ַ�Ԫ��
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
     * �ƶ�ָ���ļ�
     */
    public static void copyGenerator(String oldfilepath,String newfilepath){
        File file_path=new File(oldfilepath);
        File new_path=new File(newfilepath);
    }

    /**
     * ��ӡ�ļ������Ϣ��������ļ��Ƿ����
     * @param file_path
     * @throws Exception
     */
    public static boolean checkFile(String file_path) throws Exception{
    File file=new File(file_path);
        System.out.println("�ļ��Ƿ����"+file_path+":"+file.exists());
    System.out.println(file_path+"��������£�");
    if(file.exists()){
        System.out.println("canExcute:" + file.canExecute());
        System.out.println("canAbsolute:" + file.getAbsolutePath());
        if (file.isFile()){
            String command = "certutil -hashfile " + file_path + " MD5";
            executive("cmd", command);
            System.out.println("MD5:" + CmdUtils.getMsg());
            return true;
        }else{
            return false;
        }
    }
    return false;
}
    /**
     * ��ӡ�ļ��������Ϣ��������ļ����Ƿ����
     * @param file_dir
     * @throws Exception
     */
public static boolean checkFileDir(String file_dir) {
    File file = new File(file_dir);
    System.out.println("�ļ����Ƿ����" + file_dir + ":" + file.exists());
    //���ļ������򷵻�true,���������򷵻�false
    if (file.exists()) {
        if (file.isDirectory()) {
            System.out.println("canExecute:" + file.canExecute());
            System.out.println("canAbsolute:" + file.getAbsolutePath());
            return true;
        } else {
            return false;
        }
    }
    return false;
}



    /**
     * �޸��ļ�MD5��ֵ
     * @param file_path
     * @param original_file
     * @param target_file
     * @throws Exception
     */
    public static void changeMD5(String file_path,String original_file,String target_file) throws Exception{
        String valid_file_path=file_path.endsWith("\\")?file_path:file_path+"\\";
        String input_text="hello";
        String additional_file_name=valid_file_path+"a.temp";
        writeFormattedFile(input_text,"UTF-8",additional_file_name);
        System.out.println("�޸�MD5ֵ�ɹ���");
        String command="copy /b \""+valid_file_path+original_file+"\"+\""+additional_file_name+"\" "+valid_file_path+target_file;
        System.out.println(command);
        executive("cmd",command);
        System.out.println("�޸�MD5ֵ�ɹ���");
        deleteFile(additional_file_name);
//
}

    /**
     * ɾ��ָ���ļ�
     * @param file_path
     */
    public static void deleteFile(String file_path) throws Exception{
    File file=new File(file_path);
    if(checkFile(file_path)){
        file.delete();
        System.out.println(file_path+"ɾ���ɹ�");
    }
}

    /**
     *ɾ��ָ���ļ���Ŀ¼
     */
    public static boolean deleteFileDir(String file_dir) throws  Exception{
        File file=new File(file_dir);
        String command="RMDIR /S /Q "+file_dir;
        if(checkFileDir(file_dir)){
            executive("cmd",command);
            System.out.println(file_dir+"ɾ���ɹ�");
            return true;
        }else{
            System.out.println(file_dir+"������");
            return false;
        }
    }


    /**
     *д��ָ�������ݲ���ָ���ı��뱣��
     * @param input_text
     * @param format_type
     * @param file_path
     * @throws Exception
     */
    public static void writeFormattedFile(String input_text,String format_type,String file_path) throws Exception{
        ByteUtils.bytesToFile(ByteUtils.getStringBytes(input_text,format_type),file_path);
    }

}