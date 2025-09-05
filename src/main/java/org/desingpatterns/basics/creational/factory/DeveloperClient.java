package org.desingpatterns.basics.creational.factory;

public class DeveloperClient {

    public static void main(String[] args) {
//         tightly coupled class we need to manually specify employee type to solve this we create employee factory
//        Employee employee1 = new AndroidDeveloper();
        Employee employee1 = EmployeeFactory.getEmployee("ANDROID DEVELOPER");
        System.out.println(employee1);
        System.out.println(employee1.getSalary());

        Employee employee2 = EmployeeFactory.getEmployee("WEB DEVELOPER");
        System.out.println(employee2);
        System.out.println(employee2.getSalary());

    }
}
