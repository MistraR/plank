package com.mistra.plank.job;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.pojo.Stock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
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
public class StockProcessor implements CommandLineRunner {

    Logger logger = Logger.getLogger(StockProcessor.class);

    private final StockMapper stockMapper;
    private final PlankConfig plankConfig;
    private final DailyRecordProcessor dailyRecordProcessor;

    public static final HashMap<String, String> stockMap = new HashMap<>();

    public StockProcessor(StockMapper stockMapper, PlankConfig plankConfig, DailyRecordProcessor dailyRecordProcessor) {
        this.stockMapper = stockMapper;
        this.plankConfig = plankConfig;
        this.dailyRecordProcessor = dailyRecordProcessor;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                .notLike("name", "%ST%")
                .notLike("name", "%st%")
                .notLike("name", "%A%")
                .notLike("name", "%C%")
                .notLike("name", "%N%")
                .notLike("name", "%U%")
                .notLike("name", "%W%")
                .notLike("code", "%BJ%")
                .notLike("code", "%688%")
        );
        stocks.forEach(stock -> stockMap.put(stock.getCode(), stock.getName()));
        logger.info("一共加载 " + stocks.size() + "支股票！");
        dailyRecordProcessor.run();
    }

    @Scheduled(cron = "0 0,30 0,15 ? * ? ")
    public void run() throws Exception {
        logger.info("开始更新股票每日成交量！");
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
                data = (JSONObject) o;
                // volume 值不准确忽略
                Stock stock = new Stock(data.getString("symbol"), data.getString("name"), data.getLongValue("mc"),
                        data.getLongValue("volume") / 10000, today);
                Stock exist = stockMapper.selectById(stock.getCode());
                if (Objects.nonNull(exist)) {
                    exist.setVolume(stock.getVolume());
                    exist.setModifyTime(today);
                    stockMapper.updateById(exist);
                } else {
                    stockMapper.insert(stock);
                }
            }
        }
        logger.info("更新股票每日成交量完成！");
    }

}
