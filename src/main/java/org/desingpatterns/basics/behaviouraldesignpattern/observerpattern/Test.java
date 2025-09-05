package org.desingpatterns.basics.behaviouraldesignpattern.observerpattern;

import org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.model.PrimeUser;
import org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.model.User;
import org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.notifier.Notify;
import org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.notifier.NotifyPrimeUser;

public class Test {

    public static void main(String[] args) {
        User user1 = new PrimeUser("7878","Raj");
        User user2 = new PrimeUser("7877","Ravi");
        User user3 = new PrimeUser("7876","Pankaj");
        Notify notify = new NotifyPrimeUser();
        notify.addUser(user1);
        notify.addUser(user2);
        notify.addUser(user3);
        System.out.println(notify.getUserList());
        notify.setData(user1);
        notify.removeUser(user3);
        System.out.println(notify.getUserList());
    }
}
