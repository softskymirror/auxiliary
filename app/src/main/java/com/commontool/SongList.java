package com.commontool;

import java.util.ArrayList;

public class SongList {
    ArrayList<MusicInfo> musicinfos;
    
public void setMusicInfo(ArrayList<MusicInfo> musicinfos){
    this.musicinfos=musicinfos;
}
public ArrayList<MusicInfo> getMusicInfos(){
    return musicinfos;
}
}
