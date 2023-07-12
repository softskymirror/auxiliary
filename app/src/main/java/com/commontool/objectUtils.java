package com.commontool;

import java.text.DecimalFormat;

public class objectUtils {
    DecimalFormat df = new DecimalFormat("0.00");
    
    public static void checkInt(String str){
      
        int strint=Integer.parseInt(str);
        System.out.println(strint);
    }
public void mathbit(){
int a=820,b=830;
int alpha=(int)Math.pow(255,1.0/3);
int d=(int)(100*(double)a/(double)b);
float d1=Float.valueOf(df.format(a));
System.out.println(d);
}
public void mathcal(){
int i=-1;
System.out.println(Math.abs(i));

}
public static void countInt(){
int i=-3-(-6);
System.out.println(i);
}
    
}
