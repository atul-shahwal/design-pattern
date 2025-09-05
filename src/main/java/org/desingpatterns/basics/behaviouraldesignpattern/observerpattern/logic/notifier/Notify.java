package org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.notifier;

import org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.model.User;
import java.util.Map;

public interface Notify {

    void addUser(User user);
    void removeUser(User user);
    void notifyUser(User user);
    void setData(User user);
    Map<String, User> getUserList();

}
