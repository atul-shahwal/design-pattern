package org.behaviouraldesignpattern.observerpattern.logic.notifier;

import org.behaviouraldesignpattern.observerpattern.logic.model.PrimeUser;
import org.behaviouraldesignpattern.observerpattern.logic.model.User;
import java.util.HashMap;
import java.util.Map;

public class NotifyPrimeUser implements Notify{

    private Map<String, User> users;

    public NotifyPrimeUser() {
        this.users = new HashMap<>();
    }

    @Override
    public void addUser(User user) {
        PrimeUser primeUser = (PrimeUser) user;
        users.put(primeUser.getUserId(),user);
    }

    @Override
    public void removeUser(User user) {
        PrimeUser primeUser = (PrimeUser) user;
        users.remove(primeUser.getUserId(),user);
    }

    @Override
    public void notifyUser(User user) {
        PrimeUser primeUser = (PrimeUser) user;
        System.out.println("Notified User : "+ primeUser.getUserId() + " data updated successfully");
    }

    @Override
    public void setData(User user) {
        user.updateUserDetails(user);
        notifyUser(user);
    }

    @Override
    public Map<String, User> getUserList(){
        return this.users;
    }
}
