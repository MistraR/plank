package com.mistra.plank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao extends BaseMapper<User> {

    User get(String username, String password);

    User get(int id);

    void update(User user);

}
