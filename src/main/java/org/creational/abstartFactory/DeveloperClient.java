package org.creational.abstartFactory;

public class DeveloperClient {
    public static void main(String[] args) {
        //to get android developer
        Employee employee1 = EmployeeFactory.getEmployee(new AndroidDevFactory());
        employee1.name();
        //to get web developer
        Employee employee2 = EmployeeFactory.getEmployee(new WebDevFactory());
        employee2.name();
    }
}
