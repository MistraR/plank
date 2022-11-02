package com.mistra.plank.dao.impl;

import com.mistra.plank.dao.BaseDao;
import com.mistra.plank.model.entity.HolidayCalendar;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@Component
public class HolidayCalendarDao extends BaseDao {

    public void deleteByYear(int year) {
        jdbcTemplate.update("delete from holiday_calendar where DATE_FORMAT(date, '%Y') = ?", year);
    }

    public void save(List<HolidayCalendar> list) {
        jdbcTemplate.batchUpdate("insert into holiday_calendar(date) values(?)", new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setDate(1, new java.sql.Date(list.get(i).getDate().getTime()));
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }

        });
    }

    public HolidayCalendar getByDate(Date date) {
        List<HolidayCalendar> list = jdbcTemplate.query(
                "select id, date from holiday_calendar where date = date(?)",
                BeanPropertyRowMapper.newInstance(HolidayCalendar.class), date);
        return list.isEmpty() ? null : list.get(0);
    }

}
