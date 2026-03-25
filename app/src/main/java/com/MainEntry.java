package com;

import com.system.ConfigUtils;
import com.toolui.MainUI;
import life.Person;


//import static com.sqltool.MySQLUtils.testDemo;
import static life.Person.testDemo;

public class MainEntry {
    public static void main(String [] args){
//        Person person=new Person();
//        person.getPersonJson();
//        double bmi1 = testDemo();
//        System.out.printf("BMI: %.2f (%s)%n",
//                bmi1, getBMICategory(bmi1));
        testDemo();
    }

    public void startService(){
        ConfigUtils configUtils=new ConfigUtils();
        System.out.println(configUtils.globalData.get("pomFilepath"));
    }
}
