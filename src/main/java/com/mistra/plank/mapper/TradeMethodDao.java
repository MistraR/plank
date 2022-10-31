package com.mistra.plank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.model.po.TradeMethod;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeMethodDao extends BaseMapper<TradeMethod> {

    TradeMethod getByName(String name);

}
