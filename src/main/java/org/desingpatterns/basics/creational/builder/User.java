package org.desingpatterns.basics.creational.builder;

public class User {
    private final String userId;
    private final String userName;
    private final String emailId;
    private final String userAge;
    private User(UserBuilder builder){
        //initialze
        this.userId =builder.userId;
        this.userName =builder.userName;
        this.emailId =builder.emailId;
        this.userAge =builder.userAge;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getUserAge() {
        return userAge;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", emailId='" + emailId + '\'' +
                ", userAge='" + userAge + '\'' +
                '}';
    }

    //inner class to create object
    static class UserBuilder{
        private String userId;
        private String userName;
        private String emailId;
        private String userAge;
        public UserBuilder(){

        }
//         we can me default constructor private
//        private UserBuilder(){
//
//        }
        public static UserBuilder getUserBuilder(){
            return new UserBuilder();
        }

        public UserBuilder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserBuilder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public UserBuilder setEmailId(String emailId) {
            this.emailId = emailId;
            return this;
        }

        public UserBuilder setUserAge(String userAge) {
            this.userAge = userAge;
            return this;
        }

        public User build(){
            User user = new User(this);
            return user;
        }
    }
}
