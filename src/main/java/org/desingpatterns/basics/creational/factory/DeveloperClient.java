package org.desingpatterns.basics.creational.factory;

import org.desingpatterns.basics.creational.factory.common.AndroidDeveloper;
import org.desingpatterns.basics.creational.factory.common.Employee;
import org.desingpatterns.basics.creational.factory.common.WebDeveloper;
import org.desingpatterns.basics.creational.factory.solution.EmployeeFactory;;
public class DeveloperClient {

    public static void main(String[] args) {
//         tightly coupled class we need to manually specify employee type to solve this we create employee factory
//        As our no of child class will increase it will be much difficult to create object.
        Employee employee1 = new AndroidDeveloper();
        System.out.println(employee1);
        System.out.println(employee1.getSalary());

        Employee employee2 = new WebDeveloper();
        System.out.println(employee2);
        System.out.println(employee2.getSalary());


        //solution use factory design pattern.

        Employee employee3 = EmployeeFactory.getEmployee("ANDROID DEVELOPER");
        System.out.println(employee2);


        Employee employee4 = EmployeeFactory.getEmployee("WEB DEVELOPER");
        System.out.println(employee1);








    }
}
