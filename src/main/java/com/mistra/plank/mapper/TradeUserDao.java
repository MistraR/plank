package com.mistra.plank.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.model.po.TradeUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeUserDao extends BaseMapper<TradeUser> {

    TradeUser getById(int id);

    void update(TradeUser tradeUser);

    List<TradeUser> getList();

}
