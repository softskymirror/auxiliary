package com.commontool;

public class MusicInfo {
    public String songname;
    public String singer;
public void setName(String name){
    this.songname=name;
}

public void setSinger(String singer){
this.singer=singer;
}

public String getName(){
    return songname;
}
public String getSinger(){
    return singer;
}
}
