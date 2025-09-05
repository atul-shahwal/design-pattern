package org.desingpatterns.basics.structural.facade.dao;

import org.desingpatterns.basics.structural.facade.model.Student;

import java.util.HashMap;
import java.util.Map;

public class StudentDaoImpl implements StudentDao{

    Map<String, Student> studentsDDB = new HashMap<>();

    @Override
    public void addStudent(Student student) {
        if (!studentsDDB.containsKey(student.getStudentId())){
          studentsDDB.put(student.getStudentId(),student);
        }
    }

    @Override
    public void updateStudent(Student student) {
        if (studentsDDB.containsKey(student.getStudentId())){
            studentsDDB.put(student.getStudentId(),student);
        }
    }

    @Override
    public void deleteStudent(Student student) {
        if (studentsDDB.containsKey(student.getStudentId())){
            studentsDDB.remove(student.getStudentId(),student);
        }
    }
}
