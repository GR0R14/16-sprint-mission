package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    void create(User user);

    default User create(String username, String email, String password) {
        User user = new User(username, email, password);
        create(user);
        return user;
    }

    User findById(UUID id);

    List<User> findAll();

    void update(UUID id, User user);

    void delete(UUID id);
}
