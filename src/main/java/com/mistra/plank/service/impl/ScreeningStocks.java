package com.mistra.plank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.StringUtil;
import com.mistra.plank.config.SystemConstant;
import com.mistra.plank.dao.DailyRecordMapper;
import com.mistra.plank.dao.DragonListMapper;
import com.mistra.plank.dao.StockMapper;
import com.mistra.plank.job.Barbarossa;
import com.mistra.plank.model.dto.UpwardTrendSample;
import com.mistra.plank.model.entity.DailyRecord;
import com.mistra.plank.model.entity.DragonList;
import com.mistra.plank.model.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.mistra.plank.common.util.StringUtil.collectionToString;

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
    private final PlankConfig plankConfig;

    public ScreeningStocks(DailyRecordMapper dailyRecordMapper, StockMapper stockMapper,
                           DragonListMapper dragonListMapper, PlankConfig plankConfig) {
        this.dailyRecordMapper = dailyRecordMapper;
        this.stockMapper = stockMapper;
        this.dragonListMapper = dragonListMapper;
        this.plankConfig = plankConfig;
    }

    /**
     * 找出日k均线多头排列的股票
     */
    public void movingAverageRise() {
        List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .ge(Stock::getMa5, 0).ge(Stock::getTransactionAmount, SystemConstant.TRANSACTION_AMOUNT_FILTER));
        List<String> list = stocks.stream().filter(stock -> stock.getMa5().compareTo(stock.getMa10()) > 0
                        && stock.getMa10().compareTo(stock.getMa20()) > 0).map(stock -> StringUtils.substring(stock.getCode(), 2, 8))
                .collect(Collectors.toList());
        log.warn("日k均线多头排列:{}", collectionToString(list));
    }

    /**
     * 找出最近5天或10天 爆量回踩的票
     * <p>
     * 最高成交量>MA10*1.5 最低成交量<MA10*0.7 最低成交量出现在最高成交量之后 最低成交量那天的收盘价>最高成交量那天(收盘价+开盘价)/2
     * <p>
     * 最高成交额>10亿 最低成交额>5亿
     */
    public List<Stock> explosiveVolumeBack(Date date) {
        List<Stock> result = new ArrayList<>();
        List<DailyRecord> dailyRecords = dailyRecordMapper
                .selectList(new LambdaQueryWrapper<DailyRecord>().ge(DailyRecord::getDate, DateUtils.addDays(date, -30))
                        .le(DailyRecord::getDate, date).orderByDesc(DailyRecord::getDate));
        Map<String, List<DailyRecord>> dailyRecordMap =
                dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        for (Map.Entry<String, List<DailyRecord>> entry : dailyRecordMap.entrySet()) {
            List<DailyRecord> recordList = entry.getValue();
            if (recordList.size() >= 10 && recordList.get(0).getAmount() > 30000) {
                Stock stock = explosiveVolumeBack(recordList.subList(0, 5), entry.getKey());
                if (Objects.nonNull(stock)) {
                    result.add(stock);
                } else {
                    stock = explosiveVolumeBack(recordList.subList(0, 10), entry.getKey());
                    if (Objects.nonNull(stock)) {
                        result.add(stock);
                    }
                }
            }
        }
        Collections.sort(result);
        log.warn("{}爆量回踩股票[{}]支:{}", sdf.format(date), result.size(), StringUtil.collectionToString(result.stream()
                .map(plankConfig.getPrintName() ? Stock::getName : Stock::getCode).collect(Collectors.toList())));
        return result;
    }

    public void explosiveVolumeBack() {
        explosiveVolumeBack(new Date());
    }

    private Stock explosiveVolumeBack(List<DailyRecord> recordList, String code) {
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
        if (high.getClosePrice().compareTo(high.getOpenPrice()) > 0 && high.getDate().before(low.getDate())
                && high.getAmount() > Math.max(ma10 * 1.5, 100000) && low.getAmount() < ma10 * 0.7
                && low.getAmount() > 50000 && low.getClosePrice()
                .doubleValue() > ((high.getClosePrice().doubleValue() + high.getOpenPrice().doubleValue()) * 0.5)) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, code));
            if (Objects.nonNull(stock.getMa20()) && recordList.get(0).getClosePrice().compareTo(stock.getMa20()) > 0) {
                stock.setCode(StringUtils.substring(stock.getCode(), 2, 8));
                return stock;
            }
        }
        return null;
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
        return (double) Math.round(((x.doubleValue() - y.doubleValue()) / y.doubleValue()) * 100) / 100;
    }

    /**
     * 找出周均线向上发散，上升趋势的股票
     * 周均线MA3>MA5>MA10>MA20
     * 上市不足20个交易日的次新股就不计算了
     * 趋势股选出来之后我一般会直接用.txt文档导入到东方财富windows版客户端，再来人为筛选一遍k线好看的票
     */
    public void upwardTrend() {
        List<UpwardTrendSample> samples = new ArrayList<>(Barbarossa.STOCK_MAP_ALL.size());
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, String> entry : Barbarossa.STOCK_MAP_ALL.entrySet()) {
            Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getCode, entry.getKey()));
            if (stock.getTrack() || stock.getTransactionAmount().doubleValue() < SystemConstant.TRANSACTION_AMOUNT_FILTER) {
                continue;
            }
            List<DailyRecord> dailyRecords = dailyRecordMapper
                    .selectList(new LambdaQueryWrapper<DailyRecord>().eq(DailyRecord::getCode, entry.getKey())
                            .ge(DailyRecord::getDate, DateUtils.addDays(new Date(), -200)).orderByDesc(DailyRecord::getDate));
            if (dailyRecords.size() < 100) {
                failed.add(entry.getKey());
                continue;
            }
            dailyRecords = dailyRecords.subList(0, 100);
            // 计算周k线，直接取5天为默认一周，不按自然周计算。先把100条日交易记录转换为20周k线。周五收盘价即为当前周收盘价。
            List<BigDecimal> week = new ArrayList<>();
            for (int i = 0; i < dailyRecords.size(); i += 5) {
                week.add(dailyRecords.get(i).getClosePrice());
            }
            // 计算MA3
            double ma3 = week.subList(0, 3).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // 计算MA5
            double ma5 = week.subList(0, 5).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // 计算MA10
            double ma10 = week.subList(0, 10).stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            // 计算MA20
            double ma20 = week.stream().collect(Collectors.averagingDouble(BigDecimal::doubleValue));
            if (ma3 > ma5 && ma5 > ma10 && ma10 > ma20) {
                // 计算方差
                double v = variance(new double[]{ma3, ma5, ma10, ma20});
                samples.add(UpwardTrendSample.builder().ma3(new BigDecimal(ma3).setScale(2, RoundingMode.HALF_UP))
                        .ma5(new BigDecimal(ma5).setScale(2, RoundingMode.HALF_UP))
                        .ma10(new BigDecimal(ma10).setScale(2, RoundingMode.HALF_UP))
                        .ma20(new BigDecimal(ma20).setScale(2, RoundingMode.HALF_UP)).name(entry.getValue())
                        .code(StringUtils.substring(entry.getKey(), 2, 8)).variance(v).build());
            }
        }
        if (CollectionUtils.isNotEmpty(failed)) {
//            log.error("{}的交易数据不完整(可能是次新股，上市不足100个交易日)", collectionToString(failed));
        }
        Collections.sort(samples);
        log.warn("新发现的上升趋势的股票一共[{}]支:{}", samples.size(),
                collectionToString(samples.stream()
                        .map(plankConfig.getPrintName() ? UpwardTrendSample::getName : UpwardTrendSample::getCode)
                        .collect(Collectors.toSet())));
        if (CollectionUtils.isNotEmpty(samples)) {
            // 找出来之后直接更新这些股票为监控股票
            List<Stock> stocks = stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getCode,
                    samples.stream().map(UpwardTrendSample::getCode).collect(Collectors.toSet())));
            for (Stock stock : stocks) {
                stock.setTrack(true);
                // stockMapper.updateById(stock);
            }
        }
    }

    /**
     * 求方差
     *
     * @param x 数组
     * @return 方差
     */
    public static double variance(double[] x) {
        int m = x.length;
        double sum = 0;
        for (double v : x) {
            sum += v;
        }
        double dAve = sum / m;
        double dVar = 0;
        for (double v : x) {
            dVar += (v - dAve) * (v - dAve);
        }
        return dVar / m;
    }
}
