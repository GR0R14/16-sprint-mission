package com.sprint.mission.discodeit.entity;

import java.util.UUID;

public class User extends BaseEntity{
    private static final long serialVersionUID = 1L;

    private String username;
    private String email;
    private String password;

    public User(String username, String email, String password) {
        super();
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }

    public void update(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        updateUpdatedAt();
    }

}
