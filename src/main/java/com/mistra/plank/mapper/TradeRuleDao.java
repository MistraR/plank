package com.mistra.plank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.pojo.entity.TradeRule;
import com.mistra.plank.pojo.vo.PageParam;
import com.mistra.plank.pojo.vo.PageVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeRuleDao extends BaseMapper<TradeRule> {

    PageVo<TradeRule> get(PageParam pageParam);

    void updateState(int state, int id);

}
