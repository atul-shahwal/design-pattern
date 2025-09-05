package org.desingpatterns.basics.creational.abstartFactory;

public class WebDevFactory extends EmployeeAbstractFactory{
    @Override
    public Employee createEmployee() {
        return new WebDeveloper();
    }
}
