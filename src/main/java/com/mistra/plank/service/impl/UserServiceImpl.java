package com.mistra.plank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mistra.plank.dao.UserDao;
import com.mistra.plank.model.entity.User;
import com.mistra.plank.service.UserService;
import com.mistra.plank.common.util.StockConsts;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public User login(String username, String password) {
        password = DigestUtils.md5Hex(password);
        return userDao.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).eq(User::getPassword, password));
    }

    @Cacheable(value = StockConsts.CACHE_KEY_TOKEN, key = "#token", unless = "#result == null")
    @Override
    public User getByToken(String token) {
        return null;
    }

    @CachePut(value = StockConsts.CACHE_KEY_TOKEN, key = "#token", unless = "#result == null")
    @Override
    public User putToSession(User user, String token) {
        return user;
    }

    @Override
    public User getById(int id) {
        return userDao.selectById(id);
    }

    @Override
    public void update(User user) {
        userDao.update(user);
    }

}
