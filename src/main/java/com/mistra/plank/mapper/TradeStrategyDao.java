package com.mistra.plank.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.model.po.TradeStrategy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeStrategyDao extends BaseMapper<TradeStrategy> {

    List<TradeStrategy> getAll();

}
