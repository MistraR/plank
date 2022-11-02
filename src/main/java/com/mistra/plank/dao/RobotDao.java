package com.mistra.plank.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.Robot;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RobotDao extends BaseMapper<Robot> {

    Robot getById(int id);

    List<Robot> getListByType(int type);

}
