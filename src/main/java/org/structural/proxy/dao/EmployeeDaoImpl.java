package org.structural.proxy.dao;

import org.structural.proxy.model.Employee;

import java.util.ArrayList;
import java.util.List;

public class EmployeeDaoImpl implements EmployeeDao{

    private final List<Employee> employees = new ArrayList<>();

    @Override
    public void addEmployee(Employee employee) {
        employees.add(employee);
    }


    @Override
    public List<Employee> getAllEmployees() {
        return employees;
    }
}
