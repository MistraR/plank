package com.mistra.plank.job;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.pojo.entity.DailyRecord;
import com.mistra.plank.util.HttpUtil;

import cn.hutool.core.thread.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Mistra @ Version: 1.0 @ Time: 2021/11/18 22:09 @ Description: 更新股票每日成交数据 @ Copyright (c) Mistra,All Rights
 *         Reserved. @ Github: https://github.com/MistraR @ CSDN: https://blog.csdn.net/axela30w
 */
@Slf4j
@Component
public class DailyRecordProcessor {

    private final DailyRecordMapper dailyRecordMapper;
    private final PlankConfig plankConfig;

    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(20, 50, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(5000), new NamedThreadFactory("每日交易数据线程-", false));

    public DailyRecordProcessor(DailyRecordMapper dailyRecordMapper, PlankConfig plankConfig) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.plankConfig = plankConfig;
    }

    public void run(HashMap<String, String> map) {
        Integer count = dailyRecordMapper.selectCount(new QueryWrapper<DailyRecord>().ge("date", checkDailyRecord()));
        if (count > 0) {
            log.warn("股票交易数据已更新到最新交易日！");
            return;
        }
        log.warn("开始更新股票每日成交数据！");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            executorService.submit(() -> {
                try {
                    String url = plankConfig.getXueQiuStockDetailUrl().replace("{code}", entry.getKey())
                        .replace("{time}", String.valueOf(System.currentTimeMillis()))
                        .replace("{recentDayNumber}", String.valueOf(plankConfig.getRecentDayNumber()));
                    String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
                    JSONObject data = JSON.parseObject(body).getJSONObject("data");
                    JSONArray list = data.getJSONArray("item");
                    if (CollectionUtils.isNotEmpty(list)) {
                        JSONArray array;
                        for (Object o : list) {
                            array = (JSONArray)o;
                            DailyRecord dailyRecord = new DailyRecord();
                            dailyRecord.setDate(new Date(array.getLongValue(0)));
                            dailyRecord.setCode(entry.getKey());
                            dailyRecord.setName(entry.getValue());
                            dailyRecord.setOpenPrice(BigDecimal.valueOf(array.getDoubleValue(2)));
                            dailyRecord.setHighest(BigDecimal.valueOf(array.getDoubleValue(3)));
                            dailyRecord.setLowest(BigDecimal.valueOf(array.getDoubleValue(4)));
                            dailyRecord.setClosePrice(BigDecimal.valueOf(array.getDoubleValue(5)));
                            dailyRecord.setIncreaseRate(BigDecimal.valueOf(array.getDoubleValue(7)));
                            dailyRecord.setAmount(array.getLongValue(9) / 10000);
                            dailyRecordMapper.insert(dailyRecord);
                            log.info("更新[ {} ]近日成交数据完成！", entry.getValue());
                        }
                    }
                    Thread.sleep(3000);
                } catch (Exception e) {
                }
            });
        }
        while (executorService.getQueue().size() != 0) {
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        log.warn("更新股票每日成交数据完成！");
    }

    /**
     * 查询上一个交易日日期
     * 
     * @return 上一个交易日日期
     */
    private Date checkDailyRecord() {
        String url = plankConfig.getXueQiuStockDetailUrl().replace("{code}", "SH600519")
            .replace("{time}", String.valueOf(System.currentTimeMillis())).replace("{recentDayNumber}", "1");
        String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
        JSONObject data = JSON.parseObject(body).getJSONObject("data");
        JSONArray list = data.getJSONArray("item");
        if (CollectionUtils.isNotEmpty(list)) {
            JSONArray array;
            for (Object o : list) {
                array = (JSONArray)o;
                return new Date(array.getLongValue(0));
            }
        }
        return new Date();
    }
}
