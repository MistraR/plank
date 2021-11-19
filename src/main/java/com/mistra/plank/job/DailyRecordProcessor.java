package com.mistra.plank.job;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.pojo.DailyRecord;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Mistra
 * @ Version: 1.0
 * @ Time: 2021/11/18 22:09
 * @ Description: 更新股票每日成交量
 * @ Copyright (c) Mistra,All Rights Reserved.
 * @ Github: https://github.com/MistraR
 * @ CSDN: https://blog.csdn.net/axela30w
 */
@Component
public class DailyRecordProcessor {

    Logger logger = Logger.getLogger(DailyRecordProcessor.class);

    private final DailyRecordMapper dailyRecordMapper;
    private final PlankConfig plankConfig;

    public DailyRecordProcessor(DailyRecordMapper dailyRecordMapper, PlankConfig plankConfig) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.plankConfig = plankConfig;
    }

    @Scheduled(cron = "0 0,30 0,15 ? * ? ")
    public void run() throws Exception {
        logger.info("开始更新股票每日成交数据！");
        for (Map.Entry<String, String> entry : StockProcessor.stockMap.entrySet()) {
            String url = plankConfig.getXueQiuStockDetailUrl();
            url = url.replace("{code}", entry.getKey()).replace("{time}", String.valueOf(System.currentTimeMillis()))
                    .replace("{recentDayNumber}", String.valueOf(plankConfig.getRecentDayNumber()));
            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(URI.create(url));
            httpGet.setHeader("Cookie", plankConfig.getXueQiuCookie());
            CloseableHttpResponse response = defaultHttpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String body = "";
            if (entity != null) {
                body = EntityUtils.toString(entity, "UTF-8");
            }
            JSONObject data = JSON.parseObject(body).getJSONObject("data");
            JSONArray list = data.getJSONArray("item");
            if (CollectionUtils.isNotEmpty(list)) {
                JSONArray array = new JSONArray();
                for (Object o : list) {
                    array = (JSONArray) o;
                    DailyRecord dailyRecord = new DailyRecord();
                    dailyRecord.setDate(new Date(array.getLongValue(0)));
                    dailyRecord.setCode(entry.getKey());
                    dailyRecord.setName(entry.getValue());
                    dailyRecord.setOpenPrice(new BigDecimal(array.getDoubleValue(2)));
                    dailyRecord.setHighest(new BigDecimal(array.getDoubleValue(3)));
                    dailyRecord.setLowest(new BigDecimal(array.getDoubleValue(4)));
                    dailyRecord.setClosePrice(new BigDecimal(array.getDoubleValue(5)));
                    dailyRecord.setIncreaseRate(new BigDecimal(array.getDoubleValue(7)));
                    dailyRecord.setAmount(array.getLongValue(9) / 10000);
                    dailyRecordMapper.insert(dailyRecord);
                    logger.info("更新" + entry.getValue() + "今日成交数据完成！");
                }
            }
        }
        logger.info("更新股票每日成交数据完成！");
    }

}
