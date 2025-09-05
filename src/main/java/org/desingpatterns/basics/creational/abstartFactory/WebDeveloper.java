package org.desingpatterns.basics.creational.abstartFactory;

public class WebDeveloper implements Employee{
    @Override
    public int salary() {
        return 1200000;
    }

    @Override
    public String name() {
        System.out.println("I am web Developer");
        return "Web Developer";
    }
}
