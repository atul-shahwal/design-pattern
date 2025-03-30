package org.structural.facade.dao;

import org.structural.facade.model.Student;

public class StudentFacade {

    StudentDao studentDao;

    public StudentFacade() {
        this.studentDao = new StudentDaoImpl();
    }

    public void addStudent(Student student){
        studentDao.addStudent(student);
    }
}
