package org.desingpatterns.basics.creational.factory.solution;

import org.desingpatterns.basics.creational.factory.common.AndroidDeveloper;
import org.desingpatterns.basics.creational.factory.common.Employee;
import org.desingpatterns.basics.creational.factory.common.WebDeveloper;

//is class ke pass factory methods honge jo employee return karenge based on input
public class EmployeeFactory {
    //get the employee
    public static Employee getEmployee(String empType){
        if(empType.trim().equalsIgnoreCase("ANDROID DEVELOPER")){
            return new AndroidDeveloper();
        }
        else if(empType.trim().equalsIgnoreCase("WEB DEVELOPER")){
            return new WebDeveloper();
        }
        else {
            return null;
        }
    }
}
