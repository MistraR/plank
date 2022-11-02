package com.mistra.plank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.TradeMethod;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeMethodDao extends BaseMapper<TradeMethod> {

    TradeMethod getByName(String name);

}
