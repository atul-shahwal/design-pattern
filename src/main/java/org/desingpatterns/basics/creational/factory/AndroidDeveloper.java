package org.desingpatterns.basics.creational.factory;

public class AndroidDeveloper implements Employee{
    @Override
    public int getSalary() {
        System.out.println("Getting AndroidDeveloper Salary");
        return 500000;
    }
}
