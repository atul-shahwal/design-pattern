package org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.repository;
import org.desingpatterns.basics.behaviouraldesignpattern.strategydesignpattern.model.user.User;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private static final Map<String, User> userDetails = new HashMap<>();

    public UserRepository() {
    }

    public static void addUser(User user){
        userDetails.put(user.getUserId(), user);
    }

    public static User getUser(String userId) throws RuntimeException{
        if(userDetails.containsKey(userId)){
            return userDetails.get(userId);
        }
        throw new RuntimeException("User does not Exist");
    }

}
