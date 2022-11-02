package com.mistra.plank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.StockInfo;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockInfoDao extends BaseMapper<StockInfo> {

    void add(List<StockInfo> list);

    void update(List<StockInfo> list);

    PageVo<StockInfo> get(PageParam pageParam);

    StockInfo getStockByFullCode(String code);

}
