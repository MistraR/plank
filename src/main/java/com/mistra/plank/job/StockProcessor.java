package com.mistra.plank.job;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.pojo.entity.Stock;

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
            log.info("开始更新股票每日成交量！");
            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(URI.create(plankConfig.getXueQiuAllStockUrl()));
            httpGet.setHeader("Cookie", plankConfig.getXueQiuCookie());
            CloseableHttpResponse response = defaultHttpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String body = "";
            if (entity != null) {
                body = EntityUtils.toString(entity, "UTF-8");
            }
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
                        Stock stock =
                            new Stock(data.getString("symbol"), data.getString("name"), data.getLongValue("mc"),
                                current, current, volume.longValue(), current.multiply(volume), today);
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
            log.info("更新股票每日成交量完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
