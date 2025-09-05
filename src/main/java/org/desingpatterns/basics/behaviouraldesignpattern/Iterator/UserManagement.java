package org.desingpatterns.basics.behaviouraldesignpattern.Iterator;


import java.util.ArrayList;

public class UserManagement {
    private ArrayList<User> userList = new ArrayList<User>();

    public void addUser(User user){
        userList.add(user);
    }
    public User getUser(int index){
        return userList.get(index);
    }

    public MyIterator getIterator(){
        return new MyIteratorImpl(userList);
    }
}
