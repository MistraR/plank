package com.mistra.plank.job;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.pojo.entity.Stock;
import com.mistra.plank.util.HttpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Mistra @ Version: 1.0 @ Time: 2021/11/18 22:09 @ Description: 更新股票每日成交量 @ Copyright (c) Mistra,All Rights
 *         Reserved. @ Github: https://github.com/MistraR @ CSDN: https://blog.csdn.net/axela30w
 */
@Slf4j
@Component
public class StockProcessor {

    private final StockMapper stockMapper;
    private final PlankConfig plankConfig;

    public StockProcessor(StockMapper stockMapper, PlankConfig plankConfig) {
        this.stockMapper = stockMapper;
        this.plankConfig = plankConfig;
    }

    public void run() {
        try {
            log.warn("开始更新股票每日成交量！");
            String body =
                HttpUtil.getHttpGetResponseString(plankConfig.getXueQiuAllStockUrl(), plankConfig.getXueQiuCookie());
            JSONObject data = JSON.parseObject(body).getJSONObject("data");
            JSONArray list = data.getJSONArray("list");
            Date today = new Date();
            if (CollectionUtils.isNotEmpty(list)) {
                for (Object o : list) {
                    data = (JSONObject)o;
                    // volume 值不准确忽略
                    BigDecimal current = data.getBigDecimal("current");
                    BigDecimal volume = data.getBigDecimal("volume");
                    if (Objects.nonNull(current) && Objects.nonNull(volume)) {
                        Stock stock = Stock.builder().code(data.getString("symbol")).name(data.getString("name"))
                            .marketValue(data.getLongValue("mc")).currentPrice(current).purchasePrice(current)
                            .volume(volume.longValue()).transactionAmount(current.multiply(volume)).modifyTime(today)
                            .track(false).shareholding(false).focus(false).classification("").build();
                        Stock exist = stockMapper.selectById(stock.getCode());
                        if (Objects.nonNull(exist)) {
                            exist.setVolume(stock.getVolume());
                            exist.setModifyTime(today);
                            exist.setCurrentPrice(stock.getCurrentPrice());
                            exist.setTransactionAmount(stock.getTransactionAmount());
                            stockMapper.updateById(exist);
                        } else {
                            stockMapper.insert(stock);
                        }
                    }
                }
            }
            log.warn("更新股票每日成交量完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
