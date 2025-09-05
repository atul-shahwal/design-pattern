package org.desingpatterns.basics.creational.abstartFactory;

public class AndroidDevFactory extends EmployeeAbstractFactory{
    @Override
    public Employee createEmployee() {
        return new AndroidDeveloper();
    }
}
