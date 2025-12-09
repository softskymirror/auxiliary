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
        System.out.println("");
        System.out.println("");
        phoneWidth=sc.nextDouble();
        System.out.println("");
        phoneHeight=sc.nextDouble();     
         System.out.println("");
        widthpiexls=sc.nextInt();
        System.out.println("");
        heightpiexls=sc.nextInt();
        diagonal=Math.sqrt(Math.pow(phoneWidth,2)+Math.pow(phoneHeight, 2));
        inches=diagonal/InchConversionRatio;
        dpi=(Math.sqrt(Math.pow(widthpiexls,2)+Math.pow(heightpiexls, 2))/inches);
        scalingRatio=dpi/MiddleDensityIndependent;
        System.out.println(""+dc.format(diagonal)+"cm\r\n"+
        ""+dc.format(inches)+"��\r\n"+
        ""+dcn.format(dpi)+"\r\n"+
        ""+dc.format(scalingRatio));
    }
    public static void main(String [] args){
        mathCalu mathcalu=new mathCalu();
        mathcalu.caluDPI();
    }
}
