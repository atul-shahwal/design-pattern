package org.desingpatterns.basics.creational.singleton;

public class StudentEager {

    //eager way of creating singleton object
    private static StudentEager studentEager = new StudentEager();


    private StudentEager(){

    }
    public static StudentEager getStudentEager(){
        return studentEager;
    }
}
