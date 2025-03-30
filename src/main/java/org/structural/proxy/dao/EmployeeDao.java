package org.structural.proxy.dao;

import org.structural.proxy.model.Employee;

import java.util.List;


public interface EmployeeDao {
    void addEmployee(Employee employee);
    List<Employee> getAllEmployees();

}
