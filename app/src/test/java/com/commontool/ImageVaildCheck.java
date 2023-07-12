package com.commontool;

import java.io.File;
import java.io.FileDescriptor;

public class ImageVaildCheck {
    static String rootpath;
    static File imageFileDir;
    public ImageVaildCheck(String path){
    this.rootpath=path;
    }
    public static void NameGenerator(){
    imageFileDir=new File(rootpath);
    if(imageFileDir.exists()){
File [] files=imageFileDir.listFiles();
for(File file:files){
String validnameString=file.getName().toLowerCase().trim().replaceAll("-", "").replaceAll(".jpg", ".png");
File mvfile=new File(rootpath+validnameString);
file.renameTo(mvfile);
    }
    }
}
    public static void main(String [] args){
        //ImageVaildCheck imageVaildCheck=new ImageVaildCheck("D:\\programfiles\\programworkplace\\Android\\AndroidStudioProject\\DBUseTable\\app\\src\\main\\res\\drawable");
        //imageVaildCheck.NameGenerator();
    }
    
}
