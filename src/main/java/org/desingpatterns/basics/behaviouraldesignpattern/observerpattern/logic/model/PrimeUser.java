package org.desingpatterns.basics.behaviouraldesignpattern.observerpattern.logic.model;

public class PrimeUser implements User{
    private String userId;
    private String userName;
    private int verSionNumber = 0;

    public PrimeUser(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.verSionNumber = 1;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return "PrimeUser{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", verSionNumber=" + verSionNumber +
                '}';
    }

    @Override
    public void updateUserDetails(User user) {
        PrimeUser primeUser = (PrimeUser) user;
        this.userName = primeUser.getUserName();
        this.verSionNumber++;
    }
}
