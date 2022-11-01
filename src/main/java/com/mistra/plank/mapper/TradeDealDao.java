package com.mistra.plank.mapper;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.entity.TradeDeal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeDealDao extends BaseMapper<TradeDeal> {

    void add(TradeDeal tradeDeal);

    List<TradeDeal> getByDate(Date date);

}
