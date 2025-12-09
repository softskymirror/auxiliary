package com.commontool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

//import org.apache.commons.io.output.AbstractByteArrayOutputStream;


public class ArrayListTest {
   static ArrayList<String> a=new ArrayList<>();
static String []songname={"Fairytale","Gloria",""};
static String [] singer={"kalafina"};
    public static ArrayList<SongList> getList(){
     ArrayList<SongList> songLists=new ArrayList<>();
     ArrayList<MusicInfo> musicInfos=new ArrayList<>();
     SongList songList=new SongList();
     for(int i=0;i<songname.length;i++){
       final MusicInfo musicInfo=new MusicInfo();
        musicInfo.setName(songname[i]);
        musicInfo.setSinger(singer[0]);
        musicInfos.add(musicInfo);
        songList.setMusicInfo(musicInfos);
        songLists.add(songList);
     }
return songLists;
    }
    public void HashMapTest(){
        String [] name={"java","kotlin","flutter"};
        String [] type={"¾²Ì¬ÓïÑÔ","¾²Ì¬ÓïÑÔ","¾²Ì¬ÓïÑÔ"};
        String [] corp={"oracle","debian","ubuntu"};
        ArrayList<HashMap<String, String>> computer_lauguage = new ArrayList<HashMap<String, String>>();
        for(int i=0;i<3;i++){
        HashMap<String,String> list=new HashMap<String,String>();
        list.put("lauguage", name[i]);
        list.put("lauguageType",type[i]);
        list.put("lauguageCorp",corp[i]);
        computer_lauguage.add(list);
    }
    Set <String> str=computer_lauguage.get(0).keySet();
    for(String n:str){
       String value=computer_lauguage.get(0).get(n);
       System.out.println("name:"+n+"value:"+value);
    }
    }

    public void HashMapTestIterator(){
       ArrayList<HashMap<String,Object>> list=new ArrayList<>();
       HashMap<String,Object> map=new HashMap<>();
       map.put("new","bag");
       list.add(map);
       for(HashMap<String,Object> m:list){
if(m.get("new").equals("bag")){
System.out.println("equals");
}else{
    System.out.println("not equals");
}
       }

    }
public void testAutoIncrement(){
    for(int i=0;i<10;++i){
        System.out.println(i);
    }
}
    public static void main(String [] args){
     for(int i=0;i<getList().size();i++){
        ArrayList<MusicInfo> musicInfos=getList().get(i).getMusicInfos();
    for(int j=0;j<musicInfos.size();j++)
     System.out.println(musicInfos.get(j).getName());
    }
    }
}

