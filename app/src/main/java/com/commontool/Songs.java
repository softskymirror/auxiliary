package com.commontool;

import java.util.ArrayList;

public class Songs {
    public ArrayList<MusicInfo> musicinfos;
    static ArrayListTest arrayListTest=new ArrayListTest();
    public static void main(String [] args){
        for(int i=0;i<arrayListTest.getList().size();i++){
            ArrayList<MusicInfo> musicInfos=arrayListTest.getList().get(i).getMusicInfos();
        for(int j=0;j<musicInfos.size();j++)
         System.out.println(musicInfos.get(j).getName());
        }
        }
    
}
