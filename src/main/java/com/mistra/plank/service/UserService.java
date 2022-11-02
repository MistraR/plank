package com.mistra.plank.service;

import com.mistra.plank.model.entity.User;

public interface UserService {

    User login(String username, String password);

    User getByToken(String token);

    User putToSession(User user, String token);

    User getById(int id);

    void update(User user);

}
