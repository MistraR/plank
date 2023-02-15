package com.mistra.plank.job;

import cn.hutool.core.date.DateUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.common.util.UploadDataListener;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.FundHoldingsTrackingMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.model.dto.StockRealTimePrice;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.ForeignFundHoldingsTracking;
import com.mistra.plank.model.entity.Stock;
import com.mistra.plank.model.param.FundHoldingsParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
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

    private final FundHoldingsTrackingMapper fundHoldingsTrackingMapper;
    private final DailyRecordProcessor dailyRecordProcessor;

    public StockProcessor(StockMapper stockMapper, DailyRecordMapper dailyRecordMapper, PlankConfig plankConfig,
                          FundHoldingsTrackingMapper fundHoldingsTrackingMapper, DailyRecordProcessor dailyRecordProcessor) {
        this.stockMapper = stockMapper;
        this.dailyRecordMapper = dailyRecordMapper;
        this.plankConfig = plankConfig;
        this.fundHoldingsTrackingMapper = fundHoldingsTrackingMapper;
        this.dailyRecordProcessor = dailyRecordProcessor;
    }

    public void run() {
        try {
            String body =
                    HttpUtil.getHttpGetResponseString(plankConfig.getXueQiuAllStockUrl(), plankConfig.getXueQiuCookie());
            JSONObject data = JSON.parseObject(body).getJSONObject("data");
            JSONArray list = data.getJSONArray("list");
            Date today = new Date();
            BigDecimal zero = new BigDecimal(0);
            for (Object o : list) {
                data = (JSONObject) o;
                // volume 值不准确忽略
                BigDecimal current = data.getBigDecimal("current");
                BigDecimal volume = data.getBigDecimal("volume");
                if (Objects.nonNull(current) && Objects.nonNull(volume)) {
                    Stock exist = stockMapper.selectById(data.getString("symbol"));
                    if (Objects.nonNull(exist)) {
                        List<DailyRecord> dailyRecords = dailyRecordMapper.selectPage(new Page<>(1, 20),
                                new LambdaQueryWrapper<DailyRecord>().eq(DailyRecord::getCode, data.getString("symbol"))
                                        .ge(DailyRecord::getDate, DateUtils.addDays(new Date(), -40))
                                        .orderByDesc(DailyRecord::getDate)).getRecords();
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
                                .ma5(zero).ma10(zero).ma20(zero)
                                .transactionAmount(current.multiply(volume)).modifyTime(today).track(false)
                                .shareholding(false).focus(false).classification("").build());
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
     * @return StockRealTimePrice
     */
    public StockRealTimePrice getStockRealTimePriceByCode(String code) {
        String url = plankConfig.getXueQiuStockLimitUpPriceUrl().replace("{code}", code);
        String body = HttpUtil.getHttpGetResponseString(url, plankConfig.getXueQiuCookie());
        JSONObject quote = JSON.parseObject(body).getJSONObject("data").getJSONObject("quote");
        return StockRealTimePrice.builder().currentPrice(quote.getDouble("current"))
                .highestPrice(quote.getDouble("high")).lowestPrice(quote.getDouble("low"))
                .isPlank(quote.getDouble("current").equals(quote.getDouble("limit_up")))
                .increaseRate(quote.getDouble("chg")).limitDown(quote.getDouble("limit_down"))
                .limitUp(quote.getDouble("limit_up")).build();
    }

    /**
     * 更新 外资+基金 持仓
     * 基金的实时持仓市值是根据该季度(quarter)季报公布的持仓股数*当日收盘价 计算的。所以跟实际情况肯定存在差距的，仅作为参考
     * 外资持仓市值是前一个交易日最新的数据，是实时的
     */
    public void updateForeignFundShareholding() {
        HashMap<String, JSONObject> foreignShareholding = getForeignShareholding();
        List<ForeignFundHoldingsTracking> fundHoldings = fundHoldingsTrackingMapper
                .selectList(new LambdaQueryWrapper<ForeignFundHoldingsTracking>().eq(ForeignFundHoldingsTracking::getQuarter, getQuarter()));
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .in(Stock::getName, fundHoldings.stream().map(ForeignFundHoldingsTracking::getName).collect(Collectors.toList())));
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(foreignShareholding.values()) || org.apache.commons.collections4.CollectionUtils.isEmpty(fundHoldings)
                || org.apache.commons.collections4.CollectionUtils.isEmpty(stocks)) {
            return;
        }
        Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getName, e -> e));
        for (ForeignFundHoldingsTracking tracking : fundHoldings) {
            JSONObject jsonObject = foreignShareholding.get(tracking.getName());
            try {
                if (Objects.nonNull(jsonObject)) {
                    long foreignTotalMarket = jsonObject.getLong("HOLD_MARKET_CAP");
                    tracking.setForeignTotalMarketDynamic(foreignTotalMarket);
                }
                tracking.setFundTotalMarketDynamic(stockMap.get(tracking.getName()).getCurrentPrice()
                        .multiply(new BigDecimal(tracking.getShareholdingCount())).longValue());
                tracking.setForeignFundTotalMarketDynamic(
                        tracking.getFundTotalMarketDynamic() + tracking.getForeignTotalMarketDynamic());
                tracking.setModifyTime(new Date());
                fundHoldingsTrackingMapper.updateById(tracking);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.warn("股票最新外资持仓市值更新完成！");
    }

    /**
     * 获取外资持股明细 截止昨日的
     *
     * @return HashMap<String, JSONObject>
     */
    private HashMap<String, JSONObject> getForeignShareholding() {
        HashMap<String, JSONObject> result = new HashMap<>();
        try {
            int pageNumber = 1;
            while (pageNumber <= 30) {
                String body = HttpUtil.getHttpGetResponseString(
                        plankConfig.getForeignShareholdingUrl().replace("{pageNumber}", pageNumber + ""), null);
                body = body.substring(body.indexOf("(") + 1, body.indexOf(")"));
                JSONObject parseObject = JSON.parseObject(body);
                if (parseObject.getJSONObject("result") != null) {
                    JSONArray array = parseObject.getJSONObject("result").getJSONArray("data");
                    for (Object o : array) {
                        JSONObject jsonObject = (JSONObject) o;
                        result.put(jsonObject.getString("SECURITY_NAME"), jsonObject);
                    }
                }
                pageNumber++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void fundHoldingsImport(FundHoldingsParam fundHoldingsParam, Date beginTime, Date endTime) {
        UploadDataListener<ForeignFundHoldingsTracking> uploadDataListener = new UploadDataListener<>(500);
        try {
            EasyExcel.read(fundHoldingsParam.getFile().getInputStream(), ForeignFundHoldingsTracking.class,
                    uploadDataListener).sheet().headRowNumber(2).doRead();
        } catch (IOException e) {
            log.error("read excel file error,file name:{}", fundHoldingsParam.getFile().getName());
        }
        for (Map.Entry<Integer, ForeignFundHoldingsTracking> entry : uploadDataListener.getMap().entrySet()) {
            Barbarossa.executorService.submit(() -> {
                ForeignFundHoldingsTracking fundHoldingsTracking = entry.getValue();
                try {
                    Stock stock =
                            stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getName, fundHoldingsTracking.getName()));
                    fundHoldingsTracking.setCode(stock.getCode());
                    fundHoldingsTracking.setQuarter(fundHoldingsParam.getQuarter());
                    List<DailyRecord> dailyRecordList = dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>()
                            .eq(DailyRecord::getName, fundHoldingsTracking.getName()).ge(DailyRecord::getDate, beginTime).le(DailyRecord::getDate, endTime));
                    if (org.apache.commons.collections4.CollectionUtils.isEmpty(dailyRecordList)) {
                        HashMap<String, String> stockMap = new HashMap<>();
                        stockMap.put(stock.getCode(), stock.getName());
                        dailyRecordProcessor.run(stockMap);
                        Thread.sleep(60 * 1000);
                        dailyRecordList = dailyRecordMapper.selectList(new LambdaQueryWrapper<DailyRecord>()
                                .eq(DailyRecord::getName, fundHoldingsTracking.getName()).ge(DailyRecord::getDate, beginTime).le(DailyRecord::getDate, endTime));
                    }
                    double average = dailyRecordList.stream().map(DailyRecord::getClosePrice)
                            .mapToInt(BigDecimal::intValue).average().orElse(0D);
                    fundHoldingsTracking.setAveragePrice(new BigDecimal(average));
                    fundHoldingsTracking
                            .setShareholdingChangeAmount(average * fundHoldingsTracking.getShareholdingChangeCount());
                    fundHoldingsTracking.setModifyTime(new Date());
                    fundHoldingsTracking.setForeignTotalMarketDynamic(0L);
                    fundHoldingsTracking.setForeignFundTotalMarketDynamic(0L);
                    fundHoldingsTrackingMapper.insert(fundHoldingsTracking);
                    log.warn("更新 [{}] {}季报基金持仓数据完成！", stock.getName(), fundHoldingsParam.getQuarter());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 获取上一个季度字符串
     *
     * @return 202204
     */
    private String getQuarter() {
        Date date = DateUtils.addDays(new Date(), -90);
        int n = DateUtil.month(date);
        if (n <= 3) {
            return DateUtil.year(date) + "01";
        } else if (n <= 6) {
            return DateUtil.year(date) + "02";
        } else if (n <= 9) {
            return DateUtil.year(date) + "03";
        } else {
            return DateUtil.year(date) + "04";
        }
    }
}
