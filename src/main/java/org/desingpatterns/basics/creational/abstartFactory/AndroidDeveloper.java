package org.desingpatterns.basics.creational.abstartFactory;

public class AndroidDeveloper implements Employee{
    @Override
    public int salary() {
        return 800000;
    }

    @Override
    public String name() {
        System.out.println("I am Android Developer");
        return "Android Developer";
    }
}
