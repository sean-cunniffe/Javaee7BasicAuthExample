package com.example.demo1.repository;

import com.example.demo1.entity.User;
import com.example.demo1.entity.UserType;

import java.util.Arrays;
import java.util.List;


public class UserRepository {
    List<User> users;

    public UserRepository() {
        User user = new User("JohnDoe","iLoveIntelliJ", UserType.STAFF);
        User user2 = new User("maryDoe","iLoveIntelliJ2", UserType.CUSTOMER);
        users = Arrays.asList(user, user2);

    }

    public User getUserByUsernameAndPassword(String username, String plainTextPassword){
        // do some DB search for username
        // check the plain text password against hashed password if you're storing them as hashed
        for (User user : users) {
            if(user.getUsername().equals(username) && user.getPassword().equals(plainTextPassword))
                return user;
        }
        return null;
    }

    public List<User> getUsers() {
        return users;
    }

    public User getUserByUsername(String username){
        for (User tempUser : users) {
            if (tempUser.getUsername().equals(username)) {
                return tempUser;
            }
        }
        return null;
    }

}
