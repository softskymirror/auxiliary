package com;

import com.toolui.MainUI;
import life.Person;


import static life.Person.calculateBMI;
import static life.PersonProperties.getBMICategory;

public class MainEntry {
    public static void main(String [] args){
//        Person person=new Person();
//        person.getPersonJson();
        double bmi1 = calculateBMI(1.80, 66);
        System.out.printf("BMI: %.2f (%s)%n",
                bmi1, getBMICategory(bmi1));

    }
}
