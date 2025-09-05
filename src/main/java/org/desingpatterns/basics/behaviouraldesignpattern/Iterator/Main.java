package org.desingpatterns.basics.behaviouraldesignpattern.Iterator;

public class Main {
    public static void main(String[] args) {

        UserManagement userManagement = new UserManagement();
        userManagement.addUser(new User("Ajeet","11"));
        userManagement.addUser(new User("Amit","12"));
        userManagement.addUser(new User("Ankit","13"));
        userManagement.addUser(new User("Rajeev","14"));

        MyIterator iterator = userManagement.getIterator();
        while (iterator.hasNext()){
            User user = (User) iterator.next();
            System.out.println(user.getName());
        }
    }
}
