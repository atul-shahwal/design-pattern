package org.structural.proxy.dao;

import org.structural.proxy.model.Employee;

import java.util.List;

public class EmployeeDaoProxy implements EmployeeDao {

    EmployeeDao employeeDao;

    public EmployeeDaoProxy(){
        employeeDao = new EmployeeDaoImpl();
    }

    @Override
    public void addEmployee(Employee employee) {
        if(employee.getAge() >= 18){
            employeeDao.addEmployee(employee);
        }
        throw new IllegalArgumentException("Employee age is less");
    }

    @Override
    public List<Employee> getAllEmployees() {
        return employeeDao.getAllEmployees();
    }
}
