package org.structural.facade;

import org.structural.facade.dao.StudentFacade;
import org.structural.facade.model.Student;

public class FacadeTest {

    public static void main(String[] args) {
        StudentFacade studentFacade = new StudentFacade();
        Student student = new Student("4545","Ajjet");
        studentFacade.addStudent(student);
    }
}
