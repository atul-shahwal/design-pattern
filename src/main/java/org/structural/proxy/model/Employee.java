package org.structural.proxy.model;

public class Employee {

    private String employeeId;
    private String employeeName;
    private int age;

    public Employee(String employeeId, String employeeName,int age) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.age = age;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId='" + employeeId + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", age=" + age +
                '}';
    }
}
