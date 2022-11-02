package com.mistra.plank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.TradeUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeUserDao extends BaseMapper<TradeUser> {

    TradeUser getById(int id);

//    void update(TradeUser tradeUser);

    List<TradeUser> getList();

}
