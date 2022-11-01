package com.mistra.plank.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeOrderDao extends BaseMapper<TradeOrder> {

    void add(TradeOrder tradeOrder);

    void update(TradeOrder tradeOrder);

    List<TradeOrder> getLastListByRuleId(int ruleId, int userId);

    void setInvalidByRuleId(int ruleId);

}
