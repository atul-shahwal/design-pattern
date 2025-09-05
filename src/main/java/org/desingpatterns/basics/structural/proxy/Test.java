package org.desingpatterns.basics.structural.proxy;

import org.desingpatterns.basics.structural.proxy.dao.EmployeeDao;
import org.desingpatterns.basics.structural.proxy.model.Employee;
import org.desingpatterns.basics.structural.proxy.dao.EmployeeDaoProxy;

public class Test {

    public static void main(String[] args) {
        Employee employee1 = new Employee("123456","Raj",20);
        Employee employee2 = new Employee("323232","Ravi",19);
        EmployeeDao employeeDao = new EmployeeDaoProxy();
        try {
            employeeDao.addEmployee(employee1);
            employeeDao.addEmployee(employee2);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        System.out.println(employeeDao.getAllEmployees());
    }
}
