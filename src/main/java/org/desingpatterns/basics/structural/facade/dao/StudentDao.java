package org.desingpatterns.basics.structural.facade.dao;

import org.desingpatterns.basics.structural.facade.model.Student;

interface StudentDao {

    void addStudent(Student student);

    void updateStudent(Student student);

    void deleteStudent(Student student);
}
