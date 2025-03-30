package org.structural.proxy;

import org.structural.proxy.dao.EmployeeDao;
import org.structural.proxy.model.Employee;
import org.structural.proxy.dao.EmployeeDaoProxy;

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
