package com.mistra.plank.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.model.po.StockInfo;
import com.mistra.plank.pojo.model.vo.PageParam;
import com.mistra.plank.pojo.model.vo.PageVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockInfoDao extends BaseMapper<StockInfo> {

    void add(List<StockInfo> list);

    void update(List<StockInfo> list);

    PageVo<StockInfo> get(PageParam pageParam);

    StockInfo getStockByFullCode(String code);

}
