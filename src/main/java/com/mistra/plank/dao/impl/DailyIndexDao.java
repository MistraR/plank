package com.mistra.plank.dao.impl;

import com.mistra.plank.dao.BaseDao;
import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.vo.DailyIndexVo;
import com.mistra.plank.model.vo.PageParam;
import com.mistra.plank.model.vo.PageVo;
import com.mistra.plank.common.util.SqlCondition;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;

@Component
public class DailyIndexDao extends BaseDao {

    private static final String INSERT_SQL = "insert into daily_index(code, date, opening_price, pre_closing_price, highest_price, closing_price, lowest_price, trading_volume, trading_value, rurnover_rate) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public void save(List<DailyIndex> list) {
        jdbcTemplate.batchUpdate(DailyIndexDao.INSERT_SQL, list, list.size(),
                DailyIndexDao::setArgument);
    }

    private static void setArgument(PreparedStatement ps, DailyIndex dailyIndex)
            throws SQLException {
        ps.setString(1, dailyIndex.getCode());
        StatementCreatorUtils.setParameterValue(ps, 2, Types.DATE,
                dailyIndex.getDate());
        ps.setBigDecimal(3, dailyIndex.getOpeningPrice());
        ps.setBigDecimal(4, dailyIndex.getPreClosingPrice());
        ps.setBigDecimal(5, dailyIndex.getHighestPrice());
        ps.setBigDecimal(6, dailyIndex.getClosingPrice());
        ps.setBigDecimal(7, dailyIndex.getLowestPrice());
        ps.setLong(8, dailyIndex.getTradingVolume());
        ps.setBigDecimal(9, dailyIndex.getTradingValue());
        ps.setBigDecimal(10, dailyIndex.getRurnoverRate());
    }

    public PageVo<DailyIndexVo> getDailyIndexList(PageParam pageParam) {
        String sql = "select"
                + " s.name, s.abbreviation, d.code, d.date, d.pre_closing_price as preClosingPrice,"
                + " d.closing_price as closingPrice, d.lowest_price as lowestPrice,"
                + " d.highest_price as highestPrice, d.opening_price as openingPrice,"
                + " d.trading_value as tradingValue, d.trading_volume as tradingVolume,"
                + " d.rurnover_rate as rurnoverRate"
                + " from daily_index d, stock_info s where d.code = concat(s.exchange, s.code)";

        SqlCondition dataSqlCondition = new SqlCondition(sql, pageParam.getCondition());
        dataSqlCondition.addString("date", "date");

        Integer totalRecords = jdbcTemplate.queryForObject(dataSqlCondition.getCountSql(), Integer.class, dataSqlCondition.toArgs());

        dataSqlCondition.addSort("tradingValue", false, true);
        dataSqlCondition.addSql(" limit ?, ?");
        dataSqlCondition.addPage(pageParam.getStart(), pageParam.getLength());

        List<DailyIndexVo> list = jdbcTemplate.query(dataSqlCondition.toSql(),
                BeanPropertyRowMapper.newInstance(DailyIndexVo.class), dataSqlCondition.toArgs());
        return new PageVo<>(list, totalRecords);
    }

    public List<DailyIndex> getDailyIndexListByDate(Date date) {
        String sql = "select"
                + " id, code, date, pre_closing_price as preClosingPrice,"
                + " closing_price as closingPrice, lowest_price as lowestPrice,"
                + " highest_price as highestPrice, opening_price as openingPrice,"
                + " trading_value as tradingValue, trading_volume as tradingVolume,"
                + " rurnover_rate as rurnoverRate"
                + " from daily_index where date = ?";
        List<DailyIndex> list = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(DailyIndex.class),
                new java.sql.Date(date.getTime()));
        return list;
    }

}
