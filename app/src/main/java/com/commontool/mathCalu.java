package com.commontool;

import java.text.DecimalFormat;
import java.util.Scanner;

public class mathCalu {
    public double phoneWidth;
    public double phoneHeight;
    public double dpi;
    public int widthpiexls;
    public int heightpiexls;
    public static double diagonal;
    public static double InchConversionRatio=2.54;
    public static double inches;
    public static double MiddleDensityIndependent=160;
    public static double scalingRatio;
    public DecimalFormat dc=new DecimalFormat("#.00");
    public DecimalFormat dcn=new DecimalFormat("#");
    public void caluDPI(){
        Scanner sc=new Scanner(System.in);
        System.out.println("欢迎进入手机适配参数计算系统");
        System.out.println("请输入手机的宽度(cm)");
        phoneWidth=sc.nextDouble();
        System.out.println("请输入手机的高度(cm):");
        phoneHeight=sc.nextDouble();     
         System.out.println("请输入手机的宽度像素值(cm)");
        widthpiexls=sc.nextInt();
        System.out.println("请输入手机的高度像素值(cm):");
        heightpiexls=sc.nextInt();
        diagonal=Math.sqrt(Math.pow(phoneWidth,2)+Math.pow(phoneHeight, 2));
        inches=diagonal/InchConversionRatio;
        dpi=(Math.sqrt(Math.pow(widthpiexls,2)+Math.pow(heightpiexls, 2))/inches);
        scalingRatio=dpi/MiddleDensityIndependent;
        System.out.println("手机的对角长度为"+dc.format(diagonal)+"cm\r\n"+
        "手机的寸数(屏幕大小)为"+dc.format(inches)+"寸\r\n"+
        "dpi(屏幕像素密度)为"+dcn.format(dpi)+"\r\n"+
        "缩放比率为"+dc.format(scalingRatio));
    }
    public static void main(String [] args){
        mathCalu mathcalu=new mathCalu();
        mathcalu.caluDPI();
    }
}
