package org.desingpatterns.basics.creational.builder;

public class Test {
    public static void main(String[] args) {
        User obj1 = new User.UserBuilder()
                .setUserName("Ajeet")
                .setUserId("USER1234")
                .setEmailId("xyz@gmail.com")
                .setUserAge("25")
                .build();
        System.out.println(obj1);
        User obj2 = User.UserBuilder.getUserBuilder()
                .setUserName("Ankit")
                .setUserId("USER1235")
                .setUserAge("29")
                .build();
        System.out.println(obj2);
    }
}
