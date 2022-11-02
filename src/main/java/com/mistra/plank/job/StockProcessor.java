package com.mistra.plank.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.common.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Mistra @ Version: 1.0 @ Time: 2021/11/18 22:09 @ Description: 更新股票每日成交量 @ Copyright (c) Mistra,All Rights
 * Reserved. @ Github: https://github.com/MistraR @ CSDN: https://blog.csdn.net/axela30w
 */
@Slf4j
@Component
public class StockProcessor {

    private final StockMapper stockMapper;
    private final DailyRecordMapper dailyRecordMapper;
    private final PlankConfig plankConfig;

    public StockProcessor(StockMapper stockMapper, DailyRecordMapper dailyRecordMapper, PlankConfig plankConfig) {
        this.stockMapper = stockMapper;
        this.dailyRecordMapper = dailyRecordMapper;
        this.plankConfig = plankConfig;
    }

    public void run() {
        try {
            String body =
                    HttpUtil.getHttpGetResponseString(plankConfig.getXueQiuAllStockUrl(), plankConfig.getXueQiuCookie());
            JSONObject data = JSON.parseObject(body).getJSONObject("data");
            JSONArray list = data.getJSONArray("list");
            Date today = new Date();
            BigDecimal zero = new BigDecimal(0);
            if (CollectionUtils.isNotEmpty(list)) {
                for (Object o : list) {
                    data = (JSONObject) o;
                    // volume 值不准确忽略
                    BigDecimal current = data.getBigDecimal("current");
                    BigDecimal volume = data.getBigDecimal("volume");
                    if (Objects.nonNull(current) && Objects.nonNull(volume)) {
                        Stock exist = stockMapper.selectById(data.getString("symbol"));
                        if (Objects.nonNull(exist)) {
                            List<DailyRecord> dailyRecords = dailyRecordMapper
                                    .selectPage(new Page<>(1, 20),
                                            new QueryWrapper<DailyRecord>().eq("code", data.getString("symbol"))
                                                    .ge("date", DateUtils.addDays(new Date(), -40)).orderByDesc("date"))
                                    .getRecords();
                            exist.setVolume(volume.longValue());
                            exist.setModifyTime(today);
                            exist.setCurrentPrice(current);
                            exist.setTransactionAmount(current.multiply(volume));
                            if (dailyRecords.size() >= 20) {
                                exist.setMa5(BigDecimal
                                        .valueOf(dailyRecords.subList(0, 5).stream().map(DailyRecord::getClosePrice)
                                                .collect(Collectors.averagingDouble(BigDecimal::doubleValue))));
                                exist.setMa10(BigDecimal
                                        .valueOf(dailyRecords.subList(0, 10).stream().map(DailyRecord::getClosePrice)
                                                .collect(Collectors.averagingDouble(BigDecimal::doubleValue))));
                                exist.setMa20(BigDecimal
                                        .valueOf(dailyRecords.subList(0, 20).stream().map(DailyRecord::getClosePrice)
                                                .collect(Collectors.averagingDouble(BigDecimal::doubleValue))));
                            }
                            stockMapper.updateById(exist);
                        } else {
                            stockMapper.insert(Stock.builder().code(data.getString("symbol"))
                                    .name(data.getString("name")).marketValue(data.getLongValue("mc")).currentPrice(current)
                                    .volume(volume.longValue()).ma5(zero).ma10(zero).ma20(zero)
                                    .transactionAmount(current.multiply(volume)).modifyTime(today).track(false)
                                    .shareholding(false).focus(false).classification("").build());
                        }
                    }
                }
            }
            log.warn("股票每日成交量、MA5、MA10、MA20更新完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取某只股票的最新价格
     *
     * @param code code
     * @return 价格
     */
    public double getCurrentPriceByCode(String code) {
        String url = plankConfig.getXueQiuStockDetailUrl().replace("{code}", code)
                .replace("{time}", String.valueOf(System.currentTimeMillis()))
                .replace("{recentDayNumber}", "1");
        String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
        JSONObject data = JSON.parseObject(body).getJSONObject("data");
        JSONArray list = data.getJSONArray("item");
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list)) {
            Object o = list.get(0);
            // 实时价格
            return ((JSONArray) o).getDoubleValue(5);

        }
        return 0d;
    }
}
