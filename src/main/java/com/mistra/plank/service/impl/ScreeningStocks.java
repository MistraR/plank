package com.mistra.plank.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.pojo.entity.DailyRecord;
import com.mistra.plank.pojo.entity.DragonList;
import com.mistra.plank.pojo.entity.Stock;
import com.mistra.plank.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2022/6/15
 */
@Slf4j
@Component
public class ScreeningStocks {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final DailyRecordMapper dailyRecordMapper;
    private final StockMapper stockMapper;
    private final DragonListMapper dragonListMapper;

    public ScreeningStocks(DailyRecordMapper dailyRecordMapper, StockMapper stockMapper,
        DragonListMapper dragonListMapper) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.stockMapper = stockMapper;
        this.dragonListMapper = dragonListMapper;
    }

    /**
     * 找出最近10天 爆量回踩的票
     *
     * 最高成交量>MA10*1.5 最低成交量<MA10*0.7 最低成交量出现在最高成交量之后 最低成交量那天的收盘价>最高成交量那天(收盘价+开盘价)/2
     * 
     * 最高成交额>10亿 最低成交额>5亿
     */
    public void explosiveVolumeBack(Date date) {
        List<Stock> result = new ArrayList<>();
        List<DailyRecord> dailyRecords = dailyRecordMapper
            .selectList(new LambdaQueryWrapper<DailyRecord>().ge(DailyRecord::getDate, DateUtils.addDays(date, -30))
                .le(DailyRecord::getDate, date).orderByDesc(DailyRecord::getDate));
        Map<String, List<DailyRecord>> dailyRecordMap =
            dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        for (Map.Entry<String, List<DailyRecord>> entry : dailyRecordMap.entrySet()) {
            List<DailyRecord> recordList = entry.getValue();
            if (recordList.size() >= 10 && recordList.get(0).getAmount() > 50000) {
                recordList = recordList.subList(0, 10);
                // 计算MA10
                double ma10 = recordList.stream().collect(Collectors.averagingLong(DailyRecord::getAmount));
                DailyRecord high = recordList.get(0), low = recordList.get(0);
                for (DailyRecord record : recordList) {
                    if (high.getAmount() < record.getAmount()) {
                        high = record;
                    }
                    if (low.getAmount() > record.getAmount()) {
                        low = record;
                    }
                }
                if (high.getAmount() > Math.max(ma10 * 1.5, 100000) && low.getAmount() < ma10 * 0.7
                    && low.getAmount() > 50000
                    && low.getClosePrice()
                        .doubleValue() < ((high.getClosePrice().doubleValue() + high.getOpenPrice().doubleValue())
                            / 2)) {
                    Stock stock =
                        stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, entry.getKey()));
                    if (Objects.nonNull(stock.getMa20())
                        && recordList.get(0).getClosePrice().compareTo(stock.getMa20()) > 0) {
                        result.add(stock);
                    }
                }
            }
        }
        Collections.sort(result);
        log.warn("{}爆量回踩股票[{}]支:{}", sdf.format(date), result.size(),
            StringUtil.collectionToString(result.stream().map(Stock::getName).collect(Collectors.toList())));
    }

    /**
     * 红三兵选股
     *
     * @param date 分析日期-截止这天的收盘数据
     * @return List<Stock>
     */
    public List<Stock> checkRedThreeSoldiersStock(Date date) {
        List<Stock> result = new ArrayList<>();
        List<DailyRecord> dailyRecords = dailyRecordMapper
            .selectList(new LambdaQueryWrapper<DailyRecord>().ge(DailyRecord::getDate, DateUtils.addDays(date, -20))
                .le(DailyRecord::getDate, date).orderByDesc(DailyRecord::getDate));
        Map<String, List<DailyRecord>> dailyRecordMap =
            dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        for (Map.Entry<String, List<DailyRecord>> entry : dailyRecordMap.entrySet()) {
            List<DailyRecord> recordList = entry.getValue();
            if (recordList.size() >= 3 && recordList.get(0).getAmount() > 80000) {
                DailyRecord one = recordList.get(2);
                DailyRecord two = recordList.get(1);
                DailyRecord three = recordList.get(0);
                double threeDayIncreaseRate = differencePercentage(three.getClosePrice(), one.getOpenPrice());
                if (one.getClosePrice().compareTo(one.getOpenPrice()) > 0
                    && (two.getClosePrice().compareTo(one.getClosePrice()) > 0
                        && two.getClosePrice().compareTo(two.getOpenPrice()) > 0)
                    && (three.getClosePrice().compareTo(two.getClosePrice()) > 0
                        && three.getClosePrice().compareTo(two.getOpenPrice()) > 0)
                    && (threeDayIncreaseRate <= 0.15 && threeDayIncreaseRate > 0.05)) {
                    Stock stock =
                        stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, entry.getKey()));
                    if (three.getClosePrice().compareTo(stock.getMa10()) > 0
                        && three.getClosePrice().compareTo(stock.getMa5()) > 0
                        && differencePercentage(three.getClosePrice(), stock.getMa5()) < 0.15
                        && differencePercentage(three.getClosePrice(), stock.getMa10()) < 0.20) {
                        result.add(stock);
                    }
                }
            }
        }
        Collections.sort(result);
        log.warn("{}日红三兵股票[{}]支:{}", sdf.format(date), result.size(),
            StringUtil.collectionToString(result.stream().map(Stock::getName).collect(Collectors.toList())));
        return result;
    }

    /**
     * 根据龙虎榜检查可以买的票 首板或者2板 10日涨幅介于10-22% 计算前8天的振幅在15%以内
     *
     * @param date 开盘日期
     * @return List<Stock>
     */
    public List<Stock> checkDragonListStock(Date date) {
        List<DragonList> dragonLists = dragonListMapper.selectList(new QueryWrapper<DragonList>().ge("date", date)
            .lt("date", DateUtils.addDays(date, 1)).ge("price", 5).le("price", 100).notLike("name", "%ST%")
            .notLike("name", "%st%").notLike("name", "%A%").notLike("name", "%C%").notLike("name", "%N%")
            .notLike("name", "%U%").notLike("name", "%W%").notLike("code", "%BJ%").notLike("code", "%688%"));
        if (CollectionUtils.isEmpty(dragonLists)) {
            return null;
        }
        List<DailyRecord> dailyRecords = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            Page<DailyRecord> page = dailyRecordMapper.selectPage(new Page<>(1, 30),
                new QueryWrapper<DailyRecord>().eq("code", dragonList.getCode()).le("date", date)
                    .ge("date", DateUtils.addDays(date, -30)).orderByDesc("date"));
            if (page.getRecords().size() > 10) {
                dailyRecords.addAll(page.getRecords().subList(0, 10));
            }
        }
        Map<String, List<DailyRecord>> map = dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        List<String> stockCode = new ArrayList<>();
        for (Map.Entry<String, List<DailyRecord>> entry : map.entrySet()) {
            // 近8日涨幅
            BigDecimal eightRatio = entry.getValue().get(0).getClosePrice()
                .divide(entry.getValue().get(8).getClosePrice(), 2, RoundingMode.HALF_UP);
            // 近3日涨幅
            BigDecimal threeRatio = entry.getValue().get(0).getClosePrice()
                .divide(entry.getValue().get(3).getClosePrice(), 2, RoundingMode.HALF_UP);
            // 前3个交易日大跌的也排除
            if (eightRatio.doubleValue() <= 1.22 && eightRatio.doubleValue() >= 1.1 && threeRatio.doubleValue() < 1.22
                && entry.getValue().get(0).getIncreaseRate().doubleValue() > 0.04
                && entry.getValue().get(1).getIncreaseRate().doubleValue() > -0.04
                && entry.getValue().get(2).getIncreaseRate().doubleValue() > -0.04) {
                stockCode.add(entry.getKey());
            }
        }
        dragonLists = dragonLists.stream().filter(dragonList -> stockCode.contains(dragonList.getCode()))
            .collect(Collectors.toList());
        dragonLists =
            dragonLists.stream().sorted((a, b) -> b.getNetBuy().compareTo(a.getNetBuy())).collect(Collectors.toList());
        List<Stock> result = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            result.add(stockMapper.selectOne(new QueryWrapper<Stock>().eq("code", dragonList.getCode())));
        }
        return result;
    }

    private double differencePercentage(BigDecimal x, BigDecimal y) {
        return (double)Math.round(((x.doubleValue() - y.doubleValue()) / y.doubleValue()) * 100) / 100;
    }
}
