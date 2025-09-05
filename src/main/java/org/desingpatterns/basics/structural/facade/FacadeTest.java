package org.desingpatterns.basics.structural.facade;

import org.desingpatterns.basics.structural.facade.dao.StudentFacade;
import org.desingpatterns.basics.structural.facade.model.Student;

public class FacadeTest {

    public static void main(String[] args) {
        StudentFacade studentFacade = new StudentFacade();
        Student student = new Student("4545","Ajjet");
        studentFacade.addStudent(student);
    }
}
