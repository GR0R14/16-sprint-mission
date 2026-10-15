package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    void create(User user);

    User findById(UUID id);

    List<User> findAll();

    void update(UUID id, User user);
    // void update(UUID id, String username, String email, String password);

    void delete(UUID id);
}
