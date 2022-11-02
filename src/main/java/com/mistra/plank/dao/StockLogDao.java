package com.mistra.plank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockLogDao extends BaseMapper<StockLog> {

    void add(List<StockLog> list);

    void setStockIdByCodeType(List<String> list, int type);

}
