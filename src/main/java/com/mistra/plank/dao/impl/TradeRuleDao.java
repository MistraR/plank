package com.mistra.plank.dao.impl;

import com.mistra.plank.dao.BaseDao;
import com.mistra.plank.model.entity.TradeRule;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.common.util.SqlCondition;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradeRuleDao extends BaseDao {

    private static final String SELECT_SQL = "select id, stock_code as stockCode, strategy_id as strategyId, user_id as userId, type, value, volume, open_price as openPrice, highest_price as highestPrice, lowest_price as lowestPrice, highest_volume as highestVolume, lowest_volume as lowestVolume, state, description, create_time as createTime, update_time as updateTime from trade_rule where 1 = 1";

    public PageVo<TradeRule> get(PageParam pageParam) {
        SqlCondition dataSqlCondition = new SqlCondition(
                TradeRuleDao.SELECT_SQL,
                pageParam.getCondition());

        int totalRecords = jdbcTemplate.queryForObject(dataSqlCondition.getCountSql(), Integer.class,
                dataSqlCondition.toArgs());

        dataSqlCondition.addSql(" limit ?, ?");
        dataSqlCondition.addPage(pageParam.getStart(), pageParam.getLength());

        List<TradeRule> list = jdbcTemplate.query(dataSqlCondition.toSql(), BeanPropertyRowMapper.newInstance(TradeRule.class),
                dataSqlCondition.toArgs());
        return new PageVo<>(list, totalRecords);
    }

    public void updateState(int state, int id) {
        jdbcTemplate.update("update trade_rule set state = ? where id = ?", state, id);
    }

}
