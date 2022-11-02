package com.mistra.plank.service.impl;

import com.mistra.plank.dao.impl.HolidayCalendarDao;
import com.mistra.plank.model.entity.HolidayCalendar;
import com.mistra.plank.service.HolidayCalendarService;
import com.mistra.plank.service.SystemConfigService;
import com.mistra.plank.common.util.StockConsts;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HolidayCalendarServiceImpl implements HolidayCalendarService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HolidayCalendarDao holidayCalendarDao;

    @Autowired
    private SystemConfigService systemConfigService;

    @CacheEvict(value = StockConsts.CACHE_KEY_DATA_BUSINESS_DATE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCurrentYear() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        Map<?, ?> data = restTemplate.getForObject("http://tool.bitefu.net/jiari/?d=" + year, Map.class);

        Assert.notNull(data, "data is null");

        @SuppressWarnings("unchecked")
        Map<String, Integer> dateInfo = (Map<String, Integer>) data.get(String.valueOf(year));
        List<HolidayCalendar> list = dateInfo.entrySet().stream().filter(entry -> entry.getValue() != 0).map(entry -> {
            Date date;
            try {
                date = DateUtils.parseDate(year + entry.getKey(), "yyyyMMdd");
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
            HolidayCalendar holidayCalendar = new HolidayCalendar();
            holidayCalendar.setDate(date);
            return holidayCalendar;
        }).collect(Collectors.toList());

        holidayCalendarDao.deleteByYear(year);
        holidayCalendarDao.save(list);
    }

    @Cacheable(value = StockConsts.CACHE_KEY_DATA_BUSINESS_DATE, key = "#date.getDate().toString()")
    @Override
    public boolean isBusinessDate(Date date) {
        boolean isMock = systemConfigService.isMock();
        if (isMock) {
            return true;
        }

        if (date == null) {
            date = new Date();
        }

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int day = c.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return false;
        }
        return holidayCalendarDao.getByDate(date) == null;
    }

    @Override
    public boolean isBusinessTime(Date date) {
        boolean isMock = systemConfigService.isMock();
        if (isMock) {
            return true;
        }

        if (date == null) {
            date = new Date();
        }

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        if (hour < 9 || hour == 12 || hour > 14) {
            return false;
        }

        int minute = c.get(Calendar.MINUTE);
        return !(hour == 9 && minute < 30 || hour == 11 && minute >= 30);
    }

}
