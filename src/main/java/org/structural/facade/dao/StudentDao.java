package org.structural.facade.dao;

import org.structural.facade.model.Student;

interface StudentDao {

    void addStudent(Student student);

    void updateStudent(Student student);

    void deleteStudent(Student student);
}
