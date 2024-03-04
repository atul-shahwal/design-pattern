package org.creational.abstartFactory;

public class EmployeeFactory {
    //get Employee
    public static Employee getEmployee(EmployeeAbstractFactory factory){
        return factory.createEmployee();
    }
}
