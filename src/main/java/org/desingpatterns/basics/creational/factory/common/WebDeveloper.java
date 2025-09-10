package org.desingpatterns.basics.creational.factory.common;

public class WebDeveloper implements Employee {
    @Override
    public int getSalary() {
        System.out.println("Getting WebDeveloper Salary");
        return 800000;
    }
}
