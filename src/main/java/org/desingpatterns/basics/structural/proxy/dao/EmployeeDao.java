package org.desingpatterns.basics.structural.proxy.dao;

import org.desingpatterns.basics.structural.proxy.model.Employee;

import java.util.List;


public interface EmployeeDao {
    void addEmployee(Employee employee);
    List<Employee> getAllEmployees();

}
